package de.shinz.rickandmortyshowcase.core.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen

/**
 * Degrades to [fallback] on failure **without ending the flow**.
 *
 * The observe functions in this app have no error channel by contract — a read
 * failure is meant to show as "no favourites" or "follow the device" rather than
 * as something the user must act on. The obvious spelling of that,
 * `retry(n).catch { emit(fallback) }`, has a trap: **`catch` completes the
 * flow.** Paired with `stateIn(Eagerly)`, which subscribes once for a
 * ViewModel's whole life and never resubscribes, one burst of failures degrades
 * that screen permanently — until the process dies.
 *
 * That is not cosmetic where the fallback is also an *input to a decision*.
 * `CharacterListViewModel.onFavoriteClick` branches add-versus-remove on the
 * favourite ids; stuck at `emptySet()` it would show every heart unfilled,
 * make removal from Home unreachable — which `SPEC.md` requires — and turn each
 * tap into a re-upsert that re-stamps `addedAt` and silently reorders Favorites.
 *
 * So the fallback is emitted and then the upstream is **collected again**, after
 * a pause. [retries] still absorbs a transient blip immediately (zero is a legal
 * value, meaning degrade at once); the restart is
 * for the persistent case, and it is what lets a screen recover once the
 * database or the preference file does.
 *
 * `distinctUntilChanged` is part of the operator rather than left to callers:
 * a failure that persists re-emits the same fallback on every pass, and nothing
 * downstream should see that as news. It also absorbs Room re-running a query
 * because some *other* row in the table changed.
 */
internal fun <T> Flow<T>.degradeTo(
    fallback: T,
    retries: Long,
    restartDelayMillis: Long = DEGRADE_RESTART_DELAY_MILLIS,
): Flow<T> = flow {
    while (true) {
        var degraded = false

        this@degradeTo
            // `retryWhen`, not `retry`: the latter rejects a count of zero,
            // which makes "degrade immediately" unexpressible and turns a
            // boundary value into a crash.
            .retryWhen { _, attempt -> attempt < retries }
            .catch {
                degraded = true
                emit(fallback)
            }
            .collect { emit(it) }

        // The upstream finished on its own terms rather than by failing, so
        // there is nothing to restart. Room and DataStore flows never do this;
        // a test double might.
        if (!degraded) return@flow

        delay(restartDelayMillis)
    }
}.distinctUntilChanged()

/**
 * Long enough that a database that is genuinely broken is not re-opened in a
 * tight loop, short enough that recovery feels like the screen fixing itself.
 * `retries` already covers anything briefer.
 */
internal const val DEGRADE_RESTART_DELAY_MILLIS: Long = 5_000
