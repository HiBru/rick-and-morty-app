package de.shinz.rickandmortyshowcase.features.characterdetail.presentation.previews

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.format.AppDateTimeManager
import de.shinz.rickandmortyshowcase.features.characterdetail.domain.model.CharacterDetailData
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.assembler.CharacterDetailUiStateAssembler
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailLoad
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailUiState
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailViewModelState
import de.shinz.rickandmortyshowcase.features.shared.presentation.previews.CharacterPreviewSamples

/**
 * Every state of the detail screen worth looking at, each built by running the
 * **real** assembler — so a preview cannot drift from the screen.
 *
 * **No removal-dialog variant.** `AppConfirmDialog` renders into its own
 * platform window, which never appears in a `@Preview` — the value would be
 * visually identical to "loaded". The dialog's own look is previewed through
 * `AppConfirmDialogContent`, and its wiring through the screen's UI tests.
 *
 * `load` has to be named for anything that is not loading: its default is
 * `Loading`, because the ViewModel resolves as soon as it exists.
 *
 * The date in these is formatted by a real [AppDateTimeManager] in the device's
 * zone, so the "Added to the API" row can differ by a day between machines. That
 * is the formatter doing its job, not preview drift.
 */
internal class CharacterDetailScreenPreviewParameterProvider :
    PreviewParameterProvider<CharacterDetailUiState> {

    private val assembler = CharacterDetailUiStateAssembler(AppDateTimeManager())

    private fun assemble(
        vmState: CharacterDetailViewModelState,
        isFavorite: Boolean = false,
    ) = assembler.assemble(CharacterDetailData(isFavorite), vmState)

    override val values: Sequence<CharacterDetailUiState> = sequenceOf(
        assemble(CharacterDetailViewModelState()),
        assemble(
            CharacterDetailViewModelState(
                character = CharacterPreviewSamples.rick,
                load = CharacterDetailLoad.Idle,
            ),
        ),
        assemble(
            vmState = CharacterDetailViewModelState(
                character = CharacterPreviewSamples.rick,
                load = CharacterDetailLoad.Idle,
            ),
            isFavorite = true,
        ),
        // The one with a non-blank `type`, and a name long enough to wrap under
        // the favourite button.
        assemble(
            CharacterDetailViewModelState(
                character = CharacterPreviewSamples.longNameNoSpecies,
                load = CharacterDetailLoad.Idle,
            ),
        ),
        assemble(
            CharacterDetailViewModelState(
                load = CharacterDetailLoad.Failed(DataError.Network.NO_INTERNET),
            ),
        ),
    )

    override fun getDisplayName(index: Int): String = listOf(
        "loading",
        "loaded",
        "loaded, favourited",
        "long name, with a type",
        "failed",
    )[index]
}
