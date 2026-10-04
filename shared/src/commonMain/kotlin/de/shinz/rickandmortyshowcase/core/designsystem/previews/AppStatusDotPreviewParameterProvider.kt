package de.shinz.rickandmortyshowcase.core.designsystem.previews

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import de.shinz.rickandmortyshowcase.core.domain.model.CharacterStatus

/**
 * Every status, because each one is a different colour and the whole point of
 * the component is that mapping. `getDisplayName` keeps them apart in the
 * preview pane, where three identical dots would otherwise be unlabelled.
 */
internal class AppStatusDotPreviewParameterProvider :
    PreviewParameterProvider<CharacterStatus> {

    override val values: Sequence<CharacterStatus> = CharacterStatus.entries.asSequence()

    override fun getDisplayName(index: Int): String =
        CharacterStatus.entries[index].name.lowercase()
}
