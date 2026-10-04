package de.shinz.rickandmortyshowcase.core.ui

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.ktor.client.HttpClient

/**
 * Builds the one Coil [ImageLoader] the app loads character portraits with.
 *
 * **Configured explicitly, although Coil would work without this.**
 * `coil-network-ktor3` registers its fetcher by itself — through `ServiceLoader`
 * on Android and an `@EagerInitialization` hook on iOS — and would then build an
 * `HttpClient()` of its own with no timeouts at all. Two reasons not to lean on
 * that: a stalled image request would hang for as long as the socket stays open,
 * and the registration goes through declarations Coil marks `@InternalCoilApi`.
 *
 * It takes a **factory** rather than a client, because the client it is given
 * must not be built until an image is actually requested — see [create].
 * Configuring that client is `HttpClientFactory.createForImages`'s job in
 * `core/data`, so nothing here decides anything about HTTP.
 *
 * A class rather than a top-level function so the composition root can inject it
 * and never names the data layer itself. Coil's `PlatformContext` only exists
 * inside composition, which is why it is a parameter of [create] rather than of
 * the constructor.
 */
class AppImageLoaderFactory(
    private val imageHttpClient: () -> HttpClient,
) {
    fun create(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .components {
            // Passing the lambda, not a client: Coil wraps it in a `lazy`, so
            // the client is built on the first image and never on a screen that
            // shows none.
            add(KtorNetworkFetcherFactory(httpClient = imageHttpClient))
        }
        // Memory and disk caches are left at Coil's defaults deliberately —
        // naming the builder's `memoryCache`/`diskCache` slots at all would
        // *replace* those defaults, and a half-configured cache is worse than
        // the one Coil sizes against the platform.
        .build()
}
