package de.shinz.rickandmortyshowcase.di

import io.ktor.client.engine.HttpClientEngine

/**
 * The platform's Ktor engine — OkHttp on Android, Darwin on iOS.
 *
 * A plain `expect fun` rather than the Koin-module seam used for the database and
 * DataStore: those need `androidContext()` to resolve a path, and this needs
 * nothing but the type.
 */
internal expect fun httpClientEngine(): HttpClientEngine
