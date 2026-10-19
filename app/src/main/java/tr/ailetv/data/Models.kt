package tr.ailetv.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Mirrors channels/channels.json. Unknown fields are ignored so the remote list can grow. */
@Serializable
data class ChannelCatalog(
    val version: Int = 1,
    @SerialName("updated_at") val updatedAt: String = "",
    val policy: Policy = Policy(),
    val categories: List<Category> = emptyList(),
    val channels: List<Channel> = emptyList(),
) {
    val enabledChannels: List<Channel> get() = channels.filter { it.enabled }.sortedBy { it.number }
    fun channel(id: String?): Channel? = channels.firstOrNull { it.id == id }
    fun categoryLabel(id: String): String = categories.firstOrNull { it.id == id }?.label ?: id
}

@Serializable
data class Policy(
    @SerialName("token_reading") val tokenReading: Boolean = true,
    @SerialName("youtube_playback") val youtubePlayback: String = "embedded-webview",
    @SerialName("thirdparty_restreams") val thirdpartyRestreams: Boolean = false,
)

@Serializable
data class Category(val id: String, val label: String)

@Serializable
data class Channel(
    val id: String,
    val number: Int,
    val name: String,
    val category: String,
    @SerialName("default_favorite") val defaultFavorite: Boolean = false,
    val enabled: Boolean = true,
    val sources: List<ChannelSource> = emptyList(),
)

@Serializable
data class ChannelSource(
    /** hls | page-token | api-token | youtube | json-youtube | web */
    val type: String,
    val url: String = "",
    val enabled: Boolean = true,
    val origin: String = "",
    val tier: Int = 1,
    val max: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val note: String? = null,
    val handle: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    /** page-token: regex that finds the tokenised URL in the official page. */
    val pattern: String? = null,
    /** api-token: "A.B.C + X.Y" dotted JSON paths concatenated. */
    val path: String? = null,
    @SerialName("live_offset_ms") val liveOffsetMs: Long? = null,
) {
    val key: String get() = "$type|$url"
    val isThirdParty: Boolean get() = origin == "thirdparty-restream"
    val isTokenReader: Boolean get() = type == "page-token" || type == "api-token"
    val isYouTube: Boolean get() = type == "youtube" || type == "json-youtube"
}

object CategoryIds {
    const val FAVORITES = "favoriler"
    const val ALL = "tumu"
}

val AppJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
}
