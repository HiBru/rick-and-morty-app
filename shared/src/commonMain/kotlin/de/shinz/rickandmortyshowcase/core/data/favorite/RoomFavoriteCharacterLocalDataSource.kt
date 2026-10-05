package de.shinz.rickandmortyshowcase.core.data.favorite

import de.shinz.rickandmortyshowcase.core.data.safeLocalCall
import de.shinz.rickandmortyshowcase.core.database.FavoriteCharacterDao
import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.EmptyResult
import de.shinz.rickandmortyshowcase.core.domain.Result
import de.shinz.rickandmortyshowcase.core.domain.asEmptyResult
import de.shinz.rickandmortyshowcase.core.domain.datasource.FavoriteCharacterLocalDataSource
import de.shinz.rickandmortyshowcase.core.domain.model.Character
import de.shinz.rickandmortyshowcase.core.data.degradeTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * The favourites table over Room.
 *
 * Named for what it wraps rather than suffixed `Impl`.
 *
 * @param clock supplies the favourited-at timestamp. Injected rather than read
 *   statically, so "now" is something a test can control.
 */
internal class RoomFavoriteCharacterLocalDataSource(
    private val dao: FavoriteCharacterDao,
    private val clock: Clock,
) : FavoriteCharacterLocalDataSource {

    /**
     * `catch` is required by the contract, not defensive habit: this `Flow` has
     * no error channel, so without it a storage failure or a malformed
     * `episodeUrlsJson` would reach the collector as a raw exception and crash
     * the screen. `SPEC.md` gives Favorites an empty state and no error state,
     * so degrading to "no favourites" is the specified behaviour.
     *
     * `retry` comes first because `catch` **completes** the flow after emitting
     * its fallback. The ViewModel collects this once under
     * `stateIn(…, Eagerly)`, so without a retry a single transient read failure
     * would leave Favorites empty for the rest of the process — and the screen
     * has no refresh affordance to recover with.
     *
     * `flowOn` is not cosmetic either. Room moves only the *query* to its own
     * context and preserves flow context downstream, so without this the JSON
     * decode and enum scan for every favourite would run on the main thread.
     */
    override fun observeFavorites(): Flow<List<Character>> = dao.observeAll()
        .map { entities -> entities.map { it.toCharacter() } }
        .flowOn(Dispatchers.IO)
        .degradeTo(fallback = emptyList(), retries = TRANSIENT_RETRIES)

    /**
     * `distinctUntilChanged` is required rather than optional, and it comes from
     * [degradeTo]. Room invalidates per *table*, so every write to
     * `favorite_characters` re-runs even this id-only query — without the filter
     * the whole character list would recompose each time any unrelated
     * favourite changed.
     *
     * **This is the flow whose permanent degradation would be a behaviour
     * change rather than an empty screen**: the list's heart branches on it, so
     * a terminated leg makes removal from Home unreachable. `degradeTo` is why
     * it recovers.
     */
    override fun observeFavoriteIds(): Flow<Set<Int>> = dao.observeIds()
        .map { it.toSet() }
        .degradeTo(fallback = emptySet(), retries = TRANSIENT_RETRIES)

    override suspend fun getFavorite(id: Int): Result<Character?, DataError.Local> =
        safeLocalCall { dao.getById(id)?.toCharacter() }

    override suspend fun addFavorite(character: Character): EmptyResult<DataError.Local> =
        safeLocalCall { dao.upsert(character.toFavoriteEntity(addedAt = clock.now())) }
            .asEmptyResult()

    override suspend fun removeFavorite(id: Int): EmptyResult<DataError.Local> =
        safeLocalCall { dao.deleteById(id) }.asEmptyResult()

    private companion object {
        /** Enough to ride out lock contention or a momentary IO failure. */
        const val TRANSIENT_RETRIES = 2L
    }
}
