package tr.ailetv.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tr.ailetv.AileTvApp
import tr.ailetv.BuildConfig
import tr.ailetv.data.CategoryIds
import tr.ailetv.data.Channel
import tr.ailetv.data.ChannelCatalog
import tr.ailetv.player.FailReason
import tr.ailetv.player.PlaybackState

sealed interface Screen {
    data object Splash : Screen
    /** [fromSettings]: return to Settings afterwards instead of continuing the startup flow. */
    data class Guide(val fromSettings: Boolean = false) : Screen
    data class Home(val editFavorites: Boolean = false) : Screen
    data class Player(val channelId: String) : Screen
    data object Settings : Screen
    data object About : Screen
    data object Diagnostics : Screen
}

/** Single source of truth for the UI. Screens read state from here and call these functions; nothing else. */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    val container = (app as AileTvApp).container
    val engine get() = container.engine

    val catalog: StateFlow<ChannelCatalog> = container.channels.catalog
    val settings = container.prefs.settings
    val online = container.network.online
    private val _offlineShown = MutableStateFlow(false)
    /** [online] for the UI: true only 2.5 s after the connection dropped, so brief Wi-Fi hiccups never flash the offline screen. */
    val offlineShown: StateFlow<Boolean> = _offlineShown.asStateFlow()
    val keyLog = container.keyLog.entries
    val playback: StateFlow<PlaybackState> get() = engine.state

    /** Ordered favourite ids; falls back to the catalog's defaults until the user edits them. */
    val favorites: StateFlow<List<String>> = combine(settings, catalog) { s, c ->
        s.favorites ?: c.enabledChannels.filter { it.defaultFavorite }.map { it.id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val stack = MutableStateFlow<List<Screen>>(listOf(Screen.Splash))
    val screen: StateFlow<Screen> = MutableStateFlow<Screen>(Screen.Splash).also { out ->
        viewModelScope.launch { stack.collect { out.value = it.last() } }
    }.asStateFlow()

    /** Set when leaving the player so Home can put focus back on the channel that was playing. Home clears it. */
    var homeFocusChannelId: String? = null

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()
    private var toastJob: Job? = null

    // Automatic retry (see autoRetry). Declared before init, which starts collecting them.
    /** Bumped on every reconnect. */
    private val reconnects = MutableStateFlow(0)
    private var reconnectsHandled = 0
    /** The channel on the Failed card and when its current run of failures began; null once it plays or the player closes. */
    private var failingId: String? = null
    private var failingSince = 0L

    init {
        viewModelScope.launch { container.channels.refresh() }
        viewModelScope.launch {
            online.collectLatest { on ->
                if (on) {
                    _offlineShown.value = false
                } else {
                    delay(OFFLINE_GRACE_MS)
                    _offlineShown.value = true
                }
            }
        }
        // PROMPTLAR §7.5: once the connection is back the screen returns to where the user was, so a
        // channel that failed during the outage starts again by itself.
        viewModelScope.launch {
            // StateFlow only emits changes: each `true` after the first value is a down -> up transition.
            // Both count: the Wi-Fi coming back, and Android seeing the internet behind it again (a restarted
            // modem's Wi-Fi is up 30–90 s before its DSL link; an internet-only outage keeps the Wi-Fi up).
            merge(online.drop(1).filter { it }, container.network.validated.drop(1).filter { it })
                .collect { reconnects.value += 1 }
        }
        viewModelScope.launch {
            combine(engine.state, reconnects) { s, r -> s to r }.collectLatest { (s, r) -> autoRetry(s, r) }
        }
    }

    // ---------- automatic retry ----------

    /**
     * Runs for every playback state (and reconnect); a newer one cancels it, so a zap, Back, Home or the user's
     * own "Tekrar dene" always wins. A failed channel is tried again by itself: [RECONNECT_SETTLE_MS] after a
     * reconnect, otherwise every 20 s, then 30 s, for [AUTO_RETRY_FOR_MS], and beyond that only while Android sees
     * no internet behind the router (then the outage is the cause, not the channel), up to [AUTO_RETRY_OUTAGE_MS];
     * after that it only keeps Android re-checking the internet (see [keepRecheckingUntilValidated]).
     */
    private suspend fun autoRetry(s: PlaybackState, reconnect: Int) {
        when (s) {
            // Still walking its sources (an embed not ready yet included): wait for the outcome. A reconnect
            // meanwhile stays pending, so a chain from the outage that fails only now is retried at once.
            is PlaybackState.Opening -> return
            is PlaybackState.YouTube -> if (!s.ready) return
            is PlaybackState.Web -> if (!s.ready) return
            else -> Unit
        }
        if (s !is PlaybackState.Failed) {
            // Playing again, or the player is gone: nothing to retry, and a later failure starts a new round.
            failingId = null
            reconnectsHandled = reconnect
            if (s !is PlaybackState.Idle) container.network.reportConnectivity(true)
            return
        }
        if (s.reason == FailReason.NoSource) return
        val now = SystemClock.elapsedRealtime()
        if (failingId != s.channel.id) {
            failingId = s.channel.id
            failingSince = now
        }
        val pause = if (reconnect != reconnectsHandled) {
            // The link is back: a new round, starting as soon as the router has settled.
            reconnectsHandled = reconnect
            failingSince = now
            RECONNECT_SETTLE_MS
        } else {
            // Every source failed: if Android still thinks the internet works, have it check now. A dead link
            // behind the router then shows in `validated`, and its return comes back as a reconnect.
            container.network.reportConnectivity(false)
            val failingFor = now - failingSince
            when {
                failingFor < 60_000L -> 20_000L
                failingFor < AUTO_RETRY_FOR_MS -> 30_000L
                !container.network.validated.value && failingFor < AUTO_RETRY_OUTAGE_MS -> 60_000L
                !container.network.validated.value -> keepRecheckingUntilValidated()
                else -> return // a channel that is really down: the card stays, its buttons keep the focus
            }
        }
        delay(pause)
        // Never under the user's finger: the retry moves focus off the card (they may be on "Sonraki kanal").
        while (true) {
            val quietFor = SystemClock.elapsedRealtime() - container.keyLog.lastKeyAt
            if (quietFor >= USER_QUIET_MS) break
            delay(USER_QUIET_MS - quietFor)
        }
        // Offline: the "İnternet yok" overlay is up, and the reconnect starts the next round.
        if (online.value && screen.value is Screen.Player) retryPlayback()
    }

    /**
     * Long outage behind a working Wi-Fi: no more playback retries (focus stays on the card), but keep asking
     * Android to re-check the internet. Its own re-check backoff grows to 10 min; this way the internet's return
     * shows in `validated` within about a minute, and that reconnect cancels this loop and retries the channel.
     */
    private suspend fun keepRecheckingUntilValidated(): Nothing {
        while (true) {
            delay(60_000L)
            container.network.reportConnectivity(true) // no-op once validated; else forces a re-evaluation
        }
    }

    // ---------- navigation ----------

    fun navigate(to: Screen) {
        stack.value = stack.value + to
    }

    /** Replace the whole stack (used after splash/guide so Back never returns to them). */
    fun resetTo(vararg screens: Screen) {
        stack.value = screens.toList()
    }

    /** Pops the top screen. Returns false when the stack is at its root (the activity should finish). */
    fun back(): Boolean {
        val s = stack.value
        if (s.size <= 1) return false
        val top = s.last()
        if (top is Screen.Player) {
            engine.stop()
            homeFocusChannelId = top.channelId
        }
        stack.value = s.dropLast(1)
        return true
    }

    /** Called by the splash once settings are loaded. */
    fun finishSplash() {
        val s = settings.value
        when {
            !s.guideSeen -> resetTo(Screen.Guide())
            else -> startMain()
        }
    }

    fun finishGuide(fromSettings: Boolean) {
        viewModelScope.launch { container.prefs.setGuideSeen(true) }
        if (fromSettings) back() else startMain()
    }

    private fun startMain() {
        val s = settings.value
        val last = lastChannel()
        if (s.autoStartLastChannel && last != null) {
            resetTo(Screen.Home(), Screen.Player(last.id))
            // Home pressed during the splash: only set the stack; onAppStarted plays it when the user is back.
            if (engine.foreground) engine.play(last)
        } else {
            resetTo(Screen.Home())
        }
    }

    // ---------- activity lifecycle ----------

    /** Activity visible again (or for the first time): playback may run; resume the channel that was on screen. */
    fun onAppStarted() {
        engine.foreground = true
        val top = stack.value.last()
        if (top is Screen.Player && engine.state.value is PlaybackState.Idle) {
            (enabledChannel(top.channelId) ?: lastChannel())?.let { playChannel(it) }
        }
    }

    /** Activity stopped (Google TV home, screen saver): stop, and let nothing start again until [onAppStarted]. */
    fun onAppStopped() {
        engine.foreground = false
        engine.stop()
    }

    // ---------- channels ----------

    fun channelsFor(filter: String): List<Channel> {
        val all = catalog.value.enabledChannels
        return when (filter) {
            CategoryIds.ALL -> all
            CategoryIds.FAVORITES -> favorites.value.mapNotNull { id -> all.firstOrNull { it.id == id } }
            else -> all.filter { it.category == filter }
        }
    }

    /** The last watched channel, unless the list has disabled it since; then the first channel. */
    fun lastChannel(): Channel? =
        enabledChannel(settings.value.lastChannelId) ?: catalog.value.enabledChannels.firstOrNull()

    private fun enabledChannel(id: String?): Channel? = catalog.value.enabledChannels.firstOrNull { it.id == id }

    /** The channel [delta] steps away from [from] inside the active filter, wrapping around. */
    fun neighbor(from: Channel, delta: Int): Channel {
        val filter = settings.value.activeFilter
        val filtered = channelsFor(filter)
        val list = filtered.ifEmpty { catalog.value.enabledChannels }
        val i = list.indexOfFirst { it.id == from.id }
        if (i < 0) {
            // Playing channel is outside the filter (resume card, autostart, just un-favourited): keep the
            // direction. Number-sorted lists continue from its number; the favourites order is the user's own.
            val byNumber = filter != CategoryIds.FAVORITES || filtered.isEmpty()
            return if (delta > 0) {
                list.firstOrNull { byNumber && it.number > from.number } ?: list.first()
            } else {
                list.lastOrNull { byNumber && it.number < from.number } ?: list.last()
            }
        }
        return list[((i + delta) % list.size + list.size) % list.size]
    }

    /** Opens the player for [channel] (from Home, resume card, search…). */
    fun openChannel(channel: Channel) {
        val top = stack.value.last()
        if (top is Screen.Player) {
            stack.value = stack.value.dropLast(1) + Screen.Player(channel.id)
        } else {
            navigate(Screen.Player(channel.id))
        }
        playChannel(channel)
    }

    /** Switches the stream (zapping inside the player). Ignored once the player is gone, e.g. a late debounced zap. */
    fun playChannel(channel: Channel) {
        val top = stack.value.last()
        if (top !is Screen.Player) return
        if (top.channelId != channel.id) {
            stack.value = stack.value.dropLast(1) + Screen.Player(channel.id)
        }
        viewModelScope.launch { container.prefs.setLastChannel(channel.id) }
        engine.play(channel)
    }

    /** "Tekrar dene" and the automatic retry. The engine takes the channel as the current list has it. */
    fun retryPlayback() = engine.retry()

    fun stopPlayback() = engine.stop()

    // ---------- favourites & filter ----------

    fun isFavorite(id: String) = favorites.value.contains(id)

    /** Returns true if the channel is now a favourite. */
    fun toggleFavorite(id: String): Boolean {
        val cur = favorites.value
        val added = !cur.contains(id)
        val next = if (added) cur + id else cur - id
        viewModelScope.launch { container.prefs.setFavorites(next) }
        showToast(if (added) "Favorilere eklendi" else "Favorilerden çıkarıldı")
        return added
    }

    fun setFilter(id: String) {
        if (settings.value.activeFilter == id) return
        viewModelScope.launch { container.prefs.setFilter(id) }
    }

    // ---------- settings ----------

    fun setTextScale(v: Float) = viewModelScope.launch { container.prefs.setTextScale(v) }
    fun setAutoStart(v: Boolean) = viewModelScope.launch { container.prefs.setAutoStart(v) }
    fun setUpIsNext(v: Boolean) = viewModelScope.launch { container.prefs.setUpIsNext(v) }
    fun setBannerSeconds(v: Int) = viewModelScope.launch { container.prefs.setBannerSeconds(v) }
    fun setMatch50Hz(v: Boolean) = viewModelScope.launch { container.prefs.setMatch50Hz(v) }

    fun refreshChannelList() {
        viewModelScope.launch {
            val updated = container.channels.refresh()
            showToast(if (updated || BuildConfig.CHANNELS_URL.isBlank()) "Kanal listesi güncel" else "Liste şu an güncellenemedi")
        }
    }

    // ---------- toast ----------

    fun showToast(message: String) {
        _toast.value = message
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            delay(2000)
            _toast.value = null
        }
    }

    // ---------- diagnostics ----------

    fun appVersion(): String = BuildConfig.VERSION_NAME

    private companion object {
        const val OFFLINE_GRACE_MS = 2_500L
        const val RECONNECT_SETTLE_MS = 3_000L
        const val AUTO_RETRY_FOR_MS = 5 * 60_000L
        const val AUTO_RETRY_OUTAGE_MS = 30 * 60_000L
        const val USER_QUIET_MS = 8_000L
    }
}
