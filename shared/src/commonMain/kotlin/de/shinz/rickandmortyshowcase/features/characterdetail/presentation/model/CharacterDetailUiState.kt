package de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model

import androidx.compose.runtime.Immutable
import de.shinz.rickandmortyshowcase.core.ui.UiText

/**
 * Everything the detail screen renders, with every decision already made.
 *
 * Three mutually exclusive top-level states — loading, failed, loaded — and the
 * assembler guarantees exactly one is set. They are separate nullable/boolean
 * fields rather than a sealed UiState so the screen renders each with its own
 * `?.let`, which is also what keeps the removal dialog orthogonal to them.
 */
@Immutable
data class CharacterDetailUiState(
    val isLoading: Boolean = false,
    /** The full-screen error, with a retry. `null` when there is nothing wrong. */
    val error: UiText? = null,
    /** The character, once resolved. `null` while loading or failed. */
    val character: CharacterDetailUi? = null,
    /** What the removal dialog says, or `null` when it is closed. */
    val removeDialogText: UiText? = null,
)
