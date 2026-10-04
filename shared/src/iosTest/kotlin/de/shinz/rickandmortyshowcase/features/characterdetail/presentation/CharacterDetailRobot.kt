package de.shinz.rickandmortyshowcase.features.characterdetail.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import de.shinz.rickandmortyshowcase.core.designsystem.theme.AppTheme
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailUiAction
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailUiState

/**
 * All of the detail screen's Compose interactions in one place.
 *
 * `CLAUDE.md` asks for a robot once a screen has three or more UI tests — this
 * one has five, and the nine-line `setContent` block was repeated in every one
 * of them. Each function returns `this`, so a test reads as the sequence a user
 * performs.
 *
 * It takes a `ComposeUiTest` rather than the skill's `ComposeContentTestRule`:
 * these run through `runComposeUiTest`, which is the multiplatform entry point —
 * there is no JUnit rule on this side.
 */
@OptIn(ExperimentalTestApi::class)
internal class CharacterDetailRobot(private val test: ComposeUiTest) {

    private val actions = mutableListOf<CharacterDetailUiAction>()
    private var backCount = 0

    fun setContent(uiState: CharacterDetailUiState) = apply {
        test.setContent {
            AppTheme {
                CharacterDetailScreen(
                    uiState = uiState,
                    onAction = actions::add,
                    onNavigateBack = { backCount++ },
                    contentPadding = PaddingValues(),
                    snackbarHostState = SnackbarHostState(),
                )
            }
        }
    }

    fun tapFavorite(description: String) = apply {
        test.onNodeWithContentDescription(description).performClick()
    }

    fun tapBack() = apply {
        test.onNodeWithContentDescription("Back").performClick()
    }

    fun tap(label: String) = apply {
        test.onNodeWithText(label).performClick()
    }

    fun assertFieldReadsAsOneNode(label: String, value: String) = apply {
        test.onNodeWithText(label).assertTextEquals(label, value)
    }

    fun assertDispatched(vararg expected: CharacterDetailUiAction) = apply {
        assertThat(actions).containsExactly(*expected)
    }

    fun assertNavigatedBack(times: Int) = apply {
        assertThat(backCount).isEqualTo(times)
    }
}
