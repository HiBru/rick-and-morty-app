package de.shinz.rickandmortyshowcase.features.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.shinz.rickandmortyshowcase.core.designsystem.components.AppCard
import de.shinz.rickandmortyshowcase.core.designsystem.components.AppSnackbarHost
import de.shinz.rickandmortyshowcase.core.designsystem.plus
import de.shinz.rickandmortyshowcase.core.designsystem.theme.AppTheme
import de.shinz.rickandmortyshowcase.core.ui.ObserveAsEvents
import de.shinz.rickandmortyshowcase.core.ui.resolve
import de.shinz.rickandmortyshowcase.features.settings.presentation.model.SettingsUiAction
import de.shinz.rickandmortyshowcase.features.settings.presentation.model.SettingsUiEvent
import de.shinz.rickandmortyshowcase.features.settings.presentation.model.SettingsUiState
import de.shinz.rickandmortyshowcase.features.settings.presentation.model.ThemeOptionUi
import de.shinz.rickandmortyshowcase.features.settings.presentation.previews.SettingsScreenPreviewParameterProvider
import de.shinz.rickandmortyshowcase.generated.resources.Res
import de.shinz.rickandmortyshowcase.generated.resources.settings_appearance
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.rememberResourceEnvironment
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Holds the ViewModel and turns a failed write into a message. */
@Composable
fun SettingsRoot(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = rememberResourceEnvironment()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SettingsUiEvent.ShowSnackbar -> scope.launch {
                snackbarHostState.showSnackbar(event.message.resolve(resources))
            }
        }
    }

    SettingsScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        contentPadding = contentPadding,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onAction: (SettingsUiAction) -> Unit,
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    val padding = contentPadding.plus(
        direction = LocalLayoutDirection.current,
        horizontal = spacing.screenPadding,
        vertical = spacing.lg,
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(
                // Static: this heading never depends on data.
                text = stringResource(Res.string.settings_appearance),
                style = AppTheme.typography.screenTitle,
                color = AppTheme.colors.onSurface,
            )

            AppCard(
                contentPadding = PaddingValues(vertical = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                // One radio group, so a screen reader announces "1 of 3" rather
                // than three unrelated toggles.
                Column(modifier = Modifier.selectableGroup()) {
                    uiState.themeOptions.forEach { option ->
                        ThemeOptionRow(
                            option = option,
                            onClick = {
                                onAction(SettingsUiAction.OnThemeModeClick(option.mode))
                            },
                        )
                    }
                }
            }
        }

        AppSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(contentPadding),
        )
    }
}

/**
 * One choice.
 *
 * **`selectable`, not `clickable`.** The two look identical and are not: only
 * `selectable` puts `Selected` and `Role.RadioButton` on the node, so a screen
 * reader says "Dark, selected, radio button" instead of "Dark, double tap to
 * activate" — with no way to tell which theme is in use. A UI test caught the
 * difference; nothing visual would have.
 *
 * The whole row is the target, not just the radio: a 20dp circle is a poor thing
 * to ask anyone to hit, and `AppSize.touchTarget` is the floor the design system
 * sets. `selectable` merges its descendants, so the label and the radio are one
 * node rather than two.
 */
@Composable
private fun ThemeOptionRow(
    option: ThemeOptionUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AppTheme.size.touchTarget)
            .selectable(
                selected = option.isSelected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = AppTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        RadioButton(
            selected = option.isSelected,
            // The row owns the selection; a second target inside it would be
            // two things to hit for one choice.
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = colors.accent,
                unselectedColor = colors.onSurfaceMuted,
            ),
        )
        Text(
            text = option.label.asString(),
            style = AppTheme.typography.body,
            color = colors.onSurface,
        )
    }
}

/* ============================================================
   Previews
   ============================================================ */

@PreviewLightDark
@Composable
private fun SettingsScreenPreview(
    @PreviewParameter(SettingsScreenPreviewParameterProvider::class)
    uiState: SettingsUiState,
) {
    AppTheme {
        SettingsScreen(
            uiState = uiState,
            onAction = {},
            contentPadding = PaddingValues(),
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
