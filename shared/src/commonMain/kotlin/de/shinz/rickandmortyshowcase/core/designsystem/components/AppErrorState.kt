package de.shinz.rickandmortyshowcase.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import de.shinz.rickandmortyshowcase.core.designsystem.previews.AppErrorStatePreviewData
import de.shinz.rickandmortyshowcase.core.designsystem.previews.AppErrorStatePreviewParameterProvider
import de.shinz.rickandmortyshowcase.core.designsystem.theme.AppTheme
import de.shinz.rickandmortyshowcase.generated.resources.Res
import de.shinz.rickandmortyshowcase.generated.resources.action_retry
import de.shinz.rickandmortyshowcase.generated.resources.ic_error
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Something went wrong, with a way out.
 *
 * @param onRetry when null, no button is shown. That is not a convenience: a
 *   retry that cannot help is worse than none, and `SPEC.md` has at least one
 *   such case — a page `NOT_FOUND` means end-of-list, not a transient failure.
 */
@Composable
fun AppErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
            .padding(spacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.lg, Alignment.CenterVertically),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_error),
            contentDescription = null,
            modifier = Modifier.size(AppTheme.size.iconLarge),
            tint = colors.statusDead,
        )
        Text(
            text = message,
            style = AppTheme.typography.body,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.onAccent,
                ),
                // ButtonDefaults.shape reads a Material token, not
                // MaterialTheme.shapes, so the radius has to be passed here.
                shape = RoundedCornerShape(AppTheme.radius.full),
            ) {
                Text(text = stringResource(Res.string.action_retry))
            }
        }
    }
}

/* ============================================================
   Previews
   ============================================================ */

/** The retryable and non-retryable looks are the two states worth seeing. */
@PreviewLightDark
@Composable
private fun AppErrorStatePreview(
    @PreviewParameter(AppErrorStatePreviewParameterProvider::class)
    data: AppErrorStatePreviewData,
) {
    AppTheme {
        AppErrorState(
            message = data.message,
            onRetry = NoOpRetry.takeIf { data.retryable },
        )
    }
}

/** Hoisted so the preview body reads as data rather than as `if (x) {{}} else null`. */
private val NoOpRetry: () -> Unit = {}
