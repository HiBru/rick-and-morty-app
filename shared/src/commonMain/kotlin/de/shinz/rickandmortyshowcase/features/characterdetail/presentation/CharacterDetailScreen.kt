package de.shinz.rickandmortyshowcase.features.characterdetail.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import de.shinz.rickandmortyshowcase.core.designsystem.components.AppCard
import de.shinz.rickandmortyshowcase.core.designsystem.components.AppConfirmDialog
import de.shinz.rickandmortyshowcase.core.designsystem.components.AppErrorState
import de.shinz.rickandmortyshowcase.core.designsystem.components.AppIconButton
import de.shinz.rickandmortyshowcase.core.designsystem.components.AppLoadingIndicator
import de.shinz.rickandmortyshowcase.core.designsystem.components.AppSnackbarHost
import de.shinz.rickandmortyshowcase.core.designsystem.components.AppStatusDot
import de.shinz.rickandmortyshowcase.core.designsystem.theme.AppTheme
import de.shinz.rickandmortyshowcase.core.ui.ObserveAsEvents
import de.shinz.rickandmortyshowcase.core.ui.resolve
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailFieldUi
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailUi
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailUiAction
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailUiEvent
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailUiState
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.previews.CharacterDetailScreenPreviewParameterProvider
import de.shinz.rickandmortyshowcase.features.shared.presentation.components.FavoriteButton
import de.shinz.rickandmortyshowcase.generated.resources.Res
import de.shinz.rickandmortyshowcase.generated.resources.action_remove
import de.shinz.rickandmortyshowcase.generated.resources.cd_navigate_back
import de.shinz.rickandmortyshowcase.generated.resources.favorite_remove_title
import de.shinz.rickandmortyshowcase.generated.resources.ic_arrow_back
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.rememberResourceEnvironment
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Holds the ViewModel and turns events into messages.
 *
 * **`parametersOf` is not optional here.** `CharacterDetailViewModel` takes the
 * character id as a plain `Int` — see its KDoc for why it is not a
 * `SavedStateHandle` — so a bare `koinViewModel()` compiles and then fails at
 * runtime with "No value found for type Int". Nothing in the test suite catches
 * that; the device check does.
 *
 * @param onNavigateBack the only way off this screen, so the ViewModel has no
 *   navigation event at all.
 */
@Composable
fun CharacterDetailRoot(
    characterId: Int,
    contentPadding: PaddingValues,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CharacterDetailViewModel = koinViewModel { parametersOf(characterId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = rememberResourceEnvironment()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is CharacterDetailUiEvent.ShowSnackbar -> scope.launch {
                snackbarHostState.showSnackbar(event.message.resolve(resources))
            }
        }
    }

    CharacterDetailScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack,
        contentPadding = contentPadding,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
fun CharacterDetailScreen(
    uiState: CharacterDetailUiState,
    onAction: (CharacterDetailUiAction) -> Unit,
    onNavigateBack: () -> Unit,
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> AppLoadingIndicator(
                modifier = Modifier.padding(contentPadding),
            )

            uiState.error != null -> AppErrorState(
                message = uiState.error.asString(),
                modifier = Modifier.padding(contentPadding),
                onRetry = { onAction(CharacterDetailUiAction.OnRetryClick) },
            )

            uiState.character != null -> ResolvedCharacter(
                character = uiState.character,
                onAction = onAction,
                contentPadding = contentPadding,
            )

            // The assembler sets exactly one of the three, so this cannot
            // happen — but a subjectless `when` gets no exhaustiveness check,
            // and the alternative to spelling it out is a blank screen with a
            // back button floating on it. "Nothing resolved and nothing wrong"
            // is the same thing to look at as "still resolving".
            else -> AppLoadingIndicator(modifier = Modifier.padding(contentPadding))
        }

        // Above the content, always — it is the only way back, and on a screen
        // whose hero image runs under the status bar it has to carry its own
        // background or it would sit on the artwork.
        BackButton(
            onNavigateBack = onNavigateBack,
            modifier = Modifier.align(Alignment.TopStart).padding(contentPadding),
        )

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
            onConfirm = { onAction(CharacterDetailUiAction.OnConfirmRemoveFavorite) },
            onDismiss = { onAction(CharacterDetailUiAction.OnDismissRemoveFavorite) },
        )
    }
}

