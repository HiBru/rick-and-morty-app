package de.shinz.rickandmortyshowcase.core.ui

import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.getString

/**
 * [UiText.asString] outside composition.
 *
 * `asString()` is `@Composable`, because `stringResource` is — which makes it
 * unusable anywhere a `UiText` has to be resolved from a callback: showing a
 * snackbar from an event handler, or reading one in a test.
 *
 * This is the same lookup through the suspend `getString`, and it takes the
 * [environment] explicitly so a caller inside composition can pass
 * `rememberResourceEnvironment()` — resolving against the composition's locale
 * rather than the system's, which is the difference that matters once a message
 * raised minutes ago is finally shown.
 *
 * The joining rule is **not** restated here: [UiText.Joined] delegates to
 * [joinNonBlank], the same function `asString` uses, so the two can only
 * disagree about resolution and never about the rule.
 */
suspend fun UiText.resolve(environment: ResourceEnvironment): String = when (this) {
    is UiText.DynamicString -> value
    is UiText.StringResourceText ->
        if (args.isEmpty()) getString(environment, id)
        else getString(environment, id, *args.toTypedArray())
    is UiText.Joined -> joinNonBlank(parts.map { it.resolve(environment) }, separator)
}
