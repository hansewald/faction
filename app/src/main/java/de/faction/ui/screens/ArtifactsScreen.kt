package de.faction.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import de.faction.data.model.ArtifactSet
import de.faction.data.model.ArtifactSetContent
import de.faction.data.model.SetKind
import de.faction.ui.components.FactionCard
import de.faction.ui.components.SectionTitle
import de.faction.ui.components.Tag
import de.faction.ui.theme.FactionColors

/**
 * Nachschlagewerk der Artefakt-Sets: welche Teile ein Champion gleicher Sorte
 * braucht, und was er dafür bekommt. Ergänzt den Aufbauplan der Detailseite, der
 * konkrete Sets bereits empfiehlt — hier steht der volle Bestand zum Nachschlagen
 * und Vergleichen.
 */
@Composable
fun ArtifactsScreen(modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    var pieces by remember { mutableStateOf<Int?>(null) }
    var kind by remember { mutableStateOf<SetKind?>(null) }

    val filtered = remember(query, pieces, kind) {
        ArtifactSetContent.sets.filter { set ->
            (query.isBlank() || set.name.contains(query.trim(), ignoreCase = true)) &&
                (pieces == null || set.pieces == pieces) &&
                (kind == null || set.kind == kind)
        }.sortedWith(compareBy({ it.pieces }, { it.name }))
    }

    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                "Artefakte",
                style = MaterialTheme.typography.headlineMedium,
                color = FactionColors.TextPrimary,
            )
            Text(
                "Sets verstehen, gezielt zusammenstellen.",
                style = MaterialTheme.typography.bodyMedium,
                color = FactionColors.TextSecondary,
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Set suchen", color = FactionColors.TextSecondary) },
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
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToggleChip("Alle", pieces == null) { pieces = null }
                ToggleChip("2er-Sets", pieces == 2) { pieces = 2 }
                ToggleChip("4er-Sets", pieces == 4) { pieces = 4 }
            }
            Spacer(Modifier.height(8.dp))
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { ToggleChip("Alle Arten", kind == null) { kind = null } }
            items(SetKind.entries) { candidate ->
                ToggleChip(candidate.label, kind == candidate) { kind = candidate }
            }
        }
        Spacer(Modifier.height(4.dp))

        if (filtered.isEmpty()) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    "Kein Set passt zu diesen Filtern.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FactionColors.TextSecondary,
                )
            }
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(filtered, key = { it.id }) { set -> ArtifactSetCard(set) }
            item {
                Text(
                    "Nicht aufgeführt: Sets mit 1 oder bis zu 9 Teilen (etwa Merciless, " +
                        "Slayer, Stone Skin) — für ihre Zwischenstufen gibt es keine " +
                        "belastbar geprüfte Quelle.",
                    style = MaterialTheme.typography.labelSmall,
                    color = FactionColors.TextSecondary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ArtifactSetCard(set: ArtifactSet) {
    FactionCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    set.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = FactionColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Tag(if (set.pieces == 2) "2 Teile" else "4 Teile", FactionColors.GoldSoft)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                set.effect,
                style = MaterialTheme.typography.bodySmall,
                color = FactionColors.TextSecondary,
            )
            Spacer(Modifier.height(8.dp))
            Tag(set.kind.label, FactionColors.Teal)
        }
    }
}

@Composable
private fun ToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) FactionColors.Night else FactionColors.TextSecondary,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) FactionColors.Gold else FactionColors.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
