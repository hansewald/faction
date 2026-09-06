package de.faction.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.faction.FactionApplication
import de.faction.data.local.GuideProgressStore
import de.faction.data.model.Champion
import de.faction.data.model.GuideContent
import de.faction.data.model.ImportCandidate
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

/**
 * Die zu bestätigenden Vorschläge eines Imports. Solange diese Liste steht, ist noch
 * nichts im Kader gelandet.
 */
data class ImportReview(
    val sourceLabel: String,
    val candidates: List<ImportCandidate>,
) {
    val acceptedCount: Int get() = candidates.count { it.accepted }
    val unresolvedCount: Int get() = candidates.count { !it.isResolved }
    val uncertainCount: Int
        get() = candidates.count { it.isResolved && it.confidence < ImportCandidate.RELIABLE }
}

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

    private val _importReview = MutableStateFlow<ImportReview?>(null)
    val importReview: StateFlow<ImportReview?> = _importReview.asStateFlow()

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

    /** Namenssuche über den gesamten Katalog, für die Zuordnung im Bestätigungsschritt. */
    fun searchCatalog(query: String): List<Champion> {
        val all = catalog.all()
        if (query.isBlank()) return all.take(40)
        return all.filter { it.name.contains(query.trim(), ignoreCase = true) }
            .sortedBy { it.name.length }
    }

    fun factions(): List<String> = catalog.factions()

    /** Lesezeichen: nimmt einen Champion in den Kader auf oder entfernt ihn wieder. */
    fun toggleOwned(championId: String) {
        viewModelScope.launch { repository.toggleOwned(championId) }
    }

    fun updateOwned(champion: OwnedChampion) = viewModelScope.launch { repository.update(champion) }

    fun toggleChapterRead(chapterId: String) = guideProgress.toggle(chapterId)

    /**
     * Startet einen Import. Das Ergebnis wandert **nicht** direkt in den Kader, sondern
     * in [importReview] zur Bestätigung.
     */
    fun import(source: AccountSource, input: SourceInput) {
        viewModelScope.launch {
            _importState.value = ImportState(running = true)
            source.load(input).fold(
                onSuccess = { candidates ->
                    if (candidates.isEmpty()) {
                        _importState.value = ImportState(
                            message = "Es wurde keine Legende erkannt. Zeigt der Screenshot die Championliste?",
                            isError = true,
                        )
                    } else {
                        _importState.value = ImportState()
                        _importReview.value = ImportReview(source.label, candidates)
                    }
                },
                onFailure = {
                    _importState.value = ImportState(
                        message = it.message ?: "Import fehlgeschlagen.",
                        isError = true,
                    )
                },
            )
        }
    }

    /** Schaltet einen Vorschlag im Bestätigungsschritt an oder aus. */
    fun toggleCandidate(index: Int) = updateCandidate(index) { candidate ->
        if (!candidate.isResolved) candidate else candidate.copy(accepted = !candidate.accepted)
    }

    /** Weist einem Vorschlag eine andere Legende zu — etwa nach falscher Erkennung. */
    fun assignCandidate(index: Int, championId: String) = updateCandidate(index) {
        // Eine Zuordnung von Hand ist verlaesslich, unabhaengig von der Erkennung.
        it.copy(championId = championId, confidence = 1f, accepted = true)
    }

    fun setCandidateLevel(index: Int, level: Int, rank: Int) = updateCandidate(index) {
        it.copy(level = level.coerceIn(1, 60), rank = rank.coerceIn(1, 6))
    }

    fun setAllCandidates(accepted: Boolean) {
        val review = _importReview.value ?: return
        _importReview.value = review.copy(
            candidates = review.candidates.map {
                if (it.isResolved) it.copy(accepted = accepted) else it
            },
        )
    }

    private fun updateCandidate(index: Int, transform: (ImportCandidate) -> ImportCandidate) {
        val review = _importReview.value ?: return
        if (index !in review.candidates.indices) return
        _importReview.value = review.copy(
            candidates = review.candidates.toMutableList().also { it[index] = transform(it[index]) },
        )
    }

    /** Übernimmt die bestätigten Vorschläge. [replace] ersetzt den bisherigen Kader. */
    fun confirmImport(replace: Boolean) {
        val review = _importReview.value ?: return
        val champions = review.candidates.filter { it.accepted }.mapNotNull { it.toOwnedChampion() }
        viewModelScope.launch {
            if (replace) repository.replaceAll(champions) else repository.add(champions)
            _importReview.value = null
            _importState.value = ImportState(
                message = "${champions.size} Legenden übernommen.",
            )
        }
    }

    fun cancelImport() {
        _importReview.value = null
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
