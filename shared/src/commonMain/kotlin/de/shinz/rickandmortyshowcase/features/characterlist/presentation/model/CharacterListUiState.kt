package de.shinz.rickandmortyshowcase.features.characterlist.presentation.model

import androidx.compose.runtime.Immutable
import de.shinz.rickandmortyshowcase.core.ui.UiText
import de.shinz.rickandmortyshowcase.features.shared.presentation.model.CharacterUi

/**
 * Everything the character list renders, with every decision already made.
 *
 * `@Immutable` because of [characters]: a `List` is unstable to the Compose
 * compiler, so without an annotation here the whole state is inferred unstable
 * and the screen never skips recomposition — the same trap `UiText` hit in Task
 * 13. The compose skill names `@Stable` for this; `@Immutable` is the stronger
 * and equally true claim, since the assembler builds a fresh state every time
 * and nothing ever mutates one.
 *
 * No derived `get()` properties. Every flag below is a plain field the assembler
 * computed, where a test can reach it.
 */
@Immutable
data class CharacterListUiState(
    val characters: List<CharacterUi> = emptyList(),

    /**
     * The first page is loading and there is nothing to show yet — a full-screen
     * indicator.
     */
    val isInitialLoading: Boolean = false,

    /**
     * A further page is loading beneath characters already on screen — the
     * distinct inline indicator `SPEC.md` asks for.
     */
    val isNextPageLoading: Boolean = false,

    /**
     * The first page failed and the list is empty: a full-screen error with
     * retry. `null` when there is no such error.
     *
     * Two nullable fields rather than one error plus a placement flag, so the
     * screen renders each with its own `?.let` and never branches on data to
     * decide where an error goes.
     */
    val initialLoadError: UiText? = null,

    /**
     * A later page failed while characters are on screen: an inline retry at the
     * end of the list, because `SPEC.md` forbids discarding what is already
     * shown.
     */
    val nextPageError: UiText? = null,

    /**
     * What the removal dialog says, or `null` when it is closed.
     *
     * Only the message is here. The dialog's title and its confirm label never
     * depend on data, so they stay `stringResource(...)` at the call site — the
     * message is the one part that names a character.
     */
    val removeDialogText: UiText? = null,
)
