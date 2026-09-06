package de.faction.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import de.faction.ui.components.FactionCard
import de.faction.ui.theme.FactionColors

private data class NewsSource(val title: String, val description: String, val url: String)

/**
 * Neuigkeiten erscheinen als Verweise auf die offiziellen Kanäle, nicht als
 * gespiegelte Inhalte: die Texte gehören Plarium, und ein Link bleibt immer aktuell.
 */
private val sources = listOf(
    NewsSource(
        "Offizielle News",
        "Patchnotes, Events und Ankündigungen direkt vom Entwickler.",
        "https://raidshadowlegends.com/news",
    ),
    NewsSource(
        "Offizielles Forum",
        "Diskussionen und Erläuterungen zu jedem Update.",
        "https://forum.plarium.com/en/raid-shadow-legends/",
    ),
    NewsSource(
        "Spieler-Support",
        "Account-Sicherheit, Fehler melden, Fortschritt sichern.",
        "https://raid-support.plarium.com/",
    ),
)

@Composable
fun NewsScreen(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(
                    "News",
                    style = MaterialTheme.typography.headlineMedium,
                    color = FactionColors.TextPrimary,
                )
                Text(
                    "Direkt von den offiziellen Kanälen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FactionColors.TextSecondary,
                )
                Spacer(Modifier.height(4.dp))
            }
        }
        items(sources, key = { it.url }) { source ->
            FactionCard(Modifier.fillMaxWidth(), onClick = { uriHandler.openUri(source.url) }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            source.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = FactionColors.TextPrimary,
                        )
                        Text(
                            source.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = FactionColors.TextSecondary,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = FactionColors.Teal,
                    )
                }
            }
        }
    }
}
