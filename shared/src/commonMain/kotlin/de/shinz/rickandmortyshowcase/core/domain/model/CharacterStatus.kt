package de.shinz.rickandmortyshowcase.core.domain.model

/**
 * Whether a character is alive, as the API reports it.
 *
 * An enum rather than the API's raw string because this is the one character
 * field that drives a colour as well as a label. The mapping from status to
 * colour belongs to `AppStatusDot` in the design system — an assembler cannot
 * return a `Color`, and a screen must not branch on data.
 *
 * `species`, `type` and `gender` stay plain strings by contrast: their values
 * are unbounded (Human, Alien, Poopybutthole, …) and they are displayed as the
 * API words them.
 */
enum class CharacterStatus {
    ALIVE,
    DEAD,
    UNKNOWN,
}
