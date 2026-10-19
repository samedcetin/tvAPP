package tr.ailetv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import tr.ailetv.player.PlaybackState
import tr.ailetv.system.DisplayModes
import tr.ailetv.ui.AppRoot
import tr.ailetv.ui.AppViewModel
import tr.ailetv.ui.Screen

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            AppRoot(
                vm = vm,
                onExit = { finish() },
                onPlaybackStarted = { if (vm.settings.value.match50Hz) DisplayModes.prefer50Hz(this) },
            )
        }
        // "Görüntü akıcılığı (50 Hz)" applies at once: off withdraws this window's request, on asks now if a stream plays.
        lifecycleScope.launch {
            vm.settings.map { it.match50Hz }.distinctUntilChanged().collect { on ->
                if (!on) {
                    DisplayModes.clear(this@MainActivity)
                } else if (vm.screen.value is Screen.Player && vm.playback.value is PlaybackState.PlayingHls) {
                    DisplayModes.prefer50Hz(this@MainActivity)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Coming back from the Google TV home: resume the channel that was on screen.
        vm.onAppStarted()
    }

    override fun onStop() {
        super.onStop()
        // Quality guideline TV-NP: video must stop when the user leaves the app, and nothing may start behind it.
        vm.onAppStopped()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) vm.stopPlayback()
    }
}
