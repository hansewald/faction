package de.faction.domain

import de.faction.data.model.Area
import de.faction.data.model.Champion
import de.faction.data.model.Rarity
import de.faction.data.model.Role
import de.faction.data.model.Utility
import kotlin.math.min

/** Ein Bereichs-Score samt der Gründe, die ihn erzeugt haben. */
data class AreaScore(
    val area: Area,
    val score: Int,
    val reasons: List<String>,
)

data class ChampionScore(
    val championId: String,
    val perArea: Map<Area, AreaScore>,
) {
    val best: AreaScore get() = perArea.values.maxBy { it.score }
    val overall: Int get() = perArea.values.maxOf { it.score }
}

/**
 * Bewertet Champions ausschließlich aus ihrem eigenen Kit heraus.
 *
 * Die Gewichte bilden ab, was ein Bereich mechanisch verlangt: der Clanboss ist ein
 * einzelner Gegner mit festem Zugrhythmus, also zählen Debuffs auf ein Ziel und
 * Debuff-Schutz; die Arena entscheidet sich am ersten Zug, also zählen Tempo und
 * Zugleiste. Die Zahlen sind Punkte, keine Prozente — vergleichbar sind sie nur
 * innerhalb desselben Bereichs.
 */
object ScoreEngine {

    private const val MAX_AREA_SCORE = 100

    private val weights: Map<Area, Map<Utility, Int>> = mapOf(
        Area.CLAN_BOSS to mapOf(
            Utility.DECREASE_ATTACK to 22,
            Utility.WEAKEN to 20,
            Utility.DECREASE_DEFENSE to 18,
            Utility.BLOCK_DEBUFFS to 18,
            Utility.POISON to 14,
            Utility.HP_BURN to 14,
            Utility.INCREASE_ATTACK to 10,
            Utility.INCREASE_DEFENSE to 10,
            Utility.INCREASE_SPEED to 10,
            Utility.HEAL to 10,
            Utility.COUNTERATTACK to 12,
            Utility.ALLY_ATTACK to 10,
            Utility.INCREASE_CRIT_RATE to 8,
            Utility.POISON_SENSITIVITY to 12,
            Utility.CLEANSE to 6,
        ),
        Area.ARENA to mapOf(
            Utility.TURN_METER_BOOST to 26,
            Utility.TURN_METER_DRAIN to 22,
            Utility.EXTRA_TURN to 20,
            Utility.AOE_DAMAGE to 18,
            Utility.STUN to 16,
            Utility.DECREASE_DEFENSE to 14,
            Utility.INCREASE_SPEED to 12,
            Utility.BLOCK_DEBUFFS to 12,
            Utility.INCREASE_ATTACK to 8,
            Utility.INCREASE_CRIT_RATE to 8,
            Utility.IGNORE_DEFENSE to 10,
            Utility.DECREASE_SPEED to 10,
        ),
        Area.DUNGEONS to mapOf(
            Utility.DECREASE_DEFENSE to 22,
            Utility.SINGLE_TARGET_NUKE to 18,
            Utility.IGNORE_DEFENSE to 16,
            Utility.DECREASE_ATTACK to 14,
            Utility.HEAL to 14,
            Utility.SHIELD to 12,
            Utility.STUN to 14,
            Utility.INCREASE_ATTACK to 10,
            Utility.INCREASE_DEFENSE to 10,
            Utility.CLEANSE to 10,
            Utility.BLOCK_DEBUFFS to 10,
            Utility.ALLY_ATTACK to 10,
            Utility.DECREASE_MAX_HP to 14,
        ),
        Area.DOOM_TOWER to mapOf(
            Utility.BLOCK_DEBUFFS to 20,
            Utility.CLEANSE to 18,
            Utility.HEAL to 16,
            Utility.REVIVE to 16,
            Utility.DECREASE_ATTACK to 14,
            Utility.DECREASE_DEFENSE to 14,
            Utility.SHIELD to 12,
            Utility.PROVOKE to 12,
            Utility.BLOCK_REVIVE to 12,
            Utility.DECREASE_MAX_HP to 12,
            Utility.STUN to 10,
        ),
        Area.FACTION_WARS to mapOf(
            Utility.AOE_DAMAGE to 20,
            Utility.HEAL to 18,
            Utility.REVIVE to 16,
            Utility.DECREASE_DEFENSE to 16,
            Utility.CLEANSE to 12,
            Utility.SHIELD to 12,
            Utility.INCREASE_DEFENSE to 10,
            Utility.STUN to 12,
            Utility.TURN_METER_BOOST to 12,
        ),
        Area.CAMPAIGN to mapOf(
            Utility.AOE_DAMAGE to 30,
            Utility.INCREASE_SPEED to 18,
            Utility.INCREASE_ATTACK to 14,
            Utility.HEAL to 10,
            Utility.EXTRA_TURN to 12,
            Utility.TURN_METER_BOOST to 10,
        ),
    )

    /** Seltenheit begrenzt das Ausbaupotenzial (Stats, Maximallevel, Meisterschaften). */
    private fun rarityFactor(rarity: Rarity): Float = when (rarity) {
        Rarity.COMMON -> 0.45f
        Rarity.UNCOMMON -> 0.60f
        Rarity.RARE -> 0.80f
        Rarity.EPIC -> 0.95f
        Rarity.LEGENDARY -> 1.0f
    }

    /** Rollen, die in einem Bereich strukturell besser passen. */
    private fun roleBonus(role: Role, area: Area): Int = when (area) {
        Area.CLAN_BOSS -> if (role == Role.SUPPORT || role == Role.HP) 6 else 0
        Area.ARENA -> if (role == Role.ATTACK || role == Role.SUPPORT) 6 else 0
        Area.CAMPAIGN -> if (role == Role.ATTACK) 8 else 0
        Area.DOOM_TOWER -> if (role == Role.HP || role == Role.DEFENSE) 6 else 0
        else -> 0
    }

    fun score(champion: Champion): ChampionScore {
        val perArea = Area.entries.associateWith { area -> scoreArea(champion, area) }
        return ChampionScore(champion.id, perArea)
    }

    private fun scoreArea(champion: Champion, area: Area): AreaScore {
        val areaWeights = weights.getValue(area)
        val hits = champion.utilities
            .mapNotNull { util -> areaWeights[util]?.let { util to it } }
            .sortedByDescending { it.second }

        // Ein Kit gewinnt vor allem durch seine stärkste Wirkung; jede weitere zählt
        // abgeschwächt, damit breite Kits nicht automatisch alles dominieren.
        var raw = 0f
        hits.forEachIndexed { index, (_, weight) ->
            raw += weight * (1f / (1f + index * 0.35f))
        }
        raw *= rarityFactor(champion.rarity)
        // Der Rollenbonus verstärkt ein passendes Kit, er ersetzt es nicht: ohne einen
        // einzigen für den Bereich relevanten Effekt bleibt der Score bei null.
        if (hits.isNotEmpty()) {
            raw += roleBonus(champion.role, area)
            if (champion.earlyGameCarry && area == Area.CAMPAIGN) raw += 12
        }

        val score = min(MAX_AREA_SCORE, raw.toInt())
        val reasons = hits.take(3).map { (util, weight) -> "${util.label} (+$weight)" }
        return AreaScore(area, score, reasons)
    }
}
