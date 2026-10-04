package de.shinz.rickandmortyshowcase.features.characterlist.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.shinz.rickandmortyshowcase.core.designsystem.components.AppConfirmDialog
import de.shinz.rickandmortyshowcase.core.designsystem.components.AppErrorState
import de.shinz.rickandmortyshowcase.core.designsystem.components.AppLoadingIndicator
import de.shinz.rickandmortyshowcase.core.designsystem.components.AppSnackbarHost
import de.shinz.rickandmortyshowcase.core.designsystem.theme.AppTheme
import de.shinz.rickandmortyshowcase.core.ui.ObserveAsEvents
import de.shinz.rickandmortyshowcase.core.ui.resolve
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListUiAction
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListUiEvent
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListUiState
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.previews.CharacterListScreenPreviewParameterProvider
import de.shinz.rickandmortyshowcase.features.shared.presentation.components.CharacterListItem
import de.shinz.rickandmortyshowcase.features.shared.presentation.model.CharacterUi
import de.shinz.rickandmortyshowcase.generated.resources.Res
import de.shinz.rickandmortyshowcase.generated.resources.action_remove
import de.shinz.rickandmortyshowcase.generated.resources.action_retry
import de.shinz.rickandmortyshowcase.generated.resources.favorite_remove_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.rememberResourceEnvironment
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Holds the ViewModel and turns events into navigation and messages.
 *
 * @param onNavigateToDetail already guarded against being called twice for one
 *   tap — see `AppNavHost`. The Root cannot do it: it has no view of the back
 *   stack.
 */
@Composable
fun CharacterListRoot(
    contentPadding: PaddingValues,
    onNavigateToDetail: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CharacterListViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    /*
     * The message is resolved in the handler, not parked in state for
     * composition to resolve. `UiText.resolve` is a suspend function rather than
     * a composable, so the only things needed here are a scope and the
     * composition's resource environment — and `showSnackbar` already serialises
     * messages behind its own mutex, which a single-slot relay would have
     * defeated by overwriting one message with the next.
     */
    val scope = rememberCoroutineScope()
    val resources = rememberResourceEnvironment()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is CharacterListUiEvent.NavigateToDetail -> onNavigateToDetail(event.characterId)
            is CharacterListUiEvent.ShowSnackbar -> scope.launch {
                snackbarHostState.showSnackbar(event.message.resolve(resources))
            }
        }
    }

    CharacterListScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        contentPadding = contentPadding,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
fun CharacterListScreen(
    uiState: CharacterListUiState,
    onAction: (CharacterListUiAction) -> Unit,
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isInitialLoading -> AppLoadingIndicator(
                modifier = Modifier.padding(contentPadding),
            )

            uiState.initialLoadError != null -> AppErrorState(
                message = uiState.initialLoadError.asString(),
                modifier = Modifier.padding(contentPadding),
                onRetry = { onAction(CharacterListUiAction.OnRetryClick) },
            )

            else -> CharacterList(
                uiState = uiState,
                onAction = onAction,
                contentPadding = contentPadding,
            )
        }

        AppSnackbarHost(
            hostState = snackbarHostState,
            // Above the bottom bar rather than behind it: the shell's insets
            // arrive here as padding, and a snackbar is the one thing that must
            // not scroll under them.
            modifier = Modifier.align(Alignment.BottomCenter).padding(contentPadding),
        )
    }

    uiState.removeDialogText?.let { text ->
        AppConfirmDialog(
            // Static, so they stay here: only the message names a character.
            title = stringResource(Res.string.favorite_remove_title),
            text = text.asString(),
            confirmLabel = stringResource(Res.string.action_remove),
            onConfirm = { onAction(CharacterListUiAction.OnConfirmRemoveFavorite) },
            onDismiss = { onAction(CharacterListUiAction.OnDismissRemoveFavorite) },
        )
    }
}

