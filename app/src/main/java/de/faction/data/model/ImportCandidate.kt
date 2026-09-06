package de.faction.data.model

/**
 * Ein Vorschlag aus einem Import, bevor er in den Kader übernommen wird.
 *
 * Der Zwischenschritt existiert, weil eine falsch erkannte Legende die gesamte
 * Analyse verfälscht: sie zählt als abgedeckte Wirkung, verschiebt die
 * Lückenberechnung und kann so eine falsche Aufbau-Empfehlung erzeugen. Lieber
 * einmal bestätigen als stillschweigend danebenliegen.
 */
data class ImportCandidate(
    /** Der Text, aus dem der Vorschlag stammt — bei OCR die erkannte Zeile. */
    val rawLabel: String,
    /** Die zugeordnete Legende. `null`, solange keine Zuordnung gelungen ist. */
    val championId: String?,
    val level: Int,
    val rank: Int,
    val ascension: Int = 0,
    /** 0..1. Unter [RELIABLE] ist der Vorschlag nicht vorausgewählt. */
    val confidence: Float,
    val source: ImportSource,
    /**
     * Die aus dem Screenshot ausgeschnittene Kachel, noch nicht übernommen. Sie ist im
     * Bestätigungsschritt das Erkennungsmerkmal, wenn die Ansicht keine Namen zeigt.
     */
    val portraitPath: String? = null,
    val accepted: Boolean = championId != null && confidence >= RELIABLE,
) {
    /** Nur zugeordnete Vorschläge lassen sich übernehmen. */
    val isResolved: Boolean get() = championId != null

    fun toOwnedChampion(): OwnedChampion? = championId?.let {
        OwnedChampion(
            championId = it,
            level = level.coerceIn(1, 60),
            ascension = ascension.coerceIn(0, 6),
            rank = rank.coerceIn(1, 6),
            source = source,
        )
    }

    companion object {
        /** Ab dieser Trefferschärfe wird ein Vorschlag vorausgewählt. */
        const val RELIABLE = 0.85f
    }
}
