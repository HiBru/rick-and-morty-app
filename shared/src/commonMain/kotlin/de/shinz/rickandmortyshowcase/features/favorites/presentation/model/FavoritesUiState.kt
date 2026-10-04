package de.shinz.rickandmortyshowcase.features.favorites.presentation.model

import androidx.compose.runtime.Immutable
import de.shinz.rickandmortyshowcase.core.ui.UiText
import de.shinz.rickandmortyshowcase.features.shared.presentation.model.CharacterUi

/**
 * Everything Favorites renders.
 *
 * No error field: `SPEC.md` gives this screen an empty state and nothing else,
 * and the data source absorbs a read failure into an empty list by contract.
 *
 * `@Immutable` because of [characters] — a `List` is unstable to the compiler.
 */
@Immutable
data class FavoritesUiState(
    val characters: List<CharacterUi> = emptyList(),
    /** The database has not answered yet. Distinct from having no favourites. */
    val isLoading: Boolean = false,
    /** There are genuinely none — so the empty state, not the list. */
    val isEmptyStateVisible: Boolean = false,
    /** What the removal dialog says, or `null` when it is closed. */
    val removeDialogText: UiText? = null,
)
