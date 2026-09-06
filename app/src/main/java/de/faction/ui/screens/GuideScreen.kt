package de.faction.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.faction.data.model.GuideChapter
import de.faction.data.model.GuideContent
import de.faction.ui.FactionViewModel
import de.faction.ui.components.FactionCard
import de.faction.ui.components.GoldProgress
import de.faction.ui.components.SectionTitle
import de.faction.ui.components.StepBadge
import de.faction.ui.theme.FactionColors

@Composable
fun GuideScreen(viewModel: FactionViewModel, modifier: Modifier = Modifier) {
    val read by viewModel.readChapters.collectAsStateWithLifecycle()
    val progress by viewModel.guideProgressRatio.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(
                    "Anleitung",
                    style = MaterialTheme.typography.headlineMedium,
                    color = FactionColors.TextPrimary,
                )
                Text(
                    "Mechaniken verstehen, Schritt für Schritt.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FactionColors.TextSecondary,
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GoldProgress(progress, Modifier.weight(1f))
                    Text(
                        "  ${read.size} von ${GuideContent.chapters.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = FactionColors.TextSecondary,
                    )
                }
            }
        }

        itemsIndexed(GuideContent.chapters, key = { _, chapter -> chapter.id }) { index, chapter ->
            ChapterCard(
                number = index + 1,
                chapter = chapter,
                isRead = chapter.id in read,
                onToggleRead = { viewModel.toggleChapterRead(chapter.id) },
            )
        }
    }
}

@Composable
private fun ChapterCard(
    number: Int,
    chapter: GuideChapter,
    isRead: Boolean,
    onToggleRead: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    FactionCard(Modifier.fillMaxWidth(), onClick = { expanded = !expanded }) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            StepBadge(number)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    chapter.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = FactionColors.TextPrimary,
                )
                Text(
                    chapter.teaser,
                    style = MaterialTheme.typography.bodySmall,
                    color = FactionColors.TextSecondary,
                )
            }
            IconButton(onClick = onToggleRead) {
                Icon(
                    imageVector = if (isRead) {
                        Icons.Filled.CheckCircle
                    } else {
                        Icons.Outlined.CheckCircle
                    },
                    contentDescription = if (isRead) "Als ungelesen markieren" else "Als gelesen markieren",
                    tint = if (isRead) FactionColors.Teal else FactionColors.Border,
                )
            }
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                SectionTitle("Kapitel $number")
                chapter.paragraphs.forEach { paragraph ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        paragraph,
                        style = MaterialTheme.typography.bodyMedium,
                        color = FactionColors.TextSecondary,
                    )
                }
            }
        }
    }
}
