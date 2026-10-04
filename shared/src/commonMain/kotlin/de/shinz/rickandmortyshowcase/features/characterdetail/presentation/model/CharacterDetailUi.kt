package de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model

import androidx.compose.runtime.Immutable
import de.shinz.rickandmortyshowcase.core.domain.model.CharacterStatus
import de.shinz.rickandmortyshowcase.core.ui.UiText

/**
 * One character as the detail screen renders it.
 *
 * Its own model rather than a widened `CharacterUi`: this carries six more
 * fields, and putting them on the row model would mean building them for every
 * row of a paginated list that shows none of them.
 *
 * `@Immutable` because of [fields] — a `List` is unstable to the compiler, and
 * the whole state is inferred unstable without it.
 */
@Immutable
data class CharacterDetailUi(
    val name: String,
    val imageUrl: String,
    /** The domain enum, for `AppStatusDot` — the same sanctioned exception as on a row. */
    val status: CharacterStatus,
    /** "Alive · Human", with a blank species dropping out along with its separator. */
    val subtitle: UiText,
    val isFavorite: Boolean,
    val favoriteContentDescription: UiText,
    /**
     * The labelled rows below the header, **already filtered**.
     *
     * A list rather than six nullable properties, because `SPEC.md` makes one of
     * them conditional ("type, only when the API provides a non-empty value") and
     * a screen must not decide whether to draw a row. Omitting it here means the
     * layout never has to know a field can be absent.
     */
    val fields: List<CharacterDetailFieldUi>,
)

/**
 * One labelled row.
 *
 * The label is a [UiText] because it is always a string resource; the value is a
 * plain `String` because it is always dynamic — the API's own wording, or a date
 * the formatter already rendered.
 */
data class CharacterDetailFieldUi(
    val label: UiText,
    val value: String,
)
