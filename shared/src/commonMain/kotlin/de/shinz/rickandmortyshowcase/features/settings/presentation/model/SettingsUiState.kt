package de.shinz.rickandmortyshowcase.features.settings.presentation.model

import androidx.compose.runtime.Immutable
import de.shinz.rickandmortyshowcase.core.domain.model.ThemeMode
import de.shinz.rickandmortyshowcase.core.ui.UiText

/**
 * Everything the settings screen renders.
 *
 * `@Immutable` because of [themeOptions] — a `List` is unstable to the compiler.
 */
@Immutable
data class SettingsUiState(
    /**
     * All three options, in a fixed order, each knowing whether it is selected.
     *
     * A list rather than "the selected mode plus three labels in the composable":
     * which option is chosen is a data-dependent decision, and so is the order,
     * so both belong to the assembler. The screen draws whatever arrives.
     */
    val themeOptions: List<ThemeOptionUi> = emptyList(),
)

/** One theme choice, as a row. */
data class ThemeOptionUi(
    /**
     * The domain enum, carried through so the row can report *which* option was
     * tapped. An identifier for an action, not a styling decision — unlike
     * `CharacterStatus`, nothing maps this to a token.
     */
    val mode: ThemeMode,
    val label: UiText,
    val isSelected: Boolean,
)
