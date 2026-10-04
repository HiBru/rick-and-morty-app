package de.shinz.rickandmortyshowcase.core.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class ConstructRouteTest {

    @Test
    fun prefixesALeadingSlashRoute() {
        assertThat(constructRoute("/character")).isEqualTo("$BASE_URL/character")
    }

    @Test
    fun prefixesARouteWithoutALeadingSlash() {
        assertThat(constructRoute("character/1")).isEqualTo("$BASE_URL/character/1")
    }

    @Test
    fun passesAnAlreadyAbsoluteUrlThrough() {
        val absolute = "$BASE_URL/character?page=2"

        assertThat(constructRoute(absolute)).isEqualTo(absolute)
    }

    @Test
    fun prefixesARouteThatMerelyMentionsTheBaseUrlInAQueryValue() {
        // The reason this uses startsWith and not contains. With `contains` the
        // whole string passed through unprefixed, and Ktor resolves a relative
        // url against its default origin — so the request would quietly go to
        // localhost rather than failing.
        val route = "/character?from=$BASE_URL/x"

        assertThat(constructRoute(route)).isEqualTo("$BASE_URL/character?from=$BASE_URL/x")
    }
}
