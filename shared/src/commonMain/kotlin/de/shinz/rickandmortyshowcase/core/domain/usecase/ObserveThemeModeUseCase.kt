package de.shinz.rickandmortyshowcase.core.domain.usecase

import de.shinz.rickandmortyshowcase.core.domain.datasource.ThemePreferencesLocalDataSource
import de.shinz.rickandmortyshowcase.core.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * The current theme preference.
 *
 * In `core/domain/usecase` rather than in the settings feature: the composition
 * root needs it to theme the whole app, and the root is not a feature — so
 * `features/shared/` would be the wrong home.
 *
 * A pass-through, deliberately. The boundary is the point: the Settings
 * ViewModel and `App()` depend on this rather than on a data source, so swapping
 * DataStore for something else never reaches the presentation layer.
 */
class ObserveThemeModeUseCase(
    private val themePreferences: ThemePreferencesLocalDataSource,
) {
    operator fun invoke(): Flow<ThemeMode> = themePreferences.observeThemeMode()
}