@Composable
private fun CharacterList(
    uiState: CharacterListUiState,
    onAction: (CharacterListUiAction) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    val listState = rememberLazyListState()

    NotifyWhenNearTheEnd(listState = listState) {
        onAction(CharacterListUiAction.OnEndOfListReached)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        // The window insets arrive as *content* padding, so rows scroll under
        // the status and navigation bars instead of stopping short of them.
        contentPadding = contentPadding.plus(
            horizontal = spacing.screenPadding,
            vertical = spacing.md,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        items(items = uiState.characters, key = CharacterUi::id) { character ->
            CharacterListItem(
                character = character,
                onClick = { onAction(CharacterListUiAction.OnCharacterClick(character.id)) },
                onFavoriteClick = { onAction(CharacterListUiAction.OnFavoriteClick(character.id)) },
            )
        }

        // One slot, one `when`: the loading row and the error row are mutually
        // exclusive — both derive from the same `CharacterListPageLoad` — and
        // writing them as two independent `if`s would let a future change emit
        // both, which `LazyColumn` answers with a duplicate-key crash.
        when {
            uiState.isNextPageLoading ->
                item(key = NEXT_PAGE_SLOT_KEY) { NextPageIndicator() }

            uiState.nextPageError != null ->
                item(key = NEXT_PAGE_SLOT_KEY) {
                    NextPageError(
                        message = uiState.nextPageError.asString(),
                        onRetry = { onAction(CharacterListUiAction.OnRetryClick) },
                    )
                }
        }
    }
}

/**
 * Asks for the next page as the list runs out.
 *
 * Fires on every change of the last visible index rather than once per
 * threshold crossing: the ViewModel owns the "already loading / already failed /
 * already finished" guard, so repeating is free, and a once-only effect would
 * stall paging the moment a loaded page left the trigger condition true.
 */
@Composable
private fun NotifyWhenNearTheEnd(
    listState: LazyListState,
    onNearTheEnd: () -> Unit,
) {
    // Not keyed on the callback: re-keying would restart the collection on every
    // recomposition, so the lambda is kept current instead — the same hazard
    // `ObserveAsEvents` documents.
    val currentOnNearTheEnd by rememberUpdatedState(onNearTheEnd)

    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            // The *rendered* count, not the character count: the footer slot is
            // an item too, and reading `layoutInfo` for both means the pair also
            // changes when a page is appended — so the effect re-arms without
            // being keyed on anything.
            (info.visibleItemsInfo.lastOrNull()?.index ?: -1) to info.totalItemsCount
        }.collect { (lastVisible, total) ->
            if (lastVisible >= 0 && lastVisible >= total - 1 - PREFETCH_DISTANCE) {
                currentOnNearTheEnd()
            }
        }
    }
}

/** Inline, and sized like a row so the list does not jump as it appears. */
@Composable
private fun NextPageIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(AppTheme.spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(AppTheme.size.icon),
            color = AppTheme.colors.accent,
            strokeWidth = AppTheme.border.hairline,
        )
    }
}

/**
 * A failed page, without taking the screen.
 *
 * `SPEC.md` forbids discarding what is already shown, so this sits at the end of
 * the list rather than replacing it.
 */
@Composable
private fun NextPageError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            style = AppTheme.typography.caption,
            color = AppTheme.colors.onSurfaceMuted,
        )
        TextButton(onClick = onRetry) {
            Text(text = stringResource(Res.string.action_retry), color = AppTheme.colors.accent)
        }
    }
}

/**
 * Adds a uniform inset to window insets that already arrived as padding.
 *
 * `PaddingValues` has no `plus`, and the two cannot simply be swapped: the
 * horizontal gutter is the screen's, the vertical and the cutout insets are the
 * window's, and the list needs both at once.
 */
@Composable
private fun PaddingValues.plus(
    horizontal: androidx.compose.ui.unit.Dp,
    vertical: androidx.compose.ui.unit.Dp,
): PaddingValues {
    val direction = LocalLayoutDirection.current

    return PaddingValues(
        start = calculateStartPadding(direction) + horizontal,
        end = calculateEndPadding(direction) + horizontal,
        top = calculateTopPadding() + vertical,
        bottom = calculateBottomPadding() + vertical,
    )
}

/**
 * The loading indicator and the error occupy the same slot and are mutually
 * exclusive, so they share a key — otherwise swapping one for the other would
 * animate as a removal plus an insertion.
 */
private const val NEXT_PAGE_SLOT_KEY = "next-page-slot"

/** How many rows from the end to ask for more. Roughly one screen of rows. */
private const val PREFETCH_DISTANCE = 5

/* ============================================================
   Previews
   ============================================================ */

@PreviewLightDark
@Composable
private fun CharacterListScreenPreview(
    @PreviewParameter(CharacterListScreenPreviewParameterProvider::class)
    uiState: CharacterListUiState,
) {
    AppTheme {
        CharacterListScreen(
            uiState = uiState,
            onAction = {},
            contentPadding = PaddingValues(),
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
