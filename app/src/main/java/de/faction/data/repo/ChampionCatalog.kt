package de.faction.data.repo

import android.content.Context
import de.faction.data.model.Champion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Stammdaten aller bekannten Champions. Wird beim Start aus den Assets geladen und
 * später durch einen Sync mit der Community-Datenbank ergänzt.
 */
/** Ein Namenstreffer mit seiner Schärfe zwischen 0 und 1. */
data class NameMatch(val champion: Champion, val similarity: Float)

class ChampionCatalog(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private var champions: List<Champion> = emptyList()
    private var byId: Map<String, Champion> = emptyMap()

    /**
     * Lädt den Katalog: bevorzugt den heruntergeladenen Stand, sonst die mitgelieferte
     * Datei. Ist der Download unlesbar, fällt die App still auf die Assets zurück —
     * lieber ein älterer Katalog als gar keiner.
     */
    suspend fun load() = withContext(Dispatchers.IO) {
        if (champions.isNotEmpty()) return@withContext
        val downloaded = File(context.filesDir, CatalogUpdater.FILE_NAME)
            .takeIf { it.isFile }
            ?.let { file -> runCatching { parseFeed(file.readText()) }.getOrNull() }
        publish(downloaded ?: parseSeed())
    }

    /** Übernimmt einen frisch geladenen Stand, ohne die App neu zu starten. */
    fun replaceWith(updated: List<Champion>) = publish(updated)

    private fun parseSeed(): List<Champion> =
        context.assets.open("champions.json").bufferedReader().use { json.decodeFromString(it.readText()) }

    /**
     * Der Feed trägt eine Version, die mitgelieferte Datei ist eine nackte Liste.
     * Beides wird gelesen — die Seed-Datei soll nicht umgeschrieben werden müssen.
     */
    private fun parseFeed(raw: String): List<Champion> =
        runCatching { json.decodeFromString<CatalogFeed>(raw).champions }
            .getOrElse { json.decodeFromString<List<Champion>>(raw) }

    private fun publish(list: List<Champion>) {
        champions = list
        byId = list.associateBy { it.id }
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
     * Bester Namenstreffer samt Trefferschärfe, für OCR-Ergebnisse.
     *
     * Die Schärfe wird mitgegeben statt verworfen: der Bestätigungsschritt entscheidet
     * damit, welche Vorschläge vorausgewählt werden und welche der Spieler prüfen muss.
     */
    fun matchByName(name: String, minSimilarity: Float = 0.6f): NameMatch? {
        val needle = name.normalizeName()
        if (needle.length < 3) return null
        return champions
            .map { NameMatch(it, similarity(needle, it.name.normalizeName())) }
            .maxByOrNull { it.similarity }
            ?.takeIf { it.similarity >= minSimilarity }
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
