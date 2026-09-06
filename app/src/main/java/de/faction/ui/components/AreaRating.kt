package de.faction.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.faction.data.model.Area
import de.faction.domain.AreaScore
import de.faction.ui.theme.FactionColors

/**
 * Die Bewertung eines Champions je Spielbereich, als Kachel mit Wert und Sternen.
 *
 * Die Sterne sind keine zweite Meinung neben der Zahl, sondern dieselbe Zahl grob
 * gerundet: fünf Stufen zu je zwanzig Punkten. Sie sind auf einen Blick lesbar, die
 * Zahl bleibt für den Vergleich daneben stehen.
 *
 * Wichtig bleibt die Einschränkung aus [de.faction.domain.ScoreEngine]: Werte sind nur
 * innerhalb desselben Bereichs vergleichbar. Deshalb trägt jede Kachel ihren Bereich,
 * und es gibt bewusst keine Gesamtnote über alle Bereiche hinweg.
 */
@Composable
fun AreaRatingTile(score: AreaScore, modifier: Modifier = Modifier) {
    val accent = areaColor(score.area)
    val shape = RoundedCornerShape(10.dp)

    Column(
        modifier
            .clip(shape)
            .border(1.dp, accent.copy(alpha = 0.45f), shape),
    ) {
        Text(
            score.area.label,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .background(accent.copy(alpha = 0.16f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.10f), FactionColors.Night),
                    ),
                )
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "${score.score}",
                style = MaterialTheme.typography.headlineMedium,
                color = FactionColors.TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            StarRow(score.score, accent)
        }
    }
}

/** Fünf Stufen zu je zwanzig Punkten — halbe Sterne gibt es bewusst nicht. */
@Composable
private fun StarRow(score: Int, accent: Color) {
    val filled = (score + 10) / 20
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(5) { index ->
            Box(
                Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (index < filled) accent else FactionColors.Border),
            )
        }
    }
}

/**
 * Eine Merkmalsleiste: Fraktion, Affinität, Seltenheit, Rolle nebeneinander. Sie
 * beantwortet die Einordnungsfragen, ohne dass man den Fließtext lesen muss.
 */
@Composable
fun TraitStrip(traits: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        traits.forEach { (label, value) ->
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(FactionColors.Night)
                    .border(1.dp, FactionColors.Border, RoundedCornerShape(8.dp))
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = FactionColors.TextSecondary,
                    maxLines = 1,
                )
                Box(Modifier.height(4.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.bodySmall,
                    color = FactionColors.TextPrimary,
                    maxLines = 2,
                )
            }
        }
    }
}

/** Jeder Bereich trägt eine eigene Farbe, damit die Kacheln unterscheidbar bleiben. */
private fun areaColor(area: Area): Color = when (area) {
    Area.CLAN_BOSS -> FactionColors.Gold
    Area.ARENA -> FactionColors.Teal
    Area.DUNGEONS -> Color(0xFF7FA8FF)
    Area.DOOM_TOWER -> Color(0xFFB58CFF)
    Area.FACTION_WARS -> Color(0xFF6FD3A8)
    Area.CAMPAIGN -> Color(0xFFE8A76B)
}
