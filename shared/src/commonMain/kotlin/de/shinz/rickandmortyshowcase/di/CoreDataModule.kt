package de.shinz.rickandmortyshowcase.di

import de.shinz.rickandmortyshowcase.core.data.HttpClientFactory
import de.shinz.rickandmortyshowcase.core.data.LocalFirstCharacterRepository
import de.shinz.rickandmortyshowcase.core.data.character.KtorCharacterRemoteDataSource
import de.shinz.rickandmortyshowcase.core.data.favorite.RoomFavoriteCharacterLocalDataSource
import de.shinz.rickandmortyshowcase.core.domain.datasource.CharacterRemoteDataSource
import de.shinz.rickandmortyshowcase.core.domain.datasource.FavoriteCharacterLocalDataSource
import de.shinz.rickandmortyshowcase.core.domain.repository.CharacterRepository
import de.shinz.rickandmortyshowcase.core.format.AppDateTimeManager
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import kotlin.time.Clock

/**
 * The data layer: the HTTP client, the two character data sources, and the one
 * repository that coordinates them.
 *
 * Everything here is a `single` — each wraps a connection, a database handle or
 * nothing at all, and none of them holds per-screen state.
 */
val coreDataModule: Module = module {

    // Networking. The lambda form is needed for both: one calls a factory
    // method, the other an expect fun.
    single { httpClientEngine() }
    single { HttpClientFactory.create(engine = get()) }

    singleOf(::KtorCharacterRemoteDataSource) { bind<CharacterRemoteDataSource>() }

    // Storage. The DAO comes from coreDatabaseModule, the Clock from here —
    // "what time is it now" is injected rather than read statically so a test
    // can pin the favourited-at timestamp.
    single<Clock> { Clock.System }
    singleOf(::RoomFavoriteCharacterLocalDataSource) {
        bind<FavoriteCharacterLocalDataSource>()
    }

    // The one genuinely multi-source read.
    singleOf(::LocalFirstCharacterRepository) { bind<CharacterRepository>() }

    // A single, not a factory: it builds its platform formatter once, and an
    // NSDateFormatter is expensive to construct. Assemblers take it as a
    // constructor dependency from Task 18 on.
    singleOf(::AppDateTimeManager)
}
