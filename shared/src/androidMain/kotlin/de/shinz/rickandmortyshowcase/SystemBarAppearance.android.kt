package de.shinz.rickandmortyshowcase

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

@Composable
actual fun SystemBarAppearance(darkTheme: Boolean) {
    val window = (LocalContext.current as? Activity)?.window ?: return

    // SideEffect, not LaunchedEffect: this writes to a non-Compose system and
    // must re-apply after every successful recomposition, including the one
    // where the user flips the theme.
    SideEffect {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}
