package tr.ailetv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guards the bundled channel list: it must parse with the app's own models and keep the agreed decisions. */
class ChannelCatalogTest {
    private val catalog: ChannelCatalog by lazy {
        val file = listOf("src/main/assets/channels.json", "app/src/main/assets/channels.json")
            .map(::File).first { it.exists() }
        AppJson.decodeFromString(ChannelCatalog.serializer(), file.readText())
    }

    @Test
    fun parsesAllChannelsInOrder() {
        val channels = catalog.enabledChannels
        assertEquals(54, channels.size)
        assertEquals((1..54).toList(), channels.map { it.number })
        assertEquals(channels.size, channels.map { it.id }.toSet().size)
    }

    @Test
    fun keepsTheFamilyDecisions() {
        assertTrue(catalog.policy.tokenReading)
        assertEquals("embedded-webview", catalog.policy.youtubePlayback)
        assertFalse(catalog.policy.thirdpartyRestreams)
        assertTrue("Halk TV was removed", catalog.channels.none { it.id == "halktv" })
    }

    @Test
    fun everyChannelHasAPlayableSourceWithoutThirdPartyRestreams() {
        for (channel in catalog.enabledChannels) {
            val usable = channel.sources.filter { it.enabled && !it.isThirdParty }
            assertTrue("${channel.name} has no usable source", usable.isNotEmpty())
            for (s in usable) {
                when (s.type) {
                    "hls", "web", "json-youtube" -> assertTrue("${channel.name}: ${s.type} without url", s.url.startsWith("https://"))
                    "youtube" -> assertTrue("${channel.name}: youtube without channel_id", !s.channelId.isNullOrBlank())
                    "page-token" -> assertTrue("${channel.name}: page-token without pattern", !s.pattern.isNullOrBlank())
                    "api-token" -> assertTrue("${channel.name}: api-token without path", !s.path.isNullOrBlank())
                    else -> throw AssertionError("${channel.name}: unknown source type ${s.type}")
                }
            }
        }
    }

    @Test
    fun thirdPartyRestreamsAreDisabled() {
        val enabledThirdParty = catalog.channels.flatMap { c -> c.sources.filter { it.isThirdParty && it.enabled }.map { c.name } }
        assertTrue("enabled third-party sources: $enabledThirdParty", enabledThirdParty.isEmpty())
    }

    @Test
    fun everyCategoryIsKnown() {
        val ids = catalog.categories.map { it.id }.toSet()
        for (c in catalog.enabledChannels) assertTrue("${c.name}: ${c.category}", c.category in ids)
    }
}
