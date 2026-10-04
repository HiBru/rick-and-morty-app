package de.shinz.rickandmortyshowcase.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import de.shinz.rickandmortyshowcase.core.designsystem.theme.AppTheme
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.CharacterDetailRoot
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.CharacterListRoot
import de.shinz.rickandmortyshowcase.features.favorites.presentation.FavoritesRoot

/**
 * The outer host: the dashboard, and the detail screen that covers it.
 *
 * Only two destinations, deliberately. Detail sits **above** the dashboard rather
 * than beside the tabs, so it hides the bottom bar by construction. The
 * alternative — four flat siblings with the bar toggled out of composition — has
 * a real defect: `currentBackStackEntryAsState` emits when the transition
 * *starts*, so the bar would appear and disappear while the outgoing screen is
 * still animating, relayouting it mid-transition.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = DashboardRoute,
        modifier = modifier.fillMaxSize(),
    ) {
        composable<DashboardRoute> {
            DashboardScreen(
                onNavigateToDetail = navController::navigateToDetailOnce,
            )
        }
        composable<CharacterDetailRoute> { entry ->
            // The route is decoded **here**, where a NavBackStackEntry is in
            // hand — the ViewModel takes a plain Int, because `toRoute` on a
            // SavedStateHandle goes through an unmocked `android.os.Bundle` in
            // host tests. See the ViewModel's KDoc.
            val route: CharacterDetailRoute = entry.toRoute()
            CharacterDetailRoot(
                characterId = route.characterId,
                // Detail is outside the dashboard's Scaffold, so it gets no
                // innerPadding — it has to take the window insets itself or its
                // content runs under the status bar.
                contentPadding = WindowInsets.safeDrawing.asPaddingValues(),
                // `navigateUp`, and no once-guard — unlike the forward edge.
                // With one entry left it takes the up-from-deep-link path
                // instead of popping, so a double tap during the exit
                // transition is a no-op rather than emptying the NavHost.
                onNavigateBack = navController::navigateUp,
            )
        }
    }
}

/**
 * The shell: the bottom bar, and the tab host it drives.
 *
 * It has no ViewModel and no assembler — the selected tab comes from the tab
 * host's own back stack, which is why this sits outside MVI. See the sanctioned
 * conventions in `docs/IMPLEMENTATION_PLAN.md`.
 */
@Composable
private fun DashboardScreen(
    onNavigateToDetail: (Int) -> Unit,
    tabController: NavHostController = rememberNavController(),
) {
    val backStackEntry by tabController.currentBackStackEntryAsState()
    // HOME while the back stack entry is still null on the first composition —
    // otherwise the bar renders with nothing selected for one frame.
    val selected = backStackEntry?.destination.toTopLevel() ?: TopLevelDestination.HOME

    Scaffold(
        containerColor = AppTheme.colors.background,
        // safeDrawing, not the Scaffold default of systemBars: the default
        // ignores displayCutout, which puts content under the notch in landscape.
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            AppBottomBar(selected = selected, onTabSelected = tabController::switchTab)
        },
    ) { innerPadding ->
        NavHost(
            navController = tabController,
            startDestination = HomeRoute,
            modifier = Modifier.fillMaxSize(),
            // A fade, not the platform default. On iOS that default is a
            // horizontal push with predictive-back, which would make switching
            // tabs look and feel like opening a detail screen — and would let a
            // right-swipe drag a tab away. The outer host keeps the push, which
            // is what detail should have.
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
        ) {
            composable<HomeRoute> {
                CharacterListRoot(
                    contentPadding = innerPadding,
                    onNavigateToDetail = onNavigateToDetail,
                )
            }
            composable<FavoritesRoute> {
                FavoritesRoot(
                    contentPadding = innerPadding,
                    // The same destination as from Home, per SPEC.
                    onNavigateToDetail = onNavigateToDetail,
                )
            }
            composable<SettingsRoute> {
                PlaceholderScreen(
                    title = "Settings",
                    replacedBy = "Task 21",
                    contentPadding = innerPadding,
                )
            }
        }
    }
}

/**
 * Opens the detail screen, at most once per tap.
 *
 * `ObserveAsEvents` collects on `Dispatchers.Main.immediate`, so a row
 * double-tapped in one frame produces two events that are both handled before
 * the frame ends. `navigate` updates the back stack synchronously, so by the
 * second one the current destination is already detail — which is what this
 * check reads.
 *
 * *An earlier revision also required the entry to be `RESUMED`, to stop a
 * navigation event that had queued behind the detail screen from firing on
 * return. That cure was worse: an entry only becomes `RESUMED` when its
 * transition **completes**, and the outer host animates for 700ms on Android,
 * during which Home is visible and hittable — so every tap in that window was
 * silently swallowed, with a ripple and nothing else. The case it protected is
 * much narrower than it first looks, because a second tap needs a visible,
 * interactive Home: it requires one landing exactly as the collector stops.*
 *
 * The residual race is recorded in `docs/IMPLEMENTATION_PLAN.md` along with the
 * fix that would close it properly — a replay-free `MutableSharedFlow` for
 * navigation, which drops rather than buffers when nothing is collecting.
 *
 * Neither check belongs in the Root, which has no view of the back stack.
 */
private fun NavHostController.navigateToDetailOnce(characterId: Int) {
    val entry = currentBackStackEntry ?: return
    if (!entry.destination.hasRoute<DashboardRoute>()) return

    navigate(CharacterDetailRoute(characterId))
}

/**
 * Which tab a destination belongs to, or `null` for anything else.
 *
 * A flat `when` rather than a walk up the parent chain: the tab host has no
 * nested graphs, so the parent is the unnamed root and could never match. (If a
 * tab ever grows its own sub-graph, `NavDestination.hierarchy` from
 * navigation-common is the thing to use — do not hand-roll it.)
 */
private fun NavDestination?.toTopLevel(): TopLevelDestination? = when {
    this == null -> null
    hasRoute<HomeRoute>() -> TopLevelDestination.HOME
    hasRoute<FavoritesRoute>() -> TopLevelDestination.FAVORITES
    hasRoute<SettingsRoute>() -> TopLevelDestination.SETTINGS
    else -> null
}

/**
 * Switches tabs without stacking them.
 *
 * The three options together are what make the bar behave: `saveState` and
 * `restoreState` preserve each tab's scroll position, and `launchSingleTop`
 * stops re-tapping a tab from pushing a second copy. Popping to the graph's
 * start destination keeps the back stack from growing one entry per switch.
 */
internal fun NavHostController.switchTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
