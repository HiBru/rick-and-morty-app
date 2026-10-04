package de.shinz.rickandmortyshowcase.features.shared.presentation.model

import de.shinz.rickandmortyshowcase.core.domain.model.Character
import de.shinz.rickandmortyshowcase.core.domain.model.CharacterStatus
import de.shinz.rickandmortyshowcase.core.ui.UiText
import de.shinz.rickandmortyshowcase.generated.resources.Res
import de.shinz.rickandmortyshowcase.generated.resources.cd_add_favorite
import de.shinz.rickandmortyshowcase.generated.resources.cd_remove_favorite
import de.shinz.rickandmortyshowcase.generated.resources.status_alive
import de.shinz.rickandmortyshowcase.generated.resources.status_dead
import de.shinz.rickandmortyshowcase.generated.resources.status_unknown
import org.jetbrains.compose.resources.StringResource

/**
 * The domain character a row renders.
 *
 * Called from each screen's assembler rather than from a composable, so it is a
 * plain function: it picks resources but never resolves them, which is what lets
 * it stay out of composition and be tested without one.
 *
 * [isFavorite] is a parameter rather than a field on [Character] because the two
 * arrive from different places — the character from a page of the API or from
 * the favourites table, the flag from `ObserveFavoriteIdsUseCase`. The assembler
 * is where they meet.
 */
fun Character.toCharacterUi(isFavorite: Boolean): CharacterUi = CharacterUi(
    id = id,
    name = name,
    imageUrl = imageUrl,
    status = status,
    subtitle = UiText.Joined(
        listOf(
            UiText.StringResourceText(status.statusLabel()),
            // Not a resource: the API's own wording, unbounded and untranslated.
            // Blank for a handful of characters, which is exactly the case
            // UiText.Joined drops along with its separator.
            UiText.DynamicString(species),
        ),
    ),
    isFavorite = isFavorite,
    favoriteContentDescription = favoriteContentDescription(name, isFavorite),
)

/**
 * What a screen reader announces for a favourite button.
 *
 * Shared by the row and the detail screen rather than written twice: the two
 * screens offer the same control, and a11y text that drifts between them is the
 * kind of defect nobody sees. Named, so a list does not read as twenty identical
 * buttons.
 */
fun favoriteContentDescription(name: String, isFavorite: Boolean): UiText =
    UiText.StringResourceText(
        id = if (isFavorite) Res.string.cd_remove_favorite else Res.string.cd_add_favorite,
        args = listOf(name),
    )

/**
 * The word for a status.
 *
 * Separate from `AppStatusDot`'s colour mapping on purpose: the two live in
 * different layers because one produces text and the other a token, and neither
 * can do the other's job. Shared with the detail screen, which shows the same
 * "Alive · Human" line under the character's name.
 */
fun CharacterStatus.statusLabel(): StringResource = when (this) {
    CharacterStatus.ALIVE -> Res.string.status_alive
    CharacterStatus.DEAD -> Res.string.status_dead
    CharacterStatus.UNKNOWN -> Res.string.status_unknown
}
