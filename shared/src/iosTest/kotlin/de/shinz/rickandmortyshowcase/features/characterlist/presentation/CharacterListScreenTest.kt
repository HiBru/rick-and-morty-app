package de.shinz.rickandmortyshowcase.features.characterlist.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.model.testCharacter
import de.shinz.rickandmortyshowcase.features.characterlist.domain.model.CharacterListData
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.assembler.CharacterListUiStateAssembler
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListPageLoad
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListUiAction
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListViewModelState
import kotlin.test.Test

/**
 * The part of Home that lives only in composition.
 *
 * Everything else is covered where it is cheaper — the assembler owns what is
 * rendered, the ViewModel owns what each action does. **The paging trigger is
 * covered nowhere else**: it is a `snapshotFlow` over `LazyListState`, so
 * neither of those tests can reach it, and a stalled one looks exactly like a
 * list that simply ended.
 *
 * iOS-only for the same platform reason as the semantics test.
 */
@OptIn(ExperimentalTestApi::class)
class CharacterListScreenTest {

    private val assembler = CharacterListUiStateAssembler()

    private fun uiState(vmState: CharacterListViewModelState) =
        assembler.assemble(CharacterListData.EMPTY, vmState)

    private fun characters(count: Int) =
        List(count) { testCharacter(it + 1, name = "Character ${it + 1}") }

    @Test
    fun scrollingTowardsTheEndAsksForTheNextPage() = runComposeUiTest {
        CharacterListRobot(this)
            .setContent(
                uiState(
                    CharacterListViewModelState(
                        characters = characters(30),
                        pageLoad = CharacterListPageLoad.Idle,
                    ),
                ),
            )
            // Nothing asked for while the top of a 30-row list is on screen.
            .assertNothingDispatched()
            .scrollTo(29)
            .assertDispatched(CharacterListUiAction.OnEndOfListReached)
    }

    /**
     * The inline retry sits *inside* the list rather than replacing it, which is
     * SPEC's "a failed page load never discards pages already shown".
     */
    @Test
    fun theInlineRetryAsksForTheFailedPageAgain() = runComposeUiTest {
        CharacterListRobot(this)
            .setContent(
                uiState(
                    CharacterListViewModelState(
                        characters = characters(3),
                        pageLoad = CharacterListPageLoad.Failed(DataError.Network.NO_INTERNET),
                    ),
                ),
            )
            .assertRowVisible("Character 1")
            .tap("Retry")
            .assertDispatched(CharacterListUiAction.OnRetryClick)
    }

    @Test
    fun confirmingTheDialogReportsIt() = runComposeUiTest {
        CharacterListRobot(this)
            .setContent(
                uiState(
                    CharacterListViewModelState(
                        characters = listOf(testCharacter(1, name = "Rick Sanchez")),
                        pageLoad = CharacterListPageLoad.Idle,
                        pendingRemovalId = 1,
                    ),
                ),
            )
            .tap("Remove")
            .assertDispatched(CharacterListUiAction.OnConfirmRemoveFavorite)
    }

    /** Cancel must dismiss, not remove. */
    @Test
    fun cancellingTheDialogDismissesIt() = runComposeUiTest {
        CharacterListRobot(this)
            .setContent(
                uiState(
                    CharacterListViewModelState(
                        characters = listOf(testCharacter(1, name = "Rick Sanchez")),
                        pageLoad = CharacterListPageLoad.Idle,
                        pendingRemovalId = 1,
                    ),
                ),
            )
            .tap("Cancel")
            .assertDispatched(CharacterListUiAction.OnDismissRemoveFavorite)
    }
}
