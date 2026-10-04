package de.shinz.rickandmortyshowcase.features.characterlist.presentation.model

/** Everything the user can do on the character list. */
sealed interface CharacterListUiAction {

    /** A row was tapped. Opens the detail screen with nothing but the id. */
    data class OnCharacterClick(val characterId: Int) : CharacterListUiAction

    /**
     * The heart was tapped — a *tap*, not a toggle.
     *
     * Deliberately one action for both directions. The row does not know which
     * it meant, and it must not: `SPEC.md` makes adding immediate but puts a
     * confirmation dialog in front of every removal, so the ViewModel is what
     * branches on the current favourite state.
     */
    data class OnFavoriteClick(val characterId: Int) : CharacterListUiAction

    /** The removal dialog was confirmed. */
    data object OnConfirmRemoveFavorite : CharacterListUiAction

    /** The removal dialog was cancelled or dismissed — nothing changes. */
    data object OnDismissRemoveFavorite : CharacterListUiAction

    /**
     * The list is scrolled near its end.
     *
     * Fired repeatedly as the user scrolls, not once per page — the ViewModel
     * owns the guard, because the screen would have to read state back to apply
     * it. The guard is exactly *"`pageLoad` is `Idle` and `endReached` is
     * false"*, and the `Idle` half is doing two jobs:
     *
     *  - `Loading` — a second request while one is in flight.
     *  - **`Failed`** — the case that is easy to miss. After a page fails the
     *    user is still parked at the bottom of the list, so every further scroll
     *    callback would re-fire the request. Offline, that is an unbounded retry
     *    loop, and it makes the retry button `SPEC.md` asks for decoration,
     *    since the screen would already be retrying by itself.
     */
    data object OnEndOfListReached : CharacterListUiAction

    /** Retry after a failed page, from either the full-screen or inline error. */
    data object OnRetryClick : CharacterListUiAction
}
