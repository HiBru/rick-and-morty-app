package de.shinz.rickandmortyshowcase.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
internal actual val platformDataStoreModule: Module = module {
    single<DataStore<Preferences>> {
        // Documents, matching the Room database in DatabaseModule.ios.kt. Apple's
        // documented home for app-internal state is Application Support; the two
        // are kept consistent deliberately, and moving either later needs a data
        // migration (and create = true, since Application Support does not exist
        // by default).
        val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        PreferenceDataStoreFactory.createWithPath(
            corruptionHandler = corruptionHandler(),
            scope = scope,
            produceFile = {
                val directory = requireNotNull(documentDirectory?.path) {
                    "No documents directory available to store $PREFERENCES_FILE_NAME"
                }
                "$directory/$PREFERENCES_FILE_NAME".toPath()
            },
        ).also { dataStoreScopes[it] = scope }
    } onClose { store -> store?.let { dataStoreScopes.remove(it)?.cancel() } }
}
