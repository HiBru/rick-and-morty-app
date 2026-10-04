package de.shinz.rickandmortyshowcase.features.characterlist.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.runComposeUiTest
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEmpty
import assertk.assertions.isNotEmpty
import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.model.testCharacter
import de.shinz.rickandmortyshowcase.core.designsystem.theme.AppTheme
import de.shinz.rickandmortyshowcase.features.characterlist.domain.model.CharacterListData
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.assembler.CharacterListUiStateAssembler
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListPageLoad
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListUiAction
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListViewModelState
import kotlin.test.Test

/**
 * The part of Home that lives only in composition.
 *
 * Everything else about this screen is already covered where it is cheaper —
 * the assembler owns what is rendered, the ViewModel owns what each action
 * does. **The paging trigger is covered nowhere else**: it is a `snapshotFlow`
 * over `LazyListState`, so neither of those tests can reach it, and a stalled
 * one looks exactly like a list that simply ended.
 *
 * iOS-only for the same platform reason as the semantics test.
 */
@OptIn(ExperimentalTestApi::class)
class CharacterListScreenTest {

    private val assembler = CharacterListUiStateAssembler()

    private fun uiState(vmState: CharacterListViewModelState) =
        assembler.assemble(CharacterListData.EMPTY, vmState)

    @Test
    fun scrollingTowardsTheEndAsksForTheNextPage() = runComposeUiTest {
        val actions = mutableListOf<CharacterListUiAction>()
        val state = uiState(
            CharacterListViewModelState(
                characters = List(30) { testCharacter(it + 1, name = "Character ${it + 1}") },
                pageLoad = CharacterListPageLoad.Idle,
            ),
        )

        setContent {
            AppTheme {
                CharacterListScreen(
                    uiState = state,
                    onAction = actions::add,
                    contentPadding = PaddingValues(),
                    snackbarHostState = SnackbarHostState(),
                )
            }
        }

        // Nothing asked for while the top of a 30-row list is on screen.
        assertThat(actions).isEmpty()

        // The scrollable container, not a row: `performScrollToIndex` is an
        // action on the list itself.
        onNode(hasScrollToIndexAction()).performScrollToIndex(29)

        assertThat(actions).contains(CharacterListUiAction.OnEndOfListReached)
    }

    /**
     * The inline retry, which sits *inside* the list rather than replacing it —
     * so it is reachable only by scrolling past every row.
     */
    @Test
    fun theInlineRetryAsksForTheFailedPageAgain() = runComposeUiTest {
        val actions = mutableListOf<CharacterListUiAction>()
        val state = uiState(
            CharacterListViewModelState(
                characters = List(3) { testCharacter(it + 1, name = "Character ${it + 1}") },
                pageLoad = CharacterListPageLoad.Failed(DataError.Network.NO_INTERNET),
            ),
        )

        setContent {
            AppTheme {
                CharacterListScreen(
                    uiState = state,
                    onAction = actions::add,
                    contentPadding = PaddingValues(),
                    snackbarHostState = SnackbarHostState(),
                )
            }
        }

        // The rows survive the failure — SPEC's "never discards pages already
        // shown" — and the error sits under them.
        onNodeWithText("Character 1").assertHasClickAction()
        onNodeWithText("Retry").performClick()

        assertThat(actions.filterIsInstance<CharacterListUiAction.OnRetryClick>()).isNotEmpty()
    }

    @Test
    fun confirmingTheDialogReportsIt() = runComposeUiTest {
        val actions = mutableListOf<CharacterListUiAction>()
        val state = uiState(
            CharacterListViewModelState(
                characters = listOf(testCharacter(1, name = "Rick Sanchez")),
                pageLoad = CharacterListPageLoad.Idle,
                pendingRemovalId = 1,
            ),
        )

        setContent {
            AppTheme {
                CharacterListScreen(
                    uiState = state,
                    onAction = actions::add,
                    contentPadding = PaddingValues(),
                    snackbarHostState = SnackbarHostState(),
                )
            }
        }

        onNodeWithText("Remove").performClick()

        assertThat(actions).contains(CharacterListUiAction.OnConfirmRemoveFavorite)
    }
}
