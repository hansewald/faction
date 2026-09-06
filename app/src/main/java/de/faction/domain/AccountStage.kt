package de.faction.domain

import de.faction.data.model.RosterEntry

/**
 * Fortschrittsphase des Accounts. Sie verschiebt die Empfehlungen: früh zählt
 * Farmgeschwindigkeit, später zählen Spezialisten für einzelne Inhalte.
 */
enum class AccountStage(val label: String, val description: String) {
    EARLY(
        "Früh",
        "Kampagne und erste Dungeons. Ziel: ein Team, das Brutal-Kampagne im Auto farmt.",
    ),
    MID(
        "Mittel",
        "Dungeons auf Stufe 20 und ein erstes Clanboss-Team. Ziel: verlässliche Ausrüstungsquellen.",
    ),
    LATE(
        "Fortgeschritten",
        "Spezialisten für einzelne Inhalte lohnen sich. Ressourcen gezielt statt breit einsetzen.",
    );

    companion object {
        fun detect(roster: List<RosterEntry>): AccountStage {
            val sixStars = roster.count { it.owned.rank >= 6 }
            val level50s = roster.count { it.owned.level >= 50 }
            return when {
                sixStars >= 6 || level50s >= 12 -> LATE
                sixStars >= 1 || level50s >= 3 -> MID
                else -> EARLY
            }
        }
    }
}
