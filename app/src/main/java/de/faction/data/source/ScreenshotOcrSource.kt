package de.faction.data.source

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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
 */
class ScreenshotOcrSource(
    private val context: Context,
    private val catalog: ChampionCatalog,
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
            val image = InputImage.fromFilePath(context, Uri.parse(uri))
            parse(recognizeLines(image))
        }
    }

    private suspend fun recognizeLines(image: InputImage): List<String> =
        suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { text ->
                    cont.resume(text.textBlocks.flatMap { block -> block.lines.map { it.text } })
                }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    /**
     * Die Championliste zeigt je Karte den Namen und darunter das Level. Wir laufen
     * die erkannten Zeilen der Reihe nach ab und ordnen jeder Namenszeile die nächste
     * plausible Zahl zu.
     */
    private fun parse(lines: List<String>): List<ImportCandidate> {
        val candidates = mutableListOf<ImportCandidate>()
        var pending: Pending? = null

        fun flush(level: Int?) {
            val open = pending ?: return
            candidates += open.toCandidate(level)
            pending = null
        }

        for (line in lines) {
            val text = line.trim()
            if (text.isEmpty()) continue

            val level = levelPattern.matchEntire(text)?.groupValues?.get(1)?.toIntOrNull()
            if (level != null && level in 1..60) {
                // Eine Zahl gehört zur zuletzt gesehenen Namenszeile.
                if (pending != null) flush(level)
                continue
            }

            if (!plausibleName.matches(text)) continue

            val match = catalog.matchByName(text)
            // Ein neuer Name schließt den vorigen Vorschlag ohne erkanntes Level ab.
            flush(null)
            pending = Pending(text, match)
        }
        flush(null)

        return candidates
    }

    private data class Pending(val rawLabel: String, val match: NameMatch?) {
        fun toCandidate(level: Int?): ImportCandidate {
            val effectiveLevel = level ?: 1
            // Ohne erkannten Rang leiten wir ihn aus dem Level ab: Level 50 setzt
            // mindestens 5 Sterne voraus.
            val rank = ((effectiveLevel + 9) / 10).coerceIn(1, 6)
            return ImportCandidate(
                rawLabel = rawLabel,
                championId = match?.champion?.id,
                level = effectiveLevel,
                rank = rank,
                // Ein nicht erkanntes Level senkt die Verlässlichkeit des Vorschlags:
                // Rang und Level sind dann geraten.
                confidence = (match?.similarity ?: 0f) * if (level == null) 0.8f else 1f,
                source = ImportSource.SCREENSHOT_OCR,
            )
        }
    }
}
