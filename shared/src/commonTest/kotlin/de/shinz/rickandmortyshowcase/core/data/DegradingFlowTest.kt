package de.shinz.rickandmortyshowcase.core.data

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * The operator the observe functions degrade through.
 *
 * Its reason for existing is the half nothing used to test: `catch` **completes**
 * a flow, and under `stateIn(Eagerly)` — which this app pins — a completed
 * upstream is never resubscribed, so one burst of read failures degraded a
 * screen until the process died. On the character list that was not an empty
 * screen but a behaviour change: removal became unreachable.
 */
class DegradingFlowTest {

    @Test
    fun passesValuesThroughWhenNothingFails() = runTest {
        flowOf(1, 2, 3).degradeTo(fallback = 0, retries = 2).test {
            assertThat(awaitItem()).isEqualTo(1)
            assertThat(awaitItem()).isEqualTo(2)
            assertThat(awaitItem()).isEqualTo(3)
            awaitComplete()
        }
    }

    /** A flow that ends on its own terms is not restarted. */
    @Test
    fun completesWhenTheUpstreamCompletesWithoutFailing() = runTest {
        flowOf(1).degradeTo(fallback = 0, retries = 0).test {
            assertThat(awaitItem()).isEqualTo(1)
            awaitComplete()
        }
    }

    @Test
    fun emitsTheFallbackOnFailureAndStaysAlive() = runTest {
        val failing = flow<Int> { throw IllegalStateException("unreadable") }

        failing.degradeTo(fallback = 0, retries = 0).test {
            assertThat(awaitItem()).isEqualTo(0)
            // The whole point: not `awaitComplete()`.
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * The behaviour the operator exists for. A source that fails and then
     * recovers must reach the collector again — with `catch` alone it never
     * could, because the flow had already ended.
     */
    @Test
    fun recoversOnceTheSourceDoes() = runTest {
        var attempt = 0
        val flaky = flow {
            attempt++
            if (attempt == 1) throw IllegalStateException("database is closed")
            emit(42)
        }

        flaky.degradeTo(fallback = 0, retries = 0, restartDelayMillis = 1_000).test {
            assertThat(awaitItem()).isEqualTo(0)

            advanceTimeBy(1_100)

            assertThat(awaitItem()).isEqualTo(42)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * A failure that persists re-emits the same fallback on every pass, and
     * nothing downstream should see that as news — so the operator carries
     * `distinctUntilChanged` rather than leaving it to each call site.
     */
    @Test
    fun aPersistentFailureDoesNotRepeatItsFallback() = runTest {
        val alwaysFailing = flow<Int> { throw IllegalStateException("unreadable") }

        alwaysFailing.degradeTo(fallback = 0, retries = 0, restartDelayMillis = 100).test {
            assertThat(awaitItem()).isEqualTo(0)

            advanceTimeBy(1_000)

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
