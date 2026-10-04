package de.shinz.rickandmortyshowcase

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Instant

/**
 * Proves the multiplatform test toolchain actually executes — under JUnit Platform on
 * the JVM and under the native runner on iOS — and that AssertK, Turbine and
 * coroutines-test resolve for both.
 *
 * This exists because three of the wirings in Task 1 fail *silently*: a build that
 * reports success is not evidence that any test ran. Delete once real tests cover
 * these source sets.
 */
class ToolchainCommonTest {

    @Test
    fun assertkResolvesInCommonTest() {
        assertThat(1 + 1).isEqualTo(2)
    }

    /**
     * Settles whether this project needs a `kotlinx-datetime` dependency: it does not,
     * as long as the stdlib can parse the API's ISO-8601 `created` field on both
     * platforms. See the dependency table in docs/IMPLEMENTATION_PLAN.md.
     */
    @Test
    fun stdlibInstantParsesTheApiTimestampFormat() {
        // An absolute value, not a delta between two parsed instants: a delta still
        // passes if the fractional second is dropped, or if both are mis-parsed the
        // same way. This pins date, time and milliseconds in one assertion.
        val created = Instant.parse("2017-11-04T18:48:46.250Z")

        assertThat(created.toEpochMilliseconds()).isEqualTo(1_509_821_326_250L)
    }

    @Test
    fun turbineAndCoroutinesTestResolveInCommonTest() = runTest {
        flowOf("a", "b").test {
            assertThat(awaitItem()).isEqualTo("a")
            assertThat(awaitItem()).isEqualTo("b")
            awaitComplete()
        }
    }
}
