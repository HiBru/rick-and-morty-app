package de.shinz.rickandmortyshowcase.di

import androidx.room.Room
import androidx.room.RoomDatabase
import de.shinz.rickandmortyshowcase.core.database.AppDatabase
import de.shinz.rickandmortyshowcase.core.database.DATABASE_NAME
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
internal actual val platformDatabaseModule: Module = module {
    single<RoomDatabase.Builder<AppDatabase>> {
        // Documents, not Caches or tmp: favourites are user data and must survive
        // the system reclaiming space.
        val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )

        Room.databaseBuilder<AppDatabase>(
            name = requireNotNull(documentDirectory?.path) {
                "No documents directory available to store $DATABASE_NAME"
            } + "/" + DATABASE_NAME,
        )
    }
}
