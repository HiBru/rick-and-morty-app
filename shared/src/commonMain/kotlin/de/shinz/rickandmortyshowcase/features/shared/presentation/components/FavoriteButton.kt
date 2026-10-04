package de.shinz.rickandmortyshowcase.features.shared.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import de.shinz.rickandmortyshowcase.core.designsystem.components.AppIconButton
import de.shinz.rickandmortyshowcase.core.designsystem.theme.AppTheme
import de.shinz.rickandmortyshowcase.features.shared.presentation.previews.FavoriteButtonPreviewParameterProvider
import de.shinz.rickandmortyshowcase.generated.resources.Res
import de.shinz.rickandmortyshowcase.generated.resources.ic_favorite
import de.shinz.rickandmortyshowcase.generated.resources.ic_favorite_border
import org.jetbrains.compose.resources.painterResource

/**
 * The heart that saves a character, or asks to drop it.
 *
 * **This is where the favourite-to-icon-and-tint mapping lives**, for the same
 * reason `AppStatusDot` owns status-to-colour: an assembler cannot return a
 * `Painter` or a `Color`, and a screen must not branch on data — so the branch
 * sits in one small composable that three screens share.
 *
 * It lives in `features/shared/` rather than in the design system because
 * nothing about "favourite" is a design concept. It is built *from* design-system
 * pieces: [AppIconButton] gives it the 48dp touch target and the circular ripple.
 *
 * The [contentDescription] comes in already decided, because the choice between
 * "Add Rick Sanchez to favorites" and "Remove Rick Sanchez from favorites" is a
 * data-dependent resource choice and those belong to the assembler. Only the
 * two token choices are made here.
 *
 * **It never asks before removing.** `SPEC.md` puts a confirmation dialog on
 * every removal, and that dialog is the screen's business — this button reports
 * the tap and nothing else, so the three screens cannot drift on what a tap
 * means.
 */
@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors

    AppIconButton(
        painter = painterResource(
            if (isFavorite) Res.drawable.ic_favorite else Res.drawable.ic_favorite_border,
        ),
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
        // The accent is the app's one colour, and a saved character is the one
        // thing on a row worth spending it on.
        tint = if (isFavorite) colors.accent else colors.onSurfaceMuted,
    )
}

/* ============================================================
   Previews
   ============================================================ */

@PreviewLightDark
@Composable
private fun FavoriteButtonPreview(
    @PreviewParameter(FavoriteButtonPreviewParameterProvider::class) isFavorite: Boolean,
) {
    AppTheme {
        FavoriteButton(
            isFavorite = isFavorite,
            contentDescription = "Add Rick Sanchez to favorites",
            onClick = {},
        )
    }
}
