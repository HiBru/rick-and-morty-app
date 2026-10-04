package de.shinz.rickandmortyshowcase.core.domain.usecase

import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.EmptyResult
import de.shinz.rickandmortyshowcase.core.domain.datasource.ThemePreferencesLocalDataSource
import de.shinz.rickandmortyshowcase.core.domain.model.ThemeMode

/** Stores the user's theme choice. Reports failure, unlike the read. */
class SetThemeModeUseCase(
    private val themePreferences: ThemePreferencesLocalDataSource,
) {
    suspend operator fun invoke(themeMode: ThemeMode): EmptyResult<DataError.Local> =
        themePreferences.setThemeMode(themeMode)
}
