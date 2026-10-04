package de.shinz.rickandmortyshowcase

import androidx.compose.runtime.Composable

/**
 * A deliberate no-op.
 *
 * UIKit derives status-bar content style from the controller's
 * `overrideUserInterfaceStyle`, which `ComposeUIViewController` already tracks,
 * so there is nothing to set here. The declaration exists so the shared call
 * site in `App()` needs no `expect`/`actual` branch of its own.
 */
@Composable
actual fun SystemBarAppearance(darkTheme: Boolean) = Unit
