package tr.ailetv.player

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import tr.ailetv.data.Channel
import tr.ailetv.data.ChannelSource
import tr.ailetv.data.HealthStore
import tr.ailetv.data.Policy
import tr.ailetv.net.UserAgents
import tr.ailetv.resolve.Resolved
import tr.ailetv.resolve.SourceResolver

sealed interface PlaybackState {
    val channel: Channel?

    data object Idle : PlaybackState {
        override val channel: Channel? = null
    }

    /** Trying source [sourceIndex] of [sourceCount]. UI shows "Yayın açılıyor…" (or the external transition). */
    data class Opening(
        override val channel: Channel,
        val source: ChannelSource,
        val sourceIndex: Int,
        val sourceCount: Int,
    ) : PlaybackState

    /** ExoPlayer is rendering video. */
    data class PlayingHls(override val channel: Channel, val source: ChannelSource) : PlaybackState

    /**
     * The UI must host the embedded YouTube player and report back via [PlaybackEngine.onExternalReady]/
     * [PlaybackEngine.onExternalError] (and, once ready, [PlaybackEngine.onExternalBuffering]/[PlaybackEngine.onExternalResumed]).
     */
    data class YouTube(
        override val channel: Channel,
        val source: ChannelSource,
        val videoId: String?,
        val youtubeChannelId: String?,
        val ready: Boolean,
        val session: Int,
    ) : PlaybackState

    /** The UI must host a WebView with the broadcaster's official live page. */
    data class Web(
        override val channel: Channel,
        val source: ChannelSource,
        val url: String,
        val ready: Boolean,
        val session: Int,
    ) : PlaybackState

    data class Failed(override val channel: Channel, val reason: FailReason) : PlaybackState
}

enum class FailReason { NoSource, AllSourcesFailed }

/**
 * Plays a channel by walking its source chain until one renders a frame, and keeps it playing:
 * behind-live-window re-sync, token refresh on 401/403, retry with backoff, stall watchdog,
 * then fallback to the next source. The UI only ever sees "opening", "playing" or "failed".
 */
