package de.shinz.rickandmortyshowcase.core.domain.model

/**
 * The app's theme preference, as chosen on the settings screen and persisted in
 * DataStore.
 *
 * [SYSTEM] is the default and stays unresolved here on purpose: whether it means
 * light or dark can only be answered inside a composition, and it keeps being
 * answered, so flipping the device theme while the app runs is picked up.
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}