@Composable
private fun ResolvedCharacter(
    character: CharacterDetailUi,
    onAction: (CharacterDetailUiAction) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    val direction = LocalLayoutDirection.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        HeroImage(imageUrl = character.imageUrl)

        Column(
            modifier = Modifier.padding(
                // The window's horizontal insets *plus* the screen gutter. In
                // landscape `safeDrawing` puts a display cutout and the side
                // navigation bar here, and 20dp of gutter does not clear a
                // notch — the back button already takes the full insets, so
                // without this the button moves and the text under it does not.
                start = contentPadding.calculateStartPadding(direction) + spacing.screenPadding,
                end = contentPadding.calculateEndPadding(direction) + spacing.screenPadding,
                top = spacing.lg,
                // The window's bottom inset, so the last field clears the
                // gesture bar. The top inset is deliberately *not* applied: the
                // image is meant to run under the status bar.
                bottom = contentPadding.calculateBottomPadding() + spacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Header(character = character, onAction = onAction)

            AppCard(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                character.fields.forEach { field ->
                    FieldRow(field = field)
                }
            }
        }
    }
}

/**
 * The portrait, edge to edge and running under the status bar.
 *
 * No `contentDescription`: the name is directly beneath it, and the API ships no
 * alt text — "Image of Rick Sanchez" would be a second label for one thing.
 */
@Composable
private fun HeroImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
) {
    val fillColor = AppTheme.colors.surfaceVariant
    val fill = remember(fillColor) { ColorPainter(fillColor) }

    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        modifier = modifier
            .fillMaxWidth()
            .height(AppTheme.size.detailImage),
        placeholder = fill,
        error = fill,
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun Header(
    character: CharacterDetailUi,
    onAction: (CharacterDetailUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            Text(
                text = character.name,
                style = AppTheme.typography.displayTitle,
                color = AppTheme.colors.onSurface,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                AppStatusDot(status = character.status)
                Text(
                    text = character.subtitle.asString(),
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.onSurfaceMuted,
                )
            }
        }

        FavoriteButton(
            isFavorite = character.isFavorite,
            contentDescription = character.favoriteContentDescription.asString(),
            onClick = { onAction(CharacterDetailUiAction.OnFavoriteClick) },
        )
    }
}

/**
 * A label above its value, so a long value never squeezes the label.
 *
 * The two are **merged into one semantics node**, so a screen reader announces
 * "Gender, Male" rather than stopping on each separately — the pairing is
 * otherwise purely visual. Verified by dumping the tree: without the merge these
 * are two unrelated nodes. Nothing here is clickable, so there is no nested
 * action for the merge to swallow.
 */
@Composable
private fun FieldRow(
    field: CharacterDetailFieldUi,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        Text(
            text = field.label.asString(),
            style = AppTheme.typography.caption,
            color = AppTheme.colors.onSurfaceMuted,
        )
        Text(
            text = field.value,
            style = AppTheme.typography.body,
            color = AppTheme.colors.onSurface,
        )
    }
}

@Composable
private fun BackButton(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors

    Box(modifier = modifier.padding(AppTheme.spacing.sm)) {
        AppIconButton(
            painter = painterResource(Res.drawable.ic_arrow_back),
            contentDescription = stringResource(Res.string.cd_navigate_back),
            onClick = onNavigateBack,
            // Its own surface, because it sits on the artwork: a bare icon would
            // be legible or not depending on the character.
            modifier = Modifier
                .clip(CircleShape)
                .background(color = colors.surface, shape = CircleShape),
            tint = colors.onSurface,
        )
    }
}

/* ============================================================
   Previews
   ============================================================ */

@PreviewLightDark
@Composable
private fun CharacterDetailScreenPreview(
    @PreviewParameter(CharacterDetailScreenPreviewParameterProvider::class)
    uiState: CharacterDetailUiState,
) {
    AppTheme {
        CharacterDetailScreen(
            uiState = uiState,
            onAction = {},
            onNavigateBack = {},
            contentPadding = PaddingValues(),
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
