package de.faction.domain

import de.faction.data.model.Area
import de.faction.data.model.Champion
import de.faction.data.model.Masteries
import de.faction.data.model.Mastery
import de.faction.data.model.MasteryTree
import de.faction.data.model.Role
import de.faction.data.model.Utility

/**
 * Der empfohlene Lernpfad durch den Meisterschaftsbaum: je Stufe eine Meisterschaft,
 * dazu die Begründung.
 */
data class MasteryPath(
    val tree: MasteryTree,
    /** Der zweite Baum, in den die verbleibenden Punkte fließen. */
    val secondary: MasteryTree,
    /** Genau eine Meisterschaft je Stufe 1..6. */
    val picks: List<Mastery>,
    val reasons: List<String>,
) {
    fun isRecommended(mastery: Mastery): Boolean = mastery in picks
}

/**
 * Wählt den Meisterschaftspfad aus dem Kit — nach denselben Regeln wie der übrige
 * Aufbauplan, nicht nach fremden Empfehlungen.
 *
 * Die Auswahl folgt der Frage, woraus dieser Champion seinen Beitrag zieht:
 * Wer Schwächungen setzt, braucht Genauigkeit und Dauer, sonst greift nichts.
 * Wer heilt oder schützt, muss den Kampf überstehen und früh dran sein.
 * Wer Schaden macht, nimmt den Schadensweg — welchen, hängt am Bereich: gegen den
 * Clanboss zählt der Zusatzschaden aus den Max-LP, in der Arena der Burst.
 *
 * Empfohlen wird bewusst genau eine Meisterschaft je Stufe. Ein Baum bietet mehr,
 * aber die Punkte reichen nicht für alles, und ein Zurücksetzen kostet echte
 * Ressourcen.
 */
object MasteryPlanner {

    private val debuffs = setOf(
        Utility.DECREASE_DEFENSE, Utility.DECREASE_ATTACK, Utility.DECREASE_SPEED,
        Utility.DECREASE_ACCURACY, Utility.DECREASE_CRIT_RATE, Utility.DECREASE_CRIT_DAMAGE,
        Utility.WEAKEN, Utility.HEAL_REDUCTION, Utility.BLOCK_BUFFS, Utility.BLOCK_COOLDOWN,
        Utility.BLOCK_REVIVE, Utility.DECREASE_MAX_HP, Utility.POISON, Utility.HP_BURN,
        Utility.STUN, Utility.SLEEP, Utility.FEAR, Utility.PROVOKE,
    )

    private val sustain = setOf(
        Utility.HEAL, Utility.CONTINUOUS_HEAL, Utility.REVIVE, Utility.SHIELD,
        Utility.CLEANSE, Utility.BLOCK_DEBUFFS, Utility.BLOCK_DAMAGE, Utility.UNKILLABLE,
        Utility.ALLY_PROTECTION, Utility.VEIL,
    )

    fun plan(champion: Champion, area: Area): MasteryPath {
        val setsDebuffs = champion.utilities.any { it in debuffs }
        val supports = champion.utilities.any { it in sustain }
        val overTime = champion.utilities.any { it == Utility.POISON || it == Utility.HP_BURN }

        return when {
            supports && !setsDebuffs -> supportPath(champion)
            setsDebuffs && (supports || champion.role != Role.ATTACK) -> hexerPath(champion, overTime)
            setsDebuffs -> offensiveHexerPath(champion, area)
            else -> damagePath(champion, area)
        }
    }

    /** Heiler und Schutzgeber: erst überleben, dann früh dran sein. */
    private fun supportPath(champion: Champion) = path(
        tree = MasteryTree.SUPPORT,
        secondary = MasteryTree.DEFENSE,
        names = listOf(
            "Steadfast", "Lay on Hands", "Healing Savior", "Merciful Aid",
            "Lasting Gifts", "Timely Intervention",
        ),
        reasons = listOf(
            "${champion.name} hält das Team am Leben — der Unterstützungsbaum verstärkt genau das.",
            "Timely Intervention zieht den Zug vor, wenn ein Verbündeter einbricht. Genau dann zählt eine Heilung.",
            "Die restlichen Punkte gehen defensiv: Ein Heiler, der stirbt, heilt nicht.",
        ),
    )

