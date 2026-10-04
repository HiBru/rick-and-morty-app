package de.shinz.rickandmortyshowcase.core.data.theme

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import de.shinz.rickandmortyshowcase.core.data.safeLocalCall
import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.EmptyResult
import de.shinz.rickandmortyshowcase.core.domain.asEmptyResult
import de.shinz.rickandmortyshowcase.core.domain.datasource.ThemePreferencesLocalDataSource
import de.shinz.rickandmortyshowcase.core.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry

/**
 * The theme preference in DataStore Preferences.
 *
 * Preferences rather than a typed (proto) store: this is one enum behind one key,
 * and a proto store would mean a schema and a serializer for a single value. No
 * skill prescribes either, so the choice is recorded in
 * `docs/IMPLEMENTATION_PLAN.md`.
 *
 * No `flowOn` here, unlike the Room data source — DataStore reads on its own
 * scope, and the only work downstream is a string-to-enum lookup.
 */
internal class DataStoreThemePreferencesLocalDataSource(
    private val dataStore: DataStore<Preferences>,
) : ThemePreferencesLocalDataSource {

    /**
     * Falls back to [ThemeMode.SYSTEM] for all three ways this can come up
     * empty: nothing stored yet, an unreadable file, and a stored value this
     * build does not recognise.
     *
     * `retry` before `catch`, exactly as on the Favorites flow — there is no
     * safe asymmetry here. `DataStore.data` is a cold flow that *throws* its
     * read exception, and `catch` completes the flow after emitting. Without the
     * retry, one failed read means this leg never emits again: the user taps
     * Dark, the write succeeds, and the radio button stays on System for the
     * life of the ViewModel. That is `SPEC.md`'s "applies immediately" failing,
     * not a brief invisible fallback.
     */
    override fun observeThemeMode(): Flow<ThemeMode> = dataStore.data
        .map { preferences -> preferences[THEME_MODE_KEY].toThemeMode() }
        .retry(TRANSIENT_RETRIES)
        .catch { emit(ThemeMode.SYSTEM) }
        .distinctUntilChanged()

    override suspend fun setThemeMode(themeMode: ThemeMode): EmptyResult<DataError.Local> =
        safeLocalCall {
            dataStore.edit { preferences -> preferences[THEME_MODE_KEY] = themeMode.name }
        }.asEmptyResult()

    internal companion object {
        /**
         * Snake_case, matching the file's own convention rather than Kotlin's —
         * the key is persisted data, so renaming it silently resets every user's
         * choice. Treat it as a schema.
         */
        internal val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

        /** Same bound as the Favorites flows. */
        private const val TRANSIENT_RETRIES = 2L
    }
}

/**
 * Not `ThemeMode.valueOf`, which throws.
 *
 * The stored string was written by an older build of this app, so a renamed
 * constant must degrade to the default rather than crash on the first read.
 */
internal fun String?.toThemeMode(): ThemeMode =
    ThemeMode.entries.firstOrNull { it.name == this } ?: ThemeMode.SYSTEM
