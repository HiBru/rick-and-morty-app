package de.shinz.rickandmortyshowcase.features.favorites.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.shinz.rickandmortyshowcase.core.domain.onFailure
import de.shinz.rickandmortyshowcase.core.ui.toUiText
import de.shinz.rickandmortyshowcase.features.favorites.domain.model.FavoritesData
import de.shinz.rickandmortyshowcase.features.favorites.domain.usecase.ObserveFavoritesUseCase
import de.shinz.rickandmortyshowcase.features.favorites.presentation.assembler.FavoritesUiStateAssembler
import de.shinz.rickandmortyshowcase.features.favorites.presentation.model.FavoritesUiAction
import de.shinz.rickandmortyshowcase.features.favorites.presentation.model.FavoritesUiEvent
import de.shinz.rickandmortyshowcase.features.favorites.presentation.model.FavoritesUiState
import de.shinz.rickandmortyshowcase.features.favorites.presentation.model.FavoritesViewModelState
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
 * The favourites list.
 *
 * The simplest of the three screens, and deliberately so: there is **no
 * `init`**, nothing to fetch and nothing to retry. The database is observed, and
 * `SPEC.md` is explicit that this screen never calls the API — which is what
 * makes it work entirely offline.
 *
 * There is also no `AddFavoriteUseCase` here. Nothing on this screen can add
 * one: every row is already a favourite, so the heart has exactly one meaning.
 */
class FavoritesViewModel(
    private val observeFavorites: ObserveFavoritesUseCase,
    private val removeFavorite: RemoveFavoriteUseCase,
    private val assembler: FavoritesUiStateAssembler,
) : ViewModel() {

    private val vmState = MutableStateFlow(FavoritesViewModelState())

    /**
     * Combined straight from the use case, with no intermediate `stateIn`.
     *
     * The character list keeps its favourite ids as state because its
     * `onFavoriteClick` reads `.value` to decide add-versus-remove; nothing here
     * does — a tap on this screen can only mean remove, and the id is all the
     * removal needs. A second `stateIn` would be a shared subscription nothing
     * shares.
     */
    val uiState: StateFlow<FavoritesUiState> =
        combine(observeFavorites().map(::FavoritesData), vmState, assembler::assemble)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = assembler.assemble(
                    FavoritesData.EMPTY,
                    FavoritesViewModelState(),
                ),
            )

    /** `BUFFERED` + `send`, never `trySend` — see `CharacterListViewModel`. */
    private val _events = Channel<FavoritesUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onAction(action: FavoritesUiAction) {
        when (action) {
            is FavoritesUiAction.OnCharacterClick -> viewModelScope.launch {
                _events.send(FavoritesUiEvent.NavigateToDetail(action.characterId))
            }

            // Always the dialog: a tap here can only mean remove.
            is FavoritesUiAction.OnFavoriteClick ->
                vmState.update { it.copy(pendingRemovalId = action.characterId) }

            FavoritesUiAction.OnConfirmRemoveFavorite -> onConfirmRemoveFavorite()

            FavoritesUiAction.OnDismissRemoveFavorite ->
                vmState.update { it.copy(pendingRemovalId = null) }
        }
    }

    /**
     * The dialog closes first; its job was to confirm, and it has.
     *
     * Guarded on a pending removal for the same reason the detail screen is:
     * `SPEC.md` makes confirmation unconditional, and a confirm arriving with no
     * dialog open — a double tap before recomposition, or a stray action — must
     * not delete.
     */
    private fun onConfirmRemoveFavorite() {
        val characterId = vmState.value.pendingRemovalId ?: return
        vmState.update { it.copy(pendingRemovalId = null) }

        viewModelScope.launch {
            removeFavorite(characterId).onFailure { error ->
                _events.send(FavoritesUiEvent.ShowSnackbar(error.toUiText()))
            }
        }
    }
}
