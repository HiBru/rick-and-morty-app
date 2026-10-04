package de.shinz.rickandmortyshowcase.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* ============================================================
   Spacing — gaps and paddings only
   Corner radii live in AppRadius, component dimensions in AppSize, stroke
   widths in AppBorder. Keeping them apart is what stops a change to one
   silently moving another.

   A 4dp rhythm. When a layout wants a value that is not here, snap to the
   nearest step rather than adding a near-duplicate — a 14dp sitting beside a
   16dp is drift, not a decision.
   Read via AppTheme.spacing (see Theme.kt).
   ============================================================ */

@Immutable
data class AppSpacing(
    /** Hairline inset — a status dot off its label, an icon off its text. */
    val xs: Dp = 4.dp,
    /** The tight gap: icon-to-label inside a button or list row. */
    val sm: Dp = 8.dp,
    /** The default gap between two related controls in a row or column. */
    val md: Dp = 12.dp,
    /** The gap between blocks inside a card, and a card's own inner padding. */
    val lg: Dp = 16.dp,
    /** The vertical rhythm between top-level sections, and the widest step. */
    val xl: Dp = 24.dp,

    /**
     * The side gutter every screen shares.
     *
     * One value rather than per-screen: the character list rows, the detail
     * screen's body and the settings options only line up down the left edge
     * if all three read this.
     */
    val screenPadding: Dp = 20.dp,
)

val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }
