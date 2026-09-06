package de.faction.domain

import de.faction.data.model.Affinity
import de.faction.data.model.Area
import de.faction.data.model.Champion
import de.faction.data.model.Masteries
import de.faction.data.model.MasteryTree
import de.faction.data.model.Rarity
import de.faction.data.model.Role
import de.faction.data.model.Utility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MasteryPlannerTest {

    private fun champion(
        role: Role,
        vararg utilities: Utility,
    ) = Champion(
        id = "test",
        name = "Testlegende",
        faction = "Banner-Herren",
        rarity = Rarity.EPIC,
        affinity = Affinity.MAGIC,
        role = role,
        utilities = utilities.toSet(),
    )

    @Test
    fun `jeder Baum hat 22 Meisterschaften`() {
        MasteryTree.entries.forEach { tree ->
            assertEquals(tree.label, 22, Masteries.tree(tree).size)
        }
    }

    @Test
    fun `Stufe 1 hat zwei Felder, alle weiteren vier`() {
        MasteryTree.entries.forEach { tree ->
            assertEquals(2, Masteries.tier(tree, 1).size)
            (2..6).forEach { tier -> assertEquals(4, Masteries.tier(tree, tier).size) }
        }
    }

    @Test
    fun `der Pfad belegt jede Stufe genau einmal`() {
        val path = MasteryPlanner.plan(champion(Role.ATTACK, Utility.AOE_DAMAGE), Area.DUNGEONS)

        assertEquals(6, path.picks.size)
        assertEquals((1..6).toList(), path.picks.map { it.tier })
        assertTrue("alle aus demselben Baum", path.picks.all { it.tree == path.tree })
        assertNotEquals("zweiter Baum ist ein anderer", path.tree, path.secondary)
    }

    @Test
    fun `wer schwaecht, bekommt Genauigkeit`() {
        val path = MasteryPlanner.plan(
            champion(Role.SUPPORT, Utility.DECREASE_DEFENSE, Utility.WEAKEN),
            Area.CLAN_BOSS,
        )

        assertEquals(MasteryTree.SUPPORT, path.tree)
        assertTrue("Eagle Eye ist der Abschluss", path.picks.any { it.name == "Eagle Eye" })
    }

    @Test
    fun `Heiler bekommen den Unterstuetzungsbaum, aber nicht den Hexer-Pfad`() {
        val path = MasteryPlanner.plan(
            champion(Role.SUPPORT, Utility.HEAL, Utility.CONTINUOUS_HEAL),
            Area.DUNGEONS,
        )

        assertEquals(MasteryTree.SUPPORT, path.tree)
        assertTrue(path.picks.any { it.name == "Timely Intervention" })
    }

    @Test
    fun `der Abschluss haengt am Bereich`() {
        val attacker = champion(Role.ATTACK, Utility.SINGLE_TARGET_NUKE)

        val bossPath = MasteryPlanner.plan(attacker, Area.CLAN_BOSS)
        val arenaPath = MasteryPlanner.plan(attacker, Area.ARENA)

        assertEquals("Warmaster", bossPath.picks.last().name)
        assertEquals("Giant Slayer", arenaPath.picks.last().name)
    }
}