@OptIn(UnstableApi::class)
class PlaybackEngine(
    context: Context,
    private val http: OkHttpClient,
    private val resolver: SourceResolver,
    private val health: HealthStore,
    private val policy: () -> Policy,
    /** Failures while the box is offline say nothing about a source, so they are not counted. */
    private val online: () -> Boolean,
    /**
     * The current list's copy of a channel (null: no longer listed). Retry and recovery use it, because a
     * list fetched since the chain started may have fixed, removed or disabled the channel's sources.
     */
    private val listed: (String) -> Channel?,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val trackSelector = DefaultTrackSelector(context).apply {
        setParameters(
            buildUponParameters()
                // TRT offers 1440p/11 Mbps; 1080p is the box's UI resolution and much kinder to Wi-Fi.
                .setMaxVideoSize(1920, 1080)
                .setPreferredAudioLanguage("tr")
                .setExceedRendererCapabilitiesIfNecessary(false)
                .build(),
        )
    }

    val player: ExoPlayer = ExoPlayer.Builder(context, DefaultRenderersFactory(context).setEnableDecoderFallback(true))
        .setTrackSelector(trackSelector)
        // Start near HD instead of ramping up from the lowest rung on every zap.
        .setBandwidthMeter(DefaultBandwidthMeter.Builder(context).setInitialBitrateEstimate(6_000_000L).build())
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(15_000, 30_000, 2_000, 4_000)
                .setTargetBufferBytes(48 * 1024 * 1024)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build(),
        )
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
            true,
        )
        // Off: the output is the TV, not headphones. On an HDMI audio drop (TV off without CEC, input switch,
        // re-handshake) Media3 would pause, and a paused live stream stays frozen: there is no play key.
        .setHandleAudioBecomingNoisy(false)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build()

    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var chainJob: Job? = null
    /** Pending [recover]; tracked so stop() and a new chain cancel it instead of it restarting playback later. */
    private var recoveryJob: Job? = null
    /** Stall watchdog for what is on screen: ExoPlayer buffering, or a ready embed buffering. */
    private var watchdog: Job? = null
    private var firstFrame: CompletableDeferred<Boolean>? = null
    private var externalOutcome: CompletableDeferred<Boolean>? = null
    private var session = 0
    /**
     * The embed on screen reported an error or its end. Its view reports nothing after that (one-shot latch),
     * so the recovery this started must replace it: a later "playing again" does not cancel it.
     */
    private var embedFailed = false
    /** Pending [resumeLive] after Media3 paused the stream (audio focus taken by another app). */
    private var resumeJob: Job? = null
    private var lastResumeAt = -RESUME_GAP_MS

    private var chainSources: List<ChannelSource> = emptyList()
    private var chainIndex = 0
    private val recoveryTimes = ArrayDeque<Long>()

    /**
     * False while the activity is stopped (Google TV home, screen saver). Nothing starts playing then:
     * a late zap, retry or autostart is ignored, and the activity resumes the channel when it starts again.
     */
    var foreground = false

    init {
        player.addListener(object : Player.Listener {
            override fun onRenderedFirstFrame() {
                firstFrame?.complete(true)
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.w(TAG, "player error ${error.errorCodeName}: ${error.cause?.message}")
                val pending = firstFrame
                if (pending != null && pending.isActive) {
                    if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                        player.seekToDefaultPosition()
                        player.prepare()
                    } else {
                        pending.complete(false)
                    }
                    return
                }
                val playing = _state.value as? PlaybackState.PlayingHls ?: return
                launchRecover(playing, error)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val playing = _state.value as? PlaybackState.PlayingHls ?: return
                when (playbackState) {
                    Player.STATE_BUFFERING -> armWatchdog(playing)
                    Player.STATE_READY -> {
                        watchdog?.cancel()
                        // The stall cleared by itself while its recovery was still waiting: keep playing.
                        recoveryJob?.cancel()
                    }
                    // A live playlist should never end; treat it like a dropped stream.
                    Player.STATE_ENDED -> {
                        watchdog?.cancel()
                        launchRecover(playing, null)
                    }
                    // IDLE comes with an error (onPlayerError recovers) or a stop(). The watchdog stays armed,
                    // so a behind-live-window re-sync that never gets going still ends in a recovery.
                    else -> Unit
                }
            }

            // Only Media3 itself ever pauses (we never do): a permanent audio-focus loss, or becoming noisy
            // should that be switched on again. A paused live channel stays frozen with nothing to resume it
            // (the remote has no play key, the watchdog only runs while buffering), so take it back to live.
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (playWhenReady || reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) return
                if (resumeJob?.isActive == true) return
                // At once the first time; then at most every RESUME_GAP_MS, so an app that grabs the audio
                // back each time cannot ping-pong with us.
                val wait = (lastResumeAt + RESUME_GAP_MS - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                resumeJob = scope.launch {
                    delay(wait)
                    resumeLive()
                }
            }
        })
    }

    private fun resumeLive() {
        val s = _state.value
        if (!foreground || player.playWhenReady) return
        if (s !is PlaybackState.PlayingHls && s !is PlaybackState.Opening) return
        lastResumeAt = SystemClock.elapsedRealtime()
        player.seekToDefaultPosition() // the live edge, not the frame it froze on
        player.playWhenReady = true
    }

    fun play(channel: Channel) {
        recoveryTimes.clear()
        val sources = health.ordered(channel, policy())
        startChain(channel, sources, fromIndex = 0, forceFirst = false)
    }

    /** "Tekrar dene": the whole chain again, with fresh tokens, as the current list has the channel. */
    fun retry() {
        val shown = _state.value.channel ?: return
        val channel = listed(shown.id) ?: shown
        recoveryTimes.clear()
        startChain(channel, health.ordered(channel, policy()), fromIndex = 0, forceFirst = true)
    }

    fun stop() {
        chainJob?.cancel()
        recoveryJob?.cancel()
        watchdog?.cancel()
        firstFrame?.cancel()
        externalOutcome?.cancel()
        player.stop()
        player.clearMediaItems()
        _state.value = PlaybackState.Idle
    }

    fun release() {
        stop()
        scope.cancel()
        player.release()
    }

    /** Embedded YouTube/web player loaded and is playing. */
    fun onExternalReady(session: Int) {
        if (session != this.session) return
        externalOutcome?.complete(true)
        when (val s = _state.value) {
            is PlaybackState.YouTube -> if (!s.ready) _state.value = s.copy(ready = true)
            is PlaybackState.Web -> if (!s.ready) _state.value = s.copy(ready = true)
            else -> Unit
        }
    }

    /**
     * Embedded player failed (stream offline, embedding disabled) or ended. Before it was ready, this
     * attempt failed. Once ready, it is recovered like a dropped HLS stream: the same source re-resolved
     * (a restarted 24/7 broadcast has a new videoId), a few times per window, then the next source.
     */
    fun onExternalError(session: Int) {
        if (session != this.session) return
        val pending = externalOutcome
        if (pending != null && pending.isActive) {
            pending.complete(false)
            return
        }
        val s = _state.value
        if (s.isReadyEmbed(session)) {
            // Also when a stall recovery is already pending (launchRecover then keeps that one): the flag
            // is what stops a later "playing again" from cancelling it.
            embedFailed = true
            launchRecover(s, null)
        }
    }

    /** A ready embed is buffering: recover it unless it plays again within [STALL_TIMEOUT_MS]. */
    fun onExternalBuffering(session: Int) {
        val s = _state.value
        if (session == this.session && s.isReadyEmbed(session)) armWatchdog(s)
    }

    /**
     * A ready embed is playing again after buffering, or the UI paused it on purpose (hidden behind an
     * overlay): the stall clock stops. A recovery the stall already started is dropped too, but never one
     * that an error or end started: that embed's view has reported for the last time and must be replaced.
     */
    fun onExternalResumed(session: Int) {
        if (session != this.session || !_state.value.isReadyEmbed(session)) return
        watchdog?.cancel()
        if (!embedFailed) recoveryJob?.cancel()
    }

    /** Short description of what is on screen, for the diagnostics screen. */
    fun videoInfo(): String? {
        val f = player.videoFormat ?: return null
        val fps = if (f.frameRate > 0) " · ${"%.0f".format(f.frameRate)} fps" else ""
        val kbps = if (f.bitrate > 0) " · ${f.bitrate / 1000} kb/s" else ""
        return "${f.width}×${f.height}$fps$kbps"
    }

    private fun startChain(channel: Channel, sources: List<ChannelSource>, fromIndex: Int, forceFirst: Boolean) {
        if (!foreground) return
        chainJob?.cancel()
        recoveryJob?.cancel()
        watchdog?.cancel()
        firstFrame = null       // a cancelled openHls never clears it
        externalOutcome = null  // same for a cancelled openExternal
        embedFailed = false     // the next embed is a new view
        // Silence the previous channel now, not once the next source has resolved (1–3 s for a YouTube page).
        player.stop()
        chainJob = scope.launch { runChain(channel, sources, fromIndex, forceFirst) }
    }

    private suspend fun runChain(channel: Channel, sources: List<ChannelSource>, fromIndex: Int, forceFirst: Boolean) {
        chainSources = sources
        if (sources.isEmpty()) {
            player.stop()
            _state.value = PlaybackState.Failed(channel, FailReason.NoSource)
            return
        }
        for (index in fromIndex until sources.size) {
            val source = sources[index]
            val attempts = if (source.type in RETRYABLE_TYPES) 2 else 1
            for (attempt in 1..attempts) {
                chainIndex = index
                _state.value = PlaybackState.Opening(channel, source, index, sources.size)
                val ok = try {
                    open(channel, source, forceRefresh = attempt > 1 || (forceFirst && index == fromIndex))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "open failed for ${channel.id} via ${source.type}: ${e.message}")
                    false
                }
                if (ok) {
                    health.markOk(channel, source)
                    return
                }
                if (attempt < attempts) delay(RETRY_DELAY_MS * attempt)
            }
            markFail(channel, source)
        }
        player.stop()
        _state.value = PlaybackState.Failed(channel, FailReason.AllSourcesFailed)
    }

    private suspend fun open(channel: Channel, source: ChannelSource, forceRefresh: Boolean): Boolean {
        val resolved = withTimeoutOrNull(RESOLVE_TIMEOUT_MS) { resolver.resolve(source, forceRefresh) } ?: return false
        return when (resolved) {
            is Resolved.Hls -> openHls(channel, source, resolved)
            is Resolved.YouTube -> openExternal { s ->
                PlaybackState.YouTube(channel, source, resolved.videoId, resolved.channelId, ready = false, session = s)
            }
            is Resolved.Web -> openExternal { s ->
                PlaybackState.Web(channel, source, resolved.url, ready = false, session = s)
            }
        }
    }

    private suspend fun openHls(channel: Channel, source: ChannelSource, r: Resolved.Hls): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        firstFrame = deferred
        prepareHls(r)
        val ok = withTimeoutOrNull(FIRST_FRAME_TIMEOUT_MS) { deferred.await() } ?: false
        firstFrame = null
        if (!ok) {
            player.stop()
            return false
        }
        _state.value = PlaybackState.PlayingHls(channel, source)
        // The listener only acts on PlayingHls, so catch up on what happened since the first frame.
        val playing = _state.value
        val error = player.playerError
        when {
            error != null -> launchRecover(playing, error)
            player.playbackState == Player.STATE_BUFFERING -> armWatchdog(playing)
        }
        return true
    }

    private suspend fun openExternal(build: (Int) -> PlaybackState): Boolean {
        player.stop()
        player.clearMediaItems()
        session += 1
        val deferred = CompletableDeferred<Boolean>()
        externalOutcome = deferred
        _state.value = build(session)
        val ok = withTimeoutOrNull(EXTERNAL_TIMEOUT_MS) { deferred.await() } ?: false
        externalOutcome = null
        return ok
    }

    private fun prepareHls(r: Resolved.Hls) {
        val dataSource = OkHttpDataSource.Factory(http)
            .setUserAgent(UserAgents.TV)
            .setDefaultRequestProperties(r.headers)
        val live = MediaItem.LiveConfiguration.Builder().apply {
            r.liveOffsetMs?.let { setTargetOffsetMs(it) }
        }.build()
        val item = MediaItem.Builder()
            .setUri(r.url)
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .setLiveConfiguration(live)
            .build()
        val mediaSource = HlsMediaSource.Factory(dataSource)
            .setAllowChunklessPreparation(true)
            .createMediaSource(item)
        player.setMediaSource(mediaSource)
        player.prepare()
        player.playWhenReady = true
    }

    /** Counts a failure against [source], unless the box is offline (then nothing could have worked). */
    private fun markFail(channel: Channel, source: ChannelSource) {
        if (online()) health.markFail(channel, source)
    }

    /** Recovers [playing] unless it plays again within [STALL_TIMEOUT_MS]. Re-buffering does not restart the clock. */
    private fun armWatchdog(playing: PlaybackState) {
        if (watchdog?.isActive == true) return
        watchdog = scope.launch {
            delay(STALL_TIMEOUT_MS)
            if (_state.value === playing) launchRecover(playing, null)
        }
    }

    private fun launchRecover(playing: PlaybackState, error: PlaybackException?) {
        // One incident can report twice (a stall and then its error, an embed's error and its end): recover once.
        if (recoveryJob?.isActive == true) return
        recoveryJob = scope.launch { recover(playing, error) }
    }

    /**
     * [playing] (HLS, or a ready YouTube/web embed) dropped, stalled or ended: the same source again,
     * at most [MAX_RECOVERIES_PER_WINDOW] times per [RECOVERY_WINDOW_MS], then the next source.
     */
    private suspend fun recover(playing: PlaybackState, error: PlaybackException?) {
        if (_state.value !== playing) return
        val channel = playing.channel ?: return
        val source = playing.recoverableSource() ?: return
        val now = SystemClock.elapsedRealtime()
        while (recoveryTimes.isNotEmpty() && now - recoveryTimes.first() > RECOVERY_WINDOW_MS) recoveryTimes.removeFirst()
        recoveryTimes.addLast(now)
        if (recoveryTimes.size > MAX_RECOVERIES_PER_WINDOW) {
            recoveryTimes.clear()
            markFail(channel, source)
            restartAt(channel, source, next = true, forceFirst = false)
            return
        }
        if (error?.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
            // Fell behind the live window: back to the live edge. Counted above, so a stream whose window
            // is too short for this line moves on instead of re-syncing forever.
            player.seekToDefaultPosition()
            player.prepare()
            return
        }
        // Same source again. A 401/403 means the token expired, and an embed needs a fresh /live videoId.
        delay(RETRY_DELAY_MS * recoveryTimes.size)
        if (_state.value !== playing) return
        val fresh = error.isAuthError() || playing !is PlaybackState.PlayingHls
        restartAt(channel, source, next = false, forceFirst = fresh)
    }

    /**
     * Restarts the chain at [source] (or, with [next], at the one after it) with the current list's copy of
     * [channel]. Unchanged, the chain keeps its own order. Changed (a list fetched meanwhile fixed, removed
     * or disabled sources), its sources are ordered afresh and the walk goes on from [source] in that order,
     * or from the top when [source] is gone; a disabled channel then ends as Failed(NoSource).
     */
    private fun restartAt(channel: Channel, source: ChannelSource, next: Boolean, forceFirst: Boolean) {
        val skip = if (next) 1 else 0
        val now = listed(channel.id)
        if (now == null || now == channel) {
            startChain(channel, chainSources, chainIndex + skip, forceFirst)
            return
        }
        val sources = health.ordered(now, policy())
        val at = sources.indexOfFirst { it.key == source.key }
        if (at < 0) startChain(now, sources, fromIndex = 0, forceFirst = true)
        else startChain(now, sources, at + skip, forceFirst)
    }

    /** The source on screen when [this] is a state that can drop or stall: HLS, or a ready embed. */
    private fun PlaybackState.recoverableSource(): ChannelSource? = when (this) {
        is PlaybackState.PlayingHls -> source
        is PlaybackState.YouTube -> source.takeIf { ready }
        is PlaybackState.Web -> source.takeIf { ready }
        else -> null
    }

    private fun PlaybackState.isReadyEmbed(session: Int): Boolean = when (this) {
        is PlaybackState.YouTube -> ready && this.session == session
        is PlaybackState.Web -> ready && this.session == session
        else -> false
    }

    private fun PlaybackException?.isAuthError(): Boolean {
        val cause = this?.cause as? HttpDataSource.InvalidResponseCodeException ?: return false
        return cause.responseCode == 401 || cause.responseCode == 403 || cause.responseCode == 410
    }

    private companion object {
        const val TAG = "PlaybackEngine"
        val RETRYABLE_TYPES = setOf("hls", "page-token", "api-token")
        const val RESOLVE_TIMEOUT_MS = 10_000L
        const val FIRST_FRAME_TIMEOUT_MS = 12_000L
        const val EXTERNAL_TIMEOUT_MS = 20_000L
        const val STALL_TIMEOUT_MS = 20_000L
        const val RETRY_DELAY_MS = 900L
        const val RECOVERY_WINDOW_MS = 2 * 60_000L
        const val MAX_RECOVERIES_PER_WINDOW = 3
        const val RESUME_GAP_MS = 10_000L
    }
}
