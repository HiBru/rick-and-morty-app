package de.shinz.rickandmortyshowcase.features.favorites.domain.model

import de.shinz.rickandmortyshowcase.core.domain.model.Character

/**
 * What the Favorites screen observes — which is everything it has.
 *
 * Unlike the character list, nothing here is fetched or paged: the database is
 * the whole source, which is what makes this screen work offline.
 */
data class FavoritesData(
    /**
     * `null` until the database has answered, which is **not** the same as an
     * empty list.
     *
     * `combine` cannot emit until this flow has, so `stateIn`'s initial value is
     * what the screen paints first — and with an empty list as the placeholder
     * it would flash "No favorites yet" at someone who has twenty, for as long
     * as a Room query takes. The nullable is that distinction made explicit
     * rather than guessed from a second flag.
     */
    val characters: List<Character>?,
) {
    companion object {
        /** Nothing has arrived yet. */
        val EMPTY = FavoritesData(characters = null)
    }
}
