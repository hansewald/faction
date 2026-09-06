package de.faction.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.faction.FactionApplication
import de.faction.data.local.GuideProgressStore
import de.faction.data.model.Champion
import de.faction.data.model.GuideContent
import de.faction.data.model.OwnedChampion
import de.faction.data.model.Rarity
import de.faction.data.model.Role
import de.faction.data.repo.ChampionCatalog
import de.faction.data.repo.RosterRepository
import de.faction.data.source.AccountSource
import de.faction.data.source.SourceInput
import de.faction.domain.AccountStage
import de.faction.domain.RosterAnalysis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LegendFilters(
    val query: String = "",
    val faction: String? = null,
    val rarity: Rarity? = null,
    val role: Role? = null,
    val onlyMine: Boolean = false,
)

data class ImportState(
    val running: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
)

class FactionViewModel(
    private val repository: RosterRepository,
    private val guideProgress: GuideProgressStore,
    val catalog: ChampionCatalog,
    val accountSources: List<AccountSource>,
) : ViewModel() {

    private val _catalogReady = MutableStateFlow(false)

    val analysis: StateFlow<RosterAnalysis> = repository.observeAnalysis().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RosterAnalysis(AccountStage.EARLY, emptyList(), emptyList(), emptyList()),
    )

    private val _filters = MutableStateFlow(LegendFilters())
    val filters: StateFlow<LegendFilters> = _filters.asStateFlow()

    private val _importState = MutableStateFlow(ImportState())
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    val readChapters: StateFlow<Set<String>> = guideProgress.read

    /** Anteil gelesener Guide-Kapitel, für die Fortschrittskarte auf dem Start-Reiter. */
    val guideProgressRatio: StateFlow<Float> = readChapters
        .map { it.size.toFloat() / GuideContent.chapters.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    /** Champion-Ids, die im Kader stehen — steuert das Lesezeichen auf den Karten. */
    val ownedIds: StateFlow<Set<String>> = repository.observeRoster()
        .map { roster -> roster.map { it.champion.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Die gefilterte Legendenliste, wie sie der Legenden-Reiter anzeigt. */
    val legends: StateFlow<List<Champion>> =
        combine(_filters, ownedIds, _catalogReady) { filters, owned, ready ->
            if (!ready) return@combine emptyList()
            catalog.all().filter { champion ->
                (filters.query.isBlank() || champion.name.contains(filters.query, ignoreCase = true)) &&
                    (filters.faction == null || champion.faction == filters.faction) &&
                    (filters.rarity == null || champion.rarity == filters.rarity) &&
                    (filters.role == null || champion.role == filters.role) &&
                    (!filters.onlyMine || champion.id in owned)
            }.sortedWith(compareByDescending<Champion> { it.rarity }.thenBy { it.name })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            catalog.load()
            _catalogReady.value = true
        }
    }

    fun updateFilters(transform: (LegendFilters) -> LegendFilters) {
        _filters.value = transform(_filters.value)
    }

    fun champion(id: String): Champion? = catalog.byId(id)

    fun factions(): List<String> = catalog.factions()

    /** Lesezeichen: nimmt einen Champion in den Kader auf oder entfernt ihn wieder. */
    fun toggleOwned(championId: String) {
        viewModelScope.launch { repository.toggleOwned(championId) }
    }

    fun updateOwned(champion: OwnedChampion) = viewModelScope.launch { repository.update(champion) }

    fun toggleChapterRead(chapterId: String) = guideProgress.toggle(chapterId)

    fun import(source: AccountSource, input: SourceInput, replace: Boolean) {
        viewModelScope.launch {
            _importState.value = ImportState(running = true)
            val result = source.load(input)
            _importState.value = result.fold(
                onSuccess = { champions ->
                    if (champions.isEmpty()) {
                        ImportState(message = "Es wurde kein Champion erkannt.", isError = true)
                    } else {
                        if (replace) repository.replaceAll(champions) else repository.add(champions)
                        ImportState(message = "${champions.size} Legenden übernommen.")
                    }
                },
                onFailure = { ImportState(message = it.message ?: "Import fehlgeschlagen.", isError = true) },
            )
        }
    }

    fun clearImportMessage() { _importState.value = ImportState() }

    companion object {
        fun factory(app: FactionApplication) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = FactionViewModel(
                app.repository,
                app.guideProgress,
                app.catalog,
                app.accountSources,
            ) as T
        }
    }
}
