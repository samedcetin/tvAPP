package tr.ailetv.ui.screens

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tr.ailetv.data.CategoryIds
import tr.ailetv.data.Channel
import tr.ailetv.data.ChannelCatalog
import tr.ailetv.player.PlaybackState
import tr.ailetv.ui.AppViewModel
import tr.ailetv.ui.Screen
import tr.ailetv.ui.components.AppIcons
import tr.ailetv.ui.components.ChannelLogo
import tr.ailetv.ui.components.ChannelRow
import tr.ailetv.ui.components.HintKey
import tr.ailetv.ui.components.KeyHintRow
import tr.ailetv.ui.components.LoadingLine
import tr.ailetv.ui.components.PillButton
import tr.ailetv.ui.components.Spinner
import tr.ailetv.ui.player.VideoSurface
import tr.ailetv.ui.player.WebPagePlayer
import tr.ailetv.ui.player.YouTubeEmbed
import tr.ailetv.ui.theme.LocalUiScale
import tr.ailetv.ui.theme.Palette
import tr.ailetv.ui.theme.Space
import tr.ailetv.ui.theme.Type
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.key as keyed

/*
 * Player (PROMPTLAR Prompt 5–7, Guidelines §5–§7, §11 YouTube rule).
 * Layers, bottom to top: video → opening / failed state → info banner → channel list panel → options dialog.
 * Remote keys go through one focusable root; the list, the options dialog and the failed card own focus while shown.
 */

private enum class SourceKind { Hls, YouTube, Web }

/** Which layer currently owns the remote. */
private enum class FocusOwner { Root, Failed, List, Options }

private data class OpeningInfo(val channel: Channel?, val kind: SourceKind)

private class PlayerKeys {
    /** Key (OK or ◀) that opened the list on key-down; its repeats and key-up are swallowed. */
    var swallowKey: Key? = null
    var lastListKeyAt = 0L
    var zapJob: Job? = null
}

private class PanelMemo {
    var lastFocusedId: String? = null
    var optionsWasOpen = false
}

private const val ZAP_DEBOUNCE_MS = 400L
private const val LIST_IDLE_MS = 30_000L
private const val PICKER = "\u0000picker"
private val VideoDim = Color.Black.copy(alpha = 0.45f)
private val PillShape = RoundedCornerShape(999.dp)

private fun kindOf(type: String?): SourceKind = when (type) {
    "youtube", "json-youtube" -> SourceKind.YouTube
    "web" -> SourceKind.Web
    else -> SourceKind.Hls
}

private fun Channel.firstKind(): SourceKind = kindOf(sources.firstOrNull { it.enabled }?.type)

private fun KeyEvent.isOk(): Boolean = key == Key.DirectionCenter || key == Key.Enter || key == Key.NumPadEnter

private fun FocusRequester.tryFocus(): Boolean = runCatching { requestFocus(FocusDirection.Enter) }.getOrDefault(false)

/** Items may not be composed/attached yet (lazy rows, just-opened layers): retry for a few frames. */
private suspend fun FocusRequester.focusSoon(): Boolean {
    repeat(12) {
        if (tryFocus()) return true
        withFrameNanos { }
    }
    return false
}

/** Keeps the focused row in the middle third of the channel list (Prompt 6). */
private val MiddleThirdScroll = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val center = offset + size / 2f
        val low = containerSize / 3f
        val high = containerSize * 2f / 3f
        return when {
            center < low -> center - low
            center > high -> center - high
            else -> 0f
        }
    }
}

