package de.faction.data.repo

import android.content.Context
import de.faction.data.model.Champion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Hält den Champion-Katalog aktuell.
 *
 * Neue Legenden erscheinen im Spiel laufend; eine App, die ihren Katalog nur beim
 * Erscheinen mitliefert, veraltet zwischen zwei Store-Updates. Der Sync lädt deshalb
 * einen versionierten Feed und legt ihn neben die mitgelieferte Datei. Ist er neuer,
 * wird künftig er geladen.
 *
 * **Die Feed-Adresse muss auf einen Bestand zeigen, über den du verfügen darfst** —
 * eine Datei in deinem eigenen Repository oder Speicher, erzeugt mit
 * `tools/generate_champions.py`. Fremde Seiten liefern ihre Inhalte nicht zur
 * Weiterverbreitung; sie hier einzutragen, verlagert das Problem nur auf den Server.
 *
 * Der Download ersetzt nie ungeprüft: eine leere oder unlesbare Antwort lässt den
 * bisherigen Bestand unangetastet, und eine ältere Version wird verworfen.
 */
class CatalogUpdater(
    private val context: Context,
    private val catalog: ChampionCatalog,
    private val feedUrl: String = DEFAULT_FEED_URL,
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun check(): CatalogUpdate = withContext(Dispatchers.IO) {
        if (feedUrl.isBlank()) {
            return@withContext CatalogUpdate.Failed(
                "Es ist keine Quelle eingetragen, aus der der Katalog geladen werden kann.",
            )
        }

        val body = runCatching { download(feedUrl) }.getOrElse { error ->
            return@withContext CatalogUpdate.Failed(
                error.message ?: "Der Katalog konnte nicht geladen werden.",
            )
        }

        val feed = runCatching { json.decodeFromString<CatalogFeed>(body) }.getOrElse {
            return@withContext CatalogUpdate.Failed("Die Antwort ist kein gültiger Katalog.")
        }

        if (feed.champions.isEmpty()) {
            return@withContext CatalogUpdate.Failed("Der geladene Katalog ist leer.")
        }
        if (feed.version <= localVersion()) {
            return@withContext CatalogUpdate.UpToDate(localVersion())
        }

        val known = catalog.all().map { it.id }.toSet()
        val added = feed.champions.filterNot { it.id in known }

        store(body, feed.version)
        catalog.replaceWith(feed.champions)

        CatalogUpdate.Applied(
            version = feed.version,
            released = feed.released,
            addedChampions = added.map(Champion::name).sorted(),
            totalChampions = feed.champions.size,
        )
    }

    /** Die Version des lokalen Bestands. 0, solange nur die mitgelieferte Datei vorliegt. */
    fun localVersion(): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_VERSION, 0)

    private fun download(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            requestMethod = "GET"
        }
        try {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Die Quelle antwortete mit ${connection.responseCode}.")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun store(body: String, version: Int) {
        File(context.filesDir, FILE_NAME).writeText(body)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_VERSION, version)
            .apply()
    }

    companion object {
        /**
         * Adresse des Katalog-Feeds. Leer, solange keine eingetragen ist — dann meldet
         * der Sync das offen, statt im Hintergrund nichts zu tun.
         */
        const val DEFAULT_FEED_URL = ""

        const val FILE_NAME = "champions.json"
        private const val PREFS = "catalog"
        private const val KEY_VERSION = "version"
        private const val TIMEOUT_MS = 15_000
    }
}
