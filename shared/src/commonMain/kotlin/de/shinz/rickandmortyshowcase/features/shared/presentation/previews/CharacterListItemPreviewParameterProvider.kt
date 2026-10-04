package de.shinz.rickandmortyshowcase.features.shared.presentation.previews

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import de.shinz.rickandmortyshowcase.features.shared.presentation.model.CharacterUi
import de.shinz.rickandmortyshowcase.features.shared.presentation.model.toCharacterUi

/**
 * The four rows worth looking at.
 *
 * Each one runs the **real** `toCharacterUi` mapper over a domain sample, for
 * the same reason a screen preview runs the real assembler: a hand-built
 * `CharacterUi` would keep rendering happily after the mapper changed under it.
 */
internal class CharacterListItemPreviewParameterProvider :
    PreviewParameterProvider<CharacterUi> {

    override val values: Sequence<CharacterUi> = sequenceOf(
        CharacterPreviewSamples.rick.toCharacterUi(isFavorite = false),
        CharacterPreviewSamples.rick.toCharacterUi(isFavorite = true),
        CharacterPreviewSamples.abadango.toCharacterUi(isFavorite = false),
        CharacterPreviewSamples.longNameNoSpecies.toCharacterUi(isFavorite = false),
    )

    override fun getDisplayName(index: Int): String = listOf(
        "alive",
        "alive, favourited",
        "dead",
        "long name, blank species",
    )[index]
}
