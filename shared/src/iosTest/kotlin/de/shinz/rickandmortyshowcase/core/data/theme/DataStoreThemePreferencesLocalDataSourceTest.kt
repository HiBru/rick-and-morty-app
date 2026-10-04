package de.shinz.rickandmortyshowcase.core.data.theme

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import de.shinz.rickandmortyshowcase.core.domain.Result
import de.shinz.rickandmortyshowcase.core.domain.model.ThemeMode
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.FileSystem
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import kotlin.test.Test

/**
 * The data source against a real DataStore on disk.
 *
 * **In `iosTest`**, for the same reason as `FavoriteCharacterDaoTest`: this is
 * the platform where a real store can be opened in a test without Robolectric.
 * A fake `DataStore` would only restate how the fake was written; a real one
 * exercises the actual key, the actual encoding and the actual file.
 *
 * Each test gets its own path, because DataStore permits exactly **one instance
 * per file per process** — which is also why none of these reads back through a
 * second instance.
 */
class DataStoreThemePreferencesLocalDataSourceTest {

    @Test
    fun defaultsToFollowingTheSystemWhenNothingHasBeenStored() = runTest {
        val path = uniquePath()

        dataSource(path).observeThemeMode().test {
            assertThat(awaitItem()).isEqualTo(ThemeMode.SYSTEM)
        }
    }

    @Test
    fun aStoredChoiceIsReadBack() = runTest {
        val path = uniquePath()
        val dataSource = dataSource(path)

        dataSource.setThemeMode(ThemeMode.DARK)

        dataSource.observeThemeMode().test {
            assertThat(awaitItem()).isEqualTo(ThemeMode.DARK)
        }
    }

    @Test
    fun aWriteReachesTheDisk() = runTest {
        // As close as a test can get to the app-restart requirement in SPEC.md.
        //
        // The obvious version — write through one instance, read through a second
        // at the same path — is NOT a valid probe: DataStore permits exactly one
        // instance per file per process, and a second one returns the default
        // rather than the stored value. So this asserts the file is actually
        // created and non-empty, and the restart itself is verified on-device in
        // Task 21's pass.
        val path = uniquePath()

        dataSource(path).setThemeMode(ThemeMode.LIGHT)

        // okio rather than NSFileManager: it is already a dependency here and
        // needs no cinterop opt-in.
        val metadata = FileSystem.SYSTEM.metadataOrNull(path.toPath())
        assertThat(metadata).isNotNull()
        assertThat(metadata?.size).isNotNull().isGreaterThan(0L)
    }

    @Test
    fun aLaterChoiceReplacesTheEarlierOne() = runTest {
        val path = uniquePath()
        val dataSource = dataSource(path)

        dataSource.setThemeMode(ThemeMode.DARK)
        dataSource.setThemeMode(ThemeMode.LIGHT)

        dataSource.observeThemeMode().test {
            assertThat(awaitItem()).isEqualTo(ThemeMode.LIGHT)
        }
    }

    @Test
    fun aValueThisBuildDoesNotRecogniseFallsBackToTheSystem() = runTest {
        // Simulates a downgrade, or a constant renamed between releases.
        val path = uniquePath()
        val store = store(path)
        store.edit { preferences ->
            preferences[DataStoreThemePreferencesLocalDataSource.THEME_MODE_KEY] = "SEPIA"
        }

        DataStoreThemePreferencesLocalDataSource(store).observeThemeMode().test {
            assertThat(awaitItem()).isEqualTo(ThemeMode.SYSTEM)
        }
    }

    @Test
    fun aSuccessfulWriteReportsSuccess() = runTest {
        val result = dataSource(uniquePath()).setThemeMode(ThemeMode.DARK)

        assertThat(result).isEqualTo(Result.Success(Unit))
    }

    private fun dataSource(path: String) =
        DataStoreThemePreferencesLocalDataSource(store(path))

    private fun store(path: String): DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(produceFile = { path.toPath() })

    private fun uniquePath(): String =
        NSTemporaryDirectory() + NSUUID().UUIDString() + ".preferences_pb"
}
