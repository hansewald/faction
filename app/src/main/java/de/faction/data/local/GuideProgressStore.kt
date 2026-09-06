package de.faction.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Merkt sich, welche Guide-Kapitel der Spieler bereits gelesen hat. */
class GuideProgressStore(context: Context) {

    private val prefs = context.getSharedPreferences("guide-progress", Context.MODE_PRIVATE)

    private val _read = MutableStateFlow(prefs.getStringSet(KEY, emptySet()).orEmpty())
    val read: StateFlow<Set<String>> = _read.asStateFlow()

    fun toggle(chapterId: String) {
        val updated = _read.value.toMutableSet().apply {
            if (!add(chapterId)) remove(chapterId)
        }
        // Die Menge muss kopiert werden: SharedPreferences gibt beim Lesen dieselbe
        // Instanz zurück, die es intern hält.
        prefs.edit().putStringSet(KEY, HashSet(updated)).apply()
        _read.value = updated
    }

    private companion object {
        const val KEY = "read-chapters"
    }
}
