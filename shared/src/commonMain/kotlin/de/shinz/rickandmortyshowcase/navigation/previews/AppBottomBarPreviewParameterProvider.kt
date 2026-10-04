package de.shinz.rickandmortyshowcase.navigation.previews

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import de.shinz.rickandmortyshowcase.navigation.TopLevelDestination

/** Each tab selected in turn — the selected pill is the only thing that varies. */
internal class AppBottomBarPreviewParameterProvider :
    PreviewParameterProvider<TopLevelDestination> {

    override val values: Sequence<TopLevelDestination> =
        TopLevelDestination.entries.asSequence()

    override fun getDisplayName(index: Int): String =
        TopLevelDestination.entries[index].name.lowercase()
}
