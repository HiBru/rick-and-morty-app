package de.shinz.rickandmortyshowcase.features.shared.domain.usecase

import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.EmptyResult
import de.shinz.rickandmortyshowcase.core.domain.datasource.FavoriteCharacterLocalDataSource

/**
 * Drops a character from the favourites.
 *
 * Takes only an id, unlike [AddFavoriteUseCase] — deleting a row needs nothing
 * but its key, and Favorites can remove a character it never loaded in full.
 *
 * Removing an id that is not favourited is a no-op rather than a failure, so a
 * double tap on the confirm button cannot produce an error the user has to read.
 */
class RemoveFavoriteUseCase(
    private val favoriteCharacters: FavoriteCharacterLocalDataSource,
) {
    suspend operator fun invoke(id: Int): EmptyResult<DataError.Local> =
        favoriteCharacters.removeFavorite(id)
}
