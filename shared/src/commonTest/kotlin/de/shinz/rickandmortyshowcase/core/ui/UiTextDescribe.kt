package de.shinz.rickandmortyshowcase.core.ui

/**
 * A `UiText` as a comparable string, for tests that cannot resolve one.
 *
 * The resource **key** rather than the `StringResource` itself, because that
 * class has no `toString()` — a mismatch would otherwise be reported as
 * `StringResource@1a2b` instead of a name.
 *
 * Shared rather than copied: four assembler and mapper tests had written the
 * same private helper, which is two past the point the promotion rule names.
 * Resolved *text* is deliberately not available here — `getString` needs
 * `Resources.getSystem()` on the JVM, so the `…StringsTest` files in `iosTest`
 * own wording and these own identity.
 */
internal fun UiText.describe(): String = when (this) {
    is UiText.StringResourceText -> "res:${id.key}"
    is UiText.DynamicString -> "dyn:$value"
    is UiText.Joined -> parts.joinToString(separator) { it.describe() }
}
