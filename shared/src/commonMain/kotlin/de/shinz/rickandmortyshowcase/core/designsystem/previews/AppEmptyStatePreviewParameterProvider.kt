package de.shinz.rickandmortyshowcase.core.designsystem.previews

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import de.shinz.rickandmortyshowcase.generated.resources.Res
import de.shinz.rickandmortyshowcase.generated.resources.ic_favorite_border
import org.jetbrains.compose.resources.DrawableResource

/**
 * Three data parameters, so they bundle — `@PreviewParameter` injects only one.
 *
 * The long variant is the look most likely to break: a centred block inside the
 * screen gutter either keeps its margins when the text wraps, or it does not.
 */
internal data class AppEmptyStatePreviewData(
    val icon: DrawableResource,
    val title: String,
    val description: String,
)

internal class AppEmptyStatePreviewParameterProvider :
    PreviewParameterProvider<AppEmptyStatePreviewData> {

    override val values: Sequence<AppEmptyStatePreviewData> = sequenceOf(
        AppEmptyStatePreviewData(
            icon = Res.drawable.ic_favorite_border,
            title = "No favourites yet",
            description = "Tap the heart on any character to keep them here.",
        ),
        AppEmptyStatePreviewData(
            icon = Res.drawable.ic_favorite_border,
            title = "No favourites yet",
            description = "Tap the heart on any character to keep them here, " +
                "even with no connection at all — the whole record is saved, " +
                "not just the name.",
        ),
    )

    override fun getDisplayName(index: Int): String =
        listOf("short", "long description")[index]
}