@Composable
fun PlayerScreen(vm: AppViewModel, channelId: String, onPlaybackStarted: () -> Unit) {
    val playback by vm.playback.collectAsStateWithLifecycle()
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    // The "İnternet yok" overlay (AppRoot) is on screen: it owns the remote and covers the player.
    val offlineShown by vm.offlineShown.collectAsStateWithLifecycle()
    val toast by vm.toast.collectAsStateWithLifecycle()

    val pb = playback
    val current: Channel? = pb.channel ?: catalog.channel(channelId)

    val scope = rememberCoroutineScope()
    val keys = remember { PlayerKeys() }
    val rootFocus = remember { FocusRequester() }
    val retryFocus = remember { FocusRequester() }

    var pending by remember { mutableStateOf<Channel?>(null) }
    var bannerVisible by remember { mutableStateOf(true) }
    var bannerToken by remember { mutableIntStateOf(0) }
    var listOpen by remember { mutableStateOf(false) }
    var optionsFor by remember { mutableStateOf<Channel?>(null) }
    var screenFocused by remember { mutableStateOf(false) }
    /** Last YouTube session whose first "ready" has been handled (see the banner rule below). */
    var ytReadyHandled by remember { mutableIntStateOf(-1) }

    // 50 Hz display switch etc.: once each time ExoPlayer starts rendering.
    val started by rememberUpdatedState(onPlaybackStarted)
    LaunchedEffect(vm) {
        vm.playback.collect { if (it is PlaybackState.PlayingHls) started() }
    }

    val pendingCh = pending
    val showingVideo = when (pb) {
        is PlaybackState.PlayingHls -> true
        is PlaybackState.YouTube -> pb.ready
        is PlaybackState.Web -> pb.ready
        else -> false
    }
    val optionsOpen = optionsFor != null
    val failedShown = pb is PlaybackState.Failed && pendingCh == null && !listOpen && !optionsOpen
    val owner = when {
        optionsOpen -> FocusOwner.Options
        listOpen -> FocusOwner.List
        failedShown -> FocusOwner.Failed
        else -> FocusOwner.Root
    }
    // YouTube: the opening layer has just shown "17 · A Haber", and the banner can only be drawn over a hidden
    // player, so the banner pending while the session opened is dropped when it turns ready instead of holding
    // the video back for bannerSeconds. ▶ and zap presses made after that still show it.
    val ytFirstReady: Int? = (pb as? PlaybackState.YouTube)?.takeIf { it.ready && it.session != ytReadyHandled }?.session
    val bannerDrawn = bannerVisible && showingVideo && owner == FocusOwner.Root &&
        (ytFirstReady == null || pendingCh != null)
    // Everything that covers the video. A YouTube player is hidden (and paused) while any of it is on screen,
    // the toasts and the offline overlay that AppRoot draws included.
    val overlayOpen = bannerDrawn || listOpen || optionsOpen || offlineShown ||
        (pb is PlaybackState.YouTube && toast != null)

    fun showBanner() {
        bannerVisible = true
        bannerToken++
    }

    fun cancelPending() {
        keys.zapJob?.cancel()
        keys.zapJob = null
        pending = null
    }

    fun playNow(channel: Channel) {
        keys.zapJob?.cancel()
        keys.zapJob = null
        vm.playChannel(channel)
        pending = null
        showBanner()
    }

    /** Debounced zap: the banner follows every press, the stream opens 400 ms after the last one. */
    fun zap(delta: Int) {
        if (catalog.enabledChannels.isEmpty()) return
        val from = pending ?: current ?: return
        val next = vm.neighbor(from, delta)
        pending = next
        showBanner()
        keys.zapJob?.cancel()
        keys.zapJob = scope.launch {
            delay(ZAP_DEBOUNCE_MS)
            // Landed back on the channel that is already opening or playing (one-channel filter, ▲ then ▼):
            // keep it running, the banner just stays. Checked on the engine's live state, not on `current`.
            val st = vm.playback.value
            val alreadyOn = st.channel?.id == next.id && st !is PlaybackState.Failed && st !is PlaybackState.Idle
            // playChannel first so the state is already "opening" when the pending channel is cleared.
            if (vm.screen.value is Screen.Player && !alreadyOn) vm.playChannel(next)
            pending = null
            keys.zapJob = null
        }
    }

    // Home / screen saver while a zap is pending: drop it, so the channel that was on screen resumes on return.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { cancelPending() }

    fun zapKey(k: Key): Boolean {
        val up = k == Key.DirectionUp || k == Key.ChannelUp || k == Key.PageUp
        val down = k == Key.DirectionDown || k == Key.ChannelDown || k == Key.PageDown
        if (!up && !down) return false
        zap(if (up == settings.upIsNext) 1 else -1)
        return true
    }

    fun openList() {
        listOpen = true
        bannerVisible = false
        keys.lastListKeyAt = SystemClock.uptimeMillis()
    }

    val onKey: (KeyEvent) -> Boolean = handler@{ ev ->
        // The key that opened the list: its repeats and its key-up must not reach the newly focused row
        // (a tv Surface clicks on OK key-up; a held ◀ would keep cycling the category picker).
        val opener = keys.swallowKey
        if (opener != null && ev.key == opener) {
            if (ev.type == KeyEventType.KeyUp) {
                keys.swallowKey = null
                return@handler true
            }
            if (ev.nativeKeyEvent.repeatCount > 0) return@handler true
            keys.swallowKey = null
        }
        if (ev.type != KeyEventType.KeyDown) return@handler false
        if (listOpen) keys.lastListKeyAt = SystemClock.uptimeMillis()
        when (owner) {
            FocusOwner.Options, FocusOwner.List -> false
            FocusOwner.Failed -> zapKey(ev.key)
            FocusOwner.Root -> when {
                zapKey(ev.key) -> true
                ev.isOk() || ev.key == Key.DirectionLeft -> {
                    openList()
                    keys.swallowKey = ev.key
                    true
                }
                ev.key == Key.DirectionRight -> {
                    showBanner()
                    true
                }
                else -> false
            }
        }
    }

    // Never leave the screen without a focused element: the root takes the remote whenever no layer owns it.
    // While the "İnternet yok" overlay (AppRoot) is shown it owns focus; take it back once it is gone.
    LaunchedEffect(owner, offlineShown) {
        if (offlineShown) {
            // Close the list and the dialog as soon as the overlay takes over. When it goes away the framework
            // focuses the root Box (the screen's only focusable leaf), not a row or a button, before any effect
            // here runs, so a layer kept open would come back with nothing focused. The overlay covers the
            // whole canvas, so closing them underneath is not seen.
            optionsFor = null
            listOpen = false
            return@LaunchedEffect
        }
        when (owner) {
            FocusOwner.Root -> rootFocus.focusSoon()
            // Also right after the overlay: the framework put focus on the root, the card's button gets it.
            FocusOwner.Failed -> retryFocus.focusSoon()
            // They focus their own row / first button when they open.
            FocusOwner.List, FocusOwner.Options -> Unit
        }
    }
    // Watchdog: if focus is gone for good (e.g. taken by the offline overlay and not handed back),
    // close the list/options and give the remote back to the root.
    val latestOwner by rememberUpdatedState(owner)
    LaunchedEffect(screenFocused, offlineShown) {
        if (screenFocused || offlineShown) return@LaunchedEffect
        delay(500)
        when (latestOwner) {
            FocusOwner.List, FocusOwner.Options -> {
                optionsFor = null
                listOpen = false
            }
            FocusOwner.Root -> rootFocus.focusSoon()
            FocusOwner.Failed -> retryFocus.focusSoon()
        }
    }

    // Banner auto-hide: settings.bannerSeconds after the stream is actually on screen.
    LaunchedEffect(bannerVisible, bannerToken, showingVideo, pendingCh == null, settings.bannerSeconds) {
        if (bannerVisible && showingVideo && pendingCh == null) {
            delay(settings.bannerSeconds.coerceAtLeast(1) * 1000L)
            bannerVisible = false
        }
    }
    // The YouTube rule above: drop the opening banner once per session (a zap still pending keeps its banner).
    LaunchedEffect(ytFirstReady) {
        val session = ytFirstReady ?: return@LaunchedEffect
        if (pending == null) bannerVisible = false
        ytReadyHandled = session
    }

    // Channel list closes itself after 30 s without a key (paused while the options dialog is open).
    LaunchedEffect(listOpen, optionsOpen) {
        if (!listOpen || optionsOpen) return@LaunchedEffect
        keys.lastListKeyAt = SystemClock.uptimeMillis()
        while (true) {
            val wait = keys.lastListKeyAt + LIST_IDLE_MS - SystemClock.uptimeMillis()
            if (wait <= 0) break
            delay(wait)
        }
        listOpen = false
    }

    BackHandler(enabled = owner == FocusOwner.List) { listOpen = false }
    BackHandler(enabled = bannerDrawn) {
        bannerVisible = false
        cancelPending()
    }

    val opening: OpeningInfo? = when {
        pendingCh != null && !showingVideo -> OpeningInfo(pendingCh, pendingCh.firstKind())
        pb is PlaybackState.Opening -> OpeningInfo(pb.channel, kindOf(pb.source.type))
        pb is PlaybackState.YouTube && !pb.ready -> OpeningInfo(pb.channel, SourceKind.YouTube)
        pb is PlaybackState.Web && !pb.ready -> OpeningInfo(pb.channel, SourceKind.Web)
        pb is PlaybackState.Idle -> OpeningInfo(current, current?.firstKind() ?: SourceKind.Hls)
        else -> null
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .keepScreenOn()
            .onPreviewKeyEvent(onKey)
            .onFocusChanged { screenFocused = it.hasFocus }
            .focusRequester(rootFocus)
            .focusable(),
    ) {
        // 1. Video. One call site for the SurfaceView so Opening → PlayingHls keeps the same surface.
        val hlsSurface = pb is PlaybackState.PlayingHls ||
            (pb is PlaybackState.Opening && kindOf(pb.source.type) == SourceKind.Hls)
        if (hlsSurface) VideoSurface(vm.engine.player)
        if (pb is PlaybackState.YouTube) {
            keyed(pb.session) {
                YouTubeEmbed(
                    videoId = pb.videoId,
                    youtubeChannelId = pb.youtubeChannelId,
                    // YouTube rule: nothing is drawn over a visible player; overlays hide (and pause) it first.
                    visible = pb.ready && !overlayOpen,
                    onReady = { vm.engine.onExternalReady(pb.session) },
                    onError = { vm.engine.onExternalError(pb.session) },
                    onBuffering = { vm.engine.onExternalBuffering(pb.session) },
                    onResumed = { vm.engine.onExternalResumed(pb.session) },
                )
            }
            if (pb.ready && overlayOpen) Box(Modifier.fillMaxSize().background(Palette.Bg))
        }
        if (pb is PlaybackState.Web) {
            keyed(pb.session) {
                WebPagePlayer(
                    url = pb.url,
                    onReady = { vm.engine.onExternalReady(pb.session) },
                    onError = { vm.engine.onExternalError(pb.session) },
                    // The page's video stalling / playing again: the engine's stall watchdog and recovery.
                    onBuffering = { vm.engine.onExternalBuffering(pb.session) },
                    onResumed = { vm.engine.onExternalResumed(pb.session) },
                )
            }
        }

        // 2. Opening / failed state.
        if (opening != null) {
            OpeningLayer(opening.channel, opening.kind)
        } else if (pb is PlaybackState.Failed) {
            if (failedShown) {
                FailedLayer(
                    channel = pb.channel,
                    retryFocus = retryFocus,
                    onRetry = {
                        showBanner()
                        vm.retryPlayback()
                    },
                    onNext = {
                        if (catalog.enabledChannels.isNotEmpty()) playNow(vm.neighbor(pb.channel, 1))
                    },
                    onHome = { vm.back() },
                )
            } else {
                Box(Modifier.fillMaxSize().background(Palette.VideoBg))
            }
        }

        // 3. Info banner.
        if (bannerDrawn) {
            val ch = pendingCh ?: current
            if (ch != null) {
                InfoBanner(
                    channel = ch,
                    categoryLabel = catalog.categoryLabel(ch.category),
                    favorite = ch.id in favorites,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = Space.SafeX, end = Space.SafeX, bottom = Space.SafeY),
                )
            }
        }

        // 4. Channel list panel.
        if (listOpen) {
            Box(Modifier.fillMaxSize().background(VideoDim))
            ChannelListPanel(
                vm = vm,
                catalog = catalog,
                favorites = favorites,
                initialFilter = settings.activeFilter,
                playingId = current?.id,
                optionsOpen = optionsOpen,
                onPick = { ch ->
                    listOpen = false
                    if (ch.id != current?.id || pb is PlaybackState.Failed || pb is PlaybackState.Idle) {
                        playNow(ch)
                    } else {
                        cancelPending()
                        showBanner()
                    }
                },
                onOptions = { ch -> optionsFor = ch },
            )
        }

        // 5. Channel options dialog.
        val opt = optionsFor
        if (opt != null) {
            ChannelOptionsDialog(
                channel = opt,
                categoryLabel = catalog.categoryLabel(opt.category),
                favorite = opt.id in favorites,
                onToggleFavorite = {
                    vm.toggleFavorite(opt.id)
                    optionsFor = null
                },
                // Reloads the dialog's channel (it can be any row, not only the playing one) and closes the list.
                onReload = {
                    optionsFor = null
                    listOpen = false
                    vm.showToast("Yayın yeniden yükleniyor")
                    // The engine's channel, not `current`: while Idle there is nothing to retry, so open it.
                    if (vm.playback.value.channel?.id == opt.id) {
                        cancelPending()
                        vm.retryPlayback()
                        showBanner()
                    } else {
                        playNow(opt)
                    }
                },
                onClose = { optionsFor = null },
            )
        }
    }
}

