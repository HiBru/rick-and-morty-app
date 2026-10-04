package de.shinz.rickandmortyshowcase.features.favorites.presentation.previews

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import de.shinz.rickandmortyshowcase.features.favorites.domain.model.FavoritesData
import de.shinz.rickandmortyshowcase.features.favorites.presentation.assembler.FavoritesUiStateAssembler
import de.shinz.rickandmortyshowcase.features.favorites.presentation.model.FavoritesUiState
import de.shinz.rickandmortyshowcase.features.favorites.presentation.model.FavoritesViewModelState
import de.shinz.rickandmortyshowcase.features.shared.presentation.previews.CharacterPreviewSamples

/**
 * The three states this screen has, each built by running the **real** assembler.
 *
 * The empty state is the one worth having in a preview and the hardest to reach
 * by hand — it needs a user with no favourites, which stops being true the
 * moment anyone tests the app.
 *
 * No removal-dialog variant: `AppConfirmDialog` renders into its own platform
 * window and never appears in a `@Preview`.
 */
internal class FavoritesScreenPreviewParameterProvider :
    PreviewParameterProvider<FavoritesUiState> {

    private val assembler = FavoritesUiStateAssembler()

    override val values: Sequence<FavoritesUiState> = sequenceOf(
        assembler.assemble(FavoritesData.EMPTY, FavoritesViewModelState()),
        assembler.assemble(FavoritesData(emptyList()), FavoritesViewModelState()),
        assembler.assemble(
            FavoritesData(
                listOf(
                    CharacterPreviewSamples.rick,
                    CharacterPreviewSamples.abadango,
                    CharacterPreviewSamples.longNameNoSpecies,
                ),
            ),
            FavoritesViewModelState(),
        ),
    )

    override fun getDisplayName(index: Int): String = listOf(
        "loading",
        "empty",
        "with favourites",
    )[index]
}
