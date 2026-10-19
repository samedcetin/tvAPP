package tr.ailetv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import tr.ailetv.ui.components.AppIcons
import tr.ailetv.ui.components.Toast
import tr.ailetv.ui.screens.AboutScreen
import tr.ailetv.ui.screens.DiagnosticsScreen
import tr.ailetv.ui.screens.GuideScreen
import tr.ailetv.ui.screens.HomeScreen
import tr.ailetv.ui.screens.OfflineScreen
import tr.ailetv.ui.screens.PlayerScreen
import tr.ailetv.ui.screens.SettingsScreen
import tr.ailetv.ui.screens.SplashScreen
import tr.ailetv.ui.theme.AileTvTheme
import tr.ailetv.ui.theme.Palette

/**
 * Always lays the UI out on a 960×540 dp canvas (Guidelines §2), whatever the TV's density,
 * so every size in the design maps 1:1 and the layout never shifts between devices.
 * System font scale is ignored: the app has its own "Yazı boyutu" setting.
 */
@Composable
fun FixedCanvas(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.Bg)) {
        val px = with(LocalDensity.current) { maxWidth.toPx() }
        CompositionLocalProvider(LocalDensity provides Density(density = px / 960f, fontScale = 1f)) {
            content()
        }
    }
}

@Composable
fun AppRoot(vm: AppViewModel, onExit: () -> Unit, onPlaybackStarted: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val screen by vm.screen.collectAsStateWithLifecycle()
    val toast by vm.toast.collectAsStateWithLifecycle()
    // Debounced in the VM (brief Wi-Fi hiccups never flash it); PlayerScreen hides YouTube on the same flag.
    val showOffline by vm.offlineShown.collectAsStateWithLifecycle()

    // Zapping changes Player(channelId) but must not rebuild the player screen.
    val screenKey: Any = when (screen) {
        is Screen.Player -> "player"
        is Screen.Home -> "home"
        else -> screen
    }

    // An OK press that went down before a screen change or the offline overlay coming/going is dropped.
    val okGuard = remember { OkPressGuard() }
    SideEffect { okGuard.onScene(screenKey, showOffline) }

    // The offline overlay takes focus while it is shown. When it goes away the framework focuses a leaf of the
    // screen by itself (a D-pad search from the top left, in the same frame), and the screen then puts focus
    // back on what it had: Home its card or button, the player its layer, Settings its list's restorer.
    // This is only a fallback for a screen left with nothing focused, so it never moves focus a screen has
    // placed (a plain request here took it from 'Tekrar dene' and from Home's card, and a focusRestorer on
    // the container redirected every later entry into the screen).
    val screenFocus = remember { FocusRequester() }
    var screenHasFocus by remember { mutableStateOf(false) }
    var wasOffline by remember { mutableStateOf(false) }
    LaunchedEffect(showOffline) {
        if (showOffline) {
            wasOffline = true
        } else if (wasOffline) {
            wasOffline = false
            delay(OFFLINE_HAND_BACK_FALLBACK_MS)
            if (!screenHasFocus) {
                runCatching { if (!screenFocus.restoreFocusedChild()) screenFocus.requestFocus() }
            }
        }
    }

    FixedCanvas {
        AileTvTheme(scale = settings.textScale) {
            // Lowest-priority Back: pop the screen stack, or leave to the Google TV home.
            BackHandler { if (!vm.back()) onExit() }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Palette.Bg)
                    // Diagnostics key log: observe every remote key. Only stale OK presses are consumed.
                    .onPreviewKeyEvent { vm.container.keyLog.record(it.nativeKeyEvent); okGuard.isStale(it) },
            ) {
                key(screenKey) {
                    // One focus container per screen, so what it remembers dies with the screen.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .onFocusChanged { screenHasFocus = it.hasFocus }
                            .focusRequester(screenFocus)
                            // Saves the child that had focus when the overlay took it, for the fallback above
                            // only: unlike focusRestorer() nothing redirects later requests into the screen.
                            .focusProperties { this.onExit = { screenFocus.saveFocusedChild() } }
                            .focusGroup(),
                    ) {
                        when (val s = screen) {
                            Screen.Splash -> SplashScreen(vm)
                            is Screen.Guide -> GuideScreen(vm, s.fromSettings)
                            is Screen.Home -> HomeScreen(vm, s.editFavorites)
                            is Screen.Player -> PlayerScreen(vm, s.channelId, onPlaybackStarted)
                            Screen.Settings -> SettingsScreen(vm)
                            Screen.About -> AboutScreen(vm)
                            Screen.Diagnostics -> DiagnosticsScreen(vm)
                        }
                    }
                }
                if (showOffline) OfflineScreen(vm, onExit)
                // YouTube rule (Guidelines §11): nothing is drawn over a visible YouTube player, toasts included.
                // PlayerScreen treats a pending toast as an overlay (it hides and pauses the player first).
                val msg = toast
                if (msg != null) {
                    val icon = when (msg) {
                        "Favorilere eklendi" -> AppIcons.StarFilled
                        "Favorilerden çıkarıldı" -> AppIcons.StarOff
                        else -> AppIcons.Check
                    }
                    Toast(msg, Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp), icon = icon)
                }
            }
        }
    }
}

/** How long after the offline overlay closes a screen may take to put focus back itself. */
private const val OFFLINE_HAND_BACK_FALLBACK_MS = 300L

private val OkKeys = setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)

/**
 * tv-material Surfaces click on the OK key-up alone (they never check that they saw the key-down), so a press
 * that began on one screen would click whatever the next screen focuses. Every screen change and every offline
 * show/hide starts a new scene; the repeats and key-up of an OK press that went down on an earlier scene, or
 * before the app saw it go down, are consumed.
 */
private class OkPressGuard {
    private var scene = 0
    private var lastScreen: Any? = null
    private var lastOffline = false
    private val downScene = HashMap<Key, Int>()

    fun onScene(screenKey: Any, offline: Boolean) {
        if (screenKey != lastScreen || offline != lastOffline) {
            scene++
            lastScreen = screenKey
            lastOffline = offline
        }
    }

    /** True = consume the event. */
    fun isStale(event: KeyEvent): Boolean {
        if (event.key !in OkKeys) return false
        return when (event.type) {
            KeyEventType.KeyDown -> if (event.nativeKeyEvent.repeatCount == 0) {
                downScene[event.key] = scene
                false
            } else {
                downScene[event.key] != scene
            }
            KeyEventType.KeyUp -> downScene.remove(event.key) != scene
            else -> false
        }
    }
}
