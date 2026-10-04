package de.shinz.rickandmortyshowcase.features.favorites.presentation.model

/** Everything the user can do on Favorites. */
sealed interface FavoritesUiAction {

    /** Opens the same detail screen Home opens, with nothing but the id. */
    data class OnCharacterClick(val characterId: Int) : FavoritesUiAction

    /**
     * The heart was tapped.
     *
     * On this screen it can only ever mean *remove* — every row here is a
     * favourite — so it always opens the confirmation. The action is still named
     * for the control rather than the outcome, so the three screens report the
     * same gesture the same way.
     */
    data class OnFavoriteClick(val characterId: Int) : FavoritesUiAction

    data object OnConfirmRemoveFavorite : FavoritesUiAction

    data object OnDismissRemoveFavorite : FavoritesUiAction
}
