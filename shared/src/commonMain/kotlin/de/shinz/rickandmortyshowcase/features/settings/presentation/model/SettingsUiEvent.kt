package de.shinz.rickandmortyshowcase.features.settings.presentation.model

import de.shinz.rickandmortyshowcase.core.ui.UiText

/** One-time side effects. */
sealed interface SettingsUiEvent {

    /**
     * The choice could not be stored.
     *
     * The selection is driven by the stored value, so a failed write simply
     * leaves the radio where it was — without this the user taps an option and
     * watches it refuse to move, with no explanation.
     */
    data class ShowSnackbar(val message: UiText) : SettingsUiEvent
}
