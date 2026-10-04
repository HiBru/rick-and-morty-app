package de.shinz.rickandmortyshowcase.features.shared.presentation.model

import de.shinz.rickandmortyshowcase.core.domain.model.CharacterStatus
import de.shinz.rickandmortyshowcase.core.ui.UiText

/**
 * One character as a list row renders it.
 *
 * In `features/shared/` because the character list and Favorites show the same
 * row over the same model — `SPEC.md` requires them to look identical, and two
 * copies of this is how they would stop being.
 *
 * It carries only what a *row* shows. The detail screen gets its own UI model in
 * Task 18: it renders twice as many fields, and widening this one to serve both
 * would put eight unused strings into every row of a paginated list.
 */
data class CharacterUi(
    /** What a click navigates with, and what a favourite toggle keys on. */
    val id: Int,
    /** Straight from the API and never a resource, so a plain `String`. */
    val name: String,
    val imageUrl: String,
    /**
     * The domain enum, deliberately — not a colour and not a label.
     *
     * `AppStatusDot` owns the status-to-colour mapping, because an assembler
     * cannot return a `Color` and a screen must not branch on data. That makes
     * this the one domain type that travels into a composable intact. The *word*
     * "Alive" is not here either: it is already the first part of [subtitle].
     */
    val status: CharacterStatus,
    /**
     * The secondary line — "Alive · Human".
     *
     * A [UiText.Joined] because the line mixes sources: the status is a string
     * resource, the species is whatever the API returned. It also means a blank
     * species drops out with its separator rather than leaving a dangling "· ".
     */
    val subtitle: UiText,
    /** Drives the favourite button's icon and tint, inside `FavoriteButton`. */
    val isFavorite: Boolean,
    /**
     * What a screen reader announces for the favourite button.
     *
     * Separate from [isFavorite] even though it is derived from it: the *text* is
     * a data-dependent resource choice, which belongs to the assembler, while
     * the *icon and tint* are a data-dependent token choice, which belongs to a
     * design-system-style atom. Naming the character in it is what stops every
     * row of a list announcing the identical "Add to favorites".
     */
    val favoriteContentDescription: UiText,
)
