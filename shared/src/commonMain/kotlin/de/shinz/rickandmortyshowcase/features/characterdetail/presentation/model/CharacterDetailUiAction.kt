package de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model

/** Everything the user can do on the detail screen. */
sealed interface CharacterDetailUiAction {

    /**
     * The heart was tapped — a *tap*, not a toggle, exactly as on a row.
     * Adding is immediate; removing asks first, so the ViewModel branches.
     */
    data object OnFavoriteClick : CharacterDetailUiAction

    data object OnConfirmRemoveFavorite : CharacterDetailUiAction

    data object OnDismissRemoveFavorite : CharacterDetailUiAction

    /** Retry after a failed resolve. */
    data object OnRetryClick : CharacterDetailUiAction
}
