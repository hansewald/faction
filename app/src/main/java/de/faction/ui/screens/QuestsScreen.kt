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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.faction.data.model.Quest
import de.faction.data.model.QuestContent
import de.faction.data.model.QuestCycle
import de.faction.ui.components.FactionCard
import de.faction.ui.components.SectionTitle
import de.faction.ui.components.Tag
import de.faction.ui.theme.FactionColors

@Composable
fun QuestsScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(
                    "Quests",
                    style = MaterialTheme.typography.headlineMedium,
                    color = FactionColors.TextPrimary,
                )
                Text(
                    "Was sich lohnt — und warum.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FactionColors.TextSecondary,
                )
            }
        }

        QuestCycle.entries.forEach { cycle ->
            val quests = QuestContent.quests.filter { it.cycle == cycle }
            item(key = "header-${cycle.name}") {
                Spacer(Modifier.height(4.dp))
                SectionTitle(cycle.label)
            }
            items(quests, key = { it.id }) { quest -> QuestCard(quest) }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text(
                "Die Aufgabenliste im Spiel bleibt maßgeblich. FACTION ergänzt nur, welchen " +
                    "Weg du wählst, um mehrere Aufgaben mit derselben Energie abzudecken.",
                style = MaterialTheme.typography.bodySmall,
                color = FactionColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun QuestCard(quest: Quest) {
    FactionCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    quest.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = FactionColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Tag(quest.reward, FactionColors.GoldSoft)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                quest.why,
                style = MaterialTheme.typography.bodySmall,
                color = FactionColors.TextSecondary,
            )
        }
    }
}
