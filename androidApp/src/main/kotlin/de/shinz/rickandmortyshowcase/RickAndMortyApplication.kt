package de.shinz.rickandmortyshowcase

import android.app.Application
import de.shinz.rickandmortyshowcase.di.initKoin

/**
 * Starts Koin before anything can inject.
 *
 * The `Context` is not optional: the Room and DataStore modules resolve their
 * file paths from it, so without it Koin starts cleanly and then fails on the
 * first database or preferences access.
 */
class RickAndMortyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin(this)
    }
}
