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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.faction.data.repo.CatalogUpdate
import de.faction.ui.FactionViewModel
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
fun NewsScreen(viewModel: FactionViewModel, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val update by viewModel.catalogUpdate.collectAsStateWithLifecycle()
    val checking by viewModel.catalogChecking.collectAsStateWithLifecycle()

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
        item {
            CatalogUpdateCard(
                version = viewModel.catalogVersion(),
                checking = checking,
                update = update,
                onCheck = viewModel::checkCatalog,
            )
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

        item { Disclaimer() }
    }
}

/**
 * Pflichthinweis für eine inoffizielle Fan-App: die Abgrenzung zum Rechteinhaber muss
 * in der App stehen, nicht nur im Store-Eintrag.
 */
@Composable
private fun Disclaimer() {
    Spacer(Modifier.height(8.dp))
    FactionCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Rechtliches",
                style = MaterialTheme.typography.titleMedium,
                color = FactionColors.TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "FACTION ist eine inoffizielle Fan-App und steht in keiner Verbindung zu " +
                    "Plarium Global Ltd. RAID: Shadow Legends sowie alle Namen von Legenden, " +
                    "Fraktionen und Spielinhalten sind Marken oder Eigentum von Plarium.",
                style = MaterialTheme.typography.bodySmall,
                color = FactionColors.TextSecondary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Diese App enthält kein Bildmaterial aus dem Spiel. Die Wappen der Legenden " +
                    "sind eigene Zeichnungen aus Fraktion, Affinität und Seltenheit.",
                style = MaterialTheme.typography.bodySmall,
                color = FactionColors.TextSecondary,
            )
        }
    }
}

/**
 * Der Katalog-Abgleich.
 *
 * Neue Legenden erscheinen laufend; ohne Abgleich veraltet der Katalog zwischen zwei
 * Store-Updates. Die Karte sagt offen, welcher Stand lokal liegt und was der letzte
 * Abgleich ergeben hat — auch, wenn er fehlgeschlagen ist. Ein stiller Sync, der
 * nichts meldet, wäre hier das Schlechteste: dann steht eine fehlende Legende da wie
 * eine, die es nicht gibt.
 */
@Composable
private fun CatalogUpdateCard(
    version: Int,
    checking: Boolean,
    update: CatalogUpdate?,
    onCheck: () -> Unit,
) {
    FactionCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Champion-Katalog",
                style = MaterialTheme.typography.titleMedium,
                color = FactionColors.TextPrimary,
            )
            Text(
                if (version == 0) {
                    "Mitgelieferter Stand — noch nie abgeglichen."
                } else {
                    "Stand $version"
                },
                style = MaterialTheme.typography.bodySmall,
                color = FactionColors.TextSecondary,
            )
            Spacer(Modifier.height(10.dp))

            when (update) {
                null -> Unit
                is CatalogUpdate.UpToDate -> UpdateNote(
                    "Der Katalog ist auf dem neuesten Stand.",
                    FactionColors.TextSecondary,
                )
                is CatalogUpdate.Failed -> UpdateNote(update.reason, FactionColors.food)
                is CatalogUpdate.Applied -> Column {
                    UpdateNote(
                        "Stand ${update.version} übernommen · ${update.totalChampions} Legenden.",
                        FactionColors.Teal,
                    )
                    if (update.addedChampions.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Neu: " + update.addedChampions.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = FactionColors.TextSecondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onCheck,
                enabled = !checking,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FactionColors.Surface,
                    contentColor = FactionColors.TextPrimary,
                ),
            ) {
                Text(if (checking) "Wird abgeglichen …" else "Auf neue Legenden prüfen")
            }
        }
    }
}

@Composable
private fun UpdateNote(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = color)
}
