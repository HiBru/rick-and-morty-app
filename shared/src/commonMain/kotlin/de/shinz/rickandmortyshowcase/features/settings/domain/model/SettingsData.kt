package de.shinz.rickandmortyshowcase.features.settings.domain.model

import de.shinz.rickandmortyshowcase.core.domain.model.ThemeMode

/**
 * Everything the settings screen observes — one preference, which is all
 * `SPEC.md` gives this screen.
 *
 * Unlike Favorites, there is no "not yet answered" case to express: DataStore's
 * read cannot fail and has a real default, so `SYSTEM` is both the initial value
 * and a truthful one. Showing it for the instant before the store answers tells
 * the user nothing false — it is what an app with no stored choice does.
 */
data class SettingsData(
    val themeMode: ThemeMode,
) {
    companion object {
        val EMPTY = SettingsData(themeMode = ThemeMode.SYSTEM)
    }
}
