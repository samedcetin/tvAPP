package tr.ailetv.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import tr.ailetv.AileTvApp
import tr.ailetv.data.ChannelSource
import tr.ailetv.net.UserAgents
import java.util.concurrent.TimeUnit

/**
 * Every 6 hours: pull the latest channel list, then check that every direct stream of each channel
 * still answers with a live playlist. Demoted sources are probed too, so a primary that works again
 * moves back to the front without the user sitting through a slow zap. A master playlist is followed
 * to its first variant: a static master can answer 200 while its variants are dead.
 * Failures demote the source (see HealthStore) so the next zap goes straight to a working backup.
 */
class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val c = (applicationContext as AileTvApp).container
        c.channels.refresh()
        c.health.awaitLoaded()
        val catalog = c.channels.catalog.value
        val client = c.http.newBuilder().callTimeout(PROBE_TIMEOUT_S, TimeUnit.SECONDS).build()
        val targets = catalog.enabledChannels.flatMap { channel ->
            c.health.ordered(channel, catalog.policy).filter { it.type == "hls" }.map { channel to it }
        }
        val permits = Semaphore(PARALLEL_PROBES)
        val results = coroutineScope {
            targets.map { (channel, source) ->
                async { permits.withPermit { Triple(channel, source, probe(client, source)) } }
            }.awaitAll()
        }
        // Offline, or nothing answered at all: the box's connection is the problem, not the sources.
        val countFailures = c.network.online.value && results.any { it.third }
        for ((channel, source, ok) in results) {
            if (ok) c.health.markOk(channel, source) else if (countFailures) c.health.markFail(channel, source)
        }
        Result.success()
    }

    /** True when [source] answers with a live media playlist (a master is followed to its first variant). */
    private fun probe(client: OkHttpClient, source: ChannelSource): Boolean {
        return try {
            val (master, finalUrl) = fetch(client, source.url, source) ?: return false
            if (!master.contains("#EXTM3U")) return false
            if (!master.contains("#EXT-X-STREAM-INF")) return isLiveMedia(master)
            // Relative variant URIs resolve against the URL after redirects (e.g. dygvideo's 302).
            val variant = firstVariant(master)?.let { finalUrl.resolve(it) } ?: return false
            val (media, _) = fetch(client, variant.toString(), source) ?: return false
            isLiveMedia(media)
        } catch (e: Exception) {
            false
        }
    }

    /** Body and final URL of a successful GET, else null. */
    private fun fetch(client: OkHttpClient, url: String, source: ChannelSource): Pair<String, HttpUrl>? {
        val req = Request.Builder().url(url).header("User-Agent", UserAgents.TV).apply {
            source.headers.forEach { (k, v) -> header(k, v) }
        }.build()
        client.newCall(req).execute().use { resp ->
            // Playlists are a few KB; reading them whole is cheaper than a second request.
            return if (resp.isSuccessful) resp.body.string() to resp.request.url else null
        }
    }

    private fun isLiveMedia(text: String): Boolean =
        text.contains("#EXTM3U") && text.contains("#EXTINF") && !text.contains("#EXT-X-ENDLIST")

    /** The URI line right after the first #EXT-X-STREAM-INF. */
    private fun firstVariant(master: String): String? {
        val lines = master.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        val i = lines.indexOfFirst { it.startsWith("#EXT-X-STREAM-INF") }
        if (i < 0) return null
        return lines.getOrNull(i + 1)?.takeIf { !it.startsWith("#") }
    }

    private companion object {
        const val PROBE_TIMEOUT_S = 10L
        const val PARALLEL_PROBES = 4
    }
}
