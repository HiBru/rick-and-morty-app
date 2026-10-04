package de.shinz.rickandmortyshowcase.di

import de.shinz.rickandmortyshowcase.features.characterdetail.domain.usecase.GetCharacterUseCase
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.CharacterDetailViewModel
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.assembler.CharacterDetailUiStateAssembler
import de.shinz.rickandmortyshowcase.features.characterlist.domain.usecase.GetCharacterPageUseCase
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.CharacterListViewModel
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.assembler.CharacterListUiStateAssembler
import de.shinz.rickandmortyshowcase.features.favorites.domain.usecase.ObserveFavoritesUseCase
import de.shinz.rickandmortyshowcase.features.favorites.presentation.FavoritesViewModel
import de.shinz.rickandmortyshowcase.features.favorites.presentation.assembler.FavoritesUiStateAssembler
import de.shinz.rickandmortyshowcase.features.shared.domain.usecase.AddFavoriteUseCase
import de.shinz.rickandmortyshowcase.features.shared.domain.usecase.ObserveFavoriteIdsUseCase
import de.shinz.rickandmortyshowcase.features.shared.domain.usecase.RemoveFavoriteUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
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

    // shared by characterlist, characterdetail and favorites — the favourite
    // toggle every one of them offers
    factoryOf(::ObserveFavoriteIdsUseCase)
    factoryOf(::AddFavoriteUseCase)
    factoryOf(::RemoveFavoriteUseCase)

    // characterdetail
    factoryOf(::GetCharacterUseCase)
    factoryOf(::CharacterDetailUiStateAssembler)
    // Not `viewModelOf`: the character id comes from the route, which the Root
    // decodes and passes through `parametersOf`.
    viewModel { (characterId: Int) ->
        CharacterDetailViewModel(characterId, get(), get(), get(), get(), get())
    }

    // characterlist
    factoryOf(::GetCharacterPageUseCase)
    factoryOf(::CharacterListUiStateAssembler)
    viewModelOf(::CharacterListViewModel)
    // favorites
    factoryOf(::ObserveFavoritesUseCase)
    factoryOf(::FavoritesUiStateAssembler)
    viewModelOf(::FavoritesViewModel)
    // settings — Task 21 (its use cases live in coreDataStoreModule, since the
    // composition root needs them too)
}
