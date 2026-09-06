package de.faction.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Die Portraits des eigenen Kaders — Bilder, die der Spieler selbst beisteuert.
 *
 * Ausgeliefert wird davon nichts: die Bilder stammen aus dem Screenshot des Spielers,
 * liegen in seinem privaten App-Verzeichnis und verlassen das Gerät nicht. Die App
 * selbst enthält weiterhin kein Bildmaterial aus dem Spiel (siehe [ChampionSigil]),
 * denn dessen Rechte liegen bei Plarium.
 *
 * Fehlt zu einer Legende ein Bild, zeichnet die Oberfläche wie bisher das Wappen.
 */
class PortraitStore(context: Context) {

    private val dir = File(context.filesDir, DIRECTORY).apply { mkdirs() }

    /** Zwischenablage für Zuschnitte, die noch nicht bestätigt sind. */
    private val staging = File(context.cacheDir, STAGING).apply { mkdirs() }

    private val _paths = MutableStateFlow(readFromDisk())

    /** Championid -> Dateipfad. Treibt die Oberfläche an. */
    val paths: StateFlow<Map<String, String>> = _paths.asStateFlow()

    /**
     * Legt einen Zuschnitt vorläufig ab. Er gehört noch zu keiner Legende — welcher,
     * entscheidet sich erst im Bestätigungsschritt des Imports.
     */
    suspend fun stage(bitmap: Bitmap, key: String): String? = withContext(Dispatchers.IO) {
        write(File(staging, "$key.$EXTENSION"), bitmap)?.absolutePath
    }

    /**
     * Übernimmt einen bestätigten Zuschnitt als Portrait einer Legende. Ein bereits
     * vorhandenes Bild wird ersetzt — der neuere Screenshot zeigt den aktuellen Stand.
     */
    suspend fun adopt(championId: String, stagedPath: String) = withContext(Dispatchers.IO) {
        val staged = File(stagedPath)
        if (!staged.isFile) return@withContext
        val target = fileFor(championId)
        staged.copyTo(target, overwrite = true)
        publish()
    }

    suspend fun remove(championId: String) = withContext(Dispatchers.IO) {
        fileFor(championId).delete()
        publish()
    }

    /** Verwirft die Zwischenablage — nach Abschluss oder Abbruch eines Imports. */
    suspend fun clearStaging() = withContext(Dispatchers.IO) {
        staging.listFiles()?.forEach { it.delete() }
        Unit
    }

    private fun fileFor(championId: String) = File(dir, "$championId.$EXTENSION")

    private fun publish() {
        _paths.value = readFromDisk()
    }

    private fun readFromDisk(): Map<String, String> =
        dir.listFiles().orEmpty()
            .filter { it.isFile && it.extension == EXTENSION }
            .associate { it.nameWithoutExtension to it.absolutePath }

    private fun write(target: File, bitmap: Bitmap): File? = runCatching {
        target.outputStream().use { out ->
            // JPEG statt WEBP: verlustbehaftetes WEBP setzt API 30 voraus, die App ab 26.
            bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
        }
        target
    }.getOrNull()

    companion object {
        private const val DIRECTORY = "portraits"
        private const val STAGING = "portrait-staging"
        private const val EXTENSION = "jpg"
        private const val QUALITY = 92

        /** Lädt ein Portrait von der Platte. `null`, wenn die Datei fehlt oder defekt ist. */
        fun decode(path: String): Bitmap? = runCatching {
            BitmapFactory.decodeFile(path)
        }.getOrNull()
    }
}
