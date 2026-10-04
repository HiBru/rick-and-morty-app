package de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model

import de.shinz.rickandmortyshowcase.core.ui.UiText

/**
 * One-time side effects.
 *
 * **No navigation event.** The only way off this screen is back, which the
 * platform and the shell already own — so the Root takes an `onNavigateBack`
 * callback and the ViewModel never has an opinion about it.
 */
sealed interface CharacterDetailUiEvent {

    /**
     * A favourite could not be written. The same reasoning as on the list: the
     * heart follows the favourites flow, so a failed write leaves it where it
     * was and the user would otherwise watch a tap do nothing.
     */
    data class ShowSnackbar(val message: UiText) : CharacterDetailUiEvent
}
