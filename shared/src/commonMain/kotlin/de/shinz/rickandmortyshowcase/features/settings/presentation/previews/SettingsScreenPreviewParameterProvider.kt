package de.shinz.rickandmortyshowcase.features.settings.presentation.previews

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import de.shinz.rickandmortyshowcase.core.domain.model.ThemeMode
import de.shinz.rickandmortyshowcase.features.settings.domain.model.SettingsData
import de.shinz.rickandmortyshowcase.features.settings.presentation.assembler.SettingsUiStateAssembler
import de.shinz.rickandmortyshowcase.features.settings.presentation.model.SettingsUiState
import de.shinz.rickandmortyshowcase.features.settings.presentation.model.SettingsViewModelState

/**
 * One value per stored mode, each through the **real** assembler.
 *
 * Three variants for one screen is not padding: which row carries the selection
 * is the only thing this screen renders, and `@PreviewLightDark` crossed with
 * them is also the quickest way to see that a Dark selection still reads
 * correctly when the preview itself is light.
 */
internal class SettingsScreenPreviewParameterProvider :
    PreviewParameterProvider<SettingsUiState> {

    private val assembler = SettingsUiStateAssembler()

    override val values: Sequence<SettingsUiState> = ThemeMode.entries
        .asSequence()
        .map { assembler.assemble(SettingsData(it), SettingsViewModelState()) }

    override fun getDisplayName(index: Int): String =
        "${ThemeMode.entries[index].name.lowercase()} selected"
}
