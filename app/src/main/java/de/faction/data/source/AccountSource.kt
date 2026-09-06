package de.faction.data.source

import de.faction.data.model.ImportCandidate

/**
 * Eine Quelle, aus der der Kader des Spielers in die App gelangt.
 *
 * Bewusst nicht enthalten ist ein Login mit Plarium-Zugangsdaten: es gibt keinen
 * offiziellen Endpunkt dafür, und die Nutzungsbedingungen untersagen sowohl die
 * Weitergabe von Zugangsdaten als auch Drittanbieter-Clients. Sollte Plarium je
 * eine offizielle Anmeldung anbieten, ist sie eine weitere Implementierung dieses
 * Interfaces — der Rest der App bleibt unverändert.
 */
interface AccountSource {
    val id: String
    val label: String
    val description: String

    /** True, wenn die Quelle auf diesem Gerät gerade nutzbar ist. */
    suspend fun isAvailable(): Boolean

    /**
     * Liest den Kader ein. [input] ist quellenspezifisch (Dateiinhalt, Bildpfad, …).
     *
     * Ergebnis sind *Vorschläge*, keine fertigen Kadereinträge: was übernommen wird,
     * entscheidet der Spieler im Bestätigungsschritt.
     */
    suspend fun load(input: SourceInput): Result<List<ImportCandidate>>
}

sealed interface SourceInput {
    /** Inhalt einer vom Spieler ausgewählten Datei. */
    data class FileContent(val json: String) : SourceInput
    /** URI eines Screenshots aus der Galerie oder der Kamera. */
    data class Image(val uri: String) : SourceInput
    data object None : SourceInput
}
