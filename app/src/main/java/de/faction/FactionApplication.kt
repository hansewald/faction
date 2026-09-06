package de.faction

import android.app.Application
import de.faction.data.local.GuideProgressStore
import de.faction.data.local.PortraitStore
import de.faction.data.local.RaidDatabase
import de.faction.data.repo.ChampionCatalog
import de.faction.data.repo.RosterRepository
import de.faction.data.source.AccountSource
import de.faction.data.source.ScreenshotOcrSource
import de.faction.data.source.ToolkitJsonSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FactionApplication : Application() {

    lateinit var catalog: ChampionCatalog
        private set
    lateinit var repository: RosterRepository
        private set
    lateinit var accountSources: List<AccountSource>
        private set
    lateinit var guideProgress: GuideProgressStore
        private set
    lateinit var portraits: PortraitStore
        private set

    override fun onCreate() {
        super.onCreate()
        catalog = ChampionCatalog(this)
        repository = RosterRepository(RaidDatabase.get(this).rosterDao(), catalog)
        guideProgress = GuideProgressStore(this)
        portraits = PortraitStore(this)
        accountSources = listOf(
            ScreenshotOcrSource(this, catalog, portraits),
            ToolkitJsonSource(catalog),
        )
        CoroutineScope(SupervisorJob()).launch { catalog.load() }
    }
}
