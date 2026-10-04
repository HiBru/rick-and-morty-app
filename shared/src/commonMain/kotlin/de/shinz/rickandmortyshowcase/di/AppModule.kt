package de.shinz.rickandmortyshowcase.di

import de.shinz.rickandmortyshowcase.features.characterdetail.domain.usecase.GetCharacterUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * Everything the features need, grouped by feature with a comment per group.
 *
 * A flat list wiring four features is unreadable, and this is the only place
 * allowed to reach into all of them at once — which is why it does not live
 * inside any one of them.
 *
 * Use cases and assemblers are `factoryOf`: stateless and cheap, and a factory
 * guarantees no state ever leaks between screens.
 */
val appModule: Module = module {

    // characterdetail
    factoryOf(::GetCharacterUseCase)

    // characterlist — Task 14
    // favorites — Task 20
    // settings — Task 21 (its use cases live in coreDataStoreModule, since the
    // composition root needs them too)
}
