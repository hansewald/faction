package de.faction.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.ui.graphics.vector.ImageVector

/** Die fünf Reiter der unteren Navigation. */
enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    START("start", "Start", Icons.Filled.Home),
    LEGENDS("legends", "Legenden", Icons.Filled.Shield),
    QUESTS("quests", "Quests", Icons.AutoMirrored.Filled.Assignment),
    GUIDE("guide", "Anleitung", Icons.AutoMirrored.Filled.MenuBook),
    NEWS("news", "News", Icons.Filled.Newspaper),
}

object Routes {
    /** Detailseite einer Legende, erreichbar aus dem Legenden-Reiter. */
    const val CHAMPION_DETAIL = "legends/{championId}"
    /** Nachschlagewerk der Artefakt-Sets, erreichbar vom Start-Reiter. */
    const val ARTIFACTS = "artifacts"

    fun championDetail(championId: String) = "legends/$championId"
}
