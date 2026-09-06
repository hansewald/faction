package de.faction.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.faction.data.model.Masteries
import de.faction.data.model.Mastery
import de.faction.data.model.MasteryTree
import de.faction.domain.MasteryPath
import de.faction.ui.theme.FactionColors

/**
 * Der Meisterschaftsbaum als Raster — sechs Stufen, oben zwei Felder, darunter je vier.
 *
 * Der empfohlene Pfad leuchtet, alles andere bleibt gedimmt. Das ist derselbe Aufbau
 * wie im Spielfenster: wer den Plan umsetzt, findet die Felder an derselben Stelle
 * wieder. Ein Tipp auf ein Feld zeigt Name und Wirkung, auch bei den nicht
 * empfohlenen — der Plan soll nachvollziehbar sein, nicht nur befolgt werden.
 */
@Composable
fun MasteryGrid(path: MasteryPath, modifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf<Mastery?>(null) }
    val accent = treeColor(path.tree)

    Column(modifier.fillMaxWidth()) {
        Text(
            path.tree.label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(accent.copy(alpha = 0.14f))
                .padding(vertical = 8.dp),
        )

        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(accent.copy(alpha = 0.07f), FactionColors.Night)),
                )
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Masteries.tiers.forEach { tier ->
                val row = Masteries.tier(path.tree, tier)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Stufe 1 hat nur zwei Felder. Sie stehen mittig, wie im Spiel.
                    val leading = (4 - row.size) / 2
                    repeat(leading) { EmptyCell() }
                    row.forEach { mastery ->
                        MasteryCell(
                            mastery = mastery,
                            recommended = path.isRecommended(mastery),
                            accent = accent,
                            modifier = Modifier.weight(1f),
                            onClick = { selected = if (selected == mastery) null else mastery },
                        )
                    }
                    repeat(4 - row.size - leading) { EmptyCell() }
                }
            }
        }

        val shown = selected ?: path.picks.lastOrNull()
        if (shown != null) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(FactionColors.Surface)
                    .padding(12.dp),
            ) {
                Text(
                    shown.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (path.isRecommended(shown)) accent else FactionColors.TextPrimary,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    shown.effect,
                    style = MaterialTheme.typography.bodySmall,
                    color = FactionColors.TextSecondary,
                )
            }
        }
    }
}

/** Ein leeres Feld, damit kürzere Stufen mittig unter den vollen stehen. */
@Composable
private fun RowScope.EmptyCell() {
    Box(Modifier.weight(1f))
}

/**
 * Ein Feld im Baum. Empfohlene Felder tragen die Baumfarbe und ihren Namen, die
 * übrigen bleiben dunkel — so ist der Pfad auf einen Blick zu sehen.
 */
@Composable
private fun MasteryCell(
    mastery: Mastery,
    recommended: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(if (recommended) accent.copy(alpha = 0.28f) else FactionColors.Night)
            .border(
                width = if (recommended) 1.5.dp else 1.dp,
                color = if (recommended) accent else FactionColors.Border,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            mastery.name,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 8.sp,
            lineHeight = 10.sp,
            color = if (recommended) FactionColors.TextPrimary else FactionColors.TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 3,
        )
    }
}

/** Die Farben der drei Bäume, angelehnt an die Palette der App. */
private fun treeColor(tree: MasteryTree): Color = when (tree) {
    MasteryTree.OFFENSE -> Color(0xFFE0654C)
    MasteryTree.DEFENSE -> Color(0xFF6FD3A8)
    MasteryTree.SUPPORT -> FactionColors.Teal
}
