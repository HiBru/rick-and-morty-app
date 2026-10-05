package de.shinz.rickandmortyshowcase.features.settings.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import assertk.assertThat
import assertk.assertions.containsExactly
import de.shinz.rickandmortyshowcase.core.designsystem.theme.AppTheme
import de.shinz.rickandmortyshowcase.features.settings.presentation.model.SettingsUiAction
import de.shinz.rickandmortyshowcase.features.settings.presentation.model.SettingsUiState

/**
 * All of the settings screen's Compose interactions, per `CLAUDE.md`'s 3+ rule.
 *
 * `assertIsSelected` rather than a text check is the point of several of these:
 * it reads the `Selected` semantic that only `Modifier.selectable` sets, which
 * is the defect a UI test caught on this screen.
 */
@OptIn(ExperimentalTestApi::class)
internal class SettingsRobot(private val test: ComposeUiTest) {

    private val actions = mutableListOf<SettingsUiAction>()

    fun setContent(uiState: SettingsUiState) = apply {
        test.setContent {
            AppTheme {
                SettingsScreen(
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

    fun assertVisible(text: String) = apply {
        test.onNodeWithText(text).assertIsDisplayed()
    }

    fun assertSelected(label: String) = apply {
        test.onNodeWithText(label).assertIsSelected()
    }

    fun assertNotSelected(label: String) = apply {
        test.onNodeWithText(label).assertIsNotSelected()
    }

    fun assertDispatched(vararg expected: SettingsUiAction) = apply {
        assertThat(actions).containsExactly(*expected)
    }
}
