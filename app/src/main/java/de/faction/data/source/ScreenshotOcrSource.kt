package de.faction.data.source

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import de.faction.data.local.PortraitStore
import de.faction.data.model.ImportCandidate
import de.faction.data.model.ImportSource
import de.faction.data.repo.ChampionCatalog
import de.faction.data.repo.NameMatch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Liest Legendennamen und Level aus einem Screenshot der Championliste.
 *
 * Die Quelle ordnet zu, entscheidet aber nichts: jede Zeile wird als Vorschlag
 * zurückgegeben, auch wenn die Zuordnung unsicher ist oder ganz fehlschlägt.
 * Bestätigt wird im Bestätigungsschritt der Oberfläche.
 *
 * Zu jedem Vorschlag wird zusätzlich die zugehörige Kachel ausgeschnitten und als
 * Portrait hinterlegt. Das ist der Grund, warum auch die Rasteransicht ohne Namen
 * etwas hergibt: dort ist das Bild das einzige Erkennungsmerkmal, und die Zuordnung
 * trifft der Spieler danach im Bestätigungsschritt.
 */
class ScreenshotOcrSource(
    private val context: Context,
    private val catalog: ChampionCatalog,
    private val portraits: PortraitStore,
) : AccountSource {

    override val id = "screenshot-ocr"
    override val label = "Screenshot der Championliste"
    override val description =
        "Öffne im Spiel deine Championliste und mache einen Screenshot. Kein Zugriff auf deinen Account."

    override suspend fun isAvailable() = true

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** "Lvl. 50", "50/50" oder eine nackte Zahl unter der Karte. */
    private val levelPattern =
        Regex("""(?:lvl\.?\s*)?(\d{1,2})\s*(?:/\s*\d{1,2})?""", RegexOption.IGNORE_CASE)

    /** Zeilen, die überhaupt ein Name sein können. */
    private val plausibleName = Regex("""^[\p{L}][\p{L}\p{Zs}'\-.]{2,}$""")

    override suspend fun load(input: SourceInput): Result<List<ImportCandidate>> {
        val uri = (input as? SourceInput.Image)?.uri
            ?: return Result.failure(IllegalArgumentException("Für diesen Import wird ein Bild benötigt."))

        return runCatching {
            val parsed = Uri.parse(uri)
            val bitmap = readBitmap(parsed)
            val lines = recognizeLines(InputImage.fromFilePath(context, parsed))
            val found = parse(lines)
            attachPortraits(found, bitmap)
        }
    }

    private fun toBox(rect: android.graphics.Rect) =
        TileGrid.Box(rect.left, rect.top, rect.right, rect.bottom)

    private fun readBitmap(uri: Uri): Bitmap? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            // Zuschneiden setzt ein veränderbares Bild voraus.
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }.getOrNull()

    private suspend fun recognizeLines(image: InputImage): List<Line> =
        suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { text: Text ->
                    cont.resume(
                        text.textBlocks.flatMap { block ->
                            block.lines.map { line ->
                                Line(line.text, line.boundingBox?.let(::toBox))
                            }
                        },
                    )
                }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    /**
     * Die Championliste zeigt je Kachel das Level, in der Listenansicht zusätzlich den
     * Namen. Wir laufen die erkannten Zeilen der Reihe nach ab und ordnen jeder
     * Namenszeile die nächste plausible Zahl zu. Eine Zahl ohne Namen ist kein Fehler,
     * sondern die Rasteransicht — sie wird zum Vorschlag ohne Zuordnung.
     */
    private fun parse(lines: List<Line>): List<Found> {
        val found = mutableListOf<Found>()
        var pending: Pending? = null

        for (line in lines) {
            val text = line.text.trim()
            if (text.isEmpty()) continue

            val level = levelPattern.matchEntire(text)?.groupValues?.get(1)?.toIntOrNull()
            if (level != null && level in 1..60) {
                // Eine Zahl gehört zur zuletzt gesehenen Namenszeile — oder zu keiner.
                found += Found(pending, level, line.box)
                pending = null
                continue
            }

            if (!plausibleName.matches(text)) continue

            // Ein neuer Name schließt den vorigen Vorschlag ohne erkanntes Level ab.
            pending?.let { found += Found(it, null, null) }
            pending = Pending(text, catalog.matchByName(text))
        }
        pending?.let { found += Found(it, null, null) }

        return found
    }

    /**
     * Schneidet zu jedem Vorschlag seine Kachel aus.
     *
     * Vorschläge ohne Namen *und* ohne Kachel fallen dabei weg: eine Zahl, die weder zu
     * einer Legende noch ins Raster passt, stammt aus der übrigen Oberfläche — etwa der
     * Zähler am Filterknopf.
     */
    private suspend fun attachPortraits(found: List<Found>, bitmap: Bitmap?): List<ImportCandidate> {
        val tiles = bitmap
            ?.let { TileGrid.tilesFor(found.map { entry -> entry.levelBox }, it.width, it.height) }
            ?: found.map { null }

        return found.mapIndexedNotNull { index, entry ->
            val tile = tiles[index]
            if (entry.pending == null && tile == null) return@mapIndexedNotNull null
            val path = tile?.let { crop(bitmap, it) }
                ?.let { portraits.stage(it, "tile-$index") }
            entry.toCandidate(path)
        }
    }

    private fun crop(bitmap: Bitmap?, tile: TileGrid.Box): Bitmap? = runCatching {
        Bitmap.createBitmap(bitmap!!, tile.left, tile.top, tile.width, tile.height)
    }.getOrNull()

    private data class Line(val text: String, val box: TileGrid.Box?)

    private data class Pending(val rawLabel: String, val match: NameMatch?)

    private data class Found(val pending: Pending?, val level: Int?, val levelBox: TileGrid.Box?) {
        fun toCandidate(portraitPath: String?): ImportCandidate {
            val effectiveLevel = level ?: 1
            // Ohne erkannten Rang leiten wir ihn aus dem Level ab: Level 50 setzt
            // mindestens 5 Sterne voraus.
            val rank = ((effectiveLevel + 9) / 10).coerceIn(1, 6)
            return ImportCandidate(
                rawLabel = pending?.rawLabel ?: "Kachel ohne Namen",
                championId = pending?.match?.champion?.id,
                level = effectiveLevel,
                rank = rank,
                // Ein nicht erkanntes Level senkt die Verlässlichkeit des Vorschlags:
                // Rang und Level sind dann geraten.
                confidence = (pending?.match?.similarity ?: 0f) * if (level == null) 0.8f else 1f,
                source = ImportSource.SCREENSHOT_OCR,
                portraitPath = portraitPath,
            )
        }
    }
}
