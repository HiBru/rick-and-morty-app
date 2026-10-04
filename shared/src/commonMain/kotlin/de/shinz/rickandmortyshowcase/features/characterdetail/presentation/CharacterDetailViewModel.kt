package de.shinz.rickandmortyshowcase.features.characterdetail.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.shinz.rickandmortyshowcase.core.domain.Result
import de.shinz.rickandmortyshowcase.core.domain.onFailure
import de.shinz.rickandmortyshowcase.core.ui.toUiText
import de.shinz.rickandmortyshowcase.features.characterdetail.domain.model.CharacterDetailData
import de.shinz.rickandmortyshowcase.features.characterdetail.domain.usecase.GetCharacterUseCase
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.assembler.CharacterDetailUiStateAssembler
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailLoad
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailUiAction
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailUiEvent
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailUiState
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailViewModelState
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
 * The detail screen: resolve one character from an id, and offer the same
 * favourite toggle the lists do.
 *
 * It is handed **nothing but the id** — `SPEC.md` is explicit — and
 * `GetCharacterUseCase` decides where the character comes from. That is what
 * lets the same screen open from Home over the network and from Favorites with
 * the network off, without either caller knowing the difference.
 *
 * ### Why a plain `Int` and not a `SavedStateHandle`
 *
 * `CLAUDE.md` sketches `savedStateHandle.toRoute<CharacterDetailRoute>()`, and
 * the decoding is still type-safe — it just happens one layer up, in
 * `AppNavHost`, where a `NavBackStackEntry` is in hand and Koin passes the id
 * through `parametersOf`. *`toRoute` on a `SavedStateHandle` goes through
 * `bundleOf`, and `android.os.Bundle` is not mocked in a JVM host test: every
 * test in `CharacterDetailViewModelTest` failed with "Method putInt in
 * android.os.BaseBundle not mocked". The alternatives were Robolectric, or
 * `isReturnDefaultValues` — which would silently hand this screen character 0.*
 * Nothing is lost: the id comes from the route, which navigation restores across
 * process death by itself. One thing is gained — a feature no longer imports the
 * navigation layer.
 */
class CharacterDetailViewModel(
    private val characterId: Int,
    private val getCharacter: GetCharacterUseCase,
    private val observeFavoriteIds: ObserveFavoriteIdsUseCase,
    private val addFavorite: AddFavoriteUseCase,
    private val removeFavorite: RemoveFavoriteUseCase,
    private val assembler: CharacterDetailUiStateAssembler,
) : ViewModel() {

    private val vmState = MutableStateFlow(CharacterDetailViewModelState())

    /**
     * Whether *this* character is favourited, resolved from the id set before it
     * reaches the assembler — and read by [onFavoriteClick] to decide whether a
     * tap means "add" or "ask first".
     *
     * Last-observed rather than current, as on the list — but with a narrower
     * window, because mapping to a `Boolean` before `stateIn` conflates every
     * favourite change that is not this character. Both consequences stay
     * benign: a second tap in the add window re-upserts, and one in the remove
     * window re-opens a dialog whose confirmation is a no-op.
     */
    private val isFavorite: StateFlow<Boolean> = observeFavoriteIds()
        .map { characterId in it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val uiState: StateFlow<CharacterDetailUiState> =
        combine(isFavorite.map(::CharacterDetailData), vmState, assembler::assemble)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = assembler.assemble(
                    CharacterDetailData.EMPTY,
                    CharacterDetailViewModelState(),
                ),
            )

    /** `BUFFERED` + `send`, never `trySend` — see `CharacterListViewModel`. */
    private val _events = Channel<CharacterDetailUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        // No guard: the default state already says Loading, so this is the
        // resolve that state is describing.
        resolveCharacter()
    }

    fun onAction(action: CharacterDetailUiAction) {
        when (action) {
            CharacterDetailUiAction.OnFavoriteClick -> onFavoriteClick()

            CharacterDetailUiAction.OnConfirmRemoveFavorite -> onConfirmRemoveFavorite()

            CharacterDetailUiAction.OnDismissRemoveFavorite ->
                vmState.update { it.copy(isRemovalPending = false) }

            CharacterDetailUiAction.OnRetryClick -> onRetryClick()
        }
    }

    /** Only from a failure: retrying a resolve that worked would just re-fetch. */
    private fun onRetryClick() {
        if (vmState.value.load !is CharacterDetailLoad.Failed) return

        vmState.update { it.copy(load = CharacterDetailLoad.Loading) }
        resolveCharacter()
    }

    private fun resolveCharacter() {
        viewModelScope.launch {
            when (val result = getCharacter(characterId)) {
                is Result.Success -> vmState.update {
                    it.copy(character = result.data, load = CharacterDetailLoad.Idle)
                }

                is Result.Error -> vmState.update {
                    it.copy(load = CharacterDetailLoad.Failed(result.error))
                }
            }
        }
    }

    /** Adding is immediate; removing asks first. The same split as on a row. */
    private fun onFavoriteClick() {
        // Hoisted above the branch, so both halves share it: without a resolved
        // character there is nothing to persist *and* nothing to name in a
        // confirmation — and opening the dialog anyway would leave a pending
        // removal the user has no way to dismiss, which then springs a dialog
        // the moment the character arrives.
        val character = vmState.value.character ?: return

        if (isFavorite.value) {
            vmState.update { it.copy(isRemovalPending = true) }
            return
        }

        viewModelScope.launch {
            addFavorite(character).onFailure { error ->
                _events.send(CharacterDetailUiEvent.ShowSnackbar(error.toUiText()))
            }
        }
    }

    /**
     * The dialog closes immediately; its job was to confirm, and it has.
     *
     * **The character stays on screen after removal.** Unlike Favorites, where
     * the row disappears, this screen was opened by id and keeps rendering what
     * it resolved — only the heart changes. That also means the local-first read
     * does not have to be redone.
     */
    private fun onConfirmRemoveFavorite() {
        // Nothing to confirm means nothing to remove. `SPEC.md` puts a
        // confirmation in front of *every* removal, and this is the last place
        // that can enforce it — a stray action, or a confirm button tapped twice
        // before recomposition closes the dialog, must not delete silently.
        if (!vmState.value.isRemovalPending) return

        vmState.update { it.copy(isRemovalPending = false) }

        viewModelScope.launch {
            removeFavorite(characterId).onFailure { error ->
                _events.send(CharacterDetailUiEvent.ShowSnackbar(error.toUiText()))
            }
        }
    }
}
