package de.shinz.rickandmortyshowcase.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* ============================================================
   Radius — corner radii only
   Four steps, deliberately few: a restrained design reads as restrained
   largely because its corners agree.
   Read via AppTheme.radius (see Theme.kt).
   ============================================================ */

@Immutable
data class AppRadius(
    /** Small chips and the character thumbnail. */
    val sm: Dp = 8.dp,
    /** The default: list rows, cards, dialogs. */
    val md: Dp = 12.dp,
    /** The detail screen's hero image. */
    val lg: Dp = 16.dp,
    /** Pill shape — status chips, the favourite button's ripple bounds. */
    val full: Dp = 999.dp,
)

val LocalAppRadius = staticCompositionLocalOf { AppRadius() }
