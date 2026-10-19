package tr.ailetv

import android.app.Application
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import tr.ailetv.data.ChannelRepository
import tr.ailetv.data.HealthStore
import tr.ailetv.data.Prefs
import tr.ailetv.net.Net
import tr.ailetv.player.PlaybackEngine
import tr.ailetv.resolve.SourceResolver
import tr.ailetv.system.KeyLog
import tr.ailetv.system.NetworkMonitor
import tr.ailetv.work.RefreshWorker
import java.util.concurrent.TimeUnit

/** Manual DI: one instance of each service for the whole process. */
class AppContainer(app: Application) {
    /** Background work for the whole process. A failure there is logged; it must never take the TV app down. */
    val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e -> Log.e(TAG, "background failure", e) },
    )
    val http = Net.createClient()
    val prefs = Prefs(app, scope)
    val channels = ChannelRepository(app, http)
    val health = HealthStore(prefs, scope)
    // Whole-call budget below the engine's 10 s resolve timeout: abandoned calls are torn down, and
    // youtube()'s "page unreachable → embed resolves it" fallback still fires inside that window.
    val resolver = SourceResolver(http.newBuilder().callTimeout(8, TimeUnit.SECONDS).build())
    val network = NetworkMonitor(app)
    val keyLog = KeyLog()

    /** Created on the main thread on first use (ExoPlayer is bound to the main looper). */
    val engine: PlaybackEngine by lazy {
        PlaybackEngine(
            app, http, resolver, health,
            policy = { channels.catalog.value.policy },
            // Validated, not just connected: during a modem restart or an internet-only outage the Wi-Fi is up
            // but nothing behind it answers, and those failures say nothing about a source.
            online = { network.validated.value },
            listed = { id -> channels.catalog.value.channel(id) },
        )
    }

    private companion object {
        const val TAG = "AileTv"
    }
}

class AileTvApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        schedulePeriodicRefresh()
    }

    private fun schedulePeriodicRefresh() {
        val request = PeriodicWorkRequestBuilder<RefreshWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("channel-refresh", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
