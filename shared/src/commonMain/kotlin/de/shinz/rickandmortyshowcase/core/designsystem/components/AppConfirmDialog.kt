package de.shinz.rickandmortyshowcase.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.window.Dialog
import de.shinz.rickandmortyshowcase.core.designsystem.theme.AppTheme
import de.shinz.rickandmortyshowcase.generated.resources.Res
import de.shinz.rickandmortyshowcase.generated.resources.action_cancel
import org.jetbrains.compose.resources.stringResource

/**
 * The "are you sure" dialog.
 *
 * `SPEC.md` requires one before **every** favourite removal — from the list, from
 * Favorites, and from the detail screen — so this exists once rather than three
 * times. Adding a favourite is immediate and uses nothing here.
 *
 * The confirm label is a parameter because it should name the action ("Remove"),
 * not say "OK". Dismiss defaults to Cancel, which never varies.
 */
@Composable
fun AppConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = onDismiss) {
        AppConfirmDialogContent(
            title = title,
            text = text,
            confirmLabel = confirmLabel,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            modifier = modifier,
        )
    }
}

/**
 * The dialog's body, split out so it can be previewed.
 *
 * A `Dialog` renders into its own platform window and never appears in a
 * `@Preview` — on iOS it shows nothing at all. Every sheet or dialog in this app
 * is therefore previewed through a `Content` twin, never directly.
 *
 * It paints its own surface rather than using `AlertDialog`, which keeps the
 * radius on `AppRadius` instead of Material's 28dp `CornerExtraLarge`.
 */
@Composable
internal fun AppConfirmDialogContent(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = colors.surface,
                shape = RoundedCornerShape(AppTheme.radius.lg),
            )
            .padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        // No style override: headlineSmall is the dialog-title slot and
        // appMaterialTypography already maps it to screenTitle.
        Text(text = title, style = AppTheme.typography.screenTitle, color = colors.onSurface)
        Text(text = text, style = AppTheme.typography.body, color = colors.onSurfaceMuted)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm, Alignment.End),
        ) {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.action_cancel), color = colors.accent)
            }
            TextButton(onClick = onConfirm) {
                // The destructive action is tinted, not the safe one.
                Text(text = confirmLabel, color = colors.statusDead)
            }
        }
    }
}

/* ============================================================
   Previews
   ============================================================ */

@PreviewLightDark
@Composable
private fun AppConfirmDialogContentPreview() {
    AppTheme {
        AppConfirmDialogContent(
            title = "Remove favourite?",
            text = "Rick Sanchez will no longer be available offline.",
            confirmLabel = "Remove",
            onConfirm = {},
            onDismiss = {},
            modifier = Modifier.padding(AppTheme.spacing.lg),
        )
    }
}
