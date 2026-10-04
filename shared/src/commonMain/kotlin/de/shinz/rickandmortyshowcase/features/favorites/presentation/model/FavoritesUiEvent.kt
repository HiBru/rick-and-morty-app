package de.shinz.rickandmortyshowcase.features.favorites.presentation.model

import de.shinz.rickandmortyshowcase.core.ui.UiText

/** One-time side effects. Not assembled — the ViewModel maps errors at emission. */
sealed interface FavoritesUiEvent {

    data class NavigateToDetail(val characterId: Int) : FavoritesUiEvent

    /**
     * A removal could not be written.
     *
     * The row is driven by the database, so a failed delete simply leaves it on
     * screen — without this the user confirms and watches nothing happen.
     */
    data class ShowSnackbar(val message: UiText) : FavoritesUiEvent
}
