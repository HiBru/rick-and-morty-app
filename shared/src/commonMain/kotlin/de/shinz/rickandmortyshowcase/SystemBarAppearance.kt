package de.shinz.rickandmortyshowcase

import androidx.compose.runtime.Composable

/**
 * Matches the system bars' icon contrast to the app's resolved theme.
 *
 * Necessary because `enableEdgeToEdge()` decides this **once, from the device's
 * night setting**, while this app's theme comes from a stored preference that is
 * free to disagree: choose Dark on a light-mode phone and the status bar would
 * keep painting dark icons over a dark background.
 *
 * Called inside `AppTheme` so it sees the same resolved value the UI does.
 */
@Composable
expect fun SystemBarAppearance(darkTheme: Boolean)
