package de.shinz.rickandmortyshowcase.core.ui

import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getSystemResourceEnvironment

/**
 * [resolve] against the system environment, for tests that are not in a
 * composition and have no environment to pass.
 *
 * A one-line delegate rather than a second `when` — the production resolver owns
 * the mapping, so a test can never pin a version of it that does not ship.
 */
@OptIn(ExperimentalResourceApi::class)
internal suspend fun UiText.resolve(): String = resolve(getSystemResourceEnvironment())
