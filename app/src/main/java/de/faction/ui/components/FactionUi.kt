package de.faction.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.faction.ui.theme.FactionColors

/**
 * Quellenangabe für die Portraits. Eine Auflage der Genehmigung von Plarium: überall,
 * wo Artworks erscheinen, muss der Rechteinhaber genannt sein.
 */
const val ARTWORK_CREDIT = "Artworks © Plarium Global Ltd."

/** Karte mit dem goldenen Rahmenverlauf aus dem Entwurf. */
@Composable
fun FactionCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(FactionColors.Surface)
            .border(1.dp, FactionColors.CardEdge, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        content = content,
    )
}

/** Abschnittsüberschrift in Versalien mit weiter Laufweite. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = FactionColors.TextSecondary,
        modifier = modifier,
    )
}

/** Kachel aus „Entdecke FACTION“: Symbol, Titel, Untertitel, Pfeil. */
@Composable
fun DiscoveryTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FactionCard(modifier = modifier, onClick = onClick) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = FactionColors.GoldSoft,
                    modifier = Modifier.size(26.dp),
                )
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = FactionColors.TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = FactionColors.TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = FactionColors.TextSecondary)
        }
    }
}

/** Kleines farbiges Etikett, etwa für Seltenheit oder Urteil. */
@Composable
fun Tag(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(5.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

/** Nummernkreis der Aufbauplan-Schritte. */
@Composable
fun StepBadge(number: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(FactionColors.Night)
            .border(1.dp, FactionColors.Gold.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "%02d".format(number),
            style = MaterialTheme.typography.bodySmall,
            color = FactionColors.GoldSoft,
        )
    }
}

/** Schmaler Fortschrittsbalken in Gold. */
@Composable
fun GoldProgress(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(FactionColors.Night),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(FactionColors.GoldSoft),
        )
    }
}
