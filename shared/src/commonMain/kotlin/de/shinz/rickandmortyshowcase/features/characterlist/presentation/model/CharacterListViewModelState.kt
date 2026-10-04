package de.shinz.rickandmortyshowcase.features.characterlist.presentation.model

import de.shinz.rickandmortyshowcase.core.domain.model.Character
import de.shinz.rickandmortyshowcase.features.characterlist.domain.usecase.GetCharacterPageUseCase

/**
 * The paging buffer, and the dialog — everything this screen knows and no data
 * source does.
 *
 * The characters are here rather than in `CharacterListData` because they are
 * not observed: the screen accumulates them a page at a time and is the only
 * thing that knows how far it has got. Nothing else in the app could rebuild
 * this state, which is the test for what belongs in a `ViewModelState`.
 *
 * Every field has a default, so `CharacterListViewModelState()` is the valid
 * "nothing has happened yet" input the ViewModel gives the assembler for
 * `stateIn`'s initial value — and a preview only names the field it varies.
 * **That initial value is what the screen actually paints first**, for longer
 * than it looks: `combine` cannot emit until the favourites flow has answered
 * too, and that is a Room query on another dispatcher. Hence [pageLoad]
 * defaulting to `Loading` — the alternative is a blank Home for the length of a
 * database read.
 */
data class CharacterListViewModelState(
    /**
     * Every character loaded so far, in API order, across all pages.
     *
     * Domain models, not `CharacterUi`: the favourite flag comes from a separate
     * flow and the assembler is where the two meet. Keeping mapped UI models here
     * would mean rebuilding them on every favourite change anyway.
     *
     * It only ever grows, which is what makes `characters.isEmpty()` a safe
     * reading of "no page has succeeded yet" everywhere else.
     */
    val characters: List<Character> = emptyList(),

    /** The page to request next. 1-based, as the API counts. */
    val nextPage: Int = GetCharacterPageUseCase.FIRST_PAGE,

    /**
     * Whether a page is in flight, idle, or failed — see [CharacterListPageLoad]
     * for why those are one field and not two.
     *
     * A page request may only start from [CharacterListPageLoad.Idle]. Starting
     * one from `Failed` is what would turn a scroll at the bottom of a list into
     * an auto-retry loop against an API that is already failing.
     */
    val pageLoad: CharacterListPageLoad = CharacterListPageLoad.Loading,

    /** The API reported no next page. Nothing more will be requested. */
    val endReached: Boolean = false,

    /**
     * The character a confirmation dialog is open for, or `null`.
     *
     * An id rather than the character: the dialog needs a name, and [characters]
     * is right here to resolve it from. Removing a favourite from *this* screen
     * does not remove its row — the character stays in the list and only the
     * heart changes — so the id always resolves.
     */
    val pendingRemovalId: Int? = null,
)
