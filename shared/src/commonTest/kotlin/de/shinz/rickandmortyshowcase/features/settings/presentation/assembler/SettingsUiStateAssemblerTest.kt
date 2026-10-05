package de.shinz.rickandmortyshowcase.features.settings.presentation.assembler

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import de.shinz.rickandmortyshowcase.core.domain.model.ThemeMode
import de.shinz.rickandmortyshowcase.core.ui.UiText
import de.shinz.rickandmortyshowcase.core.ui.describe
import de.shinz.rickandmortyshowcase.features.settings.domain.model.SettingsData
import de.shinz.rickandmortyshowcase.features.settings.presentation.model.SettingsViewModelState
import de.shinz.rickandmortyshowcase.features.settings.presentation.model.ThemeOptionUi
import kotlin.test.Test

/**
 * Three decisions, all data-dependent: which options exist, in what order, and
 * which one is selected.
 */
class SettingsUiStateAssemblerTest {

    private val assembler = SettingsUiStateAssembler()

    private fun assemble(themeMode: ThemeMode) =
        assembler.assemble(SettingsData(themeMode), SettingsViewModelState())

    /**
     * Order and labels together: the list *is* the layout, so a reordering or a
     * mislabelled row is a change to what the user sees.
     */
    @Test
    fun theOptionsAreLightThenDarkThenSystem() {
        val options = assemble(ThemeMode.SYSTEM).themeOptions

        assertThat(options.map { it.mode to it.label.describe() }).containsExactly(
            ThemeMode.LIGHT to "res:theme_light",
            ThemeMode.DARK to "res:theme_dark",
            ThemeMode.SYSTEM to "res:theme_system",
        )
    }

    /** Exactly one row is selected, for every stored value — including SYSTEM. */
    @Test
    fun exactlyOneOptionIsSelectedForEveryMode() {
        ThemeMode.entries.forEach { stored ->
            val options = assemble(stored).themeOptions

            assertThat(options.filter { it.isSelected }.map(ThemeOptionUi::mode), name = "$stored")
                .containsExactly(stored)
            assertThat(options, name = "$stored").hasSize(ThemeMode.entries.size)
        }
    }

    /**
     * The default a screen paints before DataStore answers. Unlike Favorites,
     * showing it is not a lie: an app with no stored choice really does follow
     * the device.
     */
    @Test
    fun theEmptyDataSelectsSystem() {
        val options = assembler.assemble(SettingsData.EMPTY, SettingsViewModelState()).themeOptions

        assertThat(options.single { it.isSelected }.mode).isEqualTo(ThemeMode.SYSTEM)
    }
}
