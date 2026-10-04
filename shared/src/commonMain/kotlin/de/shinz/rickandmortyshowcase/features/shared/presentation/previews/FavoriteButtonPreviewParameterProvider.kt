package de.shinz.rickandmortyshowcase.features.shared.presentation.previews

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

/**
 * The two looks — outline and filled — as provider values rather than two
 * preview functions. The description is not a variant: it changes what a reader
 * hears, never what the button looks like.
 */
internal class FavoriteButtonPreviewParameterProvider : PreviewParameterProvider<Boolean> {

    override val values: Sequence<Boolean> = sequenceOf(false, true)

    override fun getDisplayName(index: Int): String =
        if (values.elementAt(index)) "favourited" else "not favourited"
}
