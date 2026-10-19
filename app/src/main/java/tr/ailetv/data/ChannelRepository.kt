package tr.ailetv.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import tr.ailetv.BuildConfig
import tr.ailetv.net.UserAgents
import java.io.File

/**
 * Channel list with three layers: remote (GitHub raw, optional) → last good cached copy → bundled asset.
 * The app never starts without a list, even offline on first launch.
 */
class ChannelRepository(private val context: Context, private val http: OkHttpClient) {
    private val cacheFile = File(context.filesDir, "channels.json")
    private val _catalog = MutableStateFlow(loadInitial())
    val catalog: StateFlow<ChannelCatalog> = _catalog.asStateFlow()

    private fun loadInitial(): ChannelCatalog {
        val bundled = parse(context.assets.open("channels.json").bufferedReader().use { it.readText() })
            ?: ChannelCatalog()
        val cached = if (cacheFile.exists()) parse(cacheFile.readText()) else null
        return if (cached != null && isNewerOrSame(cached, bundled)) cached else bundled
    }

    /** Fetches the remote list if one is configured. Keeps the current list on any failure. */
    suspend fun refresh(): Boolean = withContext(Dispatchers.IO) {
        val url = BuildConfig.CHANNELS_URL
        if (url.isBlank()) return@withContext false
        try {
            val req = Request.Builder().url(url).header("User-Agent", UserAgents.TV).header("Cache-Control", "no-cache").build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext false
                val text = resp.body.string()
                val remote = parse(text) ?: return@withContext false
                if (remote.enabledChannels.isEmpty()) return@withContext false
                cacheFile.writeText(text)
                _catalog.value = remote
                true
            }
        } catch (e: Exception) {
            Log.w(TAG, "channel list refresh failed: ${e.message}")
            false
        }
    }

    private fun isNewerOrSame(a: ChannelCatalog, b: ChannelCatalog): Boolean =
        a.updatedAt >= b.updatedAt && a.enabledChannels.isNotEmpty()

    private fun parse(text: String): ChannelCatalog? = try {
        AppJson.decodeFromString(ChannelCatalog.serializer(), text)
    } catch (e: Exception) {
        Log.w(TAG, "channel list parse failed: ${e.message}")
        null
    }

    private companion object {
        const val TAG = "ChannelRepository"
    }
}
