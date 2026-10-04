package de.shinz.rickandmortyshowcase.di

import assertk.assertThat
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isSameInstanceAs
import de.shinz.rickandmortyshowcase.core.domain.datasource.CharacterRemoteDataSource
import de.shinz.rickandmortyshowcase.core.domain.datasource.FavoriteCharacterLocalDataSource
import de.shinz.rickandmortyshowcase.core.domain.datasource.ThemePreferencesLocalDataSource
import de.shinz.rickandmortyshowcase.core.domain.repository.CharacterRepository
import de.shinz.rickandmortyshowcase.core.domain.usecase.ObserveThemeModeUseCase
import de.shinz.rickandmortyshowcase.core.domain.usecase.SetThemeModeUseCase
import de.shinz.rickandmortyshowcase.core.format.AppDateTimeManager
import de.shinz.rickandmortyshowcase.core.ui.AppImageLoaderFactory
import de.shinz.rickandmortyshowcase.features.characterdetail.domain.usecase.GetCharacterUseCase
import de.shinz.rickandmortyshowcase.features.shared.domain.usecase.AddFavoriteUseCase
import de.shinz.rickandmortyshowcase.features.shared.domain.usecase.ObserveFavoriteIdsUseCase
import de.shinz.rickandmortyshowcase.features.shared.domain.usecase.RemoveFavoriteUseCase
import io.ktor.client.HttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import kotlin.time.Clock

/**
 * Resolves every binding the app will ask for.
 *
 * A missing or miswired Koin definition is a *runtime* failure — the app builds,
 * launches, and dies the moment a screen injects something. This turns that into
 * a test failure.
 *
 * `koin.get<T>()` throws `NoDefinitionFoundException` when a binding is missing,
 * so **the resolution is the assertion**; wrapping it in `isNotNull()` would add
 * a check that can never fail.
 *
 * Everything except two `Context`-bound leaves is the real wiring — see
 * `platformStubsForHostTest`.
 */
class KoinGraphTest {

    private var application: KoinApplication? = null

    /**
     * `close()` on the application, not `stopKoin()`.
     *
     * These graphs are built with `koinApplication { }`, which never registers in
     * `GlobalContext` — so `stopKoin()` would be a silent no-op, leaking an
     * `HttpClient` per test and, more importantly, never firing the `onClose`
     * that releases the DataStore file lock.
     */
    @AfterEach
    fun tearDown() {
        application?.close()
        application = null
    }

    private fun graph(): Koin = koinApplication {
        modules(
            coreDataModule,
            coreDataStoreModule,
            coreUiModule,
            appModule,
            platformStubsForHostTest(),
        )
    }.also { application = it }.koin

    @Test
    fun `every binding the app injects can be resolved`() {
        val koin = graph()

        // Data layer
        koin.get<HttpClient>()
        koin.get<Clock>()
        koin.get<CharacterRemoteDataSource>()
        koin.get<FavoriteCharacterLocalDataSource>()
        koin.get<CharacterRepository>()

        // Domain — the theme pair the composition root needs, and the formatter
        // the detail assembler will take as a constructor dependency.
        koin.get<ThemePreferencesLocalDataSource>()
        koin.get<ObserveThemeModeUseCase>()
        koin.get<SetThemeModeUseCase>()
        koin.get<AppDateTimeManager>()

        // UI infrastructure the composition root injects.
        koin.get<AppImageLoaderFactory>()

        // Features
        koin.get<GetCharacterUseCase>()
        koin.get<ObserveFavoriteIdsUseCase>()
        koin.get<AddFavoriteUseCase>()
        koin.get<RemoveFavoriteUseCase>()
    }

    @Test
    fun `use cases are factories, not singletons`() {
        // factoryOf per CLAUDE.md: stateless and cheap, and a factory guarantees
        // no state leaks between screens. A `single` here would compile and pass
        // every other test.
        val koin = graph()

        assertThat(koin.get<GetCharacterUseCase>())
            .isNotSameInstanceAs(koin.get<GetCharacterUseCase>())
        assertThat(koin.get<ObserveThemeModeUseCase>())
            .isNotSameInstanceAs(koin.get<ObserveThemeModeUseCase>())
        assertThat(koin.get<AddFavoriteUseCase>())
            .isNotSameInstanceAs(koin.get<AddFavoriteUseCase>())
    }

    @Test
    fun `the client, repository and stores are singletons`() {
        // They wrap a connection pool, a database handle and a file; a factory
        // here would quietly open a second of each per injection — and DataStore
        // throws outright on a second instance for one file.
        val koin = graph()

        assertThat(koin.get<HttpClient>()).isSameInstanceAs(koin.get<HttpClient>())
        assertThat(koin.get<CharacterRepository>())
            .isSameInstanceAs(koin.get<CharacterRepository>())
        assertThat(koin.get<FavoriteCharacterLocalDataSource>())
            .isSameInstanceAs(koin.get<FavoriteCharacterLocalDataSource>())
        assertThat(koin.get<ThemePreferencesLocalDataSource>())
            .isSameInstanceAs(koin.get<ThemePreferencesLocalDataSource>())
        // Not because a second one would do damage — Coil invokes the factory
        // at most once either way — but because the plan reserves `factoryOf`
        // for use cases and assemblers, and a scope change here would be a
        // silent drift away from that.
        assertThat(koin.get<AppImageLoaderFactory>())
            .isSameInstanceAs(koin.get<AppImageLoaderFactory>())
    }
}
