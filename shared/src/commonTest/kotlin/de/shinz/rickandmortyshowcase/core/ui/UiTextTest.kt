package de.shinz.rickandmortyshowcase.core.ui

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

/**
 * Covers the part of [UiText] that is not `@Composable`.
 *
 * [UiText.asString] itself needs a composition and no Compose test artifact is
 * wired, but [joinNonBlank] is where all of `Joined`'s actual behaviour lives —
 * which is why it was extracted in the first place.
 */
class UiTextTest {

    @Test
    fun joinsPartsWithTheSeparator() {
        assertThat(joinNonBlank(listOf("Alive", "Human"), " · "))
            .isEqualTo("Alive · Human")
    }

    @Test
    fun dropsABlankPartAndItsSeparator() {
        // The detail screen's case: the API sends type as "" for most characters,
        // so the line has to read as "Alive · Human", not "Alive ·  · Human".
        assertThat(joinNonBlank(listOf("Alive", "", "Human"), " · "))
            .isEqualTo("Alive · Human")
    }

    @Test
    fun treatsWhitespaceOnlyAsBlank() {
        assertThat(joinNonBlank(listOf("Alive", "   ", "Human"), " · "))
            .isEqualTo("Alive · Human")
    }

    @Test
    fun dropsTrailingAndLeadingBlanks() {
        assertThat(joinNonBlank(listOf("", "Human", ""), " · ")).isEqualTo("Human")
    }

    @Test
    fun resolvesAnEmptyListToAnEmptyString() {
        // The caller's cue to show a placeholder instead.
        assertThat(joinNonBlank(emptyList(), " · ")).isEqualTo("")
    }

    @Test
    fun resolvesAnAllBlankListToAnEmptyString() {
        assertThat(joinNonBlank(listOf("", "  "), " · ")).isEqualTo("")
    }

    @Test
    fun aSinglePartGetsNoSeparator() {
        assertThat(joinNonBlank(listOf("Human"), " · ")).isEqualTo("Human")
    }
}
