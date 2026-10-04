package de.shinz.rickandmortyshowcase.features.shared.domain.usecase

import de.shinz.rickandmortyshowcase.core.domain.datasource.FavoriteCharacterLocalDataSource
import kotlinx.coroutines.flow.Flow

/**
 * The ids of every favourited character, for marking rows.
 *
 * In `features/shared/` because three features inject it — the character list,
 * the detail screen and Favorites all need to know whether the character in
 * front of them is saved. Not in `core/`, because the composition root does not.
 *
 * A `Flow` rather than a one-shot read, and that is the whole point: `SPEC.md`
 * requires favouriting on Home to show up on the detail screen and in Favorites
 * without a refresh, which only an observable source gives for free.
 */
class ObserveFavoriteIdsUseCase(
    private val favoriteCharacters: FavoriteCharacterLocalDataSource,
) {
    operator fun invoke(): Flow<Set<Int>> = favoriteCharacters.observeFavoriteIds()
}
