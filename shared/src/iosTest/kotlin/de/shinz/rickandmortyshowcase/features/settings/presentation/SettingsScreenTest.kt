package de.shinz.rickandmortyshowcase.features.settings.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import de.shinz.rickandmortyshowcase.core.domain.model.ThemeMode
import de.shinz.rickandmortyshowcase.features.settings.domain.model.SettingsData
import de.shinz.rickandmortyshowcase.features.settings.presentation.assembler.SettingsUiStateAssembler
import de.shinz.rickandmortyshowcase.features.settings.presentation.model.SettingsUiAction
import de.shinz.rickandmortyshowcase.features.settings.presentation.model.SettingsViewModelState
import kotlin.test.Test

/**
 * What the settings screen says, and what tapping a row dispatches.
 *
 * The **wording** is only readable here — resource text needs iOS — and on this
 * screen the wording is very nearly all there is.
 */
@OptIn(ExperimentalTestApi::class)
class SettingsScreenTest {

    private val assembler = SettingsUiStateAssembler()

    private fun uiState(themeMode: ThemeMode) =
        assembler.assemble(SettingsData(themeMode), SettingsViewModelState())

    @Test
    fun theHeadingAndTheThreeOptionsReadAsThemselves() = runComposeUiTest {
        SettingsRobot(this)
            .setContent(uiState(ThemeMode.SYSTEM))
            .assertVisible("Appearance")
            .assertVisible("Light")
            .assertVisible("Dark")
            // Not "System": the option defers to the device rather than naming a
            // theme, and the label says so.
            .assertVisible("Follow the device")
    }

    /**
     * The row carries the `Selected` semantic, which only `Modifier.selectable`
     * sets — with `clickable` the row is activatable but a screen reader cannot
     * tell which theme is in use. That regression is invisible on screen.
     */
    @Test
    fun exactlyTheStoredOptionReadsAsSelected() = runComposeUiTest {
        SettingsRobot(this)
            .setContent(uiState(ThemeMode.DARK))
            .assertSelected("Dark")
            .assertNotSelected("Light")
            .assertNotSelected("Follow the device")
    }

    /** The whole row is the target, not the 20dp circle inside it. */
    @Test
    fun tappingARowChoosesThatMode() = runComposeUiTest {
        SettingsRobot(this)
            .setContent(uiState(ThemeMode.SYSTEM))
            .tap("Light")
            .assertDispatched(SettingsUiAction.OnThemeModeClick(ThemeMode.LIGHT))
    }
}
