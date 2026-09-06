package de.faction.data.model

/** Herkunft der Kaderdaten. Bestimmt, wie sehr wir den Werten trauen. */
enum class ImportSource(val label: String, val confidence: Float) {
    MANUAL("Manuell erfasst", 1.0f),
    TOOLKIT_JSON("Toolkit-Export", 1.0f),
    SCREENSHOT_OCR("Screenshot-Erkennung", 0.75f);
}

/** Eine Instanz im Kader des Spielers. Mehrere Instanzen je [championId] sind Duplikate. */
data class OwnedChampion(
    val instanceId: Long = 0,
    val championId: String,
    val level: Int,
    val ascension: Int,
    /** Aktuelle Sternestufe (Rang) 1..6. */
    val rank: Int,
    val awakeningLevel: Int = 0,
    val power: Int? = null,
    val locked: Boolean = false,
    val source: ImportSource = ImportSource.MANUAL,
) {
    val isMaxLevel: Boolean get() = level >= rank * 10
}

/** Champion-Stammdaten zusammen mit der Instanz des Spielers. */
data class RosterEntry(
    val owned: OwnedChampion,
    val champion: Champion,
)
