package de.shinz.rickandmortyshowcase.core.domain.datasource

import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.EmptyResult
import de.shinz.rickandmortyshowcase.core.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** The stored theme preference. Backed by DataStore, hence `Local`. */
interface ThemePreferencesLocalDataSource {

    /**
     * Emits the stored preference, falling back to [ThemeMode.SYSTEM] when
     * nothing has been chosen yet or the stored value cannot be read. A theme
     * preference has a sane default, so a read failure is not worth surfacing to
     * the user — hence `Flow<ThemeMode>` rather than `Flow<Result<…>>`.
     */
    fun observeThemeMode(): Flow<ThemeMode>

    /** A write can fail, so unlike the read this one reports it. */
    suspend fun setThemeMode(themeMode: ThemeMode): EmptyResult<DataError.Local>
}
