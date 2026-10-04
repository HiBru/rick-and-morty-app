package de.shinz.rickandmortyshowcase.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import de.shinz.rickandmortyshowcase.core.designsystem.previews.AppStatusDotPreviewParameterProvider
import de.shinz.rickandmortyshowcase.core.designsystem.theme.AppTheme
import de.shinz.rickandmortyshowcase.core.domain.model.CharacterStatus

/**
 * The coloured dot beside a character's status label.
 *
 * **This is where the status-to-colour mapping lives, deliberately.** An
 * assembler cannot return a `Color` — it must not import Compose — and a screen
 * must not branch on data. So the branch sits inside a design-system atom, which
 * is the one place allowed to turn a domain value into a token. See the
 * sanctioned conventions in `docs/IMPLEMENTATION_PLAN.md`.
 *
 * No `contentDescription`: the dot is decorative. The status it encodes is always
 * rendered as text next to it, so announcing it twice would be worse than not
 * announcing it at all.
 */
@Composable
fun AppStatusDot(
    status: CharacterStatus,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    // Aliased: a local named `size` shadows DrawScope.size.
    val sizes = AppTheme.size

    val color = when (status) {
        CharacterStatus.ALIVE -> colors.statusAlive
        CharacterStatus.DEAD -> colors.statusDead
        CharacterStatus.UNKNOWN -> colors.statusUnknown
    }

    Box(
        modifier = modifier
            .size(sizes.statusDot)
            .background(color = color, shape = CircleShape),
    )
}

/* ============================================================
   Previews
   ============================================================ */

@PreviewLightDark
@Composable
private fun AppStatusDotPreview(
    @PreviewParameter(AppStatusDotPreviewParameterProvider::class) status: CharacterStatus,
) {
    AppTheme {
        AppStatusDot(status = status)
    }
}
