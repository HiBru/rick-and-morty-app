package de.shinz.rickandmortyshowcase.core.designsystem.previews

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

/** Favourited and not — the two looks, as provider values rather than two functions. */
internal class AppIconButtonPreviewParameterProvider : PreviewParameterProvider<Boolean> {

    override val values: Sequence<Boolean> = sequenceOf(false, true)

    override fun getDisplayName(index: Int): String =
        if (values.elementAt(index)) "favourited" else "not favourited"
}
