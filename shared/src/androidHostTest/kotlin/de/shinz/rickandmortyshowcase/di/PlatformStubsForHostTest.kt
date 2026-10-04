package de.shinz.rickandmortyshowcase.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import de.shinz.rickandmortyshowcase.core.database.FavoriteCharacterDao
import de.shinz.rickandmortyshowcase.core.database.FavoriteCharacterEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Stubs the two **leaves** that need an Android `Context`, and nothing else.
 *
 * This is narrower than it first appears. Only `DataStore<Preferences>` and
 * `RoomDatabase.Builder` actually resolve a path from `androidContext()`, and
 * Koin evaluates that *inside* the definition lambda — so loading
 * `coreDataStoreModule` is harmless, and its data source and use cases are the
 * real ones. Stubbing those too would have made `KoinGraphTest` assert against
 * its own fixtures for the whole theme half of the graph.
 *
 * The DAO is stubbed rather than the `RoomDatabase.Builder` it comes from,
 * because `.build()` on a real builder opens a file. That one substitution does
 * bypass `coreDatabaseModule`, so the DAO binding itself is only ever proven by
 * the device launch check.
 */
internal fun platformStubsForHostTest(): Module = module {
    single<FavoriteCharacterDao> { NoOpFavoriteCharacterDao }
    single<DataStore<Preferences>> { EmptyPreferencesDataStore }
}

private object NoOpFavoriteCharacterDao : FavoriteCharacterDao {
    override fun observeAll(): Flow<List<FavoriteCharacterEntity>> = emptyFlow()
    override fun observeIds(): Flow<List<Int>> = emptyFlow()
    override suspend fun getById(id: Int): FavoriteCharacterEntity? = null
    override suspend fun upsert(entity: FavoriteCharacterEntity) = Unit
    override suspend fun deleteById(id: Int) = Unit
}

private object EmptyPreferencesDataStore : DataStore<Preferences> {
    override val data: Flow<Preferences> = flowOf(emptyPreferences())
    override suspend fun updateData(
        transform: suspend (Preferences) -> Preferences,
    ): Preferences = emptyPreferences()
}
