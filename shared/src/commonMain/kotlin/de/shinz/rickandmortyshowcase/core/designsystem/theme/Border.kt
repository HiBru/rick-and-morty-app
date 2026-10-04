package de.shinz.rickandmortyshowcase.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* ============================================================
   Border — stroke widths only
   One value today, and a group rather than a constant so that the second one
   has somewhere to go that is not a call site.
   Read via AppTheme.border (see Theme.kt).

   Note the name trap: `border` is also a colour. AppTheme.border.hairline is a
   width; AppTheme.colors.border is what to paint it with.
   ============================================================ */

@Immutable
data class AppBorder(
    /** Card outlines and list dividers. Read it into a local before a draw lambda. */
    val hairline: Dp = 1.dp,
    /** A stroke meant to be seen in its own right — the progress indicator's track. */
    val strong: Dp = 4.dp,
)

val LocalAppBorder = staticCompositionLocalOf { AppBorder() }
