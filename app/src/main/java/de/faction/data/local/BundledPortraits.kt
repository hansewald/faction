package de.faction.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * Die mitgelieferten Champion-Portraits aus `assets/portraits`.
 *
 * Die Bilder sind Artworks von Plarium. Ihre Nutzung ist durch die Genehmigung in
 * `docs/plarium-genehmigung.md` gedeckt, gebunden an Auflagen: Die App bleibt
 * kostenlos, erzielt keinerlei Einnahmen, nennt Plarium als Rechteinhaber und weist
 * sich als inoffiziell aus. Wer hier Geld ins Spiel bringt, beendet die Genehmigung.
 *
 * Erzeugt werden die Dateien von `tools/generate_portraits.py`. Für 20 Legenden ist
 * die Quelldatei beschädigt; sie tragen weiter ihr gezeichnetes Wappen.
 */
object BundledPortraits {

    private const val DIRECTORY = "portraits"
    private const val EXTENSION = "webp"

    @Volatile private var available: Set<String>? = null

    /**
     * Die Kennungen, für die ein Portrait vorliegt. Einmal gelesen, danach aus dem
     * Speicher: so muss nicht jede Kachel eine fehlende Datei per Ausnahme erfahren.
     */
    private fun available(context: Context): Set<String> = available ?: synchronized(this) {
        available ?: context.assets.list(DIRECTORY).orEmpty()
            .filter { it.endsWith(".$EXTENSION") }
            .map { it.removeSuffix(".$EXTENSION") }
            .toSet()
            .also { available = it }
    }

    /** Lädt das Portrait einer Legende. `null`, wenn keines mitgeliefert wird. */
    fun decode(context: Context, championId: String): Bitmap? {
        if (championId !in available(context)) return null
        return runCatching {
            context.assets.open("$DIRECTORY/$championId.$EXTENSION").use(BitmapFactory::decodeStream)
        }.getOrNull()
    }
}
