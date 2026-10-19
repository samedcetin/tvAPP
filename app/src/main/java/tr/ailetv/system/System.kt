package tr.ailetv.system

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.SystemClock
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** Internet reachability as seen by Android. */
class NetworkMonitor(context: Context) {
    private val cm = context.getSystemService(ConnectivityManager::class.java)
    private val _online = MutableStateFlow(currentlyOnline())
    /** Connected to a network (Wi-Fi/Ethernet up). Drives the "İnternet yok" overlay. */
    val online: StateFlow<Boolean> = _online.asStateFlow()
    private val _validated = MutableStateFlow(currentlyValidated())
    /**
     * Android has checked that the internet answers behind the router, not just the router. False while a
     * restarted modem's Wi-Fi is up before its DSL link, or while only the internet link is down (Wi-Fi stays up).
     * It can lag the real link by a while: Android re-checks an unvalidated network with a growing backoff.
     */
    val validated: StateFlow<Boolean> = _validated.asStateFlow()

    init {
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            // Not validated yet: onCapabilitiesChanged follows and says so.
            override fun onAvailable(network: Network) { _online.value = true }
            override fun onLost(network: Network) {
                _online.value = currentlyOnline()
                _validated.value = currentlyValidated()
            }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                _online.value = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                _validated.value = caps.isValidated()
            }
        })
    }

    /**
     * Hint from the app's own traffic when it disagrees with [validated] (a stream plays although Android says
     * there is no internet, or every source failed although it says there is): Android re-checks the network now
     * instead of on its backoff. Android ignores it while it is still in its first quick checks.
     */
    fun reportConnectivity(working: Boolean) {
        if (working == _validated.value) return
        val network = cm.activeNetwork ?: return
        try {
            cm.reportNetworkConnectivity(network, working)
        } catch (e: RuntimeException) {
            // A hint only (SecurityException on an odd build): validation then just follows Android's own schedule.
        }
    }

    fun transport(): String {
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "Bağlantı yok"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Kablolu (Ethernet)"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi · ${caps.linkDownstreamBandwidthKbps / 1000} Mb/s"
            else -> "Diğer"
        }
    }

    private fun currentlyOnline(): Boolean =
        cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

    private fun currentlyValidated(): Boolean = cm.getNetworkCapabilities(cm.activeNetwork)?.isValidated() == true

    private fun NetworkCapabilities.isValidated(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

data class KeyLogEntry(val time: String, val key: String, val android: String)

/** Last remote key presses, shown on the hidden diagnostics screen. */
class KeyLog {
    private val _entries = MutableStateFlow<List<KeyLogEntry>>(emptyList())
    val entries: StateFlow<List<KeyLogEntry>> = _entries.asStateFlow()
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.forLanguageTag("tr-TR"))

    /** [SystemClock.elapsedRealtime] of the last key press (repeats included), so automatic actions can wait for a quiet remote. */
    var lastKeyAt = 0L
        private set

    fun record(event: KeyEvent) {
        if (event.action == KeyEvent.ACTION_DOWN) lastKeyAt = SystemClock.elapsedRealtime()
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount > 0) return
        val name = KeyEvent.keyCodeToString(event.keyCode)
        val entry = KeyLogEntry(fmt.format(Date()), friendly(event.keyCode), "$name (${event.keyCode})")
        _entries.value = (listOf(entry) + _entries.value).take(10)
    }

    private fun friendly(code: Int) = when (code) {
        KeyEvent.KEYCODE_DPAD_UP -> "▲"
        KeyEvent.KEYCODE_DPAD_DOWN -> "▼"
        KeyEvent.KEYCODE_DPAD_LEFT -> "◀"
        KeyEvent.KEYCODE_DPAD_RIGHT -> "▶"
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> "OK"
        KeyEvent.KEYCODE_BACK -> "Geri"
        KeyEvent.KEYCODE_CHANNEL_UP -> "CH+"
        KeyEvent.KEYCODE_CHANNEL_DOWN -> "CH−"
        else -> "Diğer"
    }
}

/**
 * Turkish broadcasts are 25/50 fps; Google TV usually outputs 60 Hz, which judders.
 * When playback starts, the activity's window asks for a 50 Hz mode of the same resolution, if the TV
 * offers one. The request belongs to the window: every new window asks again, and [clear] withdraws it.
 */
object DisplayModes {
    fun prefer50Hz(activity: Activity) {
        val window = activity.window
        if (window.attributes.preferredDisplayModeId != 0) return // this window already asked
        val display = displayOf(activity) ?: return
        val current = display.mode
        val target = display.supportedModes
            .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
            .firstOrNull { abs(it.refreshRate - 50f) < 0.5f } ?: return
        // Set even when the TV is already at 50 Hz, so this window keeps it.
        window.attributes = window.attributes.also { it.preferredDisplayModeId = target.modeId }
    }

    /** Setting switched off: back to the system's own mode choice. */
    fun clear(activity: Activity) {
        val window = activity.window
        if (window.attributes.preferredDisplayModeId == 0) return
        window.attributes = window.attributes.also { it.preferredDisplayModeId = 0 }
    }

    fun describe(activity: Activity): String {
        val m = displayOf(activity)?.mode ?: return "?"
        return "${m.physicalWidth}×${m.physicalHeight} · ${"%.0f".format(m.refreshRate)} Hz"
    }

    private fun displayOf(activity: Activity): android.view.Display? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) activity.display
        else @Suppress("DEPRECATION") activity.windowManager.defaultDisplay

    fun uptimeMinutes(): Long = SystemClock.elapsedRealtime() / 60_000
}
