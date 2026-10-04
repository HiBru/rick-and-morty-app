package de.shinz.rickandmortyshowcase.features.characterlist.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEmpty
import de.shinz.rickandmortyshowcase.core.designsystem.theme.AppTheme
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListUiAction
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListUiState

/**
 * All of the character list's Compose interactions in one place.
 *
 * Introduced with the detail screen's robot, for the reason `CLAUDE.md` gives:
 * three or more UI tests on one screen, all repeating the same `setContent`.
 */
@OptIn(ExperimentalTestApi::class)
internal class CharacterListRobot(private val test: ComposeUiTest) {

    private val actions = mutableListOf<CharacterListUiAction>()

    fun setContent(uiState: CharacterListUiState) = apply {
        test.setContent {
            AppTheme {
                CharacterListScreen(
                    uiState = uiState,
                    onAction = actions::add,
                    contentPadding = PaddingValues(),
                    snackbarHostState = SnackbarHostState(),
                )
            }
        }
    }

    /** The scrollable container, not a row — `performScrollToIndex` acts on the list. */
    fun scrollTo(index: Int) = apply {
        test.onNode(hasScrollToIndexAction()).performScrollToIndex(index)
    }

    fun tap(label: String) = apply {
        test.onNodeWithText(label).performClick()
    }

    fun assertRowVisible(name: String) = apply {
        test.onNodeWithText(name).assertHasClickAction()
    }

    fun assertNothingDispatched() = apply {
        assertThat(actions).isEmpty()
    }

    fun assertDispatched(action: CharacterListUiAction) = apply {
        assertThat(actions).contains(action)
    }
}
