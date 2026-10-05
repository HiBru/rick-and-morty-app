package de.shinz.rickandmortyshowcase.core.designsystem

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Adds a uniform inset to window insets that already arrived as `PaddingValues`.
 *
 * `PaddingValues` has no `plus`, and the two cannot simply be swapped: the
 * gutter is the screen's and the rest is the window's, and a scrolling list
 * needs both at once — the window's so content clears the status and navigation
 * bars, the screen's so it lines up with every other screen.
 *
 * Shared rather than reimplemented, which is the state it was in after Task 20:
 * token for token in two files, so a landscape or cutout fix to one would
 * silently miss the other. Three callers now — both lists and the settings
 * screen, which is not a list but wants the same arithmetic.
 *
 * Not `@Composable`, so [direction] is a parameter and this is unit-testable —
 * see `ContentPaddingTest`, which is where the RTL round trip is pinned.
 *
 * `internal`: a public `commonMain` declaration is exported into the `Shared`
 * Obj-C header, and this is app geometry rather than API.
 */
internal fun PaddingValues.plus(
    direction: LayoutDirection,
    horizontal: Dp = 0.dp,
    vertical: Dp = 0.dp,
): PaddingValues = PaddingValues(
    start = calculateStartPadding(direction) + horizontal,
    end = calculateEndPadding(direction) + horizontal,
    top = calculateTopPadding() + vertical,
    bottom = calculateBottomPadding() + vertical,
)
