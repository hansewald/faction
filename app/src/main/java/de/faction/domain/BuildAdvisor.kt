package de.faction.domain

import de.faction.data.model.Area
import de.faction.data.model.Rarity
import de.faction.data.model.RosterEntry
import de.faction.data.model.Utility

enum class Verdict(val label: String, val explanation: String) {
    BUILD_NOW("Jetzt aufbauen", "Deckt eine Lücke, die dich aktuell ausbremst."),
    BUILD_LATER("Später aufbauen", "Gut, aber nicht dein nächster Engpass."),
    KEEP("Behalten", "Kein Ausbau nötig — aber nicht verfüttern."),
    FOOD("Als Futter nutzen", "Kit trägt nichts bei, was du nicht schon hast."),
    ;
}

data class Recommendation(
    val entry: RosterEntry,
    val verdict: Verdict,
    val priority: Int,
    val bestArea: Area,
    val bestAreaScore: Int,
    /** Klartext-Begründung, Zeile für Zeile. */
    val rationale: List<String>,
    /** Konkreter nächster Schritt für genau diesen Champion. */
    val nextStep: String,
)

data class RosterAnalysis(
    val stage: AccountStage,
    val recommendations: List<Recommendation>,
    /** Wirkungen, die im Kader komplett fehlen — der eigentliche Engpass. */
    val gaps: List<Utility>,
    val foodCandidates: List<Recommendation>,
)

/**
 * Übersetzt Kit-Scores in Handlungsempfehlungen. Die Leitfrage ist nicht
 * "welcher Champion ist der beste", sondern "was fehlt diesem Kader gerade".
 */
object BuildAdvisor {

    /** Wirkungen, ohne die ein Kader in jeder Phase hängen bleibt. */
    private val corePillars = listOf(
        Utility.DECREASE_DEFENSE,
        Utility.HEAL,
        Utility.INCREASE_SPEED,
        Utility.DECREASE_ATTACK,
        Utility.AOE_DAMAGE,
        Utility.BLOCK_DEBUFFS,
        Utility.INCREASE_ATTACK,
        Utility.CLEANSE,
    )

    /** Ab hier ist ein Kit in seinem besten Bereich brauchbar. */
    private const val USEFUL_THRESHOLD = 34

    /** Ab hier zählt ein Champion als einsatzbereit und deckt seine Wirkungen wirklich ab. */
    private fun RosterEntry.isFielded(): Boolean = owned.rank >= 5 || owned.level >= 50

    fun analyze(roster: List<RosterEntry>): RosterAnalysis {
        val stage = AccountStage.detect(roster)

        // Lücken werden nur über die einsatzbereiten Champions berechnet. Ein Champion,
        // der ungenutzt in der Sammlung liegt, deckt nichts ab — und kann deshalb selbst
        // als Lückenfüller in Frage kommen.
        val covered = roster.filter { it.isFielded() }.flatMap { it.champion.utilities }.toSet()
        val gaps = corePillars.filterNot { it in covered }

        // Wie oft besitzt du denselben Champion? Duplikate sind die erste Futterquelle.
        val copies = roster.groupingBy { it.champion.id }.eachCount()

        // Fraktionskriege brauchen 5 Champions pro Fraktion — dünn besetzte
        // Fraktionen machen sonst schwache Champions unverzichtbar.
        val perFaction = roster.groupingBy { it.champion.faction }.eachCount()

        val recommendations = roster.map { entry ->
            evaluate(entry, stage, gaps, copies, perFaction)
        }.sortedByDescending { it.priority }

        return RosterAnalysis(
            stage = stage,
            recommendations = recommendations,
            gaps = gaps,
            foodCandidates = recommendations.filter { it.verdict == Verdict.FOOD },
        )
    }

