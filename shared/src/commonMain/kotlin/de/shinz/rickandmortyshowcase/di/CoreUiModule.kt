package de.shinz.rickandmortyshowcase.di

import de.shinz.rickandmortyshowcase.core.data.HttpClientFactory
import de.shinz.rickandmortyshowcase.core.ui.AppImageLoaderFactory
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * UI infrastructure that is neither a screen nor a design value.
 *
 * One binding so far. It is its own module rather than a line in
 * [coreDataModule] because an image loader is a presentation concern that merely
 * *uses* an HTTP engine — filing it under data would be the first step towards a
 * module that means nothing.
 *
 * `single` here buys nothing that `factory` would not: Coil invokes a
 * `SingletonImageLoader.Factory` at most once, so either scope would end up with
 * exactly one `ImageLoader`, one memory cache and one HTTP client. It is a
 * `single` because the plan reserves `factoryOf` for use cases and assemblers —
 * the things where a leaked instance would mean leaked *screen* state — and this
 * is neither.
 *
 * Not `singleOf`: the constructor takes a factory lambda, which is exactly the
 * case the Koin skill keeps `single { }` for. The lambda is what defers building
 * the image client until the first portrait is actually fetched.
 */
val coreUiModule: Module = module {
    single { AppImageLoaderFactory(imageHttpClient = { HttpClientFactory.createForImages(get()) }) }
}
