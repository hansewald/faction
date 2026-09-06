package de.faction.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.faction.data.model.Champion
import de.faction.data.model.Rarity
import de.faction.data.model.Role
import de.faction.data.source.SourceInput
import de.faction.ui.FactionViewModel
import de.faction.ui.components.ChampionSigil
import de.faction.ui.components.FactionCard
import de.faction.ui.components.SectionTitle
import de.faction.ui.components.Tag
import de.faction.ui.theme.FactionColors

@Composable
fun LegendsScreen(
    viewModel: FactionViewModel,
    onOpenChampion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val legends by viewModel.legends.collectAsStateWithLifecycle()
    val owned by viewModel.ownedIds.collectAsStateWithLifecycle()
    val review by viewModel.importReview.collectAsStateWithLifecycle()

    review?.let { ImportReviewDialog(it, viewModel) }

    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                "Legenden",
                style = MaterialTheme.typography.headlineMedium,
                color = FactionColors.TextPrimary,
            )
            Text(
                "Verstehen. Vergleichen. Aufbauen.",
                style = MaterialTheme.typography.bodyMedium,
                color = FactionColors.TextSecondary,
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = filters.query,
                onValueChange = { text -> viewModel.updateFilters { it.copy(query = text) } },
                placeholder = { Text("Legende suchen", color = FactionColors.TextSecondary) },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = FactionColors.TextSecondary)
                },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FactionColors.Gold,
                    unfocusedBorderColor = FactionColors.Border,
                    focusedContainerColor = FactionColors.Surface,
                    unfocusedContainerColor = FactionColors.Surface,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            SegmentedTabs(
                onlyMine = filters.onlyMine,
                mineCount = owned.size,
                onSelect = { mine -> viewModel.updateFilters { it.copy(onlyMine = mine) } },
            )
            Spacer(Modifier.height(12.dp))

            if (filters.onlyMine) {
                ImportRow(viewModel)
                Spacer(Modifier.height(12.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterDropdown(
                    label = "Fraktion",
                    selected = filters.faction,
                    options = viewModel.factions(),
                    optionLabel = { it },
                    onSelect = { faction -> viewModel.updateFilters { it.copy(faction = faction) } },
                    modifier = Modifier.weight(1f),
                )
                FilterDropdown(
                    label = "Seltenheit",
                    selected = filters.rarity,
                    options = Rarity.entries,
                    optionLabel = { it.label },
                    onSelect = { rarity -> viewModel.updateFilters { it.copy(rarity = rarity) } },
                    modifier = Modifier.weight(1f),
                )
                FilterDropdown(
                    label = "Rolle",
                    selected = filters.role,
                    options = Role.entries,
                    optionLabel = { it.label },
                    onSelect = { role -> viewModel.updateFilters { it.copy(role = role) } },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (legends.isEmpty()) {
            EmptyLegends(filters.onlyMine)
            return@Column
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(legends, key = { it.id }) { champion ->
                LegendCard(
                    champion = champion,
                    isOwned = champion.id in owned,
                    onToggleOwned = { viewModel.toggleOwned(champion.id) },
                    onClick = { onOpenChampion(champion.id) },
                )
            }
        }
    }
}

/**
 * Einstieg in den Screenshot-Import. Er steht bewusst nur unter "Meine Auswahl":
 * dort geht es um den eigenen Kader, nicht um das Nachschlagewerk.
 */
@Composable
private fun ImportRow(viewModel: FactionViewModel) {
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val ocrSource = remember(viewModel) { viewModel.accountSources.first { it.id == "screenshot-ocr" } }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.import(ocrSource, SourceInput.Image(it.toString())) }
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { pickImage.launch("image/*") },
                enabled = !importState.running,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FactionColors.GoldSoft),
            ) {
                Text(if (importState.running) "Lese Screenshot…" else "Screenshot lesen")
            }
        }
        importState.message?.let { message ->
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (importState.isError) FactionColors.food else FactionColors.Teal,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = viewModel::clearImportMessage) { Text("OK") }
            }
        }
    }
}

@Composable
private fun SegmentedTabs(onlyMine: Boolean, mineCount: Int, onSelect: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        SegmentTab("Alle Legenden", selected = !onlyMine, Modifier.weight(1f)) { onSelect(false) }
        SegmentTab("Meine Auswahl ($mineCount)", selected = onlyMine, Modifier.weight(1f)) { onSelect(true) }
    }
}

@Composable
private fun SegmentTab(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) FactionColors.Teal else FactionColors.TextSecondary,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (selected) FactionColors.Teal else FactionColors.Border),
        )
    }
}

/** Ein Filter-Ausklappmenue. `null` als Auswahl bedeutet "alle". */
@Composable
private fun <T> FilterDropdown(
    label: String,
    selected: T?,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(FactionColors.Surface)
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selected?.let(optionLabel) ?: label,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected != null) FactionColors.GoldSoft else FactionColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = FactionColors.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Alle") },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun LegendCard(
    champion: Champion,
    isOwned: Boolean,
    onToggleOwned: () -> Unit,
    onClick: () -> Unit,
) {
    FactionCard(onClick = onClick) {
        Box {
            ChampionSigil(
                champion = champion,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
            )
            IconButton(
                onClick = onToggleOwned,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(
                    imageVector = if (isOwned) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = if (isOwned) "Aus meiner Auswahl entfernen" else "Zu meiner Auswahl",
                    tint = if (isOwned) FactionColors.Gold else FactionColors.TextPrimary,
                )
            }
        }
        Column(Modifier.padding(10.dp)) {
            Text(
                champion.name,
                style = MaterialTheme.typography.titleMedium,
                color = FactionColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Tag(champion.rarity.label, FactionColors.rarity(champion.rarity.label))
                if (!champion.dataComplete) {
                    // Ohne Kit-Daten waere jede Bewertung irrefuehrend.
                    Tag("Daten fehlen", FactionColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun EmptyLegends(onlyMine: Boolean) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        SectionTitle(if (onlyMine) "Meine Auswahl" else "Keine Treffer")
        Spacer(Modifier.height(8.dp))
        Text(
            if (onlyMine) {
                "Du hast noch keine Legende gespeichert. Tippe auf das Lesezeichen einer Karte, " +
                    "um sie in deinen Kader aufzunehmen — die Analyse auf dem Start-Reiter baut darauf auf."
            } else {
                "Keine Legende passt zu diesen Filtern."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = FactionColors.TextSecondary,
        )
    }
}
