package de.faction.data.source

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import de.faction.data.model.ImportSource
import de.faction.data.model.OwnedChampion
import de.faction.data.repo.ChampionCatalog
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Liest Champion-Namen und Level aus einem Screenshot der Championliste.
 *
 * Bewusst konservativ: erkannte Einträge werden dem Spieler zur Bestätigung
 * vorgelegt, statt direkt in den Kader zu wandern. Ein falsch erkannter Champion
 * würde sonst die gesamte Analyse verfälschen.
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

    /** "Lvl. 50", "50/50" oder eine nackte Zahl am Kartenrand. */
    private val levelPattern = Regex("""(?:lvl\.?\s*)?(\d{1,2})\s*(?:/\s*\d{1,2})?""", RegexOption.IGNORE_CASE)

    override suspend fun load(input: SourceInput): Result<List<OwnedChampion>> {
        val uri = (input as? SourceInput.Image)?.uri
            ?: return Result.failure(IllegalArgumentException("Für diesen Import wird ein Bild benötigt."))

        return runCatching {
            val image = InputImage.fromFilePath(context, Uri.parse(uri))
            val lines = recognizeLines(image)
            parse(lines)
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
     * Die Championliste zeigt pro Karte den Namen und darunter das Level. Wir laufen
     * die erkannten Zeilen der Reihe nach ab und ordnen jeder Namenszeile die nächste
     * plausible Zahl zu.
     */
    private fun parse(lines: List<String>): List<OwnedChampion> {
        val result = mutableListOf<OwnedChampion>()
        var pending: String? = null

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val match = catalog.fuzzyFindByName(trimmed)
            if (match != null) {
                pending?.let { result += entryFor(it, null) }
                pending = match.id
                continue
            }

            val level = levelPattern.matchEntire(trimmed)?.groupValues?.get(1)?.toIntOrNull()
            if (level != null && level in 1..60 && pending != null) {
                result += entryFor(pending, level)
                pending = null
            }
        }
        pending?.let { result += entryFor(it, null) }
        return result
    }

    private fun entryFor(championId: String, level: Int?): OwnedChampion {
        val effectiveLevel = level ?: 1
        // Ohne erkannten Rang leiten wir ihn aus dem Level ab: Level 50 setzt 5 Sterne voraus.
        val rank = ((effectiveLevel + 9) / 10).coerceIn(1, 6)
        return OwnedChampion(
            championId = championId,
            level = effectiveLevel,
            ascension = 0,
            rank = rank,
            source = ImportSource.SCREENSHOT_OCR,
        )
    }
}
