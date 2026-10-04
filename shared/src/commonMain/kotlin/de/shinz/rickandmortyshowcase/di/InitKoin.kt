package de.shinz.rickandmortyshowcase.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Starts Koin with every module the app needs.
 *
 * Shared rather than living in the Android app, because **iOS has no
 * `Application` class** — there is no platform entry point on that side to put
 * `startKoin` in, so both hosts call this instead.
 *
 * @param config extra Koin configuration. The Android overload in `androidMain`
 *   uses it to supply `androidContext(…)`, which is not optional on that side:
 *   the database and DataStore modules resolve their file paths from a `Context`,
 *   so a no-arg start would succeed and then fail on first access.
 *
 * Neither host names a Koin type. `:androidApp` has no Koin dependency at all —
 * it calls the `Context` overload — and Swift calls [initKoinIos].
 */
fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(
            // core
            coreDataModule,
            coreDatabaseModule,
            coreDataStoreModule,
            coreUiModule,

            // features
            appModule,
        )
    }
}

/**
 * The iOS entry point.
 *
 * A separate zero-argument function because Kotlin default arguments are not
 * visible to Objective-C, so Swift would otherwise have to pass an explicit
 * `nil` for a parameter that only Android uses.
 */
fun initKoinIos() = initKoin()
