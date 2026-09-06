package de.faction.data.repo

import de.faction.data.local.RosterDao
import de.faction.data.local.RosterEntity
import de.faction.data.model.OwnedChampion
import de.faction.data.model.RosterEntry
import de.faction.domain.BuildAdvisor
import de.faction.domain.RosterAnalysis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RosterRepository(
    private val dao: RosterDao,
    private val catalog: ChampionCatalog,
) {
    fun observeRoster(): Flow<List<RosterEntry>> = dao.observeAll().map { entities ->
        entities.mapNotNull { entity ->
            val champion = catalog.byId(entity.championId) ?: return@mapNotNull null
            RosterEntry(entity.toModel(), champion)
        }
    }

    fun observeAnalysis(): Flow<RosterAnalysis> = observeRoster().map(BuildAdvisor::analyze)

    suspend fun add(champions: List<OwnedChampion>) {
        dao.insertAll(champions.map(RosterEntity::from))
    }

    suspend fun replaceAll(champions: List<OwnedChampion>) {
        dao.clear()
        add(champions)
    }

    /**
     * Nimmt eine Legende in den Kader auf oder entfernt alle ihre Instanzen wieder.
     * Fragt den Bestand direkt ab, statt sich auf einen womöglich nicht gesammelten
     * Flow zu verlassen. Gibt zurück, ob sie nun im Kader steht.
     */
    suspend fun toggleOwned(championId: String): Boolean {
        val existing = dao.instancesOf(championId)
        return if (existing.isEmpty()) {
            add(listOf(OwnedChampion(championId = championId, level = 1, ascension = 0, rank = 1)))
            true
        } else {
            existing.forEach { dao.delete(it.instanceId) }
            false
        }
    }

    suspend fun update(champion: OwnedChampion) = dao.update(RosterEntity.from(champion))

    suspend fun remove(instanceId: Long) = dao.delete(instanceId)
}
