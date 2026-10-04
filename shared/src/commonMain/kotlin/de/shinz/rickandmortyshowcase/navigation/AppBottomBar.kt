package de.shinz.rickandmortyshowcase.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import de.shinz.rickandmortyshowcase.core.designsystem.theme.AppTheme
import de.shinz.rickandmortyshowcase.generated.resources.Res
import de.shinz.rickandmortyshowcase.generated.resources.ic_favorite_border
import de.shinz.rickandmortyshowcase.generated.resources.ic_tab_home
import de.shinz.rickandmortyshowcase.generated.resources.ic_tab_settings
import de.shinz.rickandmortyshowcase.generated.resources.nav_favorites
import de.shinz.rickandmortyshowcase.generated.resources.nav_home
import de.shinz.rickandmortyshowcase.generated.resources.nav_settings
import de.shinz.rickandmortyshowcase.navigation.previews.AppBottomBarPreviewParameterProvider
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The three tabs.
 *
 * Takes the already-resolved [selected] tab rather than a `NavDestination`, so it
 * is a plain stateless component: the shell does the back-stack matching, and
 * this can be previewed without a `NavController` and a built graph.
 */
@Composable
internal fun AppBottomBar(
    selected: TopLevelDestination,
    onTabSelected: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors

    NavigationBar(
        modifier = modifier,
        containerColor = colors.surface,
    ) {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == selected,
                onClick = { onTabSelected(destination) },
                icon = {
                    Icon(
                        painter = painterResource(destination.icon),
                        // The label is right there and says the same thing, so
                        // announcing it twice would be worse than not at all.
                        contentDescription = null,
                    )
                },
                label = { Text(text = stringResource(destination.label)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.onAccent,
                    selectedTextColor = colors.onSurface,
                    indicatorColor = colors.accent,
                    unselectedIconColor = colors.onSurfaceMuted,
                    unselectedTextColor = colors.onSurfaceMuted,
                ),
            )
        }
    }
}

/**
 * The tabs, in bar order.
 *
 * An enum rather than three hand-written items: the bar, the selection check and
 * the navigation action all need the same list, and three copies of it is how
 * they drift. [route] is a [TopLevelRoute], not `Any`, so a non-tab route cannot
 * be wired in by mistake.
 */
internal enum class TopLevelDestination(
    val route: TopLevelRoute,
    val icon: DrawableResource,
    val label: StringResource,
) {
    HOME(HomeRoute, Res.drawable.ic_tab_home, Res.string.nav_home),
    FAVORITES(FavoritesRoute, Res.drawable.ic_favorite_border, Res.string.nav_favorites),
    SETTINGS(SettingsRoute, Res.drawable.ic_tab_settings, Res.string.nav_settings),
}

/* ============================================================
   Previews
   ============================================================ */

@PreviewLightDark
@Composable
private fun AppBottomBarPreview(
    @PreviewParameter(AppBottomBarPreviewParameterProvider::class)
    selected: TopLevelDestination,
) {
    AppTheme {
        AppBottomBar(selected = selected, onTabSelected = {})
    }
}
