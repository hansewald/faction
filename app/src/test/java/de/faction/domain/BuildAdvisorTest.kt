package de.faction.domain

import de.faction.data.model.Affinity
import de.faction.data.model.Area
import de.faction.data.model.Champion
import de.faction.data.model.OwnedChampion
import de.faction.data.model.Rarity
import de.faction.data.model.Role
import de.faction.data.model.RosterEntry
import de.faction.data.model.Utility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildAdvisorTest {

    private fun champion(
        id: String,
        rarity: Rarity = Rarity.RARE,
        utilities: Set<Utility> = emptySet(),
        faction: String = "Barbaren",
        role: Role = Role.ATTACK,
        dataComplete: Boolean = true,
    ) = Champion(
        id = id,
        name = id,
        faction = faction,
        rarity = rarity,
        affinity = Affinity.MAGIC,
        role = role,
        utilities = utilities,
        dataComplete = dataComplete,
    )

    private fun entry(champion: Champion, instanceId: Long = 1, level: Int = 1, rank: Int = 1) =
        RosterEntry(
            owned = OwnedChampion(
                instanceId = instanceId,
                championId = champion.id,
                level = level,
                ascension = 0,
                rank = rank,
            ),
            champion = champion,
        )

    @Test
    fun `champion without any utility is marked as food`() {
        val blank = champion("blank", Rarity.COMMON)
        val analysis = BuildAdvisor.analyze(listOf(entry(blank)))
        assertEquals(Verdict.FOOD, analysis.recommendations.single().verdict)
    }

    @Test
    fun `single legendary is never marked as food`() {
        val weakLegendary = champion("weak-legendary", Rarity.LEGENDARY)
        val analysis = BuildAdvisor.analyze(listOf(entry(weakLegendary)))
        assertEquals(Verdict.KEEP, analysis.recommendations.single().verdict)
    }

    @Test
    fun `unbuilt champion closing a gap outranks a comparable one that does not`() {
        // Bereits aufgebaut, deckt damit Heilung tatsaechlich ab.
        val fieldedHealer = entry(
            champion("healer", Rarity.EPIC, setOf(Utility.HEAL, Utility.CLEANSE)),
            instanceId = 1,
            level = 50,
            rank = 5,
        )
        val gapCloser = entry(
            champion("gap-closer", Rarity.EPIC, setOf(Utility.DECREASE_DEFENSE)),
            instanceId = 2,
        )
        val bruiser = entry(
            champion("bruiser", Rarity.EPIC, setOf(Utility.SINGLE_TARGET_NUKE, Utility.IGNORE_DEFENSE)),
            instanceId = 3,
        )

        val analysis = BuildAdvisor.analyze(listOf(fieldedHealer, bruiser, gapCloser))
        val gapRec = analysis.recommendations.first { it.entry.champion.id == "gap-closer" }
        val bruiserRec = analysis.recommendations.first { it.entry.champion.id == "bruiser" }

        assertTrue(
            "Der Lueckenfueller muss hoeher priorisiert sein",
            gapRec.priority > bruiserRec.priority,
        )
        assertEquals(Verdict.BUILD_NOW, gapRec.verdict)
    }

    @Test
    fun `only fielded champions close gaps`() {
        val healerKit = champion("healer", Rarity.EPIC, setOf(Utility.HEAL))

        // Ungenutzt in der Sammlung: Heilung gilt weiter als Luecke.
        val benched = BuildAdvisor.analyze(listOf(entry(healerKit, level = 1, rank = 1)))
        assertTrue(Utility.HEAL in benched.gaps)

        // Aufgebaut: die Luecke ist geschlossen.
        val fielded = BuildAdvisor.analyze(listOf(entry(healerKit, level = 50, rank = 5)))
        assertTrue(Utility.HEAL !in fielded.gaps)
        assertTrue(Utility.DECREASE_DEFENSE in fielded.gaps)
    }

    @Test
    fun `account stage rises with six star champions`() {
        val any = champion("any", utilities = setOf(Utility.HEAL))
        val early = BuildAdvisor.analyze(listOf(entry(any, rank = 3, level = 30)))
        assertEquals(AccountStage.EARLY, early.stage)

        val late = BuildAdvisor.analyze(
            (1L..6L).map { entry(champion("c$it", utilities = setOf(Utility.HEAL)), it, 60, 6) },
        )
        assertEquals(AccountStage.LATE, late.stage)
    }

    @Test
    fun `clan boss weighting rewards decrease attack over raw damage`() {
        val debuffer = champion("debuffer", Rarity.EPIC, setOf(Utility.DECREASE_ATTACK, Utility.WEAKEN))
        val nuker = champion("nuker", Rarity.EPIC, setOf(Utility.SINGLE_TARGET_NUKE, Utility.AOE_DAMAGE))
        val debufferScore = ScoreEngine.score(debuffer).perArea.getValue(Area.CLAN_BOSS).score
        val nukerScore = ScoreEngine.score(nuker).perArea.getValue(Area.CLAN_BOSS).score
        assertTrue("Debuffer sollte am Clanboss höher bewertet sein", debufferScore > nukerScore)
    }

    @Test
    fun `champion without skill data is never marked as food`() {
        // Gleiche Ausgangslage wie der Futter-Test, nur fehlen die Faehigkeitsdaten.
        val unknown = champion("unknown", Rarity.COMMON, dataComplete = false)
        val analysis = BuildAdvisor.analyze(listOf(entry(unknown)))
        val recommendation = analysis.recommendations.single()

        assertEquals(Verdict.KEEP, recommendation.verdict)
        assertTrue(
            "Die fehlende Datenlage muss in der Begruendung stehen",
            recommendation.rationale.any { it.contains("keine Fähigkeitsdaten") },
        )
    }

    @Test
    fun `arena rewards turn meter control over sustain`() {
        val controller = champion(
            "controller",
            Rarity.EPIC,
            setOf(Utility.TURN_METER_DRAIN, Utility.REMOVE_BUFFS),
        )
        val healer = champion("healer", Rarity.EPIC, setOf(Utility.HEAL, Utility.CONTINUOUS_HEAL))
        val controllerScore = ScoreEngine.score(controller).perArea.getValue(Area.ARENA).score
        val healerScore = ScoreEngine.score(healer).perArea.getValue(Area.ARENA).score
        assertTrue(
            "Zugleisten-Kontrolle sollte in der Arena hoeher zaehlen als reines Heilen",
            controllerScore > healerScore,
        )
    }

    @Test
    fun `clan boss ignores crowd control because it does not apply there`() {
        val crowdControl = champion(
            "cc",
            Rarity.EPIC,
            setOf(Utility.STUN, Utility.SLEEP, Utility.FEAR, Utility.PROVOKE),
        )
        assertEquals(0, ScoreEngine.score(crowdControl).perArea.getValue(Area.CLAN_BOSS).score)
    }
}
