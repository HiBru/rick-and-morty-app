package de.shinz.rickandmortyshowcase.core.data.theme

import assertk.assertThat
import assertk.assertions.isEqualTo
import de.shinz.rickandmortyshowcase.core.domain.model.ThemeMode
import kotlin.test.Test

/**
 * The stored-string to enum lookup, which is where every "it reset itself"
 * complaint would originate.
 */
class ToThemeModeTest {

    @Test
    fun readsEachStoredValueBack() {
        ThemeMode.entries.forEach { mode ->
            assertThat(mode.name.toThemeMode(), name = mode.name).isEqualTo(mode)
        }
    }

    @Test
    fun nothingStoredYetMeansFollowTheSystem() {
        assertThat(null.toThemeMode()).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun anUnrecognisedStoredValueFallsBackInsteadOfThrowing() {
        // Written by an older build, or a renamed constant. The user's app must
        // still open.
        assertThat("SEPIA".toThemeMode()).isEqualTo(ThemeMode.SYSTEM)
        assertThat("".toThemeMode()).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun theLookupIsCaseSensitiveBecauseItWritesTheNameItself() {
        // Only this code writes the value, and it writes ThemeMode.name — so a
        // lowercase match would mean accepting something we never produced.
        assertThat("dark".toThemeMode()).isEqualTo(ThemeMode.SYSTEM)
    }
}
