package de.shinz.rickandmortyshowcase.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import de.shinz.rickandmortyshowcase.core.designsystem.theme.AppTheme

/**
 * The full-screen loading state.
 *
 * Deliberately not parameterised with a message: a spinner that has to explain
 * itself is a sign the wait is too long to be a spinner.
 */
@Composable
fun AppLoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier.fillMaxSize().then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(AppTheme.size.spinner),
            color = AppTheme.colors.accent,
            strokeWidth = AppTheme.border.strong,
        )
    }
}

/* ============================================================
   Previews
   ============================================================ */

@PreviewLightDark
@Composable
private fun AppLoadingIndicatorPreview() {
    AppTheme {
        AppLoadingIndicator()
    }
}
