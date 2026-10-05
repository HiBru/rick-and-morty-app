package de.shinz.rickandmortyshowcase.features.settings.presentation.model

import de.shinz.rickandmortyshowcase.core.domain.model.ThemeMode

/** The screen's only interaction. */
sealed interface SettingsUiAction {

    /**
     * A theme option was chosen.
     *
     * Carries the mode rather than an index: a list position is a fact about the
     * layout, and the one thing this screen must not get wrong is *which* theme
     * the user picked.
     */
    data class OnThemeModeClick(val themeMode: ThemeMode) : SettingsUiAction
}
