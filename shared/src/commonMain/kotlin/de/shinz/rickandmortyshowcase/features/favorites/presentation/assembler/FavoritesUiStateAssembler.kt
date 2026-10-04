package de.shinz.rickandmortyshowcase.features.favorites.presentation.assembler

import de.shinz.rickandmortyshowcase.core.domain.model.Character
import de.shinz.rickandmortyshowcase.core.ui.UiText
import de.shinz.rickandmortyshowcase.features.favorites.domain.model.FavoritesData
import de.shinz.rickandmortyshowcase.features.favorites.presentation.model.FavoritesUiState
import de.shinz.rickandmortyshowcase.features.favorites.presentation.model.FavoritesViewModelState
import de.shinz.rickandmortyshowcase.features.shared.presentation.model.toCharacterUi
import de.shinz.rickandmortyshowcase.generated.resources.Res
import de.shinz.rickandmortyshowcase.generated.resources.favorite_remove_message

/**
 * Turns the stored favourites into what the screen renders.
 *
 * Pure, and with no constructor dependencies — like the character list's, and
 * unlike the detail screen's, which needs a date formatter.
 *
 * Every row is favourited by definition, so `isFavorite` is `true` for all of
 * them: the heart is always filled here, and tapping it can only mean remove.
 */
class FavoritesUiStateAssembler {

    fun assemble(
        data: FavoritesData,
        vmState: FavoritesViewModelState,
    ): FavoritesUiState {
        val characters = data.characters

        return FavoritesUiState(
            characters = characters.orEmpty().map { it.toCharacterUi(isFavorite = true) },
            isLoading = characters == null,
            // Only once the database has actually answered. `null` is "not yet",
            // and showing "No favorites yet" during a Room query would tell
            // someone with twenty of them the opposite of the truth.
            isEmptyStateVisible = characters?.isEmpty() == true,
            removeDialogText = vmState.removeDialogText(characters),
        )
    }

    /**
     * The dialog's message, or `null` when it is closed — or when the id no
     * longer resolves.
     *
     * Unlike the character list, that second case is **reachable here**: the row
     * leaves the list the moment the removal succeeds, and the same character
     * can be un-favourited from the detail screen while this dialog is open. An
     * "are you sure" naming nobody is worse than none.
     */
    private fun FavoritesViewModelState.removeDialogText(
        characters: List<Character>?,
    ): UiText? {
        val pending = pendingRemovalId ?: return null
        val character = characters?.firstOrNull { it.id == pending } ?: return null

        return UiText.StringResourceText(
            id = Res.string.favorite_remove_message,
            args = listOf(character.name),
        )
    }
}
