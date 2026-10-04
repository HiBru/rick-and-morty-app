package de.shinz.rickandmortyshowcase.di

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import de.shinz.rickandmortyshowcase.core.database.AppDatabase
import de.shinz.rickandmortyshowcase.core.database.FavoriteCharacterDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Provides the platform's [RoomDatabase.Builder].
 *
 * A Koin `Module` rather than an `expect fun getDatabaseBuilder()`, which is the
 * nicer shape for a reason: Android needs a `Context` to resolve the database
 * path, and inside a module it can take one from `androidContext()` instead of
 * having one threaded down from the app entry point into common code that has no
 * business knowing about it. iOS needs the documents directory and takes it the
 * same way.
 */
internal expect val platformDatabaseModule: Module

val coreDatabaseModule: Module = module {
    includes(platformDatabaseModule)

    single<AppDatabase> {
        get<RoomDatabase.Builder<AppDatabase>>()
            // The bundled driver ships SQLite with the app, so both platforms run
            // the same engine and version rather than whatever the OS provides.
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    single<FavoriteCharacterDao> { get<AppDatabase>().favoriteCharacterDao() }
}
