package de.faction.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.faction.data.model.Champion
import de.faction.domain.AccountStage
import de.faction.domain.BuildPlanner
import de.faction.domain.BuildStep
import de.faction.domain.ScoreEngine
import de.faction.ui.FactionViewModel
import de.faction.ui.components.AreaRatingTile
import de.faction.ui.components.ChampionSigil
import de.faction.ui.components.FactionCard
import de.faction.ui.components.SectionTitle
import de.faction.ui.components.StepBadge
import de.faction.ui.components.Tag
import de.faction.ui.components.TraitStrip
import de.faction.ui.theme.FactionColors

private enum class DetailTab(val label: String) {
    OVERVIEW("Übersicht"), BUILD("Aufbau"), SKILLS("Fähigkeiten")
}

@Composable
fun ChampionDetailScreen(
    championId: String,
    viewModel: FactionViewModel,
    modifier: Modifier = Modifier,
) {
    val champion = viewModel.champion(championId)
    if (champion == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Legende nicht gefunden.", color = FactionColors.TextSecondary)
        }
        return
    }

    val owned by viewModel.ownedIds.collectAsStateWithLifecycle()
    val analysis by viewModel.analysis.collectAsStateWithLifecycle()
    val portraits by viewModel.portraits.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(DetailTab.BUILD) }

    // Vorbelegt mit der erkannten Phase des Accounts; der Spieler kann sie überschreiben,
    // um zu sehen, wie sich der Plan später verändert.
    var stage by remember(analysis.stage) { mutableStateOf(analysis.stage) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            DetailHeader(
                champion = champion,
                isOwned = champion.id in owned,
                portraitPath = portraits[champion.id],
                onToggleOwned = { viewModel.toggleOwned(champion.id) },
            )
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                DetailTab.entries.forEach { candidate ->
                    DetailTabItem(candidate.label, tab == candidate, Modifier.weight(1f)) { tab = candidate }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }

        when (tab) {
            DetailTab.OVERVIEW -> overviewContent(champion)
            DetailTab.BUILD -> buildContent(
                champion = champion,
                stage = stage,
                onStageChange = { stage = it },
                isSaved = champion.id in owned,
                onSave = { viewModel.toggleOwned(champion.id) },
            )
            DetailTab.SKILLS -> skillsContent(champion)
        }
    }
}

@Composable
private fun DetailHeader(
    champion: Champion,
    isOwned: Boolean,
    portraitPath: String?,
    onToggleOwned: () -> Unit,
) {
    Box {
        ChampionSigil(
            champion = champion,
            portraitPath = portraitPath,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        )
        IconButton(onClick = onToggleOwned, modifier = Modifier.align(Alignment.TopEnd)) {
            Icon(
                imageVector = if (isOwned) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription = if (isOwned) "Aus meiner Auswahl entfernen" else "Zu meiner Auswahl",
                tint = if (isOwned) FactionColors.Gold else FactionColors.TextPrimary,
            )
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        ) {
            Text(
                champion.name,
                style = MaterialTheme.typography.headlineMedium,
                color = FactionColors.TextPrimary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${champion.faction} · ",
                    style = MaterialTheme.typography.bodySmall,
                    color = FactionColors.TextSecondary,
                )
                Text(
                    champion.rarity.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = FactionColors.rarity(champion.rarity.label),
                )
                Text(
                    " · ${champion.role.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = FactionColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun DetailTabItem(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) FactionColors.Teal else FactionColors.TextSecondary,
            modifier = Modifier.padding(vertical = 10.dp),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (selected) FactionColors.Teal else FactionColors.Border),
        )
    }
}