// ---------------------------------------------------------------- opening / failed

@Composable
private fun OpeningLayer(channel: Channel?, kind: SourceKind) {
    Box(Modifier.fillMaxSize().background(Palette.VideoBg), contentAlignment = Alignment.Center) {
        if (channel == null) {
            Spinner(24.dp)
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ChannelLogo(channel.name, Modifier.size(160.dp, 90.dp), large = true)
                Text(
                    "${channel.number} · ${channel.name}",
                    style = Type.titleL(),
                    color = Palette.Text,
                    textAlign = TextAlign.Center,
                )
                when (kind) {
                    SourceKind.Hls -> LoadingLine("Yayın açılıyor…")
                    SourceKind.YouTube -> ExternalSourceLine(AppIcons.MonitorPlay, "Resmi YouTube yayınından açılıyor…")
                    SourceKind.Web -> ExternalSourceLine(AppIcons.Globe, "Kanalın resmi internet sayfasından açılıyor…")
                }
            }
        }
    }
}

/** Prompt 7 "Harici kaynak": source icon + text, then a small spinner (emitted into the parent column). */
@Composable
private fun ExternalSourceLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = Palette.Accent)
        Text(text, style = Type.body(), color = Palette.Text2)
    }
    Spinner(24.dp)
}

@Composable
private fun FailedLayer(
    channel: Channel,
    retryFocus: FocusRequester,
    onRetry: () -> Unit,
    onNext: () -> Unit,
    onHome: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Palette.VideoBg), contentAlignment = Alignment.Center) {
        // 520 dp at text size 1; grows (up to the content width) only when larger text needs it.
        Column(
            Modifier
                .widthIn(min = 520.dp, max = Space.ContentWidth)
                .clip(RoundedCornerShape(24.dp))
                .background(Palette.SurfaceDialog)
                .padding(28.dp),
        ) {
            Icon(AppIcons.Alert, contentDescription = null, modifier = Modifier.size(40.dp), tint = Palette.Danger)
            Spacer(Modifier.height(14.dp))
            Text("${channel.name} şu anda açılamıyor.", style = Type.titleL(), color = Palette.Text)
            Spacer(Modifier.height(8.dp))
            Text(
                "Biraz sonra tekrar deneyin veya başka bir kanal seçin.",
                style = Type.body(),
                color = Palette.Text2,
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton("Tekrar dene", onRetry, icon = AppIcons.Refresh, focusRequester = retryFocus)
                PillButton("Sonraki kanal", onNext, icon = AppIcons.ChevronUp)
                PillButton("Ana ekran", onHome, icon = AppIcons.Home)
            }
        }
    }
}

