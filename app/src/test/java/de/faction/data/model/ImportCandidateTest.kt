package de.faction.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportCandidateTest {

    private fun candidate(
        championId: String? = "kael",
        confidence: Float = 1f,
        level: Int = 50,
        rank: Int = 5,
    ) = ImportCandidate(
        rawLabel = "Kael",
        championId = championId,
        level = level,
        rank = rank,
        confidence = confidence,
        source = ImportSource.SCREENSHOT_OCR,
    )

    @Test
    fun `a confident match is preselected`() {
        assertTrue(candidate(confidence = 0.95f).accepted)
    }

    @Test
    fun `an uncertain match is not preselected`() {
        // Genau hier liegt der Zweck des Bestätigungsschritts: unsichere Treffer
        // muss der Spieler aktiv bestätigen.
        assertFalse(candidate(confidence = 0.7f).accepted)
    }

    @Test
    fun `an unresolved line is never preselected and yields nothing`() {
        val unresolved = candidate(championId = null, confidence = 0f)
        assertFalse(unresolved.accepted)
        assertFalse(unresolved.isResolved)
        assertNull(unresolved.toOwnedChampion())
    }

    @Test
    fun `implausible values are clamped when converted`() {
        val owned = candidate(level = 99, rank = 9).toOwnedChampion()
        assertEquals(60, owned?.level)
        assertEquals(6, owned?.rank)
    }

    @Test
    fun `the source is carried into the roster entry`() {
        assertEquals(ImportSource.SCREENSHOT_OCR, candidate().toOwnedChampion()?.source)
    }
}
