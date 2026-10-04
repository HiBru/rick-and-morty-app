package de.shinz.rickandmortyshowcase.features.favorites.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import de.shinz.rickandmortyshowcase.core.domain.model.Character
import de.shinz.rickandmortyshowcase.core.domain.model.testCharacter
import de.shinz.rickandmortyshowcase.features.favorites.domain.model.FavoritesData
import de.shinz.rickandmortyshowcase.features.favorites.presentation.assembler.FavoritesUiStateAssembler
import de.shinz.rickandmortyshowcase.features.favorites.presentation.model.FavoritesUiAction
import de.shinz.rickandmortyshowcase.features.favorites.presentation.model.FavoritesViewModelState
import kotlin.test.Test

/**
 * What Favorites renders and what each control dispatches.
 *
 * The empty state's **wording** is only readable here — resource text needs iOS
 * — and it is the one piece of copy on this screen a user sees most often when
 * the app is new.
 */
@OptIn(ExperimentalTestApi::class)
class FavoritesScreenTest {

    private val assembler = FavoritesUiStateAssembler()
    private val rick = testCharacter(id = 1, name = "Rick Sanchez")

    private fun uiState(
        characters: List<Character>?,
        pendingRemovalId: Int? = null,
    ) = assembler.assemble(
        FavoritesData(characters),
        FavoritesViewModelState(pendingRemovalId = pendingRemovalId),
    )

    @Test
    fun theEmptyStateSaysWhereFavouritesComeFrom() = runComposeUiTest {
        FavoritesRobot(this)
            .setContent(uiState(characters = emptyList()))
            .assertVisible("No favorites yet")
            // Not just that it is empty: an empty screen that only announces its
            // emptiness leaves the user to guess.
            .assertVisible("Tap the heart on any character to keep them here, ready to read offline.")
    }

    @Test
    fun aRowOpensTheDetailScreen() = runComposeUiTest {
        FavoritesRobot(this)
            .setContent(uiState(listOf(rick)))
            .assertRowOpens("Rick Sanchez")
            .tap("Rick Sanchez")
            .assertDispatched(FavoritesUiAction.OnCharacterClick(characterId = 1))
    }

    /** Every heart here is filled, and its description says "Remove". */
    @Test
    fun theHeartAsksToRemove() = runComposeUiTest {
        FavoritesRobot(this)
            .setContent(uiState(listOf(rick)))
            .tapFavorite("Remove Rick Sanchez from favorites")
            .assertDispatched(FavoritesUiAction.OnFavoriteClick(characterId = 1))
    }

    @Test
    fun confirmingTheDialogReportsIt() = runComposeUiTest {
        FavoritesRobot(this)
            .setContent(uiState(listOf(rick), pendingRemovalId = 1))
            .tap("Remove")
            .assertDispatched(FavoritesUiAction.OnConfirmRemoveFavorite)
    }

    @Test
    fun cancellingTheDialogDismissesIt() = runComposeUiTest {
        FavoritesRobot(this)
            .setContent(uiState(listOf(rick), pendingRemovalId = 1))
            .tap("Cancel")
            .assertDispatched(FavoritesUiAction.OnDismissRemoveFavorite)
    }
}
