package de.shinz.rickandmortyshowcase.features.favorites.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.shinz.rickandmortyshowcase.core.designsystem.components.AppConfirmDialog
import de.shinz.rickandmortyshowcase.core.designsystem.components.AppEmptyState
import de.shinz.rickandmortyshowcase.core.designsystem.components.AppLoadingIndicator
import de.shinz.rickandmortyshowcase.core.designsystem.components.AppSnackbarHost
import de.shinz.rickandmortyshowcase.core.designsystem.plus
import de.shinz.rickandmortyshowcase.core.designsystem.theme.AppTheme
import de.shinz.rickandmortyshowcase.core.ui.ObserveAsEvents
import de.shinz.rickandmortyshowcase.core.ui.resolve
import de.shinz.rickandmortyshowcase.features.favorites.presentation.model.FavoritesUiAction
import de.shinz.rickandmortyshowcase.features.favorites.presentation.model.FavoritesUiEvent
import de.shinz.rickandmortyshowcase.features.favorites.presentation.model.FavoritesUiState
import de.shinz.rickandmortyshowcase.features.favorites.presentation.previews.FavoritesScreenPreviewParameterProvider
import de.shinz.rickandmortyshowcase.features.shared.presentation.components.CharacterListItem
import de.shinz.rickandmortyshowcase.features.shared.presentation.model.CharacterUi
import de.shinz.rickandmortyshowcase.generated.resources.Res
import de.shinz.rickandmortyshowcase.generated.resources.action_remove
import de.shinz.rickandmortyshowcase.generated.resources.favorite_remove_title
import de.shinz.rickandmortyshowcase.generated.resources.favorites_empty_description
import de.shinz.rickandmortyshowcase.generated.resources.favorites_empty_title
import de.shinz.rickandmortyshowcase.generated.resources.ic_favorite_border
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.rememberResourceEnvironment
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Holds the ViewModel and turns events into navigation and messages.
 *
 * @param onNavigateToDetail already guarded against firing twice for one tap —
 *   see `AppNavHost`. It opens the *same* destination Home opens.
 */
@Composable
fun FavoritesRoot(
    contentPadding: PaddingValues,
    onNavigateToDetail: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = rememberResourceEnvironment()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is FavoritesUiEvent.NavigateToDetail -> onNavigateToDetail(event.characterId)
            is FavoritesUiEvent.ShowSnackbar -> scope.launch {
                snackbarHostState.showSnackbar(event.message.resolve(resources))
            }
        }
    }

    FavoritesScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        contentPadding = contentPadding,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
fun FavoritesScreen(
    uiState: FavoritesUiState,
    onAction: (FavoritesUiAction) -> Unit,
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> AppLoadingIndicator(
                modifier = Modifier.padding(contentPadding),
            )

            uiState.isEmptyStateVisible -> AppEmptyState(
                // Static: this screen's empty state never names a character.
                title = stringResource(Res.string.favorites_empty_title),
                description = stringResource(Res.string.favorites_empty_description),
                painter = painterResource(Res.drawable.ic_favorite_border),
                modifier = Modifier.padding(contentPadding),
            )

            else -> FavoritesList(
                characters = uiState.characters,
                onAction = onAction,
                contentPadding = contentPadding,
            )
        }

        AppSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(contentPadding),
        )
    }

    uiState.removeDialogText?.let { text ->
        AppConfirmDialog(
            title = stringResource(Res.string.favorite_remove_title),
            text = text.asString(),
            confirmLabel = stringResource(Res.string.action_remove),
            onConfirm = { onAction(FavoritesUiAction.OnConfirmRemoveFavorite) },
            onDismiss = { onAction(FavoritesUiAction.OnDismissRemoveFavorite) },
        )
    }
}

/**
 * The same `CharacterListItem` Home uses, as `SPEC.md` requires — one row, two
 * screens, so they cannot drift apart.
 *
 * No end-of-list trigger: there is nothing to page. The database is the whole
 * source.
 */
@Composable
private fun FavoritesList(
    characters: List<CharacterUi>,
    onAction: (FavoritesUiAction) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    val direction = LocalLayoutDirection.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        // The window insets as *content* padding, so rows scroll under the bars
        // rather than stopping short of them. The horizontal halves matter in
        // landscape, where safeDrawing carries a cutout and the side nav bar.
        contentPadding = contentPadding.plus(
            direction = direction,
            horizontal = spacing.screenPadding,
            vertical = spacing.md,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        items(items = characters, key = CharacterUi::id) { character ->
            CharacterListItem(
                character = character,
                onClick = { onAction(FavoritesUiAction.OnCharacterClick(character.id)) },
                onFavoriteClick = { onAction(FavoritesUiAction.OnFavoriteClick(character.id)) },
            )
        }
    }
}

/* ============================================================
   Previews
   ============================================================ */

@PreviewLightDark
@Composable
private fun FavoritesScreenPreview(
    @PreviewParameter(FavoritesScreenPreviewParameterProvider::class)
    uiState: FavoritesUiState,
) {
    AppTheme {
        FavoritesScreen(
            uiState = uiState,
            onAction = {},
            contentPadding = PaddingValues(),
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
