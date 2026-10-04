package de.shinz.rickandmortyshowcase.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose

internal actual val platformDataStoreModule: Module = module {
    single<DataStore<Preferences>> {
        val context: Context = androidContext()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        PreferenceDataStoreFactory.createWithPath(
            corruptionHandler = corruptionHandler(),
            scope = scope,
            produceFile = {
                context.filesDir.resolve(PREFERENCES_FILE_NAME).absolutePath.toPath()
            },
        ).also { dataStoreScopes[it] = scope }
    } onClose { store -> store?.let { dataStoreScopes.remove(it)?.cancel() } }
}
