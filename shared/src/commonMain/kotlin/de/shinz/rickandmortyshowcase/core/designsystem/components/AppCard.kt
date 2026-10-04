package de.shinz.rickandmortyshowcase.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.PreviewLightDark
import de.shinz.rickandmortyshowcase.core.designsystem.theme.AppTheme

/**
 * The surface most content sits on — a hairline-outlined panel rather than a
 * shadowed one, which is what keeps the design restrained.
 *
 * A slot API, as design-system containers should be: the caller owns the content
 * and its arrangement. Feature-level composables prefer typed parameters.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(AppTheme.spacing.lg),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(AppTheme.spacing.sm),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(AppTheme.radius.md)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color = colors.surface, shape = shape)
            .border(width = AppTheme.border.hairline, color = colors.border, shape = shape)
            // Inside the component and after the clip, deliberately. A caller
            // passing `Modifier.clickable {}` would land outside the shaped
            // background and paint a square ripple across the rounded corners.
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/* ============================================================
   Previews
   ============================================================ */

@PreviewLightDark
@Composable
private fun AppCardPreview() {
    AppTheme {
        AppCard(modifier = Modifier.padding(AppTheme.spacing.lg), onClick = {}) {
            Text(text = "Rick Sanchez", style = AppTheme.typography.cardTitle)
            Text(
                text = "Human",
                style = AppTheme.typography.caption,
                color = AppTheme.colors.onSurfaceMuted,
            )
        }
    }
}
