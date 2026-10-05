package de.shinz.rickandmortyshowcase.features.settings.presentation.assembler

import de.shinz.rickandmortyshowcase.core.domain.model.ThemeMode
import de.shinz.rickandmortyshowcase.core.ui.UiText
import de.shinz.rickandmortyshowcase.features.settings.domain.model.SettingsData
import de.shinz.rickandmortyshowcase.features.settings.presentation.model.SettingsUiState
import de.shinz.rickandmortyshowcase.features.settings.presentation.model.SettingsViewModelState
import de.shinz.rickandmortyshowcase.features.settings.presentation.model.ThemeOptionUi
import de.shinz.rickandmortyshowcase.generated.resources.Res
import de.shinz.rickandmortyshowcase.generated.resources.theme_dark
import de.shinz.rickandmortyshowcase.generated.resources.theme_light
import de.shinz.rickandmortyshowcase.generated.resources.theme_system
import org.jetbrains.compose.resources.StringResource

/**
 * Turns the stored theme preference into three rows.
 *
 * The smallest assembler in the app, and still worth having: it owns the option
 * order, the labels and which row is selected — three data-dependent decisions
 * that would otherwise be `if`s in a composable.
 */
class SettingsUiStateAssembler {

    /**
     * [vmState] is unused, and the parameter stays: the signature is the app's
     * assembler contract, and this screen having no transient state is a fact
     * about the screen rather than a reason to break the shape.
     */
    fun assemble(
        data: SettingsData,
        vmState: SettingsViewModelState,
    ): SettingsUiState = SettingsUiState(
        themeOptions = ORDER.map { mode ->
            ThemeOptionUi(
                mode = mode,
                label = UiText.StringResourceText(mode.label()),
                isSelected = mode == data.themeMode,
            )
        },
    )

    private fun ThemeMode.label(): StringResource = when (this) {
        ThemeMode.LIGHT -> Res.string.theme_light
        ThemeMode.DARK -> Res.string.theme_dark
        ThemeMode.SYSTEM -> Res.string.theme_system
    }

    private companion object {
        /**
         * Light, Dark, then System — not the enum's declaration order by
         * accident, but because the deferral belongs last: the two explicit
         * choices read as a pair, and "follow the device" is the way out of
         * choosing.
         */
        val ORDER = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)
    }
}
