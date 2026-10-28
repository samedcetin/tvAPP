package tr.ailetv.resolve

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import tr.ailetv.data.AppJson
import tr.ailetv.data.ChannelSource
import tr.ailetv.net.UserAgents
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resumeWithException

/** What a channel source turns into right before playback. */
sealed interface Resolved {
    data class Hls(
        val url: String,
        val headers: Map<String, String>,
        val liveOffsetMs: Long?,
        /** Token expiry (epoch ms) when the URL carries one, else null. */
        val expiresAtMs: Long?,
    ) : Resolved

    /** [videoId] null = let YouTube pick the channel's current live stream (embed/live_stream?channel=). */
    data class YouTube(val videoId: String?, val channelId: String?) : Resolved

    data class Web(val url: String) : Resolved
}

class SourceUnavailable(message: String) : IOException(message)

/**
 * Turns a [ChannelSource] into something playable. Token sources read the token the broadcaster's
 * own page/API hands every visitor (the same request their web player makes); nothing is generated.
 * Requests are cancelled with the calling coroutine, so the engine's resolve timeout really ends them.
 */
class SourceResolver(private val http: OkHttpClient) {
    private val tokenCache = ConcurrentHashMap<String, Resolved.Hls>()

    suspend fun resolve(source: ChannelSource, forceRefresh: Boolean = false): Resolved = withContext(Dispatchers.IO) {
        when (source.type) {
            "hls" -> Resolved.Hls(source.url, source.headers, source.liveOffsetMs, null)
            "page-token" -> cachedToken(source, forceRefresh) { pageToken(source) }
            "api-token" -> cachedToken(source, forceRefresh) { apiToken(source) }
            "youtube" -> youtube(source)
            "json-youtube" -> jsonYoutube(source)
            "web" -> Resolved.Web(source.url)
            else -> throw SourceUnavailable("unsupported source type ${source.type}")
        }
    }

    private inline fun cachedToken(source: ChannelSource, force: Boolean, fetch: () -> Resolved.Hls): Resolved.Hls {
        val now = System.currentTimeMillis()
        val cached = tokenCache[source.key]
        if (!force && cached != null && (cached.expiresAtMs ?: 0L) - now > TOKEN_MIN_REMAINING_MS) return cached
        return fetch().also { tokenCache[source.key] = it }
    }

    private suspend fun pageToken(source: ChannelSource): Resolved.Hls {
        val html = get(source.url, UserAgents.DESKTOP, source.requestHeaders)
        val regex = Regex(source.pattern ?: throw SourceUnavailable("page-token without pattern"))
        val raw = regex.find(html)?.value ?: throw SourceUnavailable("token url not found on page")
        val url = raw.replace("\\/", "/").replace("&amp;", "&")
        return Resolved.Hls(url, source.headers, source.liveOffsetMs, expiry(url))
    }

    private suspend fun apiToken(source: ChannelSource): Resolved.Hls {
        // Some token services only answer requests that come from their own site (Turkuvaz checks the Referer).
        val json = AppJson.parseToJsonElement(get(source.url, UserAgents.DESKTOP, source.requestHeaders))
        val expr = source.path ?: throw SourceUnavailable("api-token without path")
        val url = expr.split("+").joinToString("") { term ->
            val t = term.trim()
            if (t.startsWith("\"") && t.endsWith("\"")) t.trim('"') else lookup(json, t) ?: throw SourceUnavailable("missing $t")
        }
        if (!url.startsWith("http")) throw SourceUnavailable("api returned no url")
        return Resolved.Hls(url, source.headers, source.liveOffsetMs, expiry(url))
    }

    private suspend fun youtube(source: ChannelSource): Resolved.YouTube {
        val channelId = source.channelId ?: throw SourceUnavailable("youtube without channel_id")
        val html = try {
            get("https://www.youtube.com/channel/$channelId/live", UserAgents.DESKTOP, youtubeHeaders)
        } catch (e: IOException) {
            // Page unreachable: let the embed resolve the live stream itself.
            return Resolved.YouTube(null, channelId)
        }
        val videoId = CANONICAL.find(html)?.groupValues?.get(1)
        val isLive = html.contains("\"isLiveNow\":true")
        if (videoId == null || !isLive) throw SourceUnavailable("channel is not live on YouTube")
        return Resolved.YouTube(videoId, channelId)
    }

    private suspend fun jsonYoutube(source: ChannelSource): Resolved.YouTube {
        val json = AppJson.parseToJsonElement(get(source.url, UserAgents.DESKTOP))
        val liveUrl = lookup(json, "live_url") ?: throw SourceUnavailable("live.json has no live_url")
        val id = Uri.parse(liveUrl).getQueryParameter("v")
            ?: liveUrl.substringAfterLast('/').takeIf { it.length == 11 }
            ?: throw SourceUnavailable("live_url is not a YouTube video")
        // The JSON can lag behind and point at a finished broadcast (a VOD that ends on YouTube's end
        // screen). Only a live video counts; otherwise the chain falls through to the channel's /live source.
        val watch = get("https://www.youtube.com/watch?v=$id", UserAgents.DESKTOP, youtubeHeaders)
        if (!watch.contains("\"isLiveNow\":true")) throw SourceUnavailable("live.json video is not live")
        return Resolved.YouTube(id, null)
    }

    private suspend fun get(url: String, ua: String, extra: Map<String, String> = emptyMap()): String {
        val builder = Request.Builder().url(url).header("User-Agent", ua).header("Accept-Language", "tr-TR,tr;q=0.9")
        extra.forEach { (k, v) -> builder.header(k, v) }
        val call = http.newCall(builder.build())
        // Async + cancel: a blocking execute() would hold the caller (and its timeout) until the socket gave up.
        return suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) = cont.resumeWithException(e)

                override fun onResponse(call: Call, response: Response) = response.use { r ->
                    cont.resumeWith(
                        runCatching {
                            if (!r.isSuccessful) throw SourceUnavailable("HTTP ${r.code} for $url")
                            r.body.string()
                        },
                    )
                }
            })
        }
    }

    private fun lookup(root: JsonElement, dotted: String): String? {
        var cur: JsonElement? = root
        for (part in dotted.split('.')) cur = (cur as? JsonObject)?.get(part) ?: return null
        return (cur as? JsonPrimitive)?.contentOrNull
    }

    /** Reads `e=<unix seconds>` from a tokenised URL. */
    private fun expiry(url: String): Long? =
        Regex("[?&]e=(\\d{9,11})").find(url)?.groupValues?.get(1)?.toLongOrNull()?.times(1000)

    private companion object {
        const val TOKEN_MIN_REMAINING_MS = 10 * 60 * 1000L
        val CANONICAL = Regex("<link rel=\"canonical\" href=\"https://www\\.youtube\\.com/watch\\?v=([A-Za-z0-9_-]{11})\"")
        val youtubeHeaders = mapOf("Cookie" to "CONSENT=YES+1; SOCS=CAI")
    }
}
