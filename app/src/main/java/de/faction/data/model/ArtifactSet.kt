package de.faction.data.model

/** Grobe Einordnung eines Artefakt-Sets, für die Filterung in der Übersicht. */
enum class SetKind(val label: String) {
    STAT("Grundwert"),
    HYBRID("Doppelwert"),
    OFFENSIVE("Schaden"),
    DEFENSIVE("Überleben"),
    CONTROL("Kontrolle"),
    UTILITY("Sonderfunktion"),
}

/**
 * Ein Artefakt-Set: die Zahl der Teile, die ein Champion gleicher Sorte tragen muss,
 * und die Wirkung, die er dafür erhält.
 *
 * Die Werte sind Fakten über die Spielmechanik — Zahlen, keine Kreativtexte — und
 * damit derselben Kategorie zuzurechnen wie die Basiswerte in [ChampionStats]: nicht
 * Plariums geschütztes Werk, sondern öffentlich dokumentierte Spielregeln. Trotzdem
 * gilt dieselbe Vorsicht wie beim Champion-Katalog: Plarium ändert Set-Werte gelegentlich
 * in Patches. Die Werte hier sind gegen zwei unabhängige, tagesaktuelle Quellen
 * geprüft (Stand: siehe `docs/artefakt-sets-quellen.md`), aber kein Versprechen auf
 * ewige Gültigkeit.
 */
data class ArtifactSet(
    val id: String,
    val name: String,
    val pieces: Int,
    val effect: String,
    val kind: SetKind,
)
