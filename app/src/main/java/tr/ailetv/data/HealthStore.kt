package tr.ailetv.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.util.concurrent.ConcurrentHashMap

/**
 * Remembers which sources failed recently so a broken primary moves behind its backups.
 * A source with 3+ failures since its last success is tried last for [DEMOTE_TTL_MS] after its latest
 * failure; then it gets one real try again (another failure demotes it for another period). One success resets it.
 */
class HealthStore(private val prefs: Prefs, private val scope: CoroutineScope) {
    /** [lastFailAt] is wall-clock time, because it is persisted across reboots. */
    @Serializable
    private data class Health(val count: Int, val lastFailAt: Long)

    private val failures = ConcurrentHashMap<String, Health>()
    private val writeLock = Mutex()

    private val loaded: Job = scope.launch(Dispatchers.IO) {
        prefs.readHealth()?.let { json ->
            // The old count-only format does not decode; starting fresh is fine.
            runCatching { AppJson.decodeFromString(serializer, json) }
                .getOrNull()?.forEach { (k, v) -> failures.putIfAbsent(k, v) }
        }
    }

    /** Waits for the stored state (a worker in a fresh process must not judge sources before it is in). */
    suspend fun awaitLoaded() = loaded.join()

    fun failureCount(channel: Channel, source: ChannelSource): Int = failures[key(channel, source)]?.count ?: 0

    fun markOk(channel: Channel, source: ChannelSource) {
        if (failures.remove(key(channel, source)) != null) persist()
    }

    fun markFail(channel: Channel, source: ChannelSource) {
        val now = System.currentTimeMillis()
        failures.merge(key(channel, source), Health(1, now)) { old, _ -> Health((old.count + 1).coerceAtMost(99), now) }
        persist()
    }

    /** Sources the app may try for [channel], in the order to try them. A disabled channel has none. */
    fun ordered(channel: Channel, policy: Policy): List<ChannelSource> {
        if (!channel.enabled) return emptyList()
        val allowed = channel.sources.filter { s ->
            s.enabled &&
                (policy.thirdpartyRestreams || !s.isThirdParty) &&
                (policy.tokenReading || !s.isTokenReader) &&
                (policy.youtubePlayback != "off" || !s.isYouTube)
        }
        // Stable: keeps the curated order, only pushes repeatedly failing sources to the back.
        val now = System.currentTimeMillis()
        return allowed.sortedBy { if (isDemoted(channel, it, now)) 1 else 0 }
    }

    private fun isDemoted(channel: Channel, source: ChannelSource, now: Long): Boolean {
        val h = failures[key(channel, source)] ?: return false
        // A clock set backwards (negative age) counts as expired: better one slow zap than a stuck demotion.
        return h.count >= DEMOTE_AFTER && now - h.lastFailAt in 0 until DEMOTE_TTL_MS
    }

    private fun key(channel: Channel, source: ChannelSource) = "${channel.id}|${source.key}"

    private fun persist() {
        scope.launch(Dispatchers.IO) {
            loaded.join() // never overwrite the stored state with a map that has not been loaded yet
            // Snapshot under the lock, so the last write always carries the newest state.
            writeLock.withLock { prefs.writeHealth(AppJson.encodeToString(serializer, HashMap(failures))) }
        }
    }

    companion object {
        const val DEMOTE_AFTER = 3
        const val DEMOTE_TTL_MS = 6 * 60 * 60 * 1000L
        private val serializer = MapSerializer(String.serializer(), Health.serializer())
    }
}
