package de.faction.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.faction.data.local.BundledPortraits
import de.faction.data.local.PortraitStore
import de.faction.data.model.Affinity
import de.faction.data.model.Champion
import de.faction.ui.theme.FactionColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Das Bild einer Legende, in dieser Reihenfolge:
 *
 * 1. das Portrait aus dem eigenen Screenshot des Spielers ([portraitPath]) - es zeigt
 *    den Stand, den er selbst hinterlegt hat, und hat deshalb Vorrang;
 * 2. das mitgelieferte offizielle Portrait ([BundledPortraits]);
 * 3. das gezeichnete Wappen aus Fraktionssymbol, Affinitätsfarbe und
 *    Seltenheitsrahmen - für die Legenden, zu denen kein Bild vorliegt.
 *
 * Die mitgelieferten Portraits sind Artworks von Plarium, genutzt mit Genehmigung
 * (siehe `docs/plarium-genehmigung.md`).
 */
@Composable
fun ChampionSigil(
    champion: Champion,
    modifier: Modifier = Modifier,
    portraitPath: String? = null,
) {
    val affinity = affinityColor(champion.affinity)
    val rarity = FactionColors.rarity(champion.rarity.label)
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        affinity.copy(alpha = 0.32f),
                        FactionColors.Night,
                    ),
                ),
            )
            .border(1.dp, rarity.copy(alpha = 0.45f), shape),
    ) {
        // Beide Quellen werden immer abgefragt: Composables duerfen nicht bedingt
        // aufgerufen werden, sonst verrutscht ihr Zustand zwischen den Kacheln.
        val own = rememberPortrait(portraitPath)
        val bundled = rememberBundledPortrait(champion.id)
        val portrait = own ?: bundled
        if (portrait != null) {
            Image(
                bitmap = portrait,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                // Die Kachel ist annähernd quadratisch, die Fläche hier oft breiter:
                // zuschneiden statt verzerren, das Gesicht sitzt in der oberen Hälfte.
                contentScale = ContentScale.Crop,
            )
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val mark = factionMark(champion.faction)
                val side = minOf(size.width, size.height) * 0.46f
                val origin = Offset((size.width - side) / 2f, (size.height - side) / 2f)
                drawMark(mark, origin, side, rarity.copy(alpha = 0.9f))
            }
        }
    }
}

private fun affinityColor(affinity: Affinity): Color = when (affinity) {
    Affinity.MAGIC -> Color(0xFF3E7BD6)
    Affinity.FORCE -> Color(0xFFC0453B)
    Affinity.SPIRIT -> Color(0xFF3E9B63)
    Affinity.VOID -> Color(0xFF8B4FC0)
}

/** Die vierzehn Fraktionszeichen. Bewusst abstrakt und selbst entworfen. */
private enum class Mark {
    CROSS, CRESCENT, CHEVRONS, PENNANT, TUSKS, SPINDLE, HORNS,
    ANVIL, HEXAGON, NESTED_SQUARE, LEAF, SCALES, CLAWS, ECLIPSE,
}

private fun factionMark(faction: String): Mark = when (faction) {
    "Heilige Orden" -> Mark.CROSS
    "Untote Horden" -> Mark.CRESCENT
    "Barbaren" -> Mark.CHEVRONS
    "Banner-Lords" -> Mark.PENNANT
    "Orks" -> Mark.TUSKS
    "Dunkelelfen" -> Mark.SPINDLE
    "Dämonenbrut" -> Mark.HORNS
    "Zwerge" -> Mark.ANVIL
    "Oger-Stämme" -> Mark.HEXAGON
    "Ritter-Wiedergänger" -> Mark.NESTED_SQUARE
    "Hochelfen" -> Mark.LEAF
    "Echsenmenschen" -> Mark.SCALES
    "Fellwandler" -> Mark.CLAWS
    "Schattenclans" -> Mark.ECLIPSE
    else -> Mark.HEXAGON
}

/**
 * Zeichnet ein Zeichen in ein Quadrat der Kantenlänge [side] ab [origin].
 * Alle Koordinaten sind auf 0..1 normiert, damit die Zeichen in jeder Größe stimmen.
 */
