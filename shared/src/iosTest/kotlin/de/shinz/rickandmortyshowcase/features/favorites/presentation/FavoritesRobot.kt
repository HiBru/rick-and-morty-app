package de.shinz.rickandmortyshowcase.features.favorites.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import assertk.assertThat
import assertk.assertions.containsExactly
import de.shinz.rickandmortyshowcase.core.designsystem.theme.AppTheme
import de.shinz.rickandmortyshowcase.features.favorites.presentation.model.FavoritesUiAction
import de.shinz.rickandmortyshowcase.features.favorites.presentation.model.FavoritesUiState

/** All of the Favorites screen's Compose interactions, per `CLAUDE.md`'s 3+ rule. */
@OptIn(ExperimentalTestApi::class)
internal class FavoritesRobot(private val test: ComposeUiTest) {

    private val actions = mutableListOf<FavoritesUiAction>()

    fun setContent(uiState: FavoritesUiState) = apply {
        test.setContent {
            AppTheme {
                FavoritesScreen(
                    uiState = uiState,
                    onAction = actions::add,
                    contentPadding = PaddingValues(),
                    snackbarHostState = SnackbarHostState(),
                )
            }
        }
    }

    fun tap(label: String) = apply {
        test.onNodeWithText(label).performClick()
    }

    fun tapFavorite(description: String) = apply {
        test.onNodeWithContentDescription(description).performClick()
    }

    fun assertVisible(text: String) = apply {
        test.onNodeWithText(text).assertIsDisplayed()
    }

    fun assertRowOpens(name: String) = apply {
        test.onNodeWithText(name).assertHasClickAction()
    }

    fun assertDispatched(vararg expected: FavoritesUiAction) = apply {
        assertThat(actions).containsExactly(*expected)
    }
}
