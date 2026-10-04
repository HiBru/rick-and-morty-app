package de.shinz.rickandmortyshowcase.features.shared.domain.usecase

import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.EmptyResult
import de.shinz.rickandmortyshowcase.core.domain.datasource.FavoriteCharacterLocalDataSource
import de.shinz.rickandmortyshowcase.core.domain.model.Character

/**
 * Saves a character to the favourites.
 *
 * Takes the whole [Character] rather than an id: `SPEC.md` requires every value
 * the API returned to be persisted, so a favourited character renders completely
 * while offline. An id would force this to go and fetch what the caller already
 * has — and would fail at exactly the moment the feature exists for.
 *
 * Separate from [RemoveFavoriteUseCase] rather than one toggle, because the two
 * are not symmetric: adding is immediate, removing opens a confirmation dialog.
 * The ViewModel therefore branches before calling either, and a single
 * `ToggleFavoriteUseCase` would have to be told which half to run.
 *
 * One source, so the error type stays narrowed to [DataError.Local].
 */
class AddFavoriteUseCase(
    private val favoriteCharacters: FavoriteCharacterLocalDataSource,
) {
    suspend operator fun invoke(character: Character): EmptyResult<DataError.Local> =
        favoriteCharacters.addFavorite(character)
}
