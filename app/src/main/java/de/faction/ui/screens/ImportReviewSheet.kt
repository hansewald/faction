package de.faction.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.faction.data.model.Champion
import de.faction.data.model.ImportCandidate
import de.faction.ui.FactionViewModel
import de.faction.ui.ImportReview
import de.faction.ui.components.Tag
import de.faction.ui.theme.FactionColors

/**
 * Bestätigungsschritt eines Imports.
 *
 * Nichts wird übernommen, ohne dass der Spieler es gesehen hat. Sichere Treffer sind
 * vorausgewählt, unsichere nicht — und eine falsch erkannte Zeile lässt sich hier
 * korrigieren, statt sie verwerfen zu müssen.
 */
@Composable
fun ImportReviewDialog(review: ImportReview, viewModel: FactionViewModel) {
    var assigningIndex by remember { mutableStateOf<Int?>(null) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var replaceRoster by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = viewModel::cancelImport,
        containerColor = FactionColors.Surface,
        title = {
            Column {
                Text("Import prüfen", color = FactionColors.TextPrimary)
                Text(
                    review.sourceLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = FactionColors.TextSecondary,
                )
            }
        },
        text = {
            Column {
                ReviewSummary(review)
                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { viewModel.setAllCandidates(true) }) { Text("Alle") }
                    TextButton(onClick = { viewModel.setAllCandidates(false) }) { Text("Keine") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { replaceRoster = !replaceRoster }) {
                        Text(
                            if (replaceRoster) "Kader ersetzen ✓" else "Kader ersetzen",
                            color = if (replaceRoster) FactionColors.Gold else FactionColors.TextSecondary,
                        )
                    }
                }

                LazyColumn(Modifier.heightIn(max = 340.dp)) {
                    itemsIndexed(review.candidates) { index, candidate ->
                        CandidateRow(
                            candidate = candidate,
                            championName = candidate.championId
                                ?.let { viewModel.champion(it)?.name },
                            onToggle = { viewModel.toggleCandidate(index) },
                            onAssign = { assigningIndex = index },
                            onEdit = { editingIndex = index },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.confirmImport(replaceRoster) },
                enabled = review.acceptedCount > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FactionColors.Gold,
                    contentColor = FactionColors.Night,
                ),
            ) { Text("${review.acceptedCount} übernehmen") }
        },
        dismissButton = {
            TextButton(onClick = viewModel::cancelImport) { Text("Abbrechen") }
        },
    )

    assigningIndex?.let { index ->
        ChampionPickerDialog(
            viewModel = viewModel,
            initialQuery = review.candidates.getOrNull(index)?.rawLabel.orEmpty(),
            onDismiss = { assigningIndex = null },
            onPick = { champion ->
                viewModel.assignCandidate(index, champion.id)
                assigningIndex = null
            },
        )
    }

    editingIndex?.let { index ->
        review.candidates.getOrNull(index)?.let { candidate ->
            LevelEditDialog(
                candidate = candidate,
                onDismiss = { editingIndex = null },
                onApply = { level, rank ->
                    viewModel.setCandidateLevel(index, level, rank)
                    editingIndex = null
                },
            )
        }
    }
}

@Composable
private fun ReviewSummary(review: ImportReview) {
    val problems = review.unresolvedCount + review.uncertainCount
    Column {
        Text(
            "${review.candidates.size} Zeilen erkannt, ${review.acceptedCount} vorausgewählt.",
            style = MaterialTheme.typography.bodySmall,
            color = FactionColors.TextSecondary,
        )
        if (problems > 0) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = FactionColors.Gold,
                    modifier = Modifier.width(20.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    buildString {
                        if (review.unresolvedCount > 0) {
                            append("${review.unresolvedCount} Zeilen ohne Zuordnung")
                        }
                        if (review.unresolvedCount > 0 && review.uncertainCount > 0) append(", ")
                        if (review.uncertainCount > 0) {
                            append("${review.uncertainCount} unsichere Treffer")
                        }
                        append(". Diese sind nicht vorausgewählt — tippe eine Zeile an, um sie zuzuordnen.")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = FactionColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun CandidateRow(
    candidate: ImportCandidate,
    championName: String?,
    onToggle: () -> Unit,
    onAssign: () -> Unit,
    onEdit: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onToggle, enabled = candidate.isResolved) {
            Icon(
                imageVector = if (candidate.accepted) {
                    Icons.Filled.CheckBox
                } else {
                    Icons.Filled.CheckBoxOutlineBlank
                },
                contentDescription = if (candidate.accepted) "Abwählen" else "Auswählen",
                tint = when {
                    !candidate.isResolved -> FactionColors.Border
                    candidate.accepted -> FactionColors.Teal
                    else -> FactionColors.TextSecondary
                },
            )
        }

        Column(
            Modifier
                .weight(1f)
                .clickable(onClick = onAssign),
        ) {
            Text(
                championName ?: "Nicht zugeordnet",
                style = MaterialTheme.typography.bodyMedium,
                color = if (championName != null) FactionColors.TextPrimary else FactionColors.food,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "„${candidate.rawLabel}“ · ${candidate.rank}★ Lvl ${candidate.level}",
                    style = MaterialTheme.typography.bodySmall,
                    color = FactionColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (candidate.isResolved && candidate.confidence < ImportCandidate.RELIABLE) {
                    Spacer(Modifier.width(6.dp))
                    Tag("unsicher", FactionColors.Gold)
                }
            }
        }

        IconButton(onClick = onEdit, enabled = candidate.isResolved) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = "Rang und Level bearbeiten",
                tint = FactionColors.TextSecondary,
            )
        }
    }
}

/** Auswahl einer Legende aus dem Katalog, vorbelegt mit dem erkannten Text. */
@Composable
private fun ChampionPickerDialog(
    viewModel: FactionViewModel,
    initialQuery: String,
    onDismiss: () -> Unit,
    onPick: (Champion) -> Unit,
) {
    var query by remember { mutableStateOf(initialQuery) }
    val results = remember(query) { viewModel.searchCatalog(query).take(40) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FactionColors.Surface,
        title = { Text("Legende zuordnen", color = FactionColors.TextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                if (results.isEmpty()) {
                    Text(
                        "Keine Legende mit diesem Namen im Katalog.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FactionColors.TextSecondary,
                    )
                }
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(results, key = { it.id }) { champion ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(champion) }
                                .padding(vertical = 8.dp),
                        ) {
                            Text(
                                champion.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = FactionColors.TextPrimary,
                            )
                            Text(
                                "${champion.rarity.label} · ${champion.faction}",
                                style = MaterialTheme.typography.bodySmall,
                                color = FactionColors.TextSecondary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schließen") } },
    )
}

@Composable
private fun LevelEditDialog(
    candidate: ImportCandidate,
    onDismiss: () -> Unit,
    onApply: (level: Int, rank: Int) -> Unit,
) {
    var level by remember { mutableStateOf(candidate.level.toString()) }
    var rank by remember { mutableStateOf(candidate.rank.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FactionColors.Surface,
        title = { Text("Rang und Level", color = FactionColors.TextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = rank,
                    onValueChange = { rank = it.filter(Char::isDigit).take(1) },
                    label = { Text("Sterne (1–6)") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = level,
                    onValueChange = { level = it.filter(Char::isDigit).take(2) },
                    label = { Text("Level") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(level.toIntOrNull() ?: 1, rank.toIntOrNull() ?: 1)
                },
            ) { Text("Übernehmen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
