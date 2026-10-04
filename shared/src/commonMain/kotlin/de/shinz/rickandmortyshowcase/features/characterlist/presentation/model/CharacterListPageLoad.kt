package de.shinz.rickandmortyshowcase.features.characterlist.presentation.model

import de.shinz.rickandmortyshowcase.core.domain.DataError

/**
 * Where the list's paging stands: asking, idle, or stuck.
 *
 * One field instead of an `isLoadingPage` flag beside a nullable `pageError`,
 * because those two are a free 2×2 and one of the four corners is nonsense — a
 * request in flight *and* an unresolved failure would tell the screen to draw a
 * spinner and an error row at once. Nothing would have caught it: the invariant
 * would have lived in a comment, and a retry that set the flag without clearing
 * the error would satisfy the compiler.
 *
 * Here a retry is `Loading`, and `Loading` has nowhere to keep an error.
 *
 * **It does not say which page, or whether anything is on screen.** A first-page
 * load and a tenth-page load are the same state; what separates them is whether
 * `CharacterListViewModelState.characters` is still empty, and deciding that is
 * the assembler's job, not this type's.
 */
sealed interface CharacterListPageLoad {

    /**
     * A page request is in flight.
     *
     * The default, because on this screen "nothing has happened yet" and
     * "waiting for page 1" are the same moment: the ViewModel asks for the first
     * page as soon as it is constructed, so there is no reachable state with no
     * characters, no error and nothing in flight.
     */
    data object Loading : CharacterListPageLoad

    /** Nothing in flight and nothing wrong. The only state a scroll may start a page from. */
    data object Idle : CharacterListPageLoad

    /**
     * The last request failed and the user has not retried yet.
     *
     * Holds the raw [DataError] rather than text: a retryable failure is
     * rendered state, so the assembler maps it — unlike an error that becomes a
     * one-off event, which the ViewModel maps at emission.
     */
    data class Failed(val error: DataError.Network) : CharacterListPageLoad
}