    /** Wer Schwächungen setzt, braucht Genauigkeit und Dauer — sonst greift nichts davon. */
    private fun hexerPath(champion: Champion, overTime: Boolean) = path(
        tree = MasteryTree.SUPPORT,
        secondary = MasteryTree.DEFENSE,
        names = listOf(
            "Pinpoint Accuracy", "Charged Focus", "Swarm Smiter",
            if (overTime) "Cycle of Magic" else "Evil Eye",
            "Master Hexer", "Eagle Eye",
        ),
        reasons = listOf(
            "${champion.name} lebt von Schwächungen. Ohne Genauigkeit prallen sie am Widerstand ab, deshalb führt der Weg über Eagle Eye.",
            "Master Hexer verlängert die Schwächungen — mehr Wirkung aus denselben Zügen.",
            if (overTime) {
                "Cycle of Magic bringt die Fähigkeit früher zurück; bei Schaden über Zeit zählt jede zusätzliche Anwendung."
            } else {
                "Evil Eye nimmt dem Ziel Zugleiste, schon beim Standardangriff."
            },
        ),
    )

    /** Angreifer, die nebenbei schwächen: Schaden zuerst, Genauigkeit aus dem zweiten Baum. */
    private fun offensiveHexerPath(champion: Champion, area: Area) = path(
        tree = MasteryTree.OFFENSE,
        secondary = MasteryTree.SUPPORT,
        names = listOf(
            "Deadly Precision", "Keen Strike", "Single Out", "Bring It Down",
            "Kill Streak", finisher(area),
        ),
        reasons = listOf(
            "${champion.name} macht Schaden und schwächt nebenbei — der offensive Baum wiegt schwerer.",
            "${finisher(area)} ist der Abschluss für ${area.label}.",
            "Die Punkte im zweiten Baum gehen in Genauigkeit, damit die Schwächungen überhaupt greifen.",
        ),
    )

    /** Reine Schadensträger. */
    private fun damagePath(champion: Champion, area: Area) = path(
        tree = MasteryTree.OFFENSE,
        secondary = if (area == Area.ARENA) MasteryTree.SUPPORT else MasteryTree.DEFENSE,
        names = listOf(
            "Deadly Precision", "Keen Strike", "Single Out", "Bring It Down",
            "Kill Streak", finisher(area),
        ),
        reasons = listOf(
            "${champion.name} trägt über Schaden bei — der offensive Baum bis zur letzten Stufe.",
            "${finisher(area)} ist der Abschluss für ${area.label}.",
            if (area == Area.ARENA) {
                "Der zweite Baum geht in Unterstützung: In der Arena entscheidet, wer zuerst zieht."
            } else {
                "Der zweite Baum geht defensiv, damit ${champion.name} den Kampf übersteht."
            },
        ),
    )

    /**
     * Die sechste Stufe des offensiven Baums hängt am Bereich: Warmaster schlägt aus
     * den Max-LP des Gegners zu und ist gegen einzelne, sehr zähe Ziele der größte
     * Anteil. Giant Slayer zahlt sich erst bei mehrfach treffenden Fähigkeiten aus.
     */
    private fun finisher(area: Area): String = when (area) {
        Area.CLAN_BOSS, Area.DUNGEONS, Area.DOOM_TOWER -> "Warmaster"
        else -> "Giant Slayer"
    }

    private fun path(
        tree: MasteryTree,
        secondary: MasteryTree,
        names: List<String>,
        reasons: List<String>,
    ): MasteryPath {
        val picks = names.mapNotNull { name -> Masteries.all.find { it.name == name } }
        return MasteryPath(tree, secondary, picks, reasons)
    }
}
