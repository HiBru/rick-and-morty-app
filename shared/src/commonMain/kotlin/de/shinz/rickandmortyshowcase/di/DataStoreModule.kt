package de.shinz.rickandmortyshowcase.di

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import de.shinz.rickandmortyshowcase.core.data.theme.DataStoreThemePreferencesLocalDataSource
import de.shinz.rickandmortyshowcase.core.domain.datasource.ThemePreferencesLocalDataSource
import de.shinz.rickandmortyshowcase.core.domain.usecase.ObserveThemeModeUseCase
import de.shinz.rickandmortyshowcase.core.domain.usecase.SetThemeModeUseCase
import kotlinx.coroutines.CoroutineScope
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Provides the single `DataStore<Preferences>` instance.
 *
 * A Koin `Module` for the same reason as `platformDatabaseModule`: Android
 * resolves the path from a `Context`, which it can take from `androidContext()`
 * here rather than having one threaded into common code.
 *
 * **Exactly one instance per file, process-wide.** DataStore enforces this by
 * throwing — `OkioStorage` keeps an `activeFiles` set and fails with "There are
 * multiple DataStores active for the same file" — so it must be a `single` and
 * nothing else may construct one. The path is released when the store's scope is
 * cancelled, which is what [dataStoreScopes] and the `onClose` exist for: a
 * second `startKoin` in one process would otherwise hit that error on first
 * access, and `observeThemeMode`'s `catch` would quietly turn it into "the theme
 * setting just doesn't work".
 */
internal expect val platformDataStoreModule: Module

/**
 * Scopes owned by the DataStore definitions, so `onClose` can cancel them.
 *
 * Koin does not cancel a `CoroutineScope` captured inside a definition, and
 * DataStore has no `close()` of its own — cancelling its scope is the only way
 * to release the file.
 */
internal val dataStoreScopes: MutableMap<DataStore<Preferences>, CoroutineScope> = mutableMapOf()

/**
 * File name of the single preferences store, identical on every platform.
 *
 * It carries `.preferences_pb` itself: `createWithPath` uses the path verbatim
 * and appends nothing, unlike Android's `Context.preferencesDataStoreFile(name)`.
 */
internal const val PREFERENCES_FILE_NAME = "settings.preferences_pb"

/**
 * Replaces an unreadable file with empty preferences instead of failing forever.
 *
 * Without this the default is `NoOpCorruptionHandler`, which rethrows on every
 * read *and* every write — so one truncated file would mean the user could never
 * change the theme again on that install. A *missing* file is not corruption, so
 * this costs nothing on first launch.
 */
internal fun corruptionHandler() =
    ReplaceFileCorruptionHandler<Preferences> { emptyPreferences() }

val coreDataStoreModule: Module = module {
    includes(platformDataStoreModule)

    singleOf(::DataStoreThemePreferencesLocalDataSource) {
        bind<ThemePreferencesLocalDataSource>()
    }

    factoryOf(::ObserveThemeModeUseCase)
    factoryOf(::SetThemeModeUseCase)
}
