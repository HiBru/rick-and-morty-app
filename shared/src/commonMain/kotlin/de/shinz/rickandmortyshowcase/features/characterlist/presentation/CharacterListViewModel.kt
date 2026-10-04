package de.shinz.rickandmortyshowcase.features.characterlist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.shinz.rickandmortyshowcase.core.domain.Result
import de.shinz.rickandmortyshowcase.core.domain.onFailure
import de.shinz.rickandmortyshowcase.core.ui.toUiText
import de.shinz.rickandmortyshowcase.features.characterlist.domain.model.CharacterListData
import de.shinz.rickandmortyshowcase.features.characterlist.domain.usecase.GetCharacterPageUseCase
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.assembler.CharacterListUiStateAssembler
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListPageLoad
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListUiAction
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListUiEvent
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListUiState
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListViewModelState
import de.shinz.rickandmortyshowcase.features.shared.domain.usecase.AddFavoriteUseCase
import de.shinz.rickandmortyshowcase.features.shared.domain.usecase.ObserveFavoriteIdsUseCase
import de.shinz.rickandmortyshowcase.features.shared.domain.usecase.RemoveFavoriteUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The character list: paging, favourites, and the confirmation in front of every
 * removal.
 *
 * Use cases and the assembler only — no repository, no data source, and nothing
 * that formats a string.
 */
