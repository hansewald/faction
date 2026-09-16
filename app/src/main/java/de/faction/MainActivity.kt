package de.faction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.faction.ui.FactionViewModel
import de.faction.ui.nav.Routes
import de.faction.ui.nav.Tab
import de.faction.ui.screens.ArtifactsScreen
import de.faction.ui.screens.ChampionDetailScreen
import de.faction.ui.screens.GuideScreen
import de.faction.ui.screens.LegendsScreen
import de.faction.ui.screens.NewsScreen
import de.faction.ui.screens.QuestsScreen
import de.faction.ui.screens.StartScreen
import de.faction.ui.theme.FactionColors
import de.faction.ui.theme.FactionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as FactionApplication
        setContent {
            FactionTheme { FactionApp(app) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FactionApp(app: FactionApplication) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val viewModel: FactionViewModel = viewModel(factory = FactionViewModel.factory(app))

    val isDetail = route?.startsWith("legends/") == true
    val isArtifacts = route == Routes.ARTIFACTS
    val hasBackButton = isDetail || isArtifacts

    Scaffold(
        containerColor = FactionColors.Night,
        topBar = {
            TopAppBar(
                title = {
                    when {
                        isDetail -> Text("Helden-Details", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                        isArtifacts -> Text("Artefakte", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                        else -> Wordmark()
                    }
                },
                navigationIcon = {
                    if (hasBackButton) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Zurück",
                                tint = FactionColors.TextPrimary,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "Benachrichtigungen",
                            tint = FactionColors.TextSecondary,
                        )
                    }
                    Box(
                        Modifier
                            .padding(end = 12.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(FactionColors.SurfaceRaised)
                            .border(1.dp, FactionColors.Gold.copy(alpha = 0.5f), CircleShape),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FactionColors.Night,
                    titleContentColor = FactionColors.TextPrimary,
                ),
            )
        },
        bottomBar = { FactionNavBar(navController, route) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Tab.START.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Tab.START.route) {
                StartScreen(
                    viewModel = viewModel,
                    onOpenGuide = { navController.switchTab(Tab.GUIDE) },
                    onOpenLegends = { navController.switchTab(Tab.LEGENDS) },
                    onOpenQuests = { navController.switchTab(Tab.QUESTS) },
                    onOpenNews = { navController.switchTab(Tab.NEWS) },
                    onOpenArtifacts = { navController.navigate(Routes.ARTIFACTS) },
                )
            }
            composable(Tab.LEGENDS.route) {
                LegendsScreen(
                    viewModel = viewModel,
                    onOpenChampion = { id -> navController.navigate(Routes.championDetail(id)) },
                )
            }
            composable(Tab.QUESTS.route) { QuestsScreen() }
            composable(Tab.GUIDE.route) { GuideScreen(viewModel) }
            composable(Tab.NEWS.route) { NewsScreen(viewModel) }
            composable(Routes.CHAMPION_DETAIL) { entry ->
                val championId = entry.arguments?.getString("championId").orEmpty()
                ChampionDetailScreen(championId = championId, viewModel = viewModel)
            }
            composable(Routes.ARTIFACTS) { ArtifactsScreen() }
        }
    }
}

/** Wechselt den Reiter, ohne den Zurück-Stapel wachsen zu lassen. */
private fun NavHostController.switchTab(tab: Tab) {
    navigate(tab.route) {
        popUpTo(Tab.START.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun Wordmark() {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text("✦", color = FactionColors.Gold, fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            "FACTION",
            color = FactionColors.TextPrimary,
            fontWeight = FontWeight.Light,
            fontSize = 20.sp,
            letterSpacing = 4.sp,
        )
    }
}

@Composable
private fun FactionNavBar(navController: NavHostController, route: String?) {
    NavigationBar(containerColor = FactionColors.Surface) {
        Tab.entries.forEach { tab ->
            NavigationBarItem(
                selected = route == tab.route,
                onClick = { navController.switchTab(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = FactionColors.Teal,
                    selectedTextColor = FactionColors.Teal,
                    unselectedIconColor = FactionColors.TextSecondary,
                    unselectedTextColor = FactionColors.TextSecondary,
                    indicatorColor = FactionColors.SurfaceRaised,
                ),
            )
        }
    }
}
