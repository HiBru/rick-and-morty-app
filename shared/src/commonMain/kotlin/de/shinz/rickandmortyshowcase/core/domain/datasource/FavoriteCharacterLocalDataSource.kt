package de.shinz.rickandmortyshowcase.core.domain.datasource

import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.EmptyResult
import de.shinz.rickandmortyshowcase.core.domain.Result
import de.shinz.rickandmortyshowcase.core.domain.model.Character
import kotlinx.coroutines.flow.Flow

/**
 * The favourites database. One source, so where these report failure at all the
 * error type stays narrowed to [DataError.Local].
 *
 * The reads are `Flow`s and not suspend functions: favouriting on one screen has
 * to show up on the others without a refresh, which only an observable source
 * gives for free.
 *
 * **The two observe functions deliberately have no error channel.** `SPEC.md`
 * gives the Favorites screen an empty state but no error state, so a read
 * failure degrades to "no favourites" rather than becoming something the user
 * has to act on. That is a contract, not an oversight: the implementation must
 * absorb failure with `catch` so nothing reaches a collector as a raw exception.
 */
interface FavoriteCharacterLocalDataSource {

    /**
     * Every favourite, most recently favourited first.
     *
     * That ordering comes from a column the database adds when the row is
     * written, not from [Character.created] — which is the API's own record
     * timestamp and has nothing to do with when the user favourited anything.
     * The domain model therefore cannot express this order, and does not need to.
     */
    fun observeFavorites(): Flow<List<Character>>

    /**
     * Just the ids, for marking rows on the list and detail screens.
     *
     * A `Set` for O(1) membership, and narrow because it is the whole point:
     * Room invalidates per *table*, so even a `SELECT id` query re-emits whenever
     * any favourite changes. What makes this cheap is that a set of ints is
     * trivially comparable, so the implementation can drop the duplicate
     * emissions with `distinctUntilChanged()` — which it must, or the list
     * recomposes on every unrelated write.
     */
    fun observeFavoriteIds(): Flow<Set<Int>>

    /**
     * The locally stored character, or `null` if it is not favourited.
     *
     * Absence is the normal case, not a failure — most characters are not
     * favourites — so it is `null` rather than a `DataError.Local.NOT_FOUND`.
     * That keeps `Result.Error` meaning "something actually went wrong", which is
     * what lets the detail screen treat a miss as "fetch it from the API" instead
     * of as an error to show.
     */
    suspend fun getFavorite(id: Int): Result<Character?, DataError.Local>

    suspend fun addFavorite(character: Character): EmptyResult<DataError.Local>

    /** A no-op if the character was not favourited. */
    suspend fun removeFavorite(id: Int): EmptyResult<DataError.Local>
}
