package de.shinz.rickandmortyshowcase.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Collects one-time events — navigation, a snackbar — from a ViewModel's event
 * `Flow`.
 *
 * Call it in a `Root` composable only, never in a `Screen`: the Root is where the
 * ViewModel and the navigation callbacks live.
 *
 * Three details carry the weight.
 *
 * `repeatOnLifecycle(STARTED)` stops collecting while the screen is not visible.
 * With the `Channel` + `receiveAsFlow()` the ViewModels use, that **defers**
 * events rather than dropping them: `send` suspends until a collector resumes,
 * and the next `repeatOnLifecycle` pass delivers what queued up. Under
 * `navigation-compose` the lifecycle owner is the `NavBackStackEntry`, on iOS as
 * well as Android, so a covered destination's events fire when the user returns
 * to it. That only holds if the emitter uses `send` on a buffered channel —
 * `trySend` discards silently when nothing is collecting.
 *
 * `Dispatchers.Main.immediate` means an event emitted on the main thread is
 * handled in the same frame instead of being posted, which keeps a navigation
 * event from racing a recomposition.
 *
 * `rememberUpdatedState` keeps [onEvent] current. The effect is deliberately
 * *not* keyed on it — re-keying would restart the collection on every
 * recomposition — so without this the lambda captured at launch would be the one
 * used for the screen's whole life, and any state it closed over would be frozen
 * at that frame.
 *
 * No skill defines this; it is modelled on the same author's production
 * implementation. See `docs/IMPLEMENTATION_PLAN.md`.
 *
 * @param key1 forces the collection to restart when something other than [flow]
 *   or the lifecycle owner changes. Rarely needed now that [onEvent] refreshes
 *   itself.
 */
@Composable
fun <T> ObserveAsEvents(
    flow: Flow<T>,
    key1: Any? = Unit,
    onEvent: (T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEvent by rememberUpdatedState(onEvent)

    LaunchedEffect(flow, lifecycleOwner, key1) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) {
                flow.collect { currentOnEvent(it) }
            }
        }
    }
}
