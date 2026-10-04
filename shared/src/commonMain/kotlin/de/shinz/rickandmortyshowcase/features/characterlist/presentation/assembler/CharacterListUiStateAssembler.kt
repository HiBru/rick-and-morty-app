package de.shinz.rickandmortyshowcase.features.characterlist.presentation.assembler

import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.ui.UiText
import de.shinz.rickandmortyshowcase.core.ui.toUiText
import de.shinz.rickandmortyshowcase.features.characterlist.domain.model.CharacterListData
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListPageLoad
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListUiState
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListViewModelState
import de.shinz.rickandmortyshowcase.features.shared.presentation.model.toCharacterUi
import de.shinz.rickandmortyshowcase.generated.resources.Res
import de.shinz.rickandmortyshowcase.generated.resources.favorite_remove_message

/**
 * Turns the paging buffer and the favourite ids into what the list renders.
 *
 * Pure: no `suspend`, no `Flow`, no coroutine, no use case. It takes no
 * constructor dependencies at all — the list shows no dates, so not even a
 * formatter.
 *
 * Two things it decides that the screen would otherwise have to:
 *
 *  - **Which indicator, and which error.** `CharacterListPageLoad` says only
 *    *that* a page is loading or has failed. Whether that is the full-screen
 *    first-page treatment or the inline later-page one comes from whether any
 *    characters are already on screen, which is the rule behind `SPEC.md`'s "a
 *    failed page load never discards pages already shown".
 *  - **Whether a row is favourited.** The characters come from the API a page at
 *    a time and the favourite ids come from the database; this is the only place
 *    the two meet.
 *
 * Every branch here is a projection of its inputs, and that is deliberate.
 * *An earlier revision synthesised a `DataError` for the one combination that
 * renders nothing — idle, with an empty list — so the user would at least get a
 * retry button. Two things were wrong with it: an assembler cannot know whether
 * a failure happened, so the fabricated error was indistinguishable downstream
 * from a real one; and the retry would have requested page **2**, since a page
 * that succeeded has already advanced the counter. The judgement belongs to
 * `GetCharacterPageUseCase`, which now rejects an empty first page outright —
 * so that combination is unreachable rather than merely unhelpful.*
 */
class CharacterListUiStateAssembler {

    fun assemble(
        data: CharacterListData,
        vmState: CharacterListViewModelState,
    ): CharacterListUiState {
        // The one condition that splits every loading and error state below.
        // Safe because the buffer only ever grows: empty really does mean "no
        // page has succeeded yet", at any point in the screen's life.
        val isListEmpty = vmState.characters.isEmpty()
        val isLoading = vmState.pageLoad is CharacterListPageLoad.Loading
        // Mapped once and placed by the same condition that places the
        // indicators, so the two can never disagree about which half of the
        // screen the user is looking at.
        val failure = (vmState.pageLoad as? CharacterListPageLoad.Failed)?.error?.toUiText()

        return CharacterListUiState(
            characters = vmState.characters.map { character ->
                character.toCharacterUi(isFavorite = character.id in data.favoriteIds)
            },
            isInitialLoading = isLoading && isListEmpty,
            isNextPageLoading = isLoading && !isListEmpty,
            initialLoadError = failure.takeIf { isListEmpty },
            nextPageError = failure.takeIf { !isListEmpty },
            removeDialogText = vmState.removeDialogText(),
        )
    }

    /**
     * The dialog's message, or `null` when it is closed.
     *
     * Resolving the id against the buffer rather than holding the character in
     * state: on this screen a removal does not drop the row — the character
     * stays and only its heart changes — so the lookup always succeeds. If it
     * ever did not, no dialog is the safe answer; an "are you sure" with a blank
     * name would be worse than none.
     */
    private fun CharacterListViewModelState.removeDialogText(): UiText? {
        val pending = pendingRemovalId ?: return null
        val character = characters.firstOrNull { it.id == pending } ?: return null

        return UiText.StringResourceText(
            id = Res.string.favorite_remove_message,
            args = listOf(character.name),
        )
    }
}
