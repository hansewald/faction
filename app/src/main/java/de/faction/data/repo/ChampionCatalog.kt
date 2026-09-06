package de.faction.data.repo

import android.content.Context
import de.faction.data.model.Champion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Stammdaten aller bekannten Champions. Wird beim Start aus den Assets geladen und
 * später durch einen Sync mit der Community-Datenbank ergänzt.
 */
class ChampionCatalog(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private var champions: List<Champion> = emptyList()
    private var byId: Map<String, Champion> = emptyMap()

    suspend fun load() = withContext(Dispatchers.IO) {
        if (champions.isNotEmpty()) return@withContext
        val raw = context.assets.open("champions.json").bufferedReader().use { it.readText() }
        champions = json.decodeFromString(raw)
        byId = champions.associateBy { it.id }
    }

    fun all(): List<Champion> = champions

    fun byId(id: String): Champion? = byId[id]

    fun factions(): List<String> = champions.map { it.faction }.distinct().sorted()

    /** Exakter Namenstreffer, unabhängig von Groß-/Kleinschreibung. */
    fun findByName(name: String): Champion? {
        val needle = name.normalizeName()
        return champions.firstOrNull { it.name.normalizeName() == needle }
    }

    /**
     * Toleranter Namenstreffer für OCR-Ergebnisse. Gibt nur zurück, was ähnlich genug
     * ist — lieber kein Treffer als ein falscher Champion im Kader.
     */
    fun fuzzyFindByName(name: String, minSimilarity: Float = 0.72f): Champion? {
        val needle = name.normalizeName()
        if (needle.length < 3) return null
        return champions
            .map { it to similarity(needle, it.name.normalizeName()) }
            .maxByOrNull { it.second }
            ?.takeIf { it.second >= minSimilarity }
            ?.first
    }

    private fun String.normalizeName(): String = lowercase(Locale.GERMAN)
        .replace("ä", "a").replace("ö", "o").replace("ü", "u").replace("ß", "ss")
        .filter { it.isLetterOrDigit() }

    private fun similarity(a: String, b: String): Float {
        val distance = levenshtein(a, b)
        val longest = max(a.length, b.length)
        return if (longest == 0) 1f else 1f - distance.toFloat() / longest
    }

    private fun levenshtein(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val current = IntArray(b.length + 1)
            current[0] = i
            for (j in 1..b.length) {
                val substitution = previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = min(min(current[j - 1] + 1, previous[j] + 1), substitution)
            }
            previous = current
        }
        return previous[b.length]
    }
}
