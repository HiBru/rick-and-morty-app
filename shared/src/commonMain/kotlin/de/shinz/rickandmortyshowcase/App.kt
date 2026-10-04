package de.shinz.rickandmortyshowcase

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.shinz.rickandmortyshowcase.core.designsystem.theme.AppTheme
import de.shinz.rickandmortyshowcase.core.designsystem.theme.isDark
import de.shinz.rickandmortyshowcase.core.domain.usecase.ObserveThemeModeUseCase
import de.shinz.rickandmortyshowcase.navigation.AppNavHost
import org.koin.compose.koinInject

/**
 * The composition root, and not a screen.
 *
 * It reads the stored theme mode and wraps the app in [AppTheme]. This is the
 * **one sanctioned place** a composable touches a use case directly: there is no
 * screen state involved, and the six-piece MVI ceremony for a single enum is not
 * worth it. Do not copy this pattern into a screen.
 *
 * It carries no `@Preview` either — a preview of the root would need a DI graph,
 * and `@PreviewLightDark` on the screens below gives the same coverage with none
 * of the setup.
 */
@Composable
fun App() {
    val observeThemeMode: ObserveThemeModeUseCase = koinInject()
    // Nothing is rendered until the store answers, which is what actually avoids
    // a flash. A non-null initial value would not: the flow is cold, so the first
    // frame would always be SYSTEM and a stored Dark on a light device would
    // still show the light theme first. Until then the platform window shows,
    // and its background is colour-matched per mode.
    val themeMode by observeThemeMode().collectAsStateWithLifecycle(null)

    themeMode?.let { mode ->
        AppTheme(themeMode = mode) {
            // Matches the window theme to the resolved app theme, which
            // enableEdgeToEdge cannot do: it reads the device's night setting
            // once, and the stored preference is free to disagree.
            SystemBarAppearance(darkTheme = mode.isDark())

            AppNavHost()
        }
    }
}
