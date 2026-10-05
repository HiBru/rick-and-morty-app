package de.shinz.rickandmortyshowcase.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.shinz.rickandmortyshowcase.core.domain.model.ThemeMode
import de.shinz.rickandmortyshowcase.core.domain.onFailure
import de.shinz.rickandmortyshowcase.core.domain.usecase.ObserveThemeModeUseCase
import de.shinz.rickandmortyshowcase.core.domain.usecase.SetThemeModeUseCase
import de.shinz.rickandmortyshowcase.core.ui.toUiText
import de.shinz.rickandmortyshowcase.features.settings.domain.model.SettingsData
import de.shinz.rickandmortyshowcase.features.settings.presentation.assembler.SettingsUiStateAssembler
import de.shinz.rickandmortyshowcase.features.settings.presentation.model.SettingsUiAction
import de.shinz.rickandmortyshowcase.features.settings.presentation.model.SettingsUiEvent
import de.shinz.rickandmortyshowcase.features.settings.presentation.model.SettingsUiState
import de.shinz.rickandmortyshowcase.features.settings.presentation.model.SettingsViewModelState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The settings screen: one preference, read and written.
 *
 * **The selection is not held locally.** It comes back through
 * `ObserveThemeModeUseCase` after the write lands, which is what makes the radio
 * and the app's actual theme the same fact — `App()` reads the same flow. An
 * optimistic local copy would let them disagree, and the disagreement would be
 * invisible until a write failed.
 */
class SettingsViewModel(
    private val observeThemeMode: ObserveThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val assembler: SettingsUiStateAssembler,
) : ViewModel() {

    private val vmState = MutableStateFlow(SettingsViewModelState())

    val uiState: StateFlow<SettingsUiState> =
        combine(observeThemeMode().map(::SettingsData), vmState, assembler::assemble)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = assembler.assemble(
                    SettingsData.EMPTY,
                    SettingsViewModelState(),
                ),
            )

    /** `BUFFERED` + `send`, never `trySend` — see `CharacterListViewModel`. */
    private val _events = Channel<SettingsUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onAction(action: SettingsUiAction) {
        when (action) {
            is SettingsUiAction.OnThemeModeClick -> onThemeModeClick(action.themeMode)
        }
    }

    /**
     * Re-tapping the selected option writes it again rather than being skipped.
     *
     * Skipping would mean knowing the current value, and the only places to get
     * it are this ViewModel's own `uiState` or a local copy — the thing the
     * class KDoc above explains it deliberately does not keep. One redundant
     * preference write is the cheaper of the two.
     */
    private fun onThemeModeClick(themeMode: ThemeMode) {
        viewModelScope.launch {
            setThemeMode(themeMode).onFailure { error ->
                _events.send(SettingsUiEvent.ShowSnackbar(error.toUiText()))
            }
        }
    }
}
