package de.shinz.rickandmortyshowcase.features.characterlist.presentation.model

import de.shinz.rickandmortyshowcase.core.ui.UiText

/**
 * One-time side effects. Not assembled — the ViewModel emits these directly, and
 * maps a domain error to [UiText] at the point of emission rather than through
 * an assembler, which only ever builds rendered state.
 */
sealed interface CharacterListUiEvent {

    data class NavigateToDetail(val characterId: Int) : CharacterListUiEvent

    /**
     * A favourite could not be written.
     *
     * A message rather than a state field, because there is nothing to retry and
     * nothing to keep on screen: the heart is driven by the favourites flow, so
     * a failed write simply leaves it where it was. Without this the user taps
     * and watches nothing happen, with no explanation — the one case where the
     * screen owes them a word.
     */
    data class ShowSnackbar(val message: UiText) : CharacterListUiEvent
}
