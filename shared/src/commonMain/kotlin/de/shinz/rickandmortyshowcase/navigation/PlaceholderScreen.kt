package de.shinz.rickandmortyshowcase.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.shinz.rickandmortyshowcase.core.designsystem.components.AppCard
import de.shinz.rickandmortyshowcase.core.designsystem.theme.AppTheme

/**
 * TEMPORARY stand-in so the shell is navigable before the screens exist.
 *
 * Each `composable<…>` entry names the task that replaces it. The scrolling list
 * is not decoration: it is what makes tab state preservation observable — scroll
 * Home, switch tabs, come back, and the position should be where it was.
 *
 * Its strings are literals rather than resources because it is scaffolding that
 * never ships; the real screens take their titles from `Res.string.nav_*`.
 *
 * @param contentPadding the shell's window insets, handed **down** rather than
 *   applied by an ancestor — which is what lets a real list screen route them
 *   into `LazyColumn(contentPadding = …)` and scroll content under the bars.
 */
@Composable
internal fun PlaceholderScreen(
    title: String,
    replacedBy: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onRowClick: ((Int) -> Unit)? = null,
) {
    val spacing = AppTheme.spacing

    Column(
        modifier = modifier.fillMaxSize().padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = AppTheme.typography.displayTitle,
            color = AppTheme.colors.onSurface,
        )
        Text(
            text = "replaced by $replacedBy",
            style = AppTheme.typography.caption,
            color = AppTheme.colors.onSurfaceMuted,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            items((1..30).toList()) { index ->
                AppCard(onClick = onRowClick?.let { click -> { click(index) } }) {
                    Text(
                        text = "$title row $index",
                        style = AppTheme.typography.cardTitle,
                        color = AppTheme.colors.onSurface,
                    )
                }
            }
        }
    }
}
