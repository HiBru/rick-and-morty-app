package de.shinz.rickandmortyshowcase.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* ============================================================
   Size — icon, control and component dimensions
   Read via AppTheme.size (see Theme.kt).

   Alias it at the call site: `val sizes = AppTheme.size`. A local named `size`
   shadows DrawScope.size, which fails in a way that reads as a Compose bug.
   ============================================================ */

@Immutable
data class AppSize(
    /** The default icon: bottom bar, favourite button, back arrow. */
    val icon: Dp = 24.dp,
    /** Inline icons that sit inside a line of text. */
    val iconSmall: Dp = 18.dp,
    /** The character thumbnail in a list row. */
    val avatar: Dp = 64.dp,
    /** The status dot beside a character's status label. */
    val statusDot: Dp = 8.dp,
    /** Minimum tappable edge. Below this, a target fails accessibility. */
    val touchTarget: Dp = 48.dp,
    /** The detail screen's hero image height. */
    val detailImage: Dp = 220.dp,
)

val LocalAppSize = staticCompositionLocalOf { AppSize() }
