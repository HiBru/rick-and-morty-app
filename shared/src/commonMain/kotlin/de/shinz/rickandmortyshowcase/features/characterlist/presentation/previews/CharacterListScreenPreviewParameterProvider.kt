package de.shinz.rickandmortyshowcase.features.characterlist.presentation.previews

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.features.characterlist.domain.model.CharacterListData
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.assembler.CharacterListUiStateAssembler
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListPageLoad
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListUiState
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListViewModelState
import de.shinz.rickandmortyshowcase.features.shared.presentation.previews.CharacterPreviewSamples

/**
 * Every state of Home worth looking at, each built by running the **real**
 * assembler over a `CharacterListViewModelState` — so a preview cannot drift
 * from the screen when the assembler changes.
 *
 * Note that `pageLoad` has to be named explicitly for anything that is *not*
 * loading: its default is `Loading`, because on this screen "nothing has
 * happened yet" means "waiting for page 1".
 */
internal class CharacterListScreenPreviewParameterProvider :
    PreviewParameterProvider<CharacterListUiState> {

    private val assembler = CharacterListUiStateAssembler()

    private val characters = listOf(
        CharacterPreviewSamples.rick,
        CharacterPreviewSamples.abadango,
        CharacterPreviewSamples.longNameNoSpecies,
    )

    private fun assemble(
        vmState: CharacterListViewModelState,
        favoriteIds: Set<Int> = emptySet(),
    ) = assembler.assemble(CharacterListData(favoriteIds), vmState)

    override val values: Sequence<CharacterListUiState> = sequenceOf(
        assemble(CharacterListViewModelState()),
        assemble(
            vmState = CharacterListViewModelState(
                characters = characters,
                pageLoad = CharacterListPageLoad.Idle,
            ),
            favoriteIds = setOf(CharacterPreviewSamples.rick.id),
        ),
        assemble(
            CharacterListViewModelState(
                characters = characters,
                pageLoad = CharacterListPageLoad.Loading,
            ),
        ),
        assemble(
            CharacterListViewModelState(
                pageLoad = CharacterListPageLoad.Failed(DataError.Network.NO_INTERNET),
            ),
        ),
        assemble(
            CharacterListViewModelState(
                characters = characters,
                pageLoad = CharacterListPageLoad.Failed(DataError.Network.REQUEST_TIMEOUT),
            ),
        ),
        assemble(
            vmState = CharacterListViewModelState(
                characters = characters,
                pageLoad = CharacterListPageLoad.Idle,
                pendingRemovalId = CharacterPreviewSamples.rick.id,
            ),
            favoriteIds = setOf(CharacterPreviewSamples.rick.id),
        ),
    )

    override fun getDisplayName(index: Int): String = listOf(
        "first load",
        "loaded",
        "loading next page",
        "first load failed",
        "next page failed",
        "remove dialog",
    )[index]
}
