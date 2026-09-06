package de.faction.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.faction.data.model.GuideContent
import de.faction.domain.Verdict
import de.faction.ui.FactionViewModel
import de.faction.ui.components.DiscoveryTile
import de.faction.ui.components.FactionCard
import de.faction.ui.components.GoldProgress
import de.faction.ui.components.SectionTitle
import de.faction.ui.components.Tag
import de.faction.ui.theme.FactionColors

@Composable
fun StartScreen(
    viewModel: FactionViewModel,
    onOpenGuide: () -> Unit,
    onOpenLegends: () -> Unit,
    onOpenQuests: () -> Unit,
    onOpenNews: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by viewModel.guideProgressRatio.collectAsStateWithLifecycle()
    val readChapters by viewModel.readChapters.collectAsStateWithLifecycle()
    val analysis by viewModel.analysis.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            NextStepCard(
                readCount = readChapters.size,
                total = GuideContent.chapters.size,
                progress = progress,
                onContinue = onOpenGuide,
            )
        }

        // Sobald ein Kader erfasst ist, ist die konkrete Empfehlung wertvoller als jede
        // allgemeine Kachel - sie steht deshalb weiter oben.
        analysis.recommendations.firstOrNull { it.verdict == Verdict.BUILD_NOW }?.let { top ->
            item {
                TopRecommendationCard(
                    championName = top.entry.champion.name,
                    areaLabel = top.bestArea.label,
                    nextStep = top.nextStep,
                    onOpen = onOpenLegends,
                )
            }
        }

        item { SectionTitle("Entdecke FACTION") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DiscoveryTile(
                    icon = Icons.Filled.Shield,
                    title = "Legenden",
                    subtitle = "Finden & vergleichen",
                    onClick = onOpenLegends,
                    modifier = Modifier.weight(1f),
                )
                DiscoveryTile(
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    title = "Quests",
                    subtitle = "Aufgaben im Blick",
                    onClick = onOpenQuests,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DiscoveryTile(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "Anleitung",
                    subtitle = "Mechaniken verstehen",
                    onClick = onOpenGuide,
                    modifier = Modifier.weight(1f),
                )
                DiscoveryTile(
                    icon = Icons.Filled.AutoAwesome,
                    title = "Builds",
                    subtitle = "Helden entwickeln",
                    onClick = onOpenLegends,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item { DeveloperNewsCard(onOpenNews) }
    }
}

@Composable
private fun NextStepCard(readCount: Int, total: Int, progress: Float, onContinue: () -> Unit) {
    FactionCard(Modifier.fillMaxWidth()) {
        // Platzhalter fuer das Kapitelbild.
        Box(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(
                    Brush.verticalGradient(listOf(FactionColors.SurfaceRaised, FactionColors.Night)),
                ),
        )
        Column(Modifier.padding(16.dp)) {
            Text(
                "Dein\nnächster Schritt",
                style = MaterialTheme.typography.headlineMedium,
                color = FactionColors.TextPrimary,
            )
            Spacer(Modifier.height(6.dp))
            SectionTitle("Anfänger-Guide")
            Spacer(Modifier.height(10.dp))
            Text(
                "$readCount von $total Kapiteln",
                style = MaterialTheme.typography.bodySmall,
                color = FactionColors.TextSecondary,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                GoldProgress(progress, Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                Text(
                    "${(progress * 100).toInt()} %",
                    style = MaterialTheme.typography.bodySmall,
                    color = FactionColors.TextSecondary,
                )
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FactionColors.Gold,
                    contentColor = FactionColors.Night,
                ),
            ) { Text("Weiterlernen", fontWeight = FontWeight.Medium) }
        }
    }
}

@Composable
private fun TopRecommendationCard(
    championName: String,
    areaLabel: String,
    nextStep: String,
    onOpen: () -> Unit,
) {
    FactionCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Aus deinem Kader", Modifier.weight(1f))
                Tag("Jetzt aufbauen", FactionColors.buildNow)
            }
            Spacer(Modifier.height(10.dp))
            Text(championName, style = MaterialTheme.typography.titleLarge, color = FactionColors.TextPrimary)
            Text(
                "Stärkster Bereich: $areaLabel",
                style = MaterialTheme.typography.bodySmall,
                color = FactionColors.Teal,
            )
            Spacer(Modifier.height(8.dp))
            Text(nextStep, style = MaterialTheme.typography.bodyMedium, color = FactionColors.TextSecondary)
        }
    }
}

@Composable
private fun DeveloperNewsCard(onOpenNews: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    FactionCard(Modifier.fillMaxWidth(), onClick = onOpenNews) {
        Column(
            Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Entwickler-News",
                style = MaterialTheme.typography.titleLarge,
                color = FactionColors.GoldSoft,
            )
            Text(
                "Patchnotes & Ankündigungen",
                style = MaterialTheme.typography.bodySmall,
                color = FactionColors.TextSecondary,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { uriHandler.openUri("https://raidshadowlegends.com/news") }) {
                Text("Offizielle Beiträge ansehen", color = FactionColors.Teal)
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = FactionColors.Teal,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}