private fun LazyListScope.overviewContent(champion: Champion) {
    item {
        Column(Modifier.padding(horizontal = 16.dp)) {
            FactionCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    SectionTitle("Das Kit in einem Satz")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        champion.kitSummary.ifBlank { "Für diese Legende liegt noch keine Einordnung vor." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = FactionColors.TextPrimary,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
    item {
        Column(Modifier.padding(horizontal = 16.dp)) {
            TraitStrip(
                listOf(
                    "Fraktion" to champion.faction,
                    "Affinität" to champion.affinity.label,
                    "Seltenheit" to champion.rarity.label,
                    "Rolle" to champion.role.label,
                ),
            )
            Spacer(Modifier.height(12.dp))
        }
    }
    item {
        val scores = ScoreEngine.score(champion).perArea.values.sortedByDescending { it.score }
        Column(Modifier.padding(horizontal = 16.dp)) {
            FactionCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    SectionTitle("Wo diese Legende zählt")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Bewertet allein aus dem Kit — je Bereich getrennt, deshalb nur innerhalb einer Kachel vergleichbar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FactionColors.TextSecondary,
                    )
                    Spacer(Modifier.height(12.dp))
                    // Zwei Spalten: ein Raster im LazyColumn wäre verschachteltes
                    // Scrollen — bei sechs festen Bereichen reichen Zeilen zu zweit.
                    scores.chunked(2).forEach { row ->
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { score ->
                                AreaRatingTile(score, Modifier.weight(1f))
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    val best = scores.first()
                    Text(
                        best.reasons.firstOrNull()
                            ?: "Für diese Legende liegt keine Begründung vor.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FactionColors.TextSecondary,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.buildContent(
    champion: Champion,
    stage: AccountStage,
    onStageChange: (AccountStage) -> Unit,
    isSaved: Boolean,
    onSave: () -> Unit,
) {
    item {
        Column(Modifier.padding(horizontal = 16.dp)) {
            StagePicker(stage, onStageChange)
            Spacer(Modifier.height(16.dp))
            Text(
                "Dein Aufbauplan",
                style = MaterialTheme.typography.titleLarge,
                color = FactionColors.TextPrimary,
            )
            Spacer(Modifier.height(12.dp))
        }
    }

    val plan = BuildPlanner.plan(champion, stage)
    itemsIndexed(plan.steps) { index, step ->
        Column(Modifier.padding(horizontal = 16.dp)) {
            BuildStepCard(index + 1, step)
            Spacer(Modifier.height(10.dp))
        }
    }

    item {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(6.dp))
            FactionCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Werte verstehen",
                        style = MaterialTheme.typography.titleMedium,
                        color = FactionColors.TextPrimary,
                    )
                    Spacer(Modifier.height(10.dp))
                    plan.statHints.forEach { hint ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            hint.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = FactionColors.GoldSoft,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            hint.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = FactionColors.TextSecondary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSaved) FactionColors.Surface else FactionColors.Gold,
                    contentColor = if (isSaved) FactionColors.TextSecondary else FactionColors.Night,
                ),
            ) {
                Text(
                    if (isSaved) "In meiner Auswahl" else "Build speichern",
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

private fun LazyListScope.skillsContent(champion: Champion) {
    item {
        Column(Modifier.padding(horizontal = 16.dp)) {
            FactionCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    SectionTitle("Wirkungen im Kit")
                    Spacer(Modifier.height(10.dp))
                    if (champion.utilities.isEmpty()) {
                        Text(
                            "Dieses Kit bringt keinen der Effekte mit, die für Teams gebraucht werden. " +
                                "Das macht die Legende zu Aufwertungsfutter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FactionColors.TextSecondary,
                        )
                    } else {
                        champion.utilities.forEach { utility ->
                            Spacer(Modifier.height(6.dp))
                            Tag(utility.label, FactionColors.Teal)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Die einzelnen Fähigkeiten mit Multiplikatoren und Abklingzeiten sind im " +
                            "Startdatensatz noch nicht enthalten. Sie kommen mit dem Katalog-Sync.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FactionColors.TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun StagePicker(stage: AccountStage, onChange: (AccountStage) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(FactionColors.Surface)
                .clickable { expanded = true }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Spielphase: ${stage.label}",
                style = MaterialTheme.typography.bodyMedium,
                color = FactionColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = FactionColors.TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AccountStage.entries.forEach { candidate ->
                DropdownMenuItem(
                    text = { Text(candidate.label) },
                    onClick = {
                        onChange(candidate)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun BuildStepCard(number: Int, step: BuildStep) {
    var expanded by remember { mutableStateOf(number == 1) }

    FactionCard(Modifier.fillMaxWidth(), onClick = { expanded = !expanded }) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepBadge(number)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(step.title, style = MaterialTheme.typography.titleMedium, color = FactionColors.TextPrimary)
                Text(step.subtitle, style = MaterialTheme.typography.bodySmall, color = FactionColors.TextSecondary)
            }
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = FactionColors.TextSecondary,
            )
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                step.details.forEach { detail ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "· $detail",
                        style = MaterialTheme.typography.bodySmall,
                        color = FactionColors.TextSecondary,
                    )
                }
            }
        }
    }
}
