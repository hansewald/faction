package de.faction.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtifactSetContentTest {

    private val sets = ArtifactSetContent.sets

    @Test
    fun `keine doppelten Kennungen`() {
        val ids = sets.map { it.id }
        assertEquals("jede id genau einmal", ids.size, ids.toSet().size)
    }

    @Test
    fun `jedes Set hat 2 oder 4 Teile`() {
        sets.forEach { set ->
            assertTrue("${set.name} hat ${set.pieces} Teile", set.pieces == 2 || set.pieces == 4)
        }
    }

    @Test
    fun `Name und Wirkung sind nie leer`() {
        sets.forEach { set ->
            assertTrue("${set.id}: Name leer", set.name.isNotBlank())
            assertTrue("${set.id}: Wirkung leer", set.effect.isNotBlank())
        }
    }

    @Test
    fun `deckt jede Kategorie mindestens einmal ab`() {
        val covered = sets.map { it.kind }.toSet()
        assertEquals(SetKind.entries.toSet(), covered)
    }

    @Test
    fun `die von BuildPlanner referenzierten Sets existieren`() {
        // BuildPlan.setName() bricht zur Laufzeit hart ab, wenn eine dieser Kennungen
        // fehlt. Dieser Test macht das Problem hier sichtbar statt erst im Aufbauplan.
        val referenced = listOf("speed", "resistance", "accuracy", "critical-rate", "critical-damage", "fatal")
        val ids = sets.map { it.id }.toSet()
        referenced.forEach { id -> assertTrue("Set '$id' fehlt im Katalog", id in ids) }
    }
}
