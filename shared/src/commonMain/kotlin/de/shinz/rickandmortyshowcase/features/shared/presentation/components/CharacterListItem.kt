package de.shinz.rickandmortyshowcase.features.shared.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import coil3.compose.AsyncImage
import de.shinz.rickandmortyshowcase.core.designsystem.components.AppCard
import de.shinz.rickandmortyshowcase.core.designsystem.components.AppStatusDot
import de.shinz.rickandmortyshowcase.core.designsystem.theme.AppTheme
import de.shinz.rickandmortyshowcase.features.shared.presentation.model.CharacterUi
import de.shinz.rickandmortyshowcase.features.shared.presentation.previews.CharacterListItemPreviewParameterProvider

/**
 * One character, as both list screens show them.
 *
 * `SPEC.md` requires Favorites to use the same row design as the character
 * list, so there is one row and both screens pass it a [CharacterUi]. That is
 * also why it takes a UI model rather than a `Character`: the row renders text
 * that is already chosen and already interpolated, and never sees a domain
 * decision.
 *
 * Typed parameters rather than slots — this is a feature composable, not a
 * design-system container.
 *
 * @param onFavoriteClick a *tap*, not a toggle. Adding happens immediately but
 *   removing opens a confirmation dialog, so the screen decides which of the two
 *   a tap meant.
 */
@Composable
fun CharacterListItem(
    character: CharacterUi,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing

    AppCard(
        modifier = modifier,
        onClick = onClick,
        // Tighter than the card default: the avatar already carries the row's
        // height, and the favourite button brings its own 48dp target, so the
        // larger inset would only push both away from the content they belong to.
        contentPadding = PaddingValues(spacing.md),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            CharacterAvatar(imageUrl = character.imageUrl)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Text(
                    text = character.name,
                    style = AppTheme.typography.cardTitle,
                    color = AppTheme.colors.onSurface,
                    // Names run long — "Rick Sanchez (Tiny Rick)" — and a row
                    // that grows to two lines breaks the list's rhythm.
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusLine(character = character)
            }

            FavoriteButton(
                isFavorite = character.isFavorite,
                contentDescription = character.favoriteContentDescription.asString(),
                onClick = onFavoriteClick,
            )
        }
    }
}

/**
 * The thumbnail.
 *
 * No `contentDescription`: the name sits beside it, so the only thing a reader
 * could announce is the name again. The API ships no alt text, and inventing
 * "Image of Rick Sanchez" would be a second label for one thing.
 *
 * Placeholder and error are the same flat fill, deliberately. A broken portrait
 * is not something the user can act on, and an error glyph on a row in a
 * paginated list would read as the row itself having failed.
 */
@Composable
private fun CharacterAvatar(
    imageUrl: String,
    modifier: Modifier = Modifier,
) {
    val placeholderColor = AppTheme.colors.surfaceVariant
    val cornerRadius = AppTheme.radius.sm
    // Remembered because AsyncImage folds the painters into a transform lambda:
    // a fresh ColorPainter per composition is a fresh lambda, which is a scroll's
    // worth of allocations in a list this is built for.
    val fill = remember(placeholderColor) { ColorPainter(placeholderColor) }
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        modifier = modifier
            .size(AppTheme.size.avatar)
            .clip(shape),
        placeholder = fill,
        error = fill,
        // The API's portraits are square, but cropping rather than fitting means
        // a non-square one can never letterbox against the card.
        contentScale = ContentScale.Crop,
    )
}

/** The dot and the "Alive · Human" line it belongs to. */
@Composable
private fun StatusLine(
    character: CharacterUi,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        AppStatusDot(status = character.status)
        Text(
            text = character.subtitle.asString(),
            style = AppTheme.typography.caption,
            color = AppTheme.colors.onSurfaceMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/* ============================================================
   Previews
   ============================================================ */

@PreviewLightDark
@Composable
private fun CharacterListItemPreview(
    @PreviewParameter(CharacterListItemPreviewParameterProvider::class) character: CharacterUi,
) {
    AppTheme {
        CharacterListItem(
            character = character,
            onClick = {},
            onFavoriteClick = {},
            modifier = Modifier.padding(AppTheme.spacing.lg),
        )
    }
}
