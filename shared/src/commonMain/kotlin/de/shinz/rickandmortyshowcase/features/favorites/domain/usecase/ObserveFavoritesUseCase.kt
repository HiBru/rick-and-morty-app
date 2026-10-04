package de.shinz.rickandmortyshowcase.features.favorites.domain.usecase

import de.shinz.rickandmortyshowcase.core.domain.datasource.FavoriteCharacterLocalDataSource
import de.shinz.rickandmortyshowcase.core.domain.model.Character
import kotlinx.coroutines.flow.Flow

/**
 * Every favourited character, most recently favourited first.
 *
 * Owned by this feature rather than shared: it is the only screen that wants the
 * whole records. The lists and the detail screen want `ObserveFavoriteIdsUseCase`
 * instead, which is a different shape over the same table.
 *
 * **No error channel**, by the data source's contract: `SPEC.md` gives this
 * screen an empty state and no error state, so a read failure degrades to "no
 * favourites" rather than becoming something the user has to act on.
 */
class ObserveFavoritesUseCase(
    private val favoriteCharacters: FavoriteCharacterLocalDataSource,
) {
    operator fun invoke(): Flow<List<Character>> = favoriteCharacters.observeFavorites()
}
