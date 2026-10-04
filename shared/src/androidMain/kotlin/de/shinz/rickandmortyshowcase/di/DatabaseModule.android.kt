package de.shinz.rickandmortyshowcase.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import de.shinz.rickandmortyshowcase.core.database.AppDatabase
import de.shinz.rickandmortyshowcase.core.database.DATABASE_NAME
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformDatabaseModule: Module = module {
    single<RoomDatabase.Builder<AppDatabase>> {
        val context: Context = androidContext()
        Room.databaseBuilder<AppDatabase>(
            context = context.applicationContext,
            // The absolute path, not the bare name: the bundled driver opens the
            // file itself rather than going through Android's SQLiteOpenHelper,
            // so it needs somewhere real to put it.
            name = context.getDatabasePath(DATABASE_NAME).absolutePath,
        )
    }
}
