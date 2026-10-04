package de.shinz.rickandmortyshowcase.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import de.shinz.rickandmortyshowcase.core.designsystem.previews.AppIconButtonPreviewParameterProvider
import de.shinz.rickandmortyshowcase.core.designsystem.theme.AppTheme
import de.shinz.rickandmortyshowcase.generated.resources.Res
import de.shinz.rickandmortyshowcase.generated.resources.ic_favorite
import de.shinz.rickandmortyshowcase.generated.resources.ic_favorite_border
import org.jetbrains.compose.resources.painterResource

/**
 * A tappable icon with a guaranteed touch target.
 *
 * Takes a [Painter] rather than an `ImageVector`: Compose Multiplatform's
 * material3 does not bundle `material-icons-core`, so every icon in this app is
 * an XML vector in `composeResources/drawable/` loaded with `painterResource`.
 *
 * @param contentDescription what a screen reader announces. **Not optional** —
 *   an icon-only control with no description is unusable without sight. Pass a
 *   string resource; the one case for `null` is a purely decorative icon, which
 *   is not what this component is for.
 */
@Composable
fun AppIconButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AppTheme.colors.onSurfaceMuted,
) {
    val sizes = AppTheme.size

    IconButton(
        onClick = onClick,
        // size, not just padding: IconButton's own 40dp container is coerced up
        // into this fixed 48dp, giving a real touch target. Its ripple is already
        // circular by default, so no clip is needed here.
        modifier = modifier.size(sizes.touchTarget),
        colors = IconButtonDefaults.iconButtonColors(contentColor = tint),
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.size(sizes.icon),
        )
    }
}

/* ============================================================
   Previews
   ============================================================ */

/**
 * One data parameter — favourited or not — so a plain `Boolean` provider, with no
 * `PreviewData` wrapper. The `Modifier`, the lambda and the description do not
 * count: none of them varies a look worth seeing.
 */
@PreviewLightDark
@Composable
private fun AppIconButtonPreview(
    @PreviewParameter(AppIconButtonPreviewParameterProvider::class) isFavorite: Boolean,
) {
    AppTheme {
        Box(
            modifier = Modifier.size(AppTheme.size.touchTarget),
            contentAlignment = Alignment.Center,
        ) {
            AppIconButton(
                painter = painterResource(
                    if (isFavorite) Res.drawable.ic_favorite else Res.drawable.ic_favorite_border,
                ),
                contentDescription = "Favourite",
                onClick = {},
                tint = if (isFavorite) AppTheme.colors.accent else AppTheme.colors.onSurfaceMuted,
            )
        }
    }
}
