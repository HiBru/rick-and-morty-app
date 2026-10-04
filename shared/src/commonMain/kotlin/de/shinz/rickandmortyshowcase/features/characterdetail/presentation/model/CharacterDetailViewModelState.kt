package de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model

import de.shinz.rickandmortyshowcase.core.domain.model.Character

/**
 * What the detail screen knows and no data source does.
 *
 * [load] defaults to `Loading` for the same reason the list's does: the
 * ViewModel starts resolving in `init`, so there is no reachable state with no
 * character, no error and nothing in flight — and `stateIn`'s initial value,
 * assembled from this, is what the screen actually paints first.
 */
data class CharacterDetailViewModelState(
    /**
     * The resolved character, from the local row or the API — the screen cannot
     * tell which, and must not: that is the repository's business.
     */
    val character: Character? = null,

    val load: CharacterDetailLoad = CharacterDetailLoad.Loading,

    /**
     * The removal confirmation is open.
     *
     * A `Boolean`, not an id as on the list: this screen only ever shows one
     * character, so an id would be a second place for the same fact to live.
     */
    val isRemovalPending: Boolean = false,
)
