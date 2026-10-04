package de.shinz.rickandmortyshowcase.di

import android.content.Context
import org.koin.android.ext.koin.androidContext

/**
 * The Android entry point.
 *
 * An overload taking a `Context` rather than exposing `KoinAppDeclaration` to
 * the host: `:androidApp` is a thin bootstrap and has no business depending on
 * Koin to hand over something it already holds.
 */
fun initKoin(context: Context) = initKoin { androidContext(context) }
