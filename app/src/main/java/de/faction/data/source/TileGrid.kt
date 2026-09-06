package de.faction.data.source

/**
 * Findet die Kacheln der Championliste im Screenshot.
 *
 * Die Liste ist ein gleichmäßiges Raster; jede Kachel trägt unten rechts ihr Level.
 * Genau diese Zahlen liefert die Texterkennung mitsamt Position — daraus lässt sich
 * das Raster zurückrechnen, ohne das Bild selbst analysieren zu müssen.
 *
 * Die Maße sind Vielfache des **Spaltenabstands**, nicht feste Pixelwerte: das Raster
 * skaliert mit der Bildschirmauflösung, die Verhältnisse bleiben gleich. Sie stammen
 * aus dem Layout der Championliste — die Zahl sitzt stets im selben Verhältnis zur
 * Kachel.
 *
 * Ein daneben liegender Zuschnitt fällt nicht durch: der Bestätigungsschritt zeigt
 * jedes Bild neben dem Namen, bevor etwas in den Kader wandert.
 */
object TileGrid {

    /**
     * Ein Rechteck im Bild. Bewusst nicht Androids `Rect`: so bleibt die Geometrie
     * reine Rechnung und lässt sich ohne Gerät testen.
     */
    data class Box(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val width: Int get() = right - left
        val height: Int get() = bottom - top
        val centerX: Float get() = (left + right) / 2f
        val centerY: Float get() = (top + bottom) / 2f
    }

    /** Kachelbreite, gemessen am Spaltenabstand — dazwischen liegt der Rahmenabstand. */
    private const val TILE_WIDTH = 0.97f
    /** Kacheln sind etwas höher als breit. */
    private const val TILE_HEIGHT = 1.12f
    /** Die Levelzahl sitzt rechts der Kachelmitte. */
    private const val LEVEL_OFFSET_X = 0.36f
    /** Und knapp über der Unterkante. */
    private const val LEVEL_OFFSET_Y = 0.13f

    /**
     * Rechnet aus den Positionen der Levelzahlen die Kachel je Zahl aus.
     *
     * [levelBoxes] muss in derselben Reihenfolge stehen wie die zugehörigen Vorschläge;
     * das Ergebnis ist gleich lang. Wo sich keine Kachel bestimmen lässt, steht `null`.
     */
    fun tilesFor(levelBoxes: List<Box?>, imageWidth: Int, imageHeight: Int): List<Box?> {
        val pitch = columnPitch(levelBoxes.filterNotNull()) ?: return levelBoxes.map { null }
        return levelBoxes.map { box -> box?.let { tileAround(it, pitch, imageWidth, imageHeight) } }
    }

    private fun tileAround(level: Box, pitch: Float, width: Int, height: Int): Box? {
        val tileWidth = pitch * TILE_WIDTH
        val tileHeight = pitch * TILE_HEIGHT
        val centerX = level.centerX - pitch * LEVEL_OFFSET_X
        val bottom = level.bottom + pitch * LEVEL_OFFSET_Y

        val rect = Box(
            (centerX - tileWidth / 2f).toInt(),
            (bottom - tileHeight).toInt(),
            (centerX + tileWidth / 2f).toInt(),
            bottom.toInt(),
        )
        // Am Bildrand angeschnittene Kacheln liefern kein brauchbares Portrait.
        if (rect.left < 0 || rect.top < 0 || rect.right > width || rect.bottom > height) return null
        return rect.takeIf { it.width > MIN_SIDE && it.height > MIN_SIDE }
    }

    /**
     * Der Abstand benachbarter Spalten. Ermittelt aus den Zahlen einer Zeile: Zahlen
     * mit ähnlicher Höhe stehen nebeneinander, ihr waagerechter Abstand ist der
     * gesuchte Rasterschritt. Der Median schluckt Ausreißer durch Lücken in der Zeile.
     */
    private fun columnPitch(boxes: List<Box>): Float? {
        if (boxes.size < 2) return null
        val gaps = rows(boxes)
            .filter { it.size >= 2 }
            .flatMap { row ->
                row.sortedBy { it.left }
                    .zipWithNext { left, right -> right.centerX - left.centerX }
            }
            .filter { it > MIN_SIDE }
            .sorted()

        return gaps.getOrNull(gaps.size / 2)
    }

    /**
     * Teilt die Zahlen in Zeilen. Der Reihe nach von oben nach unten: solange die
     * nächste Zahl nah genug an der laufenden Zeile liegt, gehört sie dazu. Feste
     * Bereichsgrenzen wären hier falsch — eine Zahl direkt auf der Grenze käme sonst
     * in eine eigene Zeile und ginge für den Rasterschritt verloren.
     */
    private fun rows(boxes: List<Box>): List<List<Box>> {
        val rows = mutableListOf<MutableList<Box>>()
        for (box in boxes.sortedBy { it.centerY }) {
            val current = rows.lastOrNull()
            val reference = current?.last()
            if (current != null && reference != null &&
                box.centerY - reference.centerY < reference.height * ROW_TOLERANCE
            ) {
                current += box
            } else {
                rows += mutableListOf(box)
            }
        }
        return rows
    }

    /** Zeilenhöhe in Vielfachen der Texthöhe — großzügig, Zahlen sitzen nicht exakt gleich hoch. */
    private const val ROW_TOLERANCE = 2.0f
    private const val MIN_SIDE = 24
}
