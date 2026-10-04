package de.shinz.rickandmortyshowcase.navigation

import kotlinx.serialization.Serializable

/*
 * Type-safe routes.
 *
 * `data object` rather than plain `object`: it gets a readable `toString()`,
 * which is the difference between a legible back stack in the debugger and a row
 * of hashes.
 *
 * Two levels, matching the structure CLAUDE.md describes. The **outer** host has
 * exactly two destinations — the dashboard and the detail screen — which is what
 * makes detail cover the bottom bar by construction rather than by toggling the
 * bar out of composition as the transition runs. The three tabs live in a
 * **nested** host inside the dashboard.
 *
 * All of them are declared in this one file: "central NavHost" is about where
 * routes and `composable<…>` entries live, not about refusing to nest.
 */

/** The shell: bottom bar plus the tab host. The start destination. */
@Serializable
data object DashboardRoute

/**
 * A tab. The marker is what stops [TopLevelDestination] accepting any `Any` —
 * and it deliberately excludes [CharacterDetailRoute], which encodes "detail is
 * not a tab" in the type system.
 */
@Serializable
sealed interface TopLevelRoute

@Serializable
data object HomeRoute : TopLevelRoute

@Serializable
data object FavoritesRoute : TopLevelRoute

@Serializable
data object SettingsRoute : TopLevelRoute

/**
 * The character detail screen.
 *
 * **Carries an id, never a character.** The destination loads its own data —
 * which is what lets the same screen open from Home (fetch from the API) and
 * from Favorites (read the local row) with no caller knowing the difference.
 *
 * It sits in the *outer* host, above the dashboard, so it covers the bottom bar
 * and is reachable identically from both lists.
 */
@Serializable
data class CharacterDetailRoute(val characterId: Int)
