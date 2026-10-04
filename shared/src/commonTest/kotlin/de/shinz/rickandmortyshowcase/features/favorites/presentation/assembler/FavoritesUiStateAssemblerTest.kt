package de.shinz.rickandmortyshowcase.features.favorites.presentation.assembler

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import de.shinz.rickandmortyshowcase.core.domain.model.Character
import de.shinz.rickandmortyshowcase.core.domain.model.testCharacter
import de.shinz.rickandmortyshowcase.core.ui.UiText
import de.shinz.rickandmortyshowcase.features.favorites.domain.model.FavoritesData
import de.shinz.rickandmortyshowcase.features.favorites.presentation.model.FavoritesUiState
import de.shinz.rickandmortyshowcase.features.favorites.presentation.model.FavoritesViewModelState
import de.shinz.rickandmortyshowcase.features.shared.presentation.model.CharacterUi
import de.shinz.rickandmortyshowcase.generated.resources.Res
import de.shinz.rickandmortyshowcase.generated.resources.favorite_remove_message
import kotlin.test.Test

/**
 * The distinction this screen turns on: "the database has not answered" is not
 * "there are no favourites". Everything else here is one short branch.
 */
class FavoritesUiStateAssemblerTest {

    private val assembler = FavoritesUiStateAssembler()

    private fun assemble(
        characters: List<Character>?,
        vmState: FavoritesViewModelState = FavoritesViewModelState(),
    ) = assembler.assemble(FavoritesData(characters), vmState)

    /** The default `FavoritesData.EMPTY` — what the screen paints first. */
    @Test
    fun aDatabaseThatHasNotAnsweredIsLoading() {
        val uiState = assemble(characters = null)

        assertThat(uiState).prop(FavoritesUiState::isLoading).isTrue()
        assertThat(uiState).prop(FavoritesUiState::isEmptyStateVisible).isFalse()
        assertThat(uiState.characters).isEqualTo(emptyList<CharacterUi>())
    }

    /**
     * The failure this file exists for: with an empty list as the placeholder,
     * someone with twenty favourites is told they have none for as long as a
     * Room query takes.
     */
    @Test
    fun anAnsweredEmptyDatabaseIsTheEmptyState() {
        val uiState = assemble(characters = emptyList())

        assertThat(uiState).prop(FavoritesUiState::isEmptyStateVisible).isTrue()
        assertThat(uiState).prop(FavoritesUiState::isLoading).isFalse()
    }

    /** Every row here is a favourite by definition, so every heart is filled. */
    @Test
    fun everyRowIsMarkedFavourite() {
        val uiState = assemble(listOf(testCharacter(1), testCharacter(2)))

        assertThat(uiState.characters.map { it.id to it.isFavorite })
            .containsExactly(1 to true, 2 to true)
        assertThat(uiState).prop(FavoritesUiState::isEmptyStateVisible).isFalse()
        assertThat(uiState).prop(FavoritesUiState::isLoading).isFalse()
    }

    /** The data source's order — most recently favourited first — is preserved. */
    @Test
    fun theStoredOrderIsKept() {
        val uiState = assemble(listOf(testCharacter(3), testCharacter(1), testCharacter(2)))

        assertThat(uiState.characters.map(CharacterUi::id)).containsExactly(3, 1, 2)
    }

    @Test
    fun theRemovalDialogNamesTheCharacter() {
        val uiState = assemble(
            characters = listOf(testCharacter(1, name = "Rick Sanchez"), testCharacter(2, name = "Morty Smith")),
            vmState = FavoritesViewModelState(pendingRemovalId = 2),
        )

        assertThat(uiState).prop(FavoritesUiState::removeDialogText).isEqualTo(
            UiText.StringResourceText(
                id = Res.string.favorite_remove_message,
                args = listOf("Morty Smith"),
            ),
        )
    }

    @Test
    fun noPendingRemovalMeansNoDialog() {
        assertThat(assemble(listOf(testCharacter(1))))
            .prop(FavoritesUiState::removeDialogText).isNull()
    }

    /**
     * **Reachable on this screen**, unlike on the character list: the row leaves
     * the list the moment the removal lands, and the same character can be
     * un-favourited from the detail screen while this dialog is open.
     */
    @Test
    fun anIdThatHasLeftTheListMeansNoDialog() {
        val uiState = assemble(
            characters = listOf(testCharacter(1)),
            vmState = FavoritesViewModelState(pendingRemovalId = 99),
        )

        assertThat(uiState).prop(FavoritesUiState::removeDialogText).isNull()
    }

    @Test
    fun aPendingRemovalBeforeTheDatabaseAnswersMeansNoDialog() {
        val uiState = assemble(
            characters = null,
            vmState = FavoritesViewModelState(pendingRemovalId = 1),
        )

        assertThat(uiState).prop(FavoritesUiState::removeDialogText).isNull()
    }
}
