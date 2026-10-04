package de.shinz.rickandmortyshowcase.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

internal const val DATABASE_NAME = "rickandmorty.db"

@Database(
    entities = [FavoriteCharacterEntity::class],
    version = 1,
)
@ConstructedBy(AppDatabaseConstructor::class)
internal abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteCharacterDao(): FavoriteCharacterDao
}

/**
 * Room's KSP processor generates the `actual object` for every target, which is
 * why this has no body and why the missing-actual warning is suppressed. Both
 * are required for Room on non-Android targets — without `@ConstructedBy` and
 * this declaration, the iOS build has no way to instantiate the database.
 */
@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
internal expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
