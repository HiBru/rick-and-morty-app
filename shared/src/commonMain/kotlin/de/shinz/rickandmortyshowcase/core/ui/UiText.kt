package de.shinz.rickandmortyshowcase.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Text that comes from — or could come from — a string resource.
 *
 * This is what lets a ViewModel and an assembler decide *what a screen says*
 * without importing Compose: they hand back a `UiText`, and composition resolves
 * it with [asString].
 *
 * Use it for anything resource-backed or localisable. Values that are always
 * dynamic and never come from a resource — a character's name, a species
 * straight from the API — stay plain `String` in the `UiState`.
 */
sealed interface UiText {

    /** Always-dynamic text that never comes from a resource. */
    data class DynamicString(val value: String) : UiText

    /** Text backed by a Compose Multiplatform string resource. */
    data class StringResourceText(
        val id: StringResource,
        val args: List<Any> = emptyList(),
    ) : UiText

    /**
     * Several texts on one line — "Alive · Human".
     *
     * Blank parts drop out, separator and all, so a row with half its data reads
     * as half a row rather than as a line of stray separators. An empty list
     * resolves to `""`, which is the caller's cue to fall back to a placeholder.
     *
     * This exists because such a line mixes sources: a character's status is a
     * string resource, its species is whatever the API returned. An assembler can
     * resolve neither, so it hands over both and lets composition do it. It is
     * also how the detail screen omits `type`, which the API usually sends blank.
     */
    data class Joined(
        val parts: List<UiText>,
        val separator: String = " · ",
    ) : UiText

    @Composable
    fun asString(): String = when (this) {
        is DynamicString -> value
        is StringResourceText ->
            if (args.isEmpty()) stringResource(id)
            else stringResource(id, *args.toTypedArray())
        is Joined -> {
            val resolved = ArrayList<String>(parts.size)
            parts.forEach { part ->
                // Keyed on the part itself so each one keeps its composition group
                // identity when the list is reordered or changes length. Without
                // it, a part positionally reused in a different slot invalidates
                // its remembered resource and re-reads it — and resource reads are
                // blocking. The value is never *stale*: stringResource remembers
                // against the resource itself, so a mismatched slot re-resolves
                // synchronously. Duplicate keys are fine here; this is not a
                // LazyColumn key, and two blank parts are the normal case.
                resolved.add(key(part) { part.asString() })
            }
            joinNonBlank(resolved, separator)
        }
    }
}

/**
 * The rule behind [UiText.Joined], kept out of composition so it can be tested:
 * blank parts contribute nothing, not even a separator.
 */
internal fun joinNonBlank(parts: List<String>, separator: String): String =
    parts.filter { it.isNotBlank() }.joinToString(separator)
