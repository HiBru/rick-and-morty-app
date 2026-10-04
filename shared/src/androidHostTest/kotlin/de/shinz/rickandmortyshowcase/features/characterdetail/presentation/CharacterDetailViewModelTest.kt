package de.shinz.rickandmortyshowcase.features.characterdetail.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import de.shinz.rickandmortyshowcase.core.data.LocalFirstCharacterRepository
import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.datasource.FakeCharacterRemoteDataSource
import de.shinz.rickandmortyshowcase.core.domain.datasource.FakeFavoriteCharacterLocalDataSource
import de.shinz.rickandmortyshowcase.core.domain.model.Character
import de.shinz.rickandmortyshowcase.core.domain.model.testCharacter
import de.shinz.rickandmortyshowcase.core.format.AppDateTimeManager
import de.shinz.rickandmortyshowcase.core.ui.toUiText
import de.shinz.rickandmortyshowcase.features.characterdetail.domain.usecase.GetCharacterUseCase
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.assembler.CharacterDetailUiStateAssembler
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailUiAction
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailUiEvent
import de.shinz.rickandmortyshowcase.features.shared.domain.usecase.AddFavoriteUseCase
import de.shinz.rickandmortyshowcase.features.shared.domain.usecase.ObserveFavoriteIdsUseCase
import de.shinz.rickandmortyshowcase.features.shared.domain.usecase.RemoveFavoriteUseCase
import de.shinz.rickandmortyshowcase.testing.MainDispatcherExtension
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * The detail screen's wiring: resolving one character from an id, and the
 * favourite toggle.
 *
 * Built from the **real** use cases over the **real** `LocalFirstCharacterRepository`
 * with fake data sources underneath — the local-first strategy is the thing this
 * screen exists for, so faking the repository would mock away its whole point.
 *
 * The id is a plain `Int` rather than a `SavedStateHandle` — see the ViewModel
 * for why. The route decoding it replaced lives in `AppNavHost`, and Task 19's
 * device check is what proves that half.
 */
class CharacterDetailViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val remote = FakeCharacterRemoteDataSource()
    private val favorites = FakeFavoriteCharacterLocalDataSource()

    private val rick = testCharacter(id = 1, name = "Rick Sanchez")

    private fun viewModel(characterId: Int = 1): CharacterDetailViewModel {
        val repository = LocalFirstCharacterRepository(
            localDataSource = favorites,
            remoteDataSource = remote,
        )

        return CharacterDetailViewModel(
            characterId = characterId,
            getCharacter = GetCharacterUseCase(repository),
            observeFavoriteIds = ObserveFavoriteIdsUseCase(favorites),
            addFavorite = AddFavoriteUseCase(favorites),
            removeFavorite = RemoveFavoriteUseCase(favorites),
            assembler = CharacterDetailUiStateAssembler(AppDateTimeManager()),
        )
    }

    /* ---------- resolving ---------- */

    @Test
    fun `a character that is not favourited is fetched from the api`() = runTest {
        remote.characters[1] = rick

        val uiState = viewModel().uiState.value

        assertThat(uiState.character?.name).isEqualTo("Rick Sanchez")
        assertThat(uiState.isLoading).isFalse()
        assertThat(remote.fetchCharacterCallCount).isEqualTo(1)
    }

    /**
     * SPEC's reason for the whole screen: a favourited character opens with no
     * network call, which is what makes it work offline.
     */
    @Test
    fun `a favourited character is read locally and never reaches the api`() = runTest {
        favorites.seed(rick)

        val uiState = viewModel().uiState.value

        assertThat(uiState.character?.name).isEqualTo("Rick Sanchez")
        assertThat(remote.fetchCharacterCallCount).isEqualTo(0)
    }

    @Test
    fun `a failed resolve becomes a retryable error`() = runTest {
        remote.error = DataError.Network.NO_INTERNET

        val uiState = viewModel().uiState.value

        assertThat(uiState.error).isEqualTo(DataError.Network.NO_INTERNET.toUiText())
        assertThat(uiState.character).isNull()
    }

    @Test
    fun `retrying after a failure resolves the character`() = runTest {
        remote.error = DataError.Network.NO_INTERNET
        val viewModel = viewModel()

        remote.error = null
        remote.characters[1] = rick
        viewModel.onAction(CharacterDetailUiAction.OnRetryClick)

        assertThat(viewModel.uiState.value.character?.name).isEqualTo("Rick Sanchez")
        assertThat(viewModel.uiState.value.error).isNull()
    }

    @Test
    fun `retrying does nothing when nothing has failed`() = runTest {
        remote.characters[1] = rick
        val viewModel = viewModel()

        viewModel.onAction(CharacterDetailUiAction.OnRetryClick)

        assertThat(remote.fetchCharacterCallCount).isEqualTo(1)
    }

    /* ---------- favourites ---------- */

    @Test
    fun `favouriting stores the whole character immediately`() = runTest {
        remote.characters[1] = rick
        val viewModel = viewModel()

        viewModel.onAction(CharacterDetailUiAction.OnFavoriteClick)

        // The whole record, so it renders offline — SPEC's cross-cutting rule.
        assertThat(favorites.stored).containsExactly(rick)
        assertThat(viewModel.uiState.value.removeDialogText).isNull()
        assertThat(viewModel.uiState.value.character?.isFavorite).isNotNull().isTrue()
    }

    @Test
    fun `tapping the heart on a favourite opens the dialog instead of removing`() = runTest {
        favorites.seed(rick)
        val viewModel = viewModel()

        viewModel.onAction(CharacterDetailUiAction.OnFavoriteClick)

        assertThat(viewModel.uiState.value.removeDialogText).isNotNull()
        assertThat(favorites.stored).containsExactly(rick)
    }

    /**
     * The character stays on screen after removal — unlike a row in Favorites,
     * which disappears. This screen was opened by id and keeps what it resolved.
     */
    @Test
    fun `confirming removes the favourite but keeps the character on screen`() = runTest {
        favorites.seed(rick)
        val viewModel = viewModel()
        viewModel.onAction(CharacterDetailUiAction.OnFavoriteClick)

        viewModel.onAction(CharacterDetailUiAction.OnConfirmRemoveFavorite)

        assertThat(favorites.stored).isEqualTo(emptyList<Character>())
        assertThat(viewModel.uiState.value.removeDialogText).isNull()
        assertThat(viewModel.uiState.value.character).isNotNull()
        assertThat(viewModel.uiState.value.character?.isFavorite).isNotNull().isFalse()
    }

    @Test
    fun `dismissing the dialog changes nothing`() = runTest {
        favorites.seed(rick)
        val viewModel = viewModel()
        viewModel.onAction(CharacterDetailUiAction.OnFavoriteClick)

        viewModel.onAction(CharacterDetailUiAction.OnDismissRemoveFavorite)

        assertThat(viewModel.uiState.value.removeDialogText).isNull()
        assertThat(favorites.stored).containsExactly(rick)
    }

    @Test
    fun `a favourite that cannot be written reports it`() = runTest {
        remote.characters[1] = rick
        favorites.writeError = DataError.Local.DISK_FULL
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(CharacterDetailUiAction.OnFavoriteClick)

            assertThat(awaitItem()).isEqualTo(
                CharacterDetailUiEvent.ShowSnackbar(DataError.Local.DISK_FULL.toUiText()),
            )
        }
    }

    @Test
    fun `a favourite that cannot be removed reports it`() = runTest {
        favorites.seed(rick)
        val viewModel = viewModel()
        viewModel.onAction(CharacterDetailUiAction.OnFavoriteClick)
        favorites.writeError = DataError.Local.UNKNOWN

        viewModel.events.test {
            viewModel.onAction(CharacterDetailUiAction.OnConfirmRemoveFavorite)

            assertThat(awaitItem()).isEqualTo(
                CharacterDetailUiEvent.ShowSnackbar(DataError.Local.UNKNOWN.toUiText()),
            )
        }
    }

    /**
     * `SPEC.md` puts a confirmation in front of **every** removal, and the
     * ViewModel is the last place that can enforce it: a confirm arriving with
     * no dialog open — a double tap before recomposition closes it, or a stray
     * action — must not delete.
     */
    @Test
    fun `confirming with no dialog open removes nothing`() = runTest {
        favorites.seed(rick)
        val viewModel = viewModel()

        viewModel.onAction(CharacterDetailUiAction.OnConfirmRemoveFavorite)

        assertThat(favorites.stored).containsExactly(rick)
    }

    @Test
    fun `confirming twice removes once`() = runTest {
        favorites.seed(rick)
        val viewModel = viewModel()
        viewModel.onAction(CharacterDetailUiAction.OnFavoriteClick)

        viewModel.events.test {
            viewModel.onAction(CharacterDetailUiAction.OnConfirmRemoveFavorite)
            viewModel.onAction(CharacterDetailUiAction.OnConfirmRemoveFavorite)

            expectNoEvents()
        }
        assertThat(favorites.stored).isEqualTo(emptyList<Character>())
    }

    /**
     * Tapping the heart before the character has resolved must not leave a
     * pending removal behind — it would have no dialog to show and no way to be
     * dismissed, and would spring one the moment the character arrived.
     */
    @Test
    fun `tapping the heart before the character resolves does nothing`() = runTest {
        favorites.seed(rick)
        remote.error = DataError.Network.NO_INTERNET
        val viewModel = viewModel(characterId = 99)

        viewModel.onAction(CharacterDetailUiAction.OnFavoriteClick)
        viewModel.onAction(CharacterDetailUiAction.OnConfirmRemoveFavorite)

        assertThat(viewModel.uiState.value.removeDialogText).isNull()
        assertThat(favorites.stored).containsExactly(rick)
    }

    /* ---------- the route argument ---------- */

    /**
     * The id really does come from the route, not from a default. A ViewModel
     * that quietly resolved character 1 for every destination would pass every
     * other test in this file.
     */
    @Test
    fun `the id is read from the route`() = runTest {
        val morty = testCharacter(id = 2, name = "Morty Smith")
        remote.characters[2] = morty

        assertThat(viewModel(characterId = 2).uiState.value.character?.name)
            .isEqualTo("Morty Smith")
    }

    @Test
    fun `the favourite state follows the id, not the first favourite`() = runTest {
        favorites.seed(rick)
        remote.characters[2] = testCharacter(id = 2, name = "Morty Smith")

        assertThat(viewModel(characterId = 2).uiState.value.character?.isFavorite)
            .isNotNull().isFalse()
    }

    /**
     * The states that only exist between "asked" and "answered".
     *
     * `@Nested` with a paused `StandardTestDispatcher`, for the same reason as
     * the list's: the fakes never suspend, so on the outer unconfined dispatcher
     * a resolve finishes inside the constructor and nothing mid-flight is
     * observable at all.
     */
    @Nested
    inner class WhileResolving {

        @JvmField
        @RegisterExtension
        val pausedDispatcher = MainDispatcherExtension(StandardTestDispatcher())

        @Test
        fun `the first state is loading, before anything has run`() = runTest {
            remote.characters[1] = rick

            val viewModel = viewModel()

            assertThat(viewModel.uiState.value.isLoading).isTrue()
            assertThat(viewModel.uiState.value.character).isNull()

            advanceUntilIdle()

            assertThat(viewModel.uiState.value.isLoading).isFalse()
            assertThat(viewModel.uiState.value.character?.name).isEqualTo("Rick Sanchez")
        }

        /**
         * Retry moves to `Loading` *before* dispatching, which is what stops a
         * second tap starting a second resolve. Without that line the guard
         * still sees `Failed` and every tap fires another request — invisible on
         * an unconfined dispatcher, where the first one has already finished.
         */
        @Test
        fun `tapping retry twice resolves once`() = runTest {
            remote.error = DataError.Network.NO_INTERNET
            val viewModel = viewModel()
            advanceUntilIdle()
            val afterFailure = remote.fetchCharacterCallCount

            viewModel.onAction(CharacterDetailUiAction.OnRetryClick)
            viewModel.onAction(CharacterDetailUiAction.OnRetryClick)
            advanceUntilIdle()

            assertThat(remote.fetchCharacterCallCount).isEqualTo(afterFailure + 1)
        }
    }
}