// ---------------------------------------------------------------- info banner

@Composable
private fun InfoBanner(channel: Channel, categoryLabel: String, favorite: Boolean, modifier: Modifier = Modifier) {
    val s = LocalUiScale.current
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.BannerBg)
            .padding(horizontal = 22.dp, vertical = if (s.factor >= 1.5f) 14.dp else 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            ChannelLogo(channel.name, Modifier.size(112.dp, 63.dp))
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        channel.number.toString(),
                        style = Type.titleM(),
                        color = Palette.Text2,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Text(
                        channel.name,
                        style = Type.display(),
                        color = Palette.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alignByBaseline().weight(1f, fill = false),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BannerPill(categoryLabel)
                    BannerPill("CANLI", live = true)
                }
            }
            BannerClock()
            if (favorite) {
                Icon(AppIcons.StarFilled, contentDescription = "Favori", modifier = Modifier.size(24.dp), tint = Palette.Star)
            }
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Outline))
        Spacer(Modifier.height(12.dp))
        KeyHintRow(
            HintKey.UpDown to "Kanal değiştir",
            HintKey.Ok to "Kanal listesi",
            HintKey.Right to "Bilgi",
            HintKey.Back to "Ana ekran",
        )
    }
}

@Composable
private fun BannerPill(text: String, live: Boolean = false) {
    val s = LocalUiScale.current
    Row(
        Modifier
            .heightIn(min = s.dp(28))
            .clip(PillShape)
            .background(Palette.Surface2)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (live) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Palette.Danger))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = Type.label(), color = Palette.Text, maxLines = 1)
    }
}

