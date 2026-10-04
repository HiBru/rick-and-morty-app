package de.shinz.rickandmortyshowcase.core.ui

import assertk.all
import assertk.assertAll
import assertk.assertThat
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.generated.resources.Res
import de.shinz.rickandmortyshowcase.generated.resources.error_not_found
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import org.jetbrains.compose.resources.getString

/**
 * Reads the error strings' actual text.
 *
 * This exists because of a bug it would have caught: apostrophes were escaped as
 * `\'` out of Android habit, and Compose Resources is not aapt — its plugin
 * unescapes only `\uXXXX`, `\n`, `\t` and `\\`, and the runtime returns the text
 * verbatim, so four messages shipped with a visible backslash. Nothing in the
 * mapping tests could see it, because they only compare resource identities.
 *
 * **In `iosTest`, not `commonTest`, and that is a platform limit rather than a
 * preference.** `getString` is a plain suspend function needing no Compose
 * runtime, but on the JVM it resolves the environment through
 * `Resources.getSystem()`, which an unmocked Android host test cannot provide —
 * it fails with "Method getSystem in android.content.res.Resources not mocked".
 * Reading resource *text* in a test therefore requires either iOS or
 * Robolectric, and iOS is already part of every task's verification.
 *
 * This costs the architecture nothing: assembler tests compare `UiText` *values*
 * and never resolved strings, which is what the MVI skill prescribes anyway.
 */
class ErrorStringsTest {

    private val allErrors: List<DataError> =
        DataError.Network.entries + DataError.Local.entries

    @Test
    fun anApostropheRendersAsAnApostrophe() = runTest {
        assertThat(getString(Res.string.error_not_found))
            .isEqualTo("We couldn't find what you were looking for.")
    }

    @Test
    fun noErrorMessageContainsAStrayEscape() = runTest {
        assertAll {
            allErrors.forEach { error ->
                val resource = (error.toUiText() as UiText.StringResourceText).id

                assertThat(getString(resource), name = error.toString()).all {
                    doesNotContain("\\")
                    // An unindexed placeholder is emitted literally: Compose
                    // Resources only substitutes %1$s / %1$d.
                    doesNotContain("%s")
                    doesNotContain("%d")
                }
            }
        }
    }
}
