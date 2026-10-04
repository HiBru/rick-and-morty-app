package de.shinz.rickandmortyshowcase.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import de.shinz.rickandmortyshowcase.core.designsystem.theme.AppTheme
import de.shinz.rickandmortyshowcase.core.designsystem.previews.AppEmptyStatePreviewData
import de.shinz.rickandmortyshowcase.generated.resources.Res
import de.shinz.rickandmortyshowcase.core.designsystem.previews.AppEmptyStatePreviewParameterProvider
import org.jetbrains.compose.resources.painterResource

/**
 * "There is nothing here, and that is fine" — distinct from an error.
 *
 * Takes resolved `String`s rather than `UiText`: the assembler decides what it
 * says and the screen resolves it, so this component only lays it out.
 *
 * @param painter decorative, hence the null `contentDescription` on the [Icon] —
 *   the title says the same thing in words.
 */
@Composable
fun AppEmptyState(
    title: String,
    description: String,
    painter: Painter,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
            .padding(spacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.md, Alignment.CenterVertically),
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.size(AppTheme.size.iconLarge),
            tint = colors.onSurfaceMuted,
        )
        Text(
            text = title,
            style = AppTheme.typography.screenTitle,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = description,
            style = AppTheme.typography.body,
            color = colors.onSurfaceMuted,
            textAlign = TextAlign.Center,
        )
    }
}

/* ============================================================
   Previews
   ============================================================ */

@PreviewLightDark
@Composable
private fun AppEmptyStatePreview(
    @PreviewParameter(AppEmptyStatePreviewParameterProvider::class)
    data: AppEmptyStatePreviewData,
) {
    AppTheme {
        AppEmptyState(
            title = data.title,
            description = data.description,
            painter = painterResource(data.icon),
        )
    }
}