@Composable
private fun BannerClock() {
    val format = remember { SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr-TR")) }
    var now by remember { mutableStateOf(format.format(Date())) }
    LaunchedEffect(format) {
        while (true) {
            delay(60_000L - System.currentTimeMillis() % 60_000L + 50L)
            now = format.format(Date())
        }
    }
    Text(now, style = Type.titleL(), color = Palette.Text)
}

// ---------------------------------------------------------------- channel list panel

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChannelListPanel(
    vm: AppViewModel,
    catalog: ChannelCatalog,
    favorites: List<String>,
    initialFilter: String,
    playingId: String?,
    optionsOpen: Boolean,
    onPick: (Channel) -> Unit,
    onOptions: (Channel) -> Unit,
) {
    val s = LocalUiScale.current
    val scope = rememberCoroutineScope()
    var filter by remember { mutableStateOf(initialFilter) }
    val channels = remember(filter, catalog, favorites) { vm.channelsFor(filter) }
    val favSet = remember(favorites) { favorites.toHashSet() }
    val pickerFocus = remember { FocusRequester() }
    val rowFocus = remember { HashMap<String, FocusRequester>() }
    val memo = remember { PanelMemo() }
    val initialIndex = remember { channels.indexOfFirst { it.id == playingId } }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (initialIndex - 2).coerceAtLeast(0))
    // Opening focus: the playing channel's row, else the first row, else the category picker.
    var focusTarget by remember {
        mutableStateOf<String?>(if (initialIndex >= 0) playingId else channels.firstOrNull()?.id ?: PICKER)
    }

    fun rowRequester(id: String): FocusRequester = rowFocus.getOrPut(id) { FocusRequester() }

    LaunchedEffect(focusTarget) {
        val id = focusTarget ?: return@LaunchedEffect
        val ok = if (id == PICKER) pickerFocus.focusSoon() else rowRequester(id).focusSoon()
        if (!ok && id != PICKER) pickerFocus.focusSoon()
        focusTarget = null
    }

    // Back from the options dialog: focus returns to the row it was opened from.
    LaunchedEffect(optionsOpen) {
        if (optionsOpen) {
            memo.optionsWasOpen = true
        } else if (memo.optionsWasOpen) {
            memo.optionsWasOpen = false
            focusTarget = memo.lastFocusedId ?: PICKER
        }
    }

    fun cycleCategory(delta: Int) {
        val cats = catalog.categories
        if (cats.isEmpty()) return
        val i = cats.indexOfFirst { it.id == filter }
        val next = if (i < 0) cats.first() else cats[((i + delta) % cats.size + cats.size) % cats.size]
        filter = next.id
        vm.setFilter(next.id)
        scope.launch { runCatching { listState.scrollToItem(0) } }
    }

    fun focusFirstRow() {
        val first = channels.firstOrNull() ?: return
        scope.launch {
            runCatching { listState.scrollToItem(0) }
            focusTarget = first.id
        }
    }

    val onPickerKey: (KeyEvent) -> Boolean = { ev ->
        if (ev.type != KeyEventType.KeyDown) {
            false
        } else {
            when (ev.key) {
                Key.DirectionLeft -> {
                    cycleCategory(-1)
                    true
                }
                Key.DirectionRight -> {
                    cycleCategory(1)
                    true
                }
                Key.DirectionDown -> {
                    focusFirstRow()
                    true
                }
                Key.DirectionUp -> true
                else -> false
            }
        }
    }

    fun onRowKey(ev: KeyEvent, index: Int, channel: Channel): Boolean {
        if (ev.type != KeyEventType.KeyDown) return false
        return when {
            ev.key == Key.DirectionLeft -> {
                pickerFocus.tryFocus()
                true
            }
            ev.key == Key.DirectionUp && index == 0 -> {
                pickerFocus.tryFocus()
                true
            }
            ev.key == Key.DirectionRight -> {
                onOptions(channel)
                true
            }
            ev.key == Key.DirectionDown && index == channels.lastIndex -> true
            else -> false
        }
    }

    Column(
        Modifier
            .fillMaxHeight()
            .width(if (s.factor >= 1.5f) 470.dp else 400.dp)
            .background(Palette.SurfacePanel)
            .padding(start = Space.SafeX, top = Space.SafeY, end = 24.dp, bottom = Space.SafeY),
    ) {
        Text("Kanallar", style = Type.titleL(), color = Palette.Text)
        Spacer(Modifier.height(14.dp))
        CategoryPicker(
            label = "${catalog.categoryLabel(filter)} · ${channels.size} kanal",
            focusRequester = pickerFocus,
            onClick = { focusFirstRow() },
            onKey = onPickerKey,
        )
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (channels.isEmpty()) {
                if (filter == CategoryIds.FAVORITES) {
                    Text(
                        "Henüz favori kanalınız yok.",
                        style = Type.body(),
                        color = Palette.Text2,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                    )
                }
            } else {
                CompositionLocalProvider(LocalBringIntoViewSpec provides MiddleThirdScroll) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        itemsIndexed(channels, key = { _, ch -> ch.id }) { index, ch ->
                            ChannelRow(
                                channel = ch,
                                favorite = ch.id in favSet,
                                playing = ch.id == playingId,
                                onClick = { onPick(ch) },
                                modifier = Modifier
                                    .focusRequester(rowRequester(ch.id))
                                    .onPreviewKeyEvent { ev -> onRowKey(ev, index, ch) },
                                onFocused = { memo.lastFocusedId = ch.id },
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        KeyHintRow(
            HintKey.Ok to "İzle",
            HintKey.Right to "Seçenekler",
            HintKey.Back to "Kapat",
        )
    }
}

@Composable
private fun CategoryPicker(
    label: String,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    onKey: (KeyEvent) -> Boolean,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent(onKey),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Palette.Surface2,
            contentColor = Palette.Text,
            focusedContainerColor = Palette.FocusBg,
            focusedContentColor = Palette.FocusText,
            pressedContainerColor = Palette.FocusBg,
            pressedContentColor = Palette.FocusText,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(AppIcons.ChevronLeft, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                label,
                style = Type.labelL(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Icon(AppIcons.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

// ---------------------------------------------------------------- options dialog

@Composable
private fun ChannelOptionsDialog(
    channel: Channel,
    categoryLabel: String,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onReload: () -> Unit,
    onClose: () -> Unit,
) {
    val first = remember { FocusRequester() }
    // DialogScaffold's frame (440 dp, scrim, focus trap, Back), with Prompt 6's header: the 96×54 logo on the
    // left, "3 · Kanal D" and the category beside it. DialogScaffold always draws its own title line first.
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(Palette.Scrim), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .width(440.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Palette.SurfaceDialog)
                .padding(28.dp)
                .focusProperties { onExit = { cancelFocusChange() } }
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ChannelLogo(channel.name, Modifier.size(96.dp, 54.dp))
                Column(Modifier.weight(1f)) {
                    Text("${channel.number} · ${channel.name}", style = Type.titleL(), color = Palette.Text)
                    Text(categoryLabel, style = Type.bodyM(), color = Palette.Text2)
                }
            }
            Spacer(Modifier.height(10.dp))
            PillButton(
                text = if (favorite) "Favorilerden çıkar" else "Favorilere ekle",
                onClick = onToggleFavorite,
                icon = if (favorite) AppIcons.StarOff else AppIcons.Star,
                wide = true,
                focusRequester = first,
            )
            PillButton("Yayını yeniden yükle", onReload, icon = AppIcons.Refresh, wide = true)
            PillButton("Kapat", onClose, icon = AppIcons.Close, wide = true)
        }
    }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
}
