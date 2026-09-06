package de.faction.data.repo

import de.faction.data.model.Champion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ein Katalogstand, wie ihn der Sync erwartet.
 *
 * Die Version ist eine laufende Zahl, keine Zeitangabe: sie entscheidet, ob ein
 * Download neuer ist als der lokale Bestand, ohne dass Uhren übereinstimmen müssen.
 */
@Serializable
data class CatalogFeed(
    val version: Int,
    /** Frei wählbar, wird nur angezeigt — etwa "2026-09-07" oder "Patch 9.10". */
    @SerialName("released") val released: String = "",
    val champions: List<Champion>,
)

/** Was ein Abgleich ergeben hat. */
sealed interface CatalogUpdate {
    /** Der lokale Bestand ist bereits der neueste. */
    data class UpToDate(val version: Int) : CatalogUpdate

    /** Ein neuer Stand wurde übernommen. */
    data class Applied(
        val version: Int,
        val released: String,
        /** Namen der Legenden, die vorher nicht im Katalog standen. */
        val addedChampions: List<String>,
        val totalChampions: Int,
    ) : CatalogUpdate

    data class Failed(val reason: String) : CatalogUpdate
}
