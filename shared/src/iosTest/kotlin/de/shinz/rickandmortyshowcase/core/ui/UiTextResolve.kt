package de.shinz.rickandmortyshowcase.core.ui

import org.jetbrains.compose.resources.getString

/**
 * [UiText.asString] without composition, for the tests that read actual text.
 *
 * `asString()` is `@Composable` — it has to be, since `stringResource` is — so a
 * test cannot call it. This mirrors it over the suspend `getString`, which is
 * the same resource lookup underneath.
 *
 * The duplication is the point of risk, so it is kept to the *resolution* only:
 * the joining rule comes from [joinNonBlank], the real one, rather than being
 * restated here.
 *
 * In `iosTest` because that is the only place resource text is readable at all —
 * on the JVM `getString` resolves through `Resources.getSystem()`, which an
 * unmocked host test cannot provide.
 */
internal suspend fun UiText.resolve(): String = when (this) {
    is UiText.DynamicString -> value
    is UiText.StringResourceText ->
        if (args.isEmpty()) getString(id) else getString(id, *args.toTypedArray())
    is UiText.Joined -> joinNonBlank(parts.map { it.resolve() }, separator)
}
