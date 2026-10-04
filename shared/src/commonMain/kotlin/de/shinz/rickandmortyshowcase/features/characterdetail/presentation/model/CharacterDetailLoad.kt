package de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model

import de.shinz.rickandmortyshowcase.core.domain.DataError

/**
 * Where resolving the character stands.
 *
 * The same shape as the list's `CharacterListPageLoad`, and for the same reason:
 * a boolean beside a nullable error is a free 2×2 whose fourth corner — in
 * flight *and* failed — would tell the screen to draw a spinner over an error.
 *
 * Deliberately **not** shared with the list's version. That one narrows to
 * `DataError.Network` because a page comes from one source; this one carries the
 * wide `DataError`, because `CharacterRepository.getCharacter` spans the
 * database and the API. Collapsing the two would mean widening the list's error
 * type to something it can never produce.
 */
sealed interface CharacterDetailLoad {

    /** The default: the ViewModel resolves the character as soon as it exists. */
    data object Loading : CharacterDetailLoad

    data object Idle : CharacterDetailLoad

    data class Failed(val error: DataError) : CharacterDetailLoad
}
