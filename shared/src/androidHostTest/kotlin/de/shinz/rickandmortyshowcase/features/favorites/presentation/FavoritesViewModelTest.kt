package de.shinz.rickandmortyshowcase.features.favorites.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.datasource.FakeFavoriteCharacterLocalDataSource
import de.shinz.rickandmortyshowcase.core.domain.model.Character
import de.shinz.rickandmortyshowcase.core.domain.model.testCharacter
import de.shinz.rickandmortyshowcase.core.ui.toUiText
import de.shinz.rickandmortyshowcase.features.favorites.domain.usecase.ObserveFavoritesUseCase
import de.shinz.rickandmortyshowcase.features.favorites.presentation.assembler.FavoritesUiStateAssembler
import de.shinz.rickandmortyshowcase.features.favorites.presentation.model.FavoritesUiAction
import de.shinz.rickandmortyshowcase.features.favorites.presentation.model.FavoritesUiEvent
import de.shinz.rickandmortyshowcase.features.shared.domain.usecase.RemoveFavoriteUseCase
import de.shinz.rickandmortyshowcase.features.shared.presentation.model.CharacterUi
import de.shinz.rickandmortyshowcase.testing.MainDispatcherExtension
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * The favourites list's wiring, over the real use cases and a fake data source.
 *
 * The screen's defining property — it never reaches the network — is pinned by
 * construction rather than by assertion: there is no remote data source in this
 * ViewModel's graph at all, so a test could not accidentally allow one.
 */
class FavoritesViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val favorites = FakeFavoriteCharacterLocalDataSource()

    private val rick = testCharacter(id = 1, name = "Rick Sanchez")
    private val morty = testCharacter(id = 2, name = "Morty Smith")

    private fun viewModel() = FavoritesViewModel(
        observeFavorites = ObserveFavoritesUseCase(favorites),
        removeFavorite = RemoveFavoriteUseCase(favorites),
        assembler = FavoritesUiStateAssembler(),
    )

    /* ---------- the list ---------- */

    @Test
    fun `stored favourites are listed, newest first`() = runTest {
        // seed() is oldest-first, and observeFavorites reverses it.
        favorites.seed(rick, morty)

        val uiState = viewModel().uiState.value

        assertThat(uiState.characters.map(CharacterUi::id)).containsExactly(2, 1)
        assertThat(uiState.isEmptyStateVisible).isFalse()
    }

    @Test
    fun `no favourites shows the empty state`() = runTest {
        val uiState = viewModel().uiState.value

        assertThat(uiState.isEmptyStateVisible).isTrue()
        assertThat(uiState.isLoading).isFalse()
    }

    /**
     * SPEC: removing the last favourite brings the empty state back — without a
     * refresh, because the screen observes the database.
     */
    @Test
    fun `removing the last favourite brings back the empty state`() = runTest {
        favorites.seed(rick)
        val viewModel = viewModel()

        viewModel.onAction(FavoritesUiAction.OnFavoriteClick(characterId = 1))
        viewModel.onAction(FavoritesUiAction.OnConfirmRemoveFavorite)

        assertThat(viewModel.uiState.value.isEmptyStateVisible).isTrue()
        assertThat(viewModel.uiState.value.characters).isEqualTo(emptyList<CharacterUi>())
    }

    /**
     * The contract that lets this screen have no error state at all: a failed
     * read degrades to "no favourites" rather than to something the user has to
     * act on. The real chain emits its fallback and then **completes**, which a
     * plain `MutableStateFlow` cannot reproduce — hence the override.
     */
    @Test
    fun `a read that fails degrades to the empty state`() = runTest {
        favorites.observeFavoritesOverride = flowOf(emptyList())

        val uiState = viewModel().uiState.value

        assertThat(uiState.isEmptyStateVisible).isTrue()
        assertThat(uiState.isLoading).isFalse()
    }

    /* ---------- removal ---------- */

    /** Every row here is a favourite, so a tap can only ever mean remove. */
    @Test
    fun `tapping the heart always asks first`() = runTest {
        favorites.seed(rick)
        val viewModel = viewModel()

        viewModel.onAction(FavoritesUiAction.OnFavoriteClick(characterId = 1))

        assertThat(viewModel.uiState.value.removeDialogText).isNotNull()
        assertThat(favorites.stored).containsExactly(rick)
    }

    @Test
    fun `confirming removes the character and closes the dialog`() = runTest {
        favorites.seed(rick, morty)
        val viewModel = viewModel()
        viewModel.onAction(FavoritesUiAction.OnFavoriteClick(characterId = 1))

        viewModel.onAction(FavoritesUiAction.OnConfirmRemoveFavorite)

        assertThat(favorites.stored).containsExactly(morty)
        assertThat(viewModel.uiState.value.removeDialogText).isNull()
        assertThat(viewModel.uiState.value.characters.map(CharacterUi::id)).containsExactly(2)
    }

    @Test
    fun `dismissing the dialog changes nothing`() = runTest {
        favorites.seed(rick)
        val viewModel = viewModel()
        viewModel.onAction(FavoritesUiAction.OnFavoriteClick(characterId = 1))

        viewModel.onAction(FavoritesUiAction.OnDismissRemoveFavorite)

        assertThat(viewModel.uiState.value.removeDialogText).isNull()
        assertThat(favorites.stored).containsExactly(rick)
    }

    /** `SPEC.md` makes confirmation unconditional; this is the last place to enforce it. */
    @Test
    fun `confirming with no dialog open removes nothing`() = runTest {
        favorites.seed(rick)
        val viewModel = viewModel()

        viewModel.onAction(FavoritesUiAction.OnConfirmRemoveFavorite)

        assertThat(favorites.stored).containsExactly(rick)
    }

    /**
     * Counted, not inferred from the contents: removing an absent id is a
     * documented no-op, so `stored` looks the same whether the second confirm
     * reached the database or not. This also fails if `pendingRemovalId` were
     * cleared *after* the suspending write rather than before it.
     */
    @Test
    fun `confirming twice removes once`() = runTest {
        favorites.seed(rick)
        val viewModel = viewModel()
        viewModel.onAction(FavoritesUiAction.OnFavoriteClick(characterId = 1))

        viewModel.onAction(FavoritesUiAction.OnConfirmRemoveFavorite)
        viewModel.onAction(FavoritesUiAction.OnConfirmRemoveFavorite)

        assertThat(favorites.removeFavoriteCallCount).isEqualTo(1)
        assertThat(favorites.stored).isEqualTo(emptyList<Character>())
    }

    @Test
    fun `a removal that cannot be written reports it`() = runTest {
        favorites.seed(rick)
        val viewModel = viewModel()
        viewModel.onAction(FavoritesUiAction.OnFavoriteClick(characterId = 1))
        favorites.writeError = DataError.Local.UNKNOWN

        viewModel.events.test {
            viewModel.onAction(FavoritesUiAction.OnConfirmRemoveFavorite)

            assertThat(awaitItem()).isEqualTo(
                FavoritesUiEvent.ShowSnackbar(DataError.Local.UNKNOWN.toUiText()),
            )
        }
        // The row is driven by the database, so a failed delete leaves it there.
        assertThat(favorites.stored).containsExactly(rick)
    }

    /* ---------- navigation ---------- */

    @Test
    fun `tapping a row navigates with the id alone`() = runTest {
        favorites.seed(rick)
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(FavoritesUiAction.OnCharacterClick(characterId = 1))

            assertThat(awaitItem())
                .isEqualTo(FavoritesUiEvent.NavigateToDetail(characterId = 1))
        }
    }

    /**
     * The flash the nullable in `FavoritesData` exists to prevent.
     *
     * `@Nested` with a paused dispatcher, because the fake answers
     * synchronously: on the outer unconfined dispatcher the database has always
     * already replied, so "has not answered yet" is unobservable — and seeding
     * `FavoritesData.EMPTY` with an empty list instead of `null` would pass
     * every other test in this file while telling someone with twenty
     * favourites that they have none.
     */
    @Nested
    inner class BeforeTheDatabaseAnswers {

        @JvmField
        @RegisterExtension
        val pausedDispatcher = MainDispatcherExtension(StandardTestDispatcher())

        @Test
        fun `the first state is loading, not the empty state`() = runTest {
            favorites.seed(rick, morty)

            val viewModel = viewModel()

            val first = viewModel.uiState.value
            assertThat(first.isLoading).isTrue()
            assertThat(first.isEmptyStateVisible).isFalse()

            advanceUntilIdle()

            assertThat(viewModel.uiState.value.isLoading).isFalse()
            assertThat(viewModel.uiState.value.characters.map(CharacterUi::id)).containsExactly(2, 1)
        }

        /** And an empty database really does reach the empty state. */
        @Test
        fun `an answered empty database is the empty state`() = runTest {
            val viewModel = viewModel()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.isEmptyStateVisible).isTrue()
            assertThat(viewModel.uiState.value.isLoading).isFalse()
        }
    }
}
