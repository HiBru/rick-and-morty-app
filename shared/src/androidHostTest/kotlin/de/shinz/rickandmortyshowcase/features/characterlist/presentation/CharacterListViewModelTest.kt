package de.shinz.rickandmortyshowcase.features.characterlist.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.datasource.FakeCharacterRemoteDataSource
import de.shinz.rickandmortyshowcase.core.domain.datasource.FakeFavoriteCharacterLocalDataSource
import de.shinz.rickandmortyshowcase.core.domain.model.Character
import de.shinz.rickandmortyshowcase.core.domain.model.CharacterPage
import de.shinz.rickandmortyshowcase.core.domain.model.testCharacter
import de.shinz.rickandmortyshowcase.core.ui.toUiText
import de.shinz.rickandmortyshowcase.features.characterlist.domain.usecase.GetCharacterPageUseCase
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.assembler.CharacterListUiStateAssembler
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListUiAction
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListUiEvent
import de.shinz.rickandmortyshowcase.features.shared.domain.usecase.AddFavoriteUseCase
import de.shinz.rickandmortyshowcase.features.shared.domain.usecase.ObserveFavoriteIdsUseCase
import de.shinz.rickandmortyshowcase.features.shared.domain.usecase.RemoveFavoriteUseCase
import de.shinz.rickandmortyshowcase.features.shared.presentation.model.CharacterUi
import de.shinz.rickandmortyshowcase.testing.MainDispatcherExtension
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * The list's wiring: paging, the guards around it, and the two-meaning favourite
 * tap.
 *
 * Built from the **real** use cases over fake data sources, per the testing
 * skill — a fake per use case would mock away the very wiring this exists to
 * check. The formatting is not re-asserted here; `CharacterListUiStateAssemblerTest`
 * owns that.
 */
class CharacterListViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val remote = FakeCharacterRemoteDataSource()
    private val favorites = FakeFavoriteCharacterLocalDataSource()

    private fun viewModel() = CharacterListViewModel(
        getCharacterPage = GetCharacterPageUseCase(remote),
        observeFavoriteIds = ObserveFavoriteIdsUseCase(favorites),
        addFavorite = AddFavoriteUseCase(favorites),
        removeFavorite = RemoveFavoriteUseCase(favorites),
        assembler = CharacterListUiStateAssembler(),
    )

    private fun page(vararg ids: Int, hasMore: Boolean = true) =
        CharacterPage(characters = ids.map { testCharacter(it) }, hasMore = hasMore)

    /* ---------- the first page ---------- */

    @Test
    fun `the first page is requested without being asked for`() = runTest {
        remote.pages[1] = page(1, 2)

        val uiState = viewModel().uiState.value

        assertThat(uiState.characters.map(CharacterUi::id)).containsExactly(1, 2)
        assertThat(uiState.isInitialLoading).isFalse()
        assertThat(remote.fetchCharacterPageCallCount).isEqualTo(1)
    }

    @Test
    fun `a failed first page becomes the full-screen error and keeps the list empty`() = runTest {
        remote.pageErrors[1] = DataError.Network.NO_INTERNET

        val uiState = viewModel().uiState.value

        assertThat(uiState.characters).isEqualTo(emptyList<CharacterUi>())
        assertThat(uiState.initialLoadError).isEqualTo(DataError.Network.NO_INTERNET.toUiText())
        assertThat(uiState.nextPageError).isNull()
    }

    /* ---------- paging ---------- */

    @Test
    fun `reaching the end of the list appends the next page`() = runTest {
        remote.pages[1] = page(1, 2)
        remote.pages[2] = page(3, 4, hasMore = false)
        val viewModel = viewModel()

        viewModel.onAction(CharacterListUiAction.OnEndOfListReached)

        assertThat(viewModel.uiState.value.characters.map(CharacterUi::id))
            .containsExactly(1, 2, 3, 4)
    }

    @Test
    fun `paging stops once the api reports no next page`() = runTest {
        remote.pages[1] = page(1, hasMore = false)
        val viewModel = viewModel()

        repeat(3) { viewModel.onAction(CharacterListUiAction.OnEndOfListReached) }

        // One request in total — the constructor's. Page 2 is never asked for,
        // which is also why the fake's NOT_FOUND default is never reached.
        assertThat(remote.fetchCharacterPageCallCount).isEqualTo(1)
    }

    /**
     * The distinct inline indicator, and the rows surviving underneath it.
     *
     * Through Turbine rather than `uiState.value`, because the loading frame is
     * *between* two emissions: on an unconfined dispatcher the state moves to
     * Loading and back to Idle before `onAction` returns, so only a collector
     * that was already subscribed sees it. (A paused dispatcher does not help —
     * then `stateIn`'s own collector has not run either, and `uiState.value`
     * lags behind.)
     */
    @Test
    fun `a later page shows the inline indicator without discarding the rows`() = runTest {
        remote.pages[1] = page(1, 2)
        remote.pages[2] = page(3, hasMore = false)
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertThat(awaitItem().characters.map(CharacterUi::id)).containsExactly(1, 2)

            viewModel.onAction(CharacterListUiAction.OnEndOfListReached)

            val loading = awaitItem()
            assertThat(loading.isNextPageLoading).isTrue()
            assertThat(loading.isInitialLoading).isFalse()
            assertThat(loading.characters.map(CharacterUi::id)).containsExactly(1, 2)

            assertThat(awaitItem().characters.map(CharacterUi::id)).containsExactly(1, 2, 3)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * The two halves of "paging stops at the last page", joined.
     *
     * `hasMore == false` and the API's `404`-past-the-end were each pinned —
     * one in `GetCharacterPageUseCaseTest`, one by call count here — but nothing
     * drove a `404` through the ViewModel to show that the *composition* of use
     * case and paging buffer actually halts. On device that case is ~800 scrolls
     * away, so it is reachable nowhere else.
     */
    @Test
    fun `a 404 past the last page ends paging instead of erroring`() = runTest {
        remote.pages[1] = page(1, 2, hasMore = true)
        remote.pageErrors[2] = DataError.Network.NOT_FOUND
        val viewModel = viewModel()

        viewModel.onAction(CharacterListUiAction.OnEndOfListReached)
        val afterEnd = remote.fetchCharacterPageCallCount
        repeat(3) { viewModel.onAction(CharacterListUiAction.OnEndOfListReached) }

        val uiState = viewModel.uiState.value
        // Not an error: the user scrolled off the end, which is not a failure.
        assertThat(uiState.nextPageError).isNull()
        assertThat(uiState.initialLoadError).isNull()
        assertThat(uiState.characters.map(CharacterUi::id)).containsExactly(1, 2)
        // And nothing is asked for again.
        assertThat(remote.fetchCharacterPageCallCount).isEqualTo(afterEnd)
    }

    /**
     * The guard that is easiest to get wrong, and whose absence is invisible
     * until someone is offline: after a page fails the user is still parked at
     * the bottom of the list, so every further scroll frame would re-fire the
     * request.
     */
    @Test
    fun `a failed page is not re-requested by further scrolling`() = runTest {
        remote.pages[1] = page(1, 2)
        remote.pageErrors[2] = DataError.Network.NO_INTERNET
        val viewModel = viewModel()

        viewModel.onAction(CharacterListUiAction.OnEndOfListReached)
        val afterFailure = remote.fetchCharacterPageCallCount

        repeat(5) { viewModel.onAction(CharacterListUiAction.OnEndOfListReached) }

        assertThat(remote.fetchCharacterPageCallCount).isEqualTo(afterFailure)
    }

    /**
     * SPEC: a failed page load never discards pages already shown — and the
     * error goes inline rather than over the top of them.
     */
    @Test
    fun `a failed later page keeps the characters already loaded`() = runTest {
        remote.pages[1] = page(1, 2)
        remote.pageErrors[2] = DataError.Network.REQUEST_TIMEOUT
        val viewModel = viewModel()

        viewModel.onAction(CharacterListUiAction.OnEndOfListReached)

        val uiState = viewModel.uiState.value
        assertThat(uiState.characters.map(CharacterUi::id)).containsExactly(1, 2)
        assertThat(uiState.nextPageError).isEqualTo(DataError.Network.REQUEST_TIMEOUT.toUiText())
        assertThat(uiState.initialLoadError).isNull()
    }

    /**
     * The page counter advances only on success, so a retry asks for the page
     * that failed. Getting this wrong skips a page silently — the list would
     * simply be missing twenty characters with nothing to show for it.
     */
    @Test
    fun `retrying asks for the page that failed, not the one after it`() = runTest {
        remote.pages[1] = page(1, 2)
        remote.pageErrors[2] = DataError.Network.NO_INTERNET
        val viewModel = viewModel()
        viewModel.onAction(CharacterListUiAction.OnEndOfListReached)

        // The page recovers, as it would when the connection comes back.
        remote.pageErrors.remove(2)
        remote.pages[2] = page(3, 4, hasMore = false)
        viewModel.onAction(CharacterListUiAction.OnRetryClick)

        val uiState = viewModel.uiState.value
        assertThat(uiState.characters.map(CharacterUi::id)).containsExactly(1, 2, 3, 4)
        assertThat(uiState.nextPageError).isNull()
    }

    /**
     * The retry `SPEC.md` is actually about — the one on the full-screen error.
     * It must ask for page 1 again, which is only true because a failed page is
     * never consumed and so never advances the counter.
     */
    @Test
    fun `retrying a failed first page asks for page one again`() = runTest {
        remote.pageErrors[1] = DataError.Network.NO_INTERNET
        val viewModel = viewModel()

        remote.pageErrors.remove(1)
        remote.pages[1] = page(1, 2, hasMore = false)
        viewModel.onAction(CharacterListUiAction.OnRetryClick)

        val uiState = viewModel.uiState.value
        assertThat(uiState.characters.map(CharacterUi::id)).containsExactly(1, 2)
        assertThat(uiState.initialLoadError).isNull()
    }

    @Test
    fun `retrying does nothing when nothing has failed`() = runTest {
        remote.pages[1] = page(1, hasMore = false)
        val viewModel = viewModel()

        viewModel.onAction(CharacterListUiAction.OnRetryClick)

        assertThat(remote.fetchCharacterPageCallCount).isEqualTo(1)
    }

    /* ---------- favourites ---------- */

    @Test
    fun `favouriting a character stores it immediately and without asking`() = runTest {
        remote.pages[1] = page(1, 2)
        val viewModel = viewModel()

        viewModel.onAction(CharacterListUiAction.OnFavoriteClick(characterId = 2))

        assertThat(favorites.stored.map(Character::id)).containsExactly(2)
        assertThat(viewModel.uiState.value.removeDialogText).isNull()
        assertThat(viewModel.uiState.value.characters.single { it.id == 2 }.isFavorite).isTrue()
    }

    /** SPEC: every removal goes through a confirmation, everywhere. */
    @Test
    fun `tapping the heart on a favourite opens the dialog instead of removing`() = runTest {
        favorites.seed(testCharacter(2))
        remote.pages[1] = page(1, 2)
        val viewModel = viewModel()

        viewModel.onAction(CharacterListUiAction.OnFavoriteClick(characterId = 2))

        assertThat(viewModel.uiState.value.removeDialogText).isNotNull()
        assertThat(favorites.stored.map(Character::id)).containsExactly(2)
    }

    @Test
    fun `confirming removes the favourite and closes the dialog`() = runTest {
        favorites.seed(testCharacter(2))
        remote.pages[1] = page(1, 2)
        val viewModel = viewModel()
        viewModel.onAction(CharacterListUiAction.OnFavoriteClick(characterId = 2))

        viewModel.onAction(CharacterListUiAction.OnConfirmRemoveFavorite)

        assertThat(favorites.stored).isEqualTo(emptyList<Character>())
        assertThat(viewModel.uiState.value.removeDialogText).isNull()
        assertThat(viewModel.uiState.value.characters.single { it.id == 2 }.isFavorite).isFalse()
    }

    @Test
    fun `dismissing the dialog changes nothing`() = runTest {
        favorites.seed(testCharacter(2))
        remote.pages[1] = page(1, 2)
        val viewModel = viewModel()
        viewModel.onAction(CharacterListUiAction.OnFavoriteClick(characterId = 2))

        viewModel.onAction(CharacterListUiAction.OnDismissRemoveFavorite)

        assertThat(viewModel.uiState.value.removeDialogText).isNull()
        assertThat(favorites.stored.map(Character::id)).containsExactly(2)
    }

    /**
     * A failed write has nowhere to live in the rendered state — the heart is
     * driven by the favourites flow and simply stays put — so the user would
     * otherwise watch a tap do nothing.
     */
    @Test
    fun `a favourite that cannot be written reports it`() = runTest {
        remote.pages[1] = page(1)
        favorites.writeError = DataError.Local.DISK_FULL
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(CharacterListUiAction.OnFavoriteClick(characterId = 1))

            assertThat(awaitItem()).isEqualTo(
                CharacterListUiEvent.ShowSnackbar(DataError.Local.DISK_FULL.toUiText()),
            )
        }
    }

    @Test
    fun `a favourite that cannot be removed reports it`() = runTest {
        favorites.seed(testCharacter(1))
        remote.pages[1] = page(1)
        val viewModel = viewModel()
        viewModel.onAction(CharacterListUiAction.OnFavoriteClick(characterId = 1))
        favorites.writeError = DataError.Local.UNKNOWN

        viewModel.events.test {
            viewModel.onAction(CharacterListUiAction.OnConfirmRemoveFavorite)

            assertThat(awaitItem()).isEqualTo(
                CharacterListUiEvent.ShowSnackbar(DataError.Local.UNKNOWN.toUiText()),
            )
        }
    }

    /* ---------- navigation ---------- */

    @Test
    fun `tapping a row navigates with the id alone`() = runTest {
        remote.pages[1] = page(1, 2)
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(CharacterListUiAction.OnCharacterClick(characterId = 2))

            assertThat(awaitItem())
                .isEqualTo(CharacterListUiEvent.NavigateToDetail(characterId = 2))
        }
    }

    /**
     * The frames that only exist between "asked" and "answered".
     *
     * `@Nested` because the **dispatcher is the fixture**: `@RegisterExtension`
     * is a per-class field, and the fakes never suspend, so on the outer class's
     * `UnconfinedTestDispatcher` a request completes inside `onAction` and no
     * state is ever observed mid-flight. A paused `StandardTestDispatcher` is the
     * only way to stop there. The inner registration wins because JUnit runs the
     * outer `beforeEach` first — if that ever changed, these tests fail rather
     * than quietly testing the wrong thing.
     */
    @Nested
    inner class WhileAPageIsInFlight {

        @JvmField
        @RegisterExtension
        val pausedDispatcher = MainDispatcherExtension(StandardTestDispatcher())

        @Test
        fun `the very first state is the full-screen indicator`() = runTest {
            remote.pages[1] = page(1, hasMore = false)

            val viewModel = viewModel()

            // Nothing has been dispatched yet: this is the frame the user sees
            // while the first request is in flight.
            val first = viewModel.uiState.value
            assertThat(first.isInitialLoading).isTrue()
            assertThat(first.isNextPageLoading).isFalse()

            advanceUntilIdle()

            val loaded = viewModel.uiState.value
            assertThat(loaded.isInitialLoading).isFalse()
            assertThat(loaded.characters.map(CharacterUi::id)).containsExactly(1)
        }

        /**
         * The half of the end-of-list guard that the outer class cannot reach.
         *
         * With a page genuinely in flight, a scroll callback must be ignored.
         * Nothing else pins this: the fakes answer instantly, so on an unconfined
         * dispatcher `Loading` is never observable and dropping the check from
         * the guard would leave every other test green.
         */
        @Test
        fun `scrolling while a page is in flight does not start a second one`() = runTest {
            remote.pages[1] = page(1, hasMore = true)
            remote.pages[2] = page(2, hasMore = false)
            val viewModel = viewModel()

            viewModel.onAction(CharacterListUiAction.OnEndOfListReached)
            advanceUntilIdle()

            // Only the constructor's request. Had the guard missed `Loading`,
            // page 1 would have been requested twice and appended twice.
            assertThat(remote.fetchCharacterPageCallCount).isEqualTo(1)
            assertThat(viewModel.uiState.value.characters.map(CharacterUi::id)).containsExactly(1)
        }
    }
}
