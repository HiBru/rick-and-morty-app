package de.shinz.rickandmortyshowcase.core.designsystem.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import de.shinz.rickandmortyshowcase.core.designsystem.theme.AppTheme

/**
 * Where transient messages appear.
 *
 * **Owned by each screen rather than by the dashboard shell**, deliberately. The
 * detail screen sits outside the shell's `Scaffold` and would need its own
 * regardless, so a shell-level host would mean two mechanisms for one job — and
 * the shell would have to hand a `SnackbarHostState` down into feature screens
 * it otherwise knows nothing about. Each screen already receives the window
 * insets it needs to place this correctly.
 *
 * It paints its own [AppSnackbar] instead of taking Material's default, for the
 * radius: `Snackbar`'s shape comes from a Material token rather than
 * `MaterialTheme.shapes`, so an app radius has to be passed explicitly.
 */
@Composable
fun AppSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.padding(AppTheme.spacing.md),
    ) { data ->
        AppSnackbar(message = data.visuals.message)
    }
}

/**
 * One message, split out so it can be previewed.
 *
 * A `SnackbarHost` renders nothing until something is queued, and queueing needs
 * a coroutine a preview cannot run — the same reason `AppConfirmDialog` has a
 * `Content` twin. Keeping the styling here rather than duplicating it in the
 * preview is the point: otherwise a change to one silently stops matching the
 * other.
 */
@Composable
internal fun AppSnackbar(
    message: String,
    modifier: Modifier = Modifier,
) {
    Snackbar(
        modifier = modifier,
        shape = RoundedCornerShape(AppTheme.radius.md),
        containerColor = AppTheme.colors.inverseSurface,
        contentColor = AppTheme.colors.onInverseSurface,
    ) {
        Text(text = message, style = AppTheme.typography.body)
    }
}

/* ============================================================
   Previews
   ============================================================ */

@PreviewLightDark
@Composable
private fun AppSnackbarPreview() {
    AppTheme {
        AppSnackbar(
            message = "There's no space left on your device.",
            modifier = Modifier.padding(AppTheme.spacing.md),
        )
    }
}
