package de.faction.data.source

import de.faction.data.model.ImportCandidate
import de.faction.data.model.ImportSource
import de.faction.data.repo.ChampionCatalog
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Importiert einen Export des Raid Toolkits (Windows), das die Daten aus der
 * laufenden Spielsitzung des Spielers liest. Die App selbst spricht nie mit
 * Plarium-Servern; sie liest nur die Datei, die der Spieler ihr gibt.
 */
class ToolkitJsonSource(private val catalog: ChampionCatalog) : AccountSource {

    override val id = "toolkit-json"
    override val label = "Toolkit-Export (JSON)"
    override val description =
        "Datei, die du am PC mit dem Raid Toolkit exportiert hast. Vollständigste Daten."

    override suspend fun isAvailable() = true

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Serializable
    private data class ToolkitExport(val champions: List<ToolkitChampion> = emptyList())

    @Serializable
    private data class ToolkitChampion(
        val id: Long? = null,
        val name: String = "",
        val level: Int = 1,
        val rank: Int = 1,
        @SerialName("ascendLevel") val ascendLevel: Int = 0,
        @SerialName("awakenLevel") val awakenLevel: Int = 0,
        val power: Int? = null,
        val locked: Boolean = false,
    )

    override suspend fun load(input: SourceInput): Result<List<ImportCandidate>> {
        val content = (input as? SourceInput.FileContent)?.json
            ?: return Result.failure(IllegalArgumentException("Für diesen Import wird eine JSON-Datei benötigt."))

        return runCatching {
            val export = json.decodeFromString<ToolkitExport>(content)
            if (export.champions.isEmpty()) {
                error("Die Datei enthält keine Champions. Stammt sie wirklich aus dem Toolkit-Export?")
            }
            export.champions.map { raw ->
                // Der Export nennt exakte Namen; ein fehlender Treffer heisst, dass die
                // Legende im Katalog fehlt - dann bleibt der Vorschlag unzugeordnet.
                val champion = catalog.findByName(raw.name)
                ImportCandidate(
                    rawLabel = raw.name,
                    championId = champion?.id,
                    level = raw.level.coerceIn(1, 60),
                    ascension = raw.ascendLevel.coerceIn(0, 6),
                    rank = raw.rank.coerceIn(1, 6),
                    confidence = if (champion != null) 1f else 0f,
                    source = ImportSource.TOOLKIT_JSON,
                )
            }
        }
    }
}
