package de.shinz.rickandmortyshowcase.core.data.theme

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.Result
import de.shinz.rickandmortyshowcase.core.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * What the data source adds on top of DataStore: the retry, the fallback, and the
 * failure mapping.
 *
 * A fake store rather than a real one, because the point is to *inject* failures
 * that a real file will not produce on demand. The real store is exercised in
 * `iosTest`.
 */
class ThemePreferencesFallbackTest {

    @Test
    fun theStoredKeyIsThemeModeAndRenamingItWouldResetEveryUser() = runTest {
        // Every other test reaches the key through the constant, so a rename
        // would pass the whole suite while silently discarding each user's
        // choice. This spells the literal out instead.
        val store = FakeDataStore(
            flowOf(mutablePreferencesOf(stringPreferencesKey("theme_mode") to "DARK")),
        )

        DataStoreThemePreferencesLocalDataSource(store).observeThemeMode().test {
            assertThat(awaitItem()).isEqualTo(ThemeMode.DARK)
            awaitComplete()
        }
    }

    @Test
    fun aTransientReadFailureIsRetriedAndThenSucceeds() = runTest {
        // The case that makes retry load-bearing: without it the flow would
        // complete on the fallback and the theme could never change again for
        // the life of the collector.
        var attempts = 0
        val store = FakeDataStore(
            flow {
                attempts++
                if (attempts == 1) throw IllegalStateException("read failed")
                emit(mutablePreferencesOf(stringPreferencesKey("theme_mode") to "LIGHT"))
            },
        )

        DataStoreThemePreferencesLocalDataSource(store).observeThemeMode().test {
            assertThat(awaitItem()).isEqualTo(ThemeMode.LIGHT)
            awaitComplete()
        }
        assertThat(attempts).isEqualTo(2)
    }

    @Test
    fun aPersistentReadFailureFallsBackToTheSystem() = runTest {
        val store = FakeDataStore(flow { throw IllegalStateException("unreadable") })

        DataStoreThemePreferencesLocalDataSource(store).observeThemeMode().test {
            assertThat(awaitItem()).isEqualTo(ThemeMode.SYSTEM)
            // Degraded, not finished — see `degradeTo`.
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aFullDiskDuringAWriteIsReportedAsSuch() = runTest {
        // DataStore writes go through okio, which reports ENOSPC as strerror
        // text — none of SQLite's wording appears, so this needs its own branch
        // in asLocalError.
        val store = FakeDataStore(
            flowOf(emptyPreferences()),
            updateError = IllegalStateException("No space left on device"),
        )

        val result = DataStoreThemePreferencesLocalDataSource(store)
            .setThemeMode(ThemeMode.DARK)

        assertThat((result as Result.Error).error).isEqualTo(DataError.Local.DISK_FULL)
    }

    @Test
    fun anyOtherWriteFailureIsUnknown() = runTest {
        val store = FakeDataStore(
            flowOf(emptyPreferences()),
            updateError = IllegalStateException("something else"),
        )

        val result = DataStoreThemePreferencesLocalDataSource(store)
            .setThemeMode(ThemeMode.DARK)

        assertThat((result as Result.Error).error).isEqualTo(DataError.Local.UNKNOWN)
    }

    private class FakeDataStore(
        override val data: Flow<Preferences>,
        private val updateError: Throwable? = null,
    ) : DataStore<Preferences> {

        override suspend fun updateData(
            transform: suspend (Preferences) -> Preferences,
        ): Preferences {
            updateError?.let { throw it }
            return transform(emptyPreferences())
        }
    }
}
