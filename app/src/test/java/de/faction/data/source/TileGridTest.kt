package de.faction.data.source

import de.faction.data.source.TileGrid.Box
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prüft die Rasterrechnung an einem echten Screenshot der Championliste: 1080 Pixel
 * breit, neun Spalten, zwei Zeilen. Die erwarteten Kacheln sind im Bild ausgemessen.
 */
class TileGridTest {

    private val pitch = 118
    private val width = 1080
    private val height = 400

    /** Die Levelzahl der Kachel in Spalte [column], Zeile [row] — unten rechts in der Kachel. */
    private fun levelBox(column: Int, row: Int): Box {
        val centerX = 120 + column * pitch
        val bottom = 197 + row * 152
        return Box(centerX - 16, bottom - 22, centerX + 16, bottom)
    }

    @Test
    fun `findet die Kachel zu einer Levelzahl`() {
        val tiles = TileGrid.tilesFor(listOf(levelBox(0, 0), levelBox(1, 0)), width, height)

        val first = requireNotNull(tiles[0])
        // Die erste Kachel beginnt am linken Rand des Rasters und endet unter der Zahl.
        assertEquals(20f, first.left.toFloat(), 6f)
        assertEquals(212f, first.bottom.toFloat(), 6f)
        assertEquals(80f, first.top.toFloat(), 6f)
        assertTrue("Kachel ist höher als breit", first.height > first.width)
    }

    @Test
    fun `Kacheln einer Zeile liegen im Spaltenabstand nebeneinander`() {
        val tiles = TileGrid.tilesFor((0 until 5).map { levelBox(it, 0) }, width, height)
            .map { requireNotNull(it) }

        tiles.zipWithNext { left, right ->
            assertEquals(pitch.toFloat(), right.centerX - left.centerX, 1.5f)
        }
    }

    @Test
    fun `erkennt das Raster ueber mehrere Zeilen hinweg`() {
        val boxes = (0 until 4).map { levelBox(it, 0) } + (0 until 4).map { levelBox(it, 1) }
        val tiles = TileGrid.tilesFor(boxes, width, height)

        assertTrue("alle acht Kacheln gefunden", tiles.all { it != null })
        val firstRow = requireNotNull(tiles[0])
        val secondRow = requireNotNull(tiles[4])
        assertEquals(152f, secondRow.top.toFloat() - firstRow.top, 6f)
    }

    @Test
    fun `eine einzelne Zahl ergibt kein Raster`() {
        assertEquals(listOf(null), TileGrid.tilesFor(listOf(levelBox(0, 0)), width, height))
    }

    @Test
    fun `Zahlen ausserhalb des Rasters liefern keine Kachel`() {
        // Der Zähler am Filterknopf sitzt oben links, weit über der ersten Kachelreihe.
        val badge = Box(360, 10, 380, 30)
        val tiles = TileGrid.tilesFor(listOf(levelBox(0, 0), levelBox(1, 0), badge), width, height)

        assertNull("angeschnitten am oberen Bildrand", tiles[2])
        assertTrue(tiles[0] != null && tiles[1] != null)
    }
}
