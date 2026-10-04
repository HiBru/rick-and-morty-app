package de.shinz.rickandmortyshowcase.features.characterlist.domain.model

/**
 * Everything the character list observes rather than fetches.
 *
 * Exactly one field, and that is not an oversight. The characters themselves are
 * paged — the screen asks for page N and keeps what came back — so they live in
 * `CharacterListViewModelState`, not here. The favourite ids are the only thing
 * that arrives as a `Flow`, because favouriting on another screen has to show up
 * on this one without a refresh.
 *
 * It still earns being a type: it gives the assembler one named input instead of
 * a bare `Set<Int>`, and [EMPTY] is what the ViewModel hands `stateIn` for its
 * initial value — the one `UiState` that exists before the database has answered.
 */
data class CharacterListData(
    val favoriteIds: Set<Int>,
) {
    companion object {
        val EMPTY = CharacterListData(favoriteIds = emptySet())
    }
}