private fun DrawScope.drawMark(mark: Mark, origin: Offset, side: Float, color: Color) {
    fun p(x: Float, y: Float) = Offset(origin.x + x * side, origin.y + y * side)
    val line = side * 0.11f
    val stroke = Stroke(width = line)

    fun path(vararg points: Pair<Float, Float>, close: Boolean = true) = Path().apply {
        points.forEachIndexed { index, (x, y) ->
            val point = p(x, y)
            if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
        }
        if (close) close()
    }

    when (mark) {
        Mark.CROSS -> {
            drawPath(path(0.42f to 0f, 0.58f to 0f, 0.58f to 1f, 0.42f to 1f), color)
            drawPath(path(0f to 0.32f, 1f to 0.32f, 1f to 0.48f, 0f to 0.48f), color)
        }

        Mark.CRESCENT -> {
            drawCircle(color, radius = side * 0.5f, center = p(0.5f, 0.5f))
            // Der Ausschnitt erzeugt die Sichel.
            drawCircle(FactionColors.Night, radius = side * 0.42f, center = p(0.68f, 0.42f))
        }

        Mark.CHEVRONS -> {
            drawPath(path(0f to 0.42f, 0.5f to 0f, 1f to 0.42f, 0.5f to 0.2f), color)
            drawPath(path(0f to 0.9f, 0.5f to 0.48f, 1f to 0.9f, 0.5f to 0.68f), color)
        }

        Mark.PENNANT -> drawPath(
            path(0.5f to 0f, 1f to 0.22f, 1f to 0.62f, 0.5f to 1f, 0f to 0.62f, 0f to 0.22f),
            color,
        )

        Mark.TUSKS -> {
            drawArc(
                color = color, startAngle = 200f, sweepAngle = 110f, useCenter = false,
                topLeft = p(-0.1f, 0f), size = Size(side * 0.8f, side), style = stroke,
            )
            drawArc(
                color = color, startAngle = 230f, sweepAngle = -110f, useCenter = false,
                topLeft = p(0.3f, 0f), size = Size(side * 0.8f, side), style = stroke,
            )
        }

        Mark.SPINDLE -> {
            drawPath(path(0.5f to 0f, 0.9f to 0.5f, 0.5f to 1f, 0.1f to 0.5f), color)
            drawPath(path(0.5f to 0.24f, 0.72f to 0.5f, 0.5f to 0.76f, 0.28f to 0.5f), FactionColors.Night)
        }

        Mark.HORNS -> {
            drawArc(
                color = color, startAngle = 180f, sweepAngle = 150f, useCenter = false,
                topLeft = p(0f, 0.1f), size = Size(side, side * 0.9f), style = stroke,
            )
            drawPath(path(0.06f to 0.5f, 0.2f to 0.05f, 0.3f to 0.5f), color)
            drawPath(path(0.7f to 0.5f, 0.8f to 0.05f, 0.94f to 0.5f), color)
        }

        Mark.ANVIL -> {
            drawPath(path(0.05f to 0.2f, 0.95f to 0.2f, 0.75f to 0.5f, 0.25f to 0.5f), color)
            drawPath(path(0.38f to 0.5f, 0.62f to 0.5f, 0.62f to 0.8f, 0.38f to 0.8f), color)
            drawPath(path(0.15f to 0.8f, 0.85f to 0.8f, 0.85f to 0.96f, 0.15f to 0.96f), color)
        }

        Mark.HEXAGON -> drawPath(
            path(0.5f to 0f, 1f to 0.27f, 1f to 0.73f, 0.5f to 1f, 0f to 0.73f, 0f to 0.27f),
            color,
            style = stroke,
        )

        Mark.NESTED_SQUARE -> {
            drawPath(path(0.5f to 0f, 1f to 0.5f, 0.5f to 1f, 0f to 0.5f), color, style = stroke)
            drawPath(path(0.5f to 0.3f, 0.7f to 0.5f, 0.5f to 0.7f, 0.3f to 0.5f), color)
        }

        Mark.LEAF -> {
            val leaf = Path().apply {
                moveTo(p(0.5f, 0f).x, p(0.5f, 0f).y)
                quadraticTo(p(1f, 0.45f).x, p(1f, 0.45f).y, p(0.5f, 1f).x, p(0.5f, 1f).y)
                quadraticTo(p(0f, 0.45f).x, p(0f, 0.45f).y, p(0.5f, 0f).x, p(0.5f, 0f).y)
                close()
            }
            drawPath(leaf, color, style = stroke)
            drawPath(path(0.5f to 0.12f, 0.5f to 0.88f, close = false), color, style = stroke)
        }

        Mark.SCALES -> {
            for (row in 0..2) {
                val top = 0.08f + row * 0.3f
                for (col in 0..row) {
                    val left = 0.5f - row * 0.16f + col * 0.32f
                    drawPath(
                        path(left to top, (left + 0.14f) to (top + 0.22f), (left - 0.14f) to (top + 0.22f)),
                        color,
                    )
                }
            }
        }

        Mark.CLAWS -> {
            for (index in 0..2) {
                val offset = index * 0.3f
                drawPath(
                    path(offset to 0.05f, (offset + 0.18f) to 0.05f, (offset + 0.4f) to 0.95f,
                        (offset + 0.22f) to 0.95f),
                    color,
                )
            }
        }

        Mark.ECLIPSE -> {
            drawCircle(color, radius = side * 0.46f, center = p(0.5f, 0.5f), style = stroke)
            drawCircle(color, radius = side * 0.24f, center = p(0.5f, 0.5f))
        }
    }
}

private fun DrawScope.drawPath(path: Path, color: Color) = drawPath(path, color, style = Fill)

/**
 * Lädt ein Portrait von der Platte, sobald sich der Pfad ändert. Die Bilder sind
 * Kachelgröße — ein Bildcache wäre hier mehr Aufwand als Nutzen.
 */
@Composable
private fun rememberPortrait(path: String?): ImageBitmap? =
    produceState<ImageBitmap?>(initialValue = null, key1 = path) {
        value = path?.let {
            withContext(Dispatchers.IO) { PortraitStore.decode(it)?.asImageBitmap() }
        }
    }.value

/** Lädt das mitgelieferte Portrait einer Legende aus den Assets. */
@Composable
private fun rememberBundledPortrait(championId: String): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    return produceState<ImageBitmap?>(initialValue = null, key1 = championId) {
        value = withContext(Dispatchers.IO) {
            BundledPortraits.decode(context, championId)?.asImageBitmap()
        }
    }.value
}

/**
 * Ein ausgeschnittenes Portrait für sich — im Bestätigungsschritt eines Imports das
 * Erkennungsmerkmal, solange noch keine Legende zugeordnet ist. Fehlt der Zuschnitt,
 * bleibt die Fläche leer statt einen Platzhalter vorzutäuschen.
 */
@Composable
fun PortraitThumb(path: String?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    val portrait = rememberPortrait(path)
    Box(
        modifier
            .clip(shape)
            .background(FactionColors.Night)
            .border(1.dp, FactionColors.Border, shape),
    ) {
        if (portrait != null) {
            Image(
                bitmap = portrait,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
