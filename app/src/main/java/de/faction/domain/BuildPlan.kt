package de.faction.domain

import de.faction.data.model.Area
import de.faction.data.model.ArtifactSetContent
import de.faction.data.model.Champion
import de.faction.data.model.Rarity
import de.faction.data.model.Utility

data class BuildStep(
    val title: String,
    val subtitle: String,
    val details: List<String>,
)

data class StatHint(val name: String, val explanation: String)

data class BuildPlan(
    val area: Area,
    val steps: List<BuildStep>,
    val statHints: List<StatHint>,
)

/**
 * Erzeugt den Aufbauplan für einen Champion in einer gewählten Spielphase.
 *
 * Die Empfehlungen folgen aus dem Kit: wer Debuffs setzt, braucht Genauigkeit; wer
 * heilt, braucht Tempo vor Schaden. Nichts davon ist eine übernommene Fremdbewertung.
 */
object BuildPlanner {

    private val debuffUtilities = setOf(
        Utility.DECREASE_DEFENSE, Utility.DECREASE_ATTACK, Utility.DECREASE_SPEED,
        Utility.DECREASE_ACCURACY, Utility.WEAKEN, Utility.HP_BURN, Utility.POISON,
        Utility.POISON_SENSITIVITY, Utility.STUN, Utility.PROVOKE, Utility.BLOCK_REVIVE,
        Utility.DECREASE_MAX_HP, Utility.TURN_METER_DRAIN,
    )

    private val supportUtilities = setOf(
        Utility.HEAL, Utility.REVIVE, Utility.SHIELD, Utility.CLEANSE,
        Utility.BLOCK_DEBUFFS, Utility.INCREASE_SPEED, Utility.TURN_METER_BOOST,
    )

    fun plan(champion: Champion, stage: AccountStage): BuildPlan {
        val score = ScoreEngine.score(champion)
        val area = if (stage == AccountStage.EARLY && score.perArea.getValue(Area.CAMPAIGN).score > 0) {
            Area.CAMPAIGN
        } else {
            score.best.area
        }

        val setsDebuffer = champion.utilities.any { it in debuffUtilities }
        val setsSupport = champion.utilities.any { it in supportUtilities }

        return BuildPlan(
            area = area,
            steps = listOf(
                rankStep(champion, stage),
                gearStep(champion, area, setsDebuffer, setsSupport),
                masteryStep(champion, area, setsDebuffer, setsSupport),
            ),
            statHints = statHints(setsDebuffer, setsSupport),
        )
    }

    private fun rankStep(champion: Champion, stage: AccountStage): BuildStep {
        val targetRank = when {
            champion.rarity == Rarity.LEGENDARY -> 6
            champion.rarity == Rarity.EPIC -> if (stage == AccountStage.LATE) 6 else 5
            else -> 5
        }
        return BuildStep(
            title = "Level & Rang",
            subtitle = "Die Grundlagen verstehen",
            details = listOf(
                "Zielrang für ${champion.name}: $targetRank Sterne. Darunter fehlen die Basiswerte, um in ${champion.faction}-Teams mitzuhalten.",
                "Rang zuerst, Level danach: jede Aufwertung setzt das Level zurück, investierte Erfahrung wäre verloren.",
                "Futter auf Level 1 belassen und für Rang 5 und 6 gleichfarbig sammeln.",
            ),
        )
    }

    /**
     * Löst eine Set-Kennung zum deutschen Namen auf. Bricht absichtlich hart ab, wenn
     * die Kennung nicht existiert — ein Tippfehler hier soll beim nächsten Testlauf
     * auffallen, nicht als falscher Name in der App landen.
     */
    private fun setName(id: String): String =
        ArtifactSetContent.sets.first { it.id == id }.name

    private fun gearStep(
        champion: Champion,
        area: Area,
        setsDebuffer: Boolean,
        setsSupport: Boolean,
    ): BuildStep {
        val details = mutableListOf<String>()
        details += when {
            setsSupport && !setsDebuffer ->
                "${setName("speed")} als Grundlage. ${champion.name} wirkt hier Effekte, keinen Schaden — " +
                    "jede zusätzliche Runde zählt doppelt. Sobald zwei volle Sets tragbar sind, ergänzt " +
                    "${setName("resistance")} das Überleben gegen gegnerische Debuffs."
            setsDebuffer ->
                "${setName("speed")} plus ${setName("accuracy")}. Ohne genügend Genauigkeit widerstehen " +
                    "Gegner den Debuffs, und das Kit wirkt gar nicht."
            else ->
                "${setName("critical-rate")} plus ${setName("critical-damage")}, sobald die Krit. Rate " +
                    "verlässlich hoch genug ist. Bis dahin trägt ${setName("fatal")} allein schon beides in " +
                    "einem einzigen Set."
        }
        details += "Hauptwerte: Stiefel auf Tempo. Für ${area.label} auf den Ringen und Amuletten das, was ${champion.role.label} braucht."
        details += "Nebenwerte schlagen Sets. Ein 5-Sterne-Teil mit Tempo ist mehr wert als ein vollständiges Set ohne."
        return BuildStep("Ausrüstung", "Sets, Hauptwerte & Nebenwerte", details)
    }

    private fun masteryStep(
        champion: Champion,
        area: Area,
        setsDebuffer: Boolean,
        setsSupport: Boolean,
    ): BuildStep {
        val path = when {
            area == Area.CLAN_BOSS && champion.utilities.any {
                it == Utility.POISON || it == Utility.HP_BURN
            } -> "Anhaltender Schaden — die Effekte über die Zeit sind hier der Hauptschaden."
            setsSupport && !setsDebuffer -> "Unterstützung und Überleben, damit ${champion.name} den Kampf übersteht."
            setsDebuffer -> "Debuff-Genauigkeit priorisieren, danach Überleben."
            else -> "Offensive Meisterschaften, danach Überleben."
        }
        return BuildStep(
            title = "Meisterschaften",
            subtitle = "Deinen Lernpfad wählen",
            details = listOf(
                path,
                "Erst setzen, wenn ${champion.name} dauerhaft in deinem Team bleibt — ein Zurücksetzen kostet echte Ressourcen.",
                "Schriftrollen kommen aus dem Meisterschaftsturm; plane den Weg vollständig, bevor du den ersten Punkt setzt.",
            ),
        )
    }

    private fun statHints(setsDebuffer: Boolean, setsSupport: Boolean): List<StatHint> = buildList {
        add(
            StatHint(
                "Tempo",
                "Bestimmt, wie oft ein Champion an der Reihe ist. Der wichtigste Wert im Spiel — mehr Züge bedeuten mehr Wirkung, unabhängig von der Rolle.",
            ),
        )
        if (setsDebuffer) {
            add(
                StatHint(
                    "Genauigkeit",
                    "Entscheidet, ob Debuffs landen. Reicht sie nicht, widerstehen Gegner, und das gesamte Kit läuft ins Leere.",
                ),
            )
        }
        if (!setsSupport || !setsDebuffer) {
            add(
                StatHint(
                    "Krit. Rate",
                    "Anteil der Angriffe, die kritisch treffen. Erst nahe 100 % lohnt es sich, in kritischen Schaden zu investieren.",
                ),
            )
        }
        add(
            StatHint(
                "Widerstand",
                "Wehrt gegnerische Debuffs ab. Zählt vor allem in der Arena und im Turm des Schicksals.",
            ),
        )
    }
}