class CharacterListViewModel(
    private val getCharacterPage: GetCharacterPageUseCase,
    private val observeFavoriteIds: ObserveFavoriteIdsUseCase,
    private val addFavorite: AddFavoriteUseCase,
    private val removeFavorite: RemoveFavoriteUseCase,
    private val assembler: CharacterListUiStateAssembler,
) : ViewModel() {

    private val vmState = MutableStateFlow(CharacterListViewModelState())

    /**
     * The favourite ids, as state rather than as a bare flow.
     *
     * Two jobs. It feeds the assembler, and it is what [onFavoriteClick] reads to
     * decide whether a tap means "add" or "ask before removing" — a question
     * about *domain* data, which is why it is answered from here rather than by
     * inspecting the `uiState` this ViewModel just produced.
     *
     * Giving it an initial value also means `combine` below does not wait on a
     * database round trip before its first emission.
     *
     * It is authoritative about the *last observed* favourites, not the current
     * ones: there is a window after a write before the Room flow re-emits. Both
     * consequences are benign and neither is worth an in-flight flag — a second
     * tap in the add window re-upserts (which only re-stamps `addedAt`, the
     * order Favorites uses), and one in the remove window re-opens a dialog whose
     * confirmation is already a no-op.
     */
    private val favoriteIds: StateFlow<Set<Int>> = observeFavoriteIds()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val uiState: StateFlow<CharacterListUiState> =
        combine(favoriteIds.map(::CharacterListData), vmState, assembler::assemble)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = assembler.assemble(
                    CharacterListData.EMPTY,
                    CharacterListViewModelState(),
                ),
            )

    /**
     * `BUFFERED` with `send`, never `trySend`.
     *
     * `ObserveAsEvents` stops collecting below `STARTED`, and under
     * navigation-compose the lifecycle owner is the `NavBackStackEntry` on both
     * platforms — so events raised while the detail screen covers this one queue
     * and fire on return. `trySend` would discard them instead.
     *
     * **That retention is right for [CharacterListUiEvent.ShowSnackbar] and
     * wrong for [CharacterListUiEvent.NavigateToDetail]**, which share the
     * channel: a double-tapped row sends two navigation events, and the one not
     * consumed before the screen drops below `STARTED` fires on return and
     * re-opens detail. The fix belongs at the collection site, which is Task 17's
     * — the ViewModel cannot tell whether a navigation already happened.
     */
    private val _events = Channel<CharacterListUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        // No guard: `CharacterListViewModelState()` already says Loading, so this
        // is the request that initial state is describing.
        fetchPage()
    }

    fun onAction(action: CharacterListUiAction) {
        when (action) {
            is CharacterListUiAction.OnCharacterClick -> viewModelScope.launch {
                _events.send(CharacterListUiEvent.NavigateToDetail(action.characterId))
            }

            is CharacterListUiAction.OnFavoriteClick -> onFavoriteClick(action.characterId)

            CharacterListUiAction.OnConfirmRemoveFavorite -> onConfirmRemoveFavorite()

            CharacterListUiAction.OnDismissRemoveFavorite ->
                vmState.update { it.copy(pendingRemovalId = null) }

            CharacterListUiAction.OnEndOfListReached -> onEndOfListReached()

            CharacterListUiAction.OnRetryClick -> onRetryClick()
        }
    }

    /**
     * Starts the next page, if starting one is allowed at all.
     *
     * Fired on every scroll frame near the end of the list, so all three
     * conditions matter. `Loading` stops a second request racing the first;
     * **`Failed` stops a page that just failed being re-requested on the very
     * next frame**, which offline is an unbounded retry loop and which would make
     * the retry affordance meaningless; `endReached` stops asking past the end.
     */
    private fun onEndOfListReached() {
        val state = vmState.value
        if (state.pageLoad !is CharacterListPageLoad.Idle || state.endReached) return

        startPage()
    }

    /** Only from a failure — retrying anything else would duplicate a page. */
    private fun onRetryClick() {
        if (vmState.value.pageLoad !is CharacterListPageLoad.Failed) return

        startPage()
    }

    /**
     * Moving to `Loading` **before** launching is what makes the guard above
     * work: `MutableStateFlow.update` is synchronous, so a second scroll callback
     * in the same frame already sees `Loading` and returns.
     */
    private fun startPage() {
        vmState.update { it.copy(pageLoad = CharacterListPageLoad.Loading) }
        fetchPage()
    }

    /**
     * The page number is read **here**, before dispatching, and passed in.
     *
     * Reading it inside the coroutine would be correct only as long as
     * `viewModelScope` is `Dispatchers.Main.immediate` — true on both platforms
     * today, and not true under the paused dispatcher the loading tests use. The
     * one read that would otherwise span a dispatch is the one that decides which
     * page gets appended, so it is the one worth not leaving to the scheduler.
     */
    private fun fetchPage(page: Int = vmState.value.nextPage) {
        viewModelScope.launch {
            when (val result = getCharacterPage(page)) {
                is Result.Success -> vmState.update {
                    it.copy(
                        characters = it.characters + result.data.characters,
                        // Advanced only here. A failed page is never consumed, so
                        // a retry asks for the same page rather than skipping one.
                        nextPage = it.nextPage + 1,
                        endReached = !result.data.hasMore,
                        pageLoad = CharacterListPageLoad.Idle,
                    )
                }

                is Result.Error -> vmState.update {
                    // The characters already loaded are untouched, which is
                    // SPEC's "a failed page load never discards pages already
                    // shown" — the assembler then renders the error inline
                    // rather than full-screen because the buffer is non-empty.
                    it.copy(pageLoad = CharacterListPageLoad.Failed(result.error))
                }
            }
        }
    }

    /**
     * One tap, two meanings — which is why the row reports a tap and this decides.
     *
     * `SPEC.md`: adding is immediate, removing always asks first.
     */
    private fun onFavoriteClick(characterId: Int) {
        if (characterId in favoriteIds.value) {
            vmState.update { it.copy(pendingRemovalId = characterId) }
            return
        }

        // The whole character, because favouriting persists every API value so
        // it renders offline. Absent from the buffer it cannot be favourited —
        // nothing the user can see is missing from it.
        val character = vmState.value.characters.firstOrNull { it.id == characterId } ?: return

        viewModelScope.launch {
            addFavorite(character).onFailure { error ->
                _events.send(CharacterListUiEvent.ShowSnackbar(error.toUiText()))
            }
        }
    }

    /**
     * The dialog closes immediately rather than waiting for the delete.
     *
     * Its job was to confirm, and it has. A failure arrives as a snackbar,
     * because there is nothing on this screen for a storage error to occupy —
     * the heart is driven by the favourites flow and simply stays where it was.
     */
    private fun onConfirmRemoveFavorite() {
        val characterId = vmState.value.pendingRemovalId ?: return
        vmState.update { it.copy(pendingRemovalId = null) }

        viewModelScope.launch {
            removeFavorite(characterId).onFailure { error ->
                _events.send(CharacterListUiEvent.ShowSnackbar(error.toUiText()))
            }
        }
    }
}
