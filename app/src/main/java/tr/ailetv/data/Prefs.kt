package tr.ailetv.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import java.io.IOException

// A corrupt file is replaced by empty settings: defaults beat a crash on every launch.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "aile_tv",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

data class Settings(
    /** null = never edited, use the catalog's default favourites. Order matters (zapping order). */
    val favorites: List<String>? = null,
    val lastChannelId: String? = null,
    val activeFilter: String = CategoryIds.ALL,
    val textScale: Float = 1f,
    val autoStartLastChannel: Boolean = true,
    /** true: ▲ goes to the next (higher-numbered) channel. */
    val upIsNext: Boolean = true,
    val bannerSeconds: Int = 5,
    val guideSeen: Boolean = false,
    val match50Hz: Boolean = true,
    val loaded: Boolean = false,
)

/** App settings in DataStore. Read and write errors (unreadable file, full storage) fall back to defaults / are logged. */
class Prefs(private val context: Context, scope: CoroutineScope) {
    private object K {
        val favorites = stringPreferencesKey("favorites")
        val lastChannel = stringPreferencesKey("last_channel")
        val filter = stringPreferencesKey("filter")
        val textScale = floatPreferencesKey("text_scale")
        val autoStart = booleanPreferencesKey("auto_start")
        val upIsNext = booleanPreferencesKey("up_is_next")
        val bannerSeconds = intPreferencesKey("banner_seconds")
        val guideSeen = booleanPreferencesKey("guide_seen")
        val match50Hz = booleanPreferencesKey("match_50hz")
        val health = stringPreferencesKey("source_health")
    }

    /** Set once a real read came through; a later read error then keeps those values instead of defaults. */
    @Volatile private var readOnce = false

    val settings: StateFlow<Settings> = context.dataStore.data
        .onEach { readOnce = true }
        .retryWhen { e, attempt ->
            // Unreadable file: run on defaults rather than kill the app at startup, and keep reading. A
            // `catch` would end the flow here and freeze every setting at its default until the process dies.
            if (e !is IOException) return@retryWhen false
            Log.w(TAG, "settings read failed (attempt $attempt)", e)
            if (attempt == 0L && !readOnce) emit(emptyPreferences()) // the splash gets loaded=true defaults at once
            delay(minOf(30_000L, 1_000L shl attempt.coerceAtMost(5L).toInt()))
            true // subscribe again: the real values, and every later edit, flow once a read succeeds
        }
        .map { p ->
            Settings(
                favorites = p[K.favorites]?.let { s -> if (s.isEmpty()) emptyList() else s.split(',') },
                lastChannelId = p[K.lastChannel],
                activeFilter = p[K.filter] ?: CategoryIds.ALL,
                textScale = (p[K.textScale] ?: 1f).coerceIn(1f, 1.5f),
                autoStartLastChannel = p[K.autoStart] ?: true,
                upIsNext = p[K.upIsNext] ?: true,
                bannerSeconds = p[K.bannerSeconds] ?: 5,
                guideSeen = p[K.guideSeen] ?: false,
                match50Hz = p[K.match50Hz] ?: true,
                loaded = true,
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, Settings())

    suspend fun setFavorites(ids: List<String>) = safeEdit { it[K.favorites] = ids.joinToString(",") }
    suspend fun setLastChannel(id: String) = safeEdit { it[K.lastChannel] = id }
    suspend fun setFilter(id: String) = safeEdit { it[K.filter] = id }
    suspend fun setTextScale(v: Float) = safeEdit { it[K.textScale] = v }
    suspend fun setAutoStart(v: Boolean) = safeEdit { it[K.autoStart] = v }
    suspend fun setUpIsNext(v: Boolean) = safeEdit { it[K.upIsNext] = v }
    suspend fun setBannerSeconds(v: Int) = safeEdit { it[K.bannerSeconds] = v }
    suspend fun setGuideSeen(v: Boolean) = safeEdit { it[K.guideSeen] = v }
    suspend fun setMatch50Hz(v: Boolean) = safeEdit { it[K.match50Hz] = v }

    suspend fun readHealth(): String? = try {
        context.dataStore.data.first()[K.health]
    } catch (e: IOException) {
        Log.w(TAG, "source health read failed", e)
        null
    }

    suspend fun writeHealth(json: String) = safeEdit { it[K.health] = json }

    /** A failed write (e.g. storage full) loses that one change; it must not crash a zap. */
    private suspend fun safeEdit(block: (MutablePreferences) -> Unit) {
        try {
            context.dataStore.edit { block(it) }
        } catch (e: IOException) {
            Log.w(TAG, "settings write failed", e)
        }
    }

    private companion object {
        const val TAG = "Prefs"
    }
}