    private fun evaluate(
        entry: RosterEntry,
        stage: AccountStage,
        gaps: List<Utility>,
        copies: Map<String, Int>,
        perFaction: Map<String, Int>,
    ): Recommendation {
        val champion = entry.champion
        val score = ScoreEngine.score(champion)
        val best = score.best

        val rationale = mutableListOf<String>()
        var priority = best.score

        // In der Frühphase zählt zusätzlich, wie schnell ein Champion die Kampagne farmt
        // — der beste Bereich bleibt davon unberührt.
        if (stage == AccountStage.EARLY) {
            val campaign = score.perArea.getValue(Area.CAMPAIGN)
            if (campaign.score > 0) {
                priority += campaign.score / 2
                rationale += "Frühphase: farmt die Kampagne mit ${campaign.score}/100."
            }
        }

        val closesGap = if (entry.isFielded()) emptyList() else champion.utilities.filter { it in gaps }
        if (closesGap.isNotEmpty()) {
            priority += 30
            rationale += "Schließt eine Lücke im Kader: ${closesGap.joinToString { it.label }}."
        }

        val factionThin = (perFaction[champion.faction] ?: 0) < 5
        if (factionThin) {
            priority += 8
            rationale += "Fraktion ${champion.faction} hat weniger als 5 Champions — für die Fraktionskriege noch gebraucht."
        }

        val duplicates = (copies[champion.id] ?: 1) - 1
        if (duplicates > 0) {
            rationale += "Du besitzt $duplicates Duplikat(e) — Kopien über die erste hinaus sind Futter oder Rang-Aufwertung."
        }

        if (champion.earlyGameCarry && stage == AccountStage.EARLY) {
            priority += 20
            rationale += "Trägt die frühe Kampagne überproportional — früh investieren zahlt sich sofort aus."
        }

        if (champion.campaignFarmable && best.score < USEFUL_THRESHOLD) {
            priority -= 15
            rationale += "Aus der Kampagne nachfarmbar — kein Grund, diese Kopie zu behalten."
        }

        rationale += "Bester Bereich: ${best.area.label} (${best.score}/100)."
        if (best.reasons.isNotEmpty()) {
            rationale += "Getragen von: ${best.reasons.joinToString()}."
        }

        val verdict = decide(entry, best.score, closesGap.isNotEmpty(), factionThin, duplicates)
        return Recommendation(
            entry = entry,
            verdict = verdict,
            priority = priority,
            bestArea = best.area,
            bestAreaScore = best.score,
            rationale = rationale,
            nextStep = nextStep(entry, verdict, best.area),
        )
    }

    private fun decide(
        entry: RosterEntry,
        bestScore: Int,
        closesGap: Boolean,
        factionThin: Boolean,
        duplicates: Int,
    ): Verdict {
        val champion = entry.champion

        // Ein einziges Exemplar eines Epischen oder Legendären wird nie verfüttert:
        // der Wiederbeschaffungsaufwand übersteigt jeden Futterwert.
        val protected = duplicates == 0 &&
            (champion.rarity == Rarity.LEGENDARY || champion.rarity == Rarity.EPIC)
        if (entry.owned.locked) return if (bestScore >= USEFUL_THRESHOLD) Verdict.BUILD_LATER else Verdict.KEEP

        return when {
            // Wer als Einziger eine fehlende Säule mitbringt, wird gebaut — auch mit
            // schmalem Kit. Ein einzelner passender Effekt schlägt hier Vielseitigkeit.
            closesGap && bestScore > 0 -> Verdict.BUILD_NOW
            bestScore >= 60 -> Verdict.BUILD_NOW
            bestScore >= USEFUL_THRESHOLD -> Verdict.BUILD_LATER
            protected -> Verdict.KEEP
            factionThin && duplicates == 0 && champion.rarity >= Rarity.RARE -> Verdict.KEEP
            else -> Verdict.FOOD
        }
    }

    private fun nextStep(entry: RosterEntry, verdict: Verdict, area: Area): String {
        val owned = entry.owned
        return when (verdict) {
            Verdict.FOOD ->
                "Zum Aufwerten eines Champions verwenden, der ${area.label} abdeckt. " +
                    "Vorher auf Level 1 lassen — Erfahrung im Futter geht verloren."
            Verdict.KEEP -> "Nicht verfüttern, aber auch nicht investieren. Sperren, damit er nicht versehentlich draufgeht."
            else -> when {
                owned.rank < 5 ->
                    "Auf ${owned.rank + 1} Sterne aufwerten. Futter dafür in der Kampagne auf 12-3 (Brutal) farmen."
                owned.level < owned.rank * 10 ->
                    "Auf Level ${owned.rank * 10} bringen — dafür Dungeon der Wächter oder XP-Brew-Events nutzen."
                owned.ascension < 4 ->
                    "Aufsteigen lassen: die Potionen dafür gibt es im Fluch-Turm der jeweiligen Affinität."
                else ->
                    "Meisterschaften auf ${area.label} auslegen und Ausrüstung dafür sammeln."
            }
        }
    }
}
