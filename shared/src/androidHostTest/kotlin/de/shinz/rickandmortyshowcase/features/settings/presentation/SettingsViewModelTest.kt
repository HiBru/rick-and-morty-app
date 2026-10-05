package de.shinz.rickandmortyshowcase.features.settings.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.EmptyResult
import de.shinz.rickandmortyshowcase.core.domain.Result
import de.shinz.rickandmortyshowcase.core.domain.datasource.ThemePreferencesLocalDataSource
import de.shinz.rickandmortyshowcase.core.domain.model.ThemeMode
import de.shinz.rickandmortyshowcase.core.domain.usecase.ObserveThemeModeUseCase
import de.shinz.rickandmortyshowcase.core.domain.usecase.SetThemeModeUseCase
import de.shinz.rickandmortyshowcase.core.ui.toUiText
import de.shinz.rickandmortyshowcase.features.settings.presentation.assembler.SettingsUiStateAssembler
import de.shinz.rickandmortyshowcase.features.settings.presentation.model.SettingsUiAction
import de.shinz.rickandmortyshowcase.features.settings.presentation.model.SettingsUiEvent
import de.shinz.rickandmortyshowcase.testing.MainDispatcherExtension
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * The settings screen's wiring: a write goes to the store, and the selection
 * comes back from it.
 *
 * The round trip is the point. `App()` reads the same flow, so a selection held
 * locally could disagree with the theme the app is actually using — these tests
 * pin that the radio follows the *store*, not the tap.
 */
class SettingsViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val preferences = FakeThemePreferences()

    private fun viewModel() = SettingsViewModel(
        observeThemeMode = ObserveThemeModeUseCase(preferences),
        setThemeMode = SetThemeModeUseCase(preferences),
        assembler = SettingsUiStateAssembler(),
    )

    private fun selected(viewModel: SettingsViewModel) =
        viewModel.uiState.value.themeOptions.single { it.isSelected }.mode

    @Test
    fun `the stored mode is the selected one`() = runTest {
        preferences.stored.value = ThemeMode.DARK

        assertThat(selected(viewModel())).isEqualTo(ThemeMode.DARK)
    }

    @Test
    fun `choosing a mode stores it`() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(SettingsUiAction.OnThemeModeClick(ThemeMode.LIGHT))

        assertThat(preferences.stored.value).isEqualTo(ThemeMode.LIGHT)
    }

    /**
     * A write that fails leaves the radio where it was, because the radio is
     * driven by the store — so the failure has nowhere to show but a message.
     */
    @Test
    fun `a mode that cannot be stored reports it and does not move the selection`() = runTest {
        preferences.stored.value = ThemeMode.SYSTEM
        preferences.writeError = DataError.Local.DISK_FULL
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(SettingsUiAction.OnThemeModeClick(ThemeMode.DARK))

            assertThat(awaitItem()).isEqualTo(
                SettingsUiEvent.ShowSnackbar(DataError.Local.DISK_FULL.toUiText()),
            )
        }
        assertThat(selected(viewModel)).isEqualTo(ThemeMode.SYSTEM)
    }

    /**
     * Re-tapping the selected option writes it again — the ViewModel's KDoc
     * calls that out as deliberate, and the stored value cannot distinguish it
     * from a skip, so the write is counted.
     *
     * Harmless by the time it lands: the data source's `distinctUntilChanged`
     * absorbs the re-emission, so nothing recomposes and the theme does not
     * flicker. One redundant disk write is the whole cost.
     */
    @Test
    fun `re-tapping the selected option writes it again`() = runTest {
        preferences.stored.value = ThemeMode.DARK
        val viewModel = viewModel()

        viewModel.onAction(SettingsUiAction.OnThemeModeClick(ThemeMode.DARK))

        assertThat(preferences.setThemeModeCallCount).isEqualTo(1)
        assertThat(selected(viewModel)).isEqualTo(ThemeMode.DARK)
    }

    /** A write that works says nothing — only a failure is worth a message. */
    @Test
    fun `a successful write reports nothing`() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(SettingsUiAction.OnThemeModeClick(ThemeMode.DARK))

            expectNoEvents()
        }
    }

    @Test
    fun `every mode can be chosen`() = runTest {
        val viewModel = viewModel()

        ThemeMode.entries.forEach { mode ->
            viewModel.onAction(SettingsUiAction.OnThemeModeClick(mode))

            assertThat(selected(viewModel), name = mode.name).isEqualTo(mode)
        }
    }
}

/**
 * In-memory theme preferences.
 *
 * Local to this file rather than beside the interface, unlike the character
 * fakes: this is the only test that needs one, and the promotion rule says a
 * second consumer is the signal — not a guess that there might be one.
 */
private class FakeThemePreferences : ThemePreferencesLocalDataSource {

    val stored = MutableStateFlow(ThemeMode.SYSTEM)

    /** Fails [setThemeMode]. The read has no error channel, by contract. */
    var writeError: DataError.Local? = null

    /**
     * Counts writes. Needed because the stored value looks identical whether a
     * redundant write happened or was skipped.
     */
    var setThemeModeCallCount: Int = 0
        private set

    override fun observeThemeMode(): Flow<ThemeMode> = stored

    override suspend fun setThemeMode(themeMode: ThemeMode): EmptyResult<DataError.Local> {
        setThemeModeCallCount++
        writeError?.let { return Result.Error(it) }
        stored.value = themeMode

        return Result.Success(Unit)
    }
}
