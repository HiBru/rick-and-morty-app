package de.shinz.rickandmortyshowcase.core.data

import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.Result
import de.shinz.rickandmortyshowcase.core.domain.datasource.CharacterRemoteDataSource
import de.shinz.rickandmortyshowcase.core.domain.datasource.FavoriteCharacterLocalDataSource
import de.shinz.rickandmortyshowcase.core.domain.model.Character
import de.shinz.rickandmortyshowcase.core.domain.repository.CharacterRepository

/**
 * Resolves a character from the favourites table first, then the API.
 *
 * Named for how it behaves rather than suffixed `Impl`. It is the only class in
 * this app that earns the word "repository": it coordinates two sources, which
 * is what the detail screen needs given it is handed nothing but an id.
 */
internal class LocalFirstCharacterRepository(
    private val localDataSource: FavoriteCharacterLocalDataSource,
    private val remoteDataSource: CharacterRemoteDataSource,
) : CharacterRepository {

    /**
     * Three outcomes, spelled out rather than collapsed.
     *
     * An exhaustive `when` instead of a `dataOrNull()`-style null-collapse,
     * because the two non-happy branches mean different things and the middle one
     * is not a failure at all: a character that is not favourited is the normal
     * case for 800-odd of them.
     *
     * **A local *failure* also falls through to the network.** The favourites
     * table is an offline cache for reading a character, not the authority on
     * whether it exists — so a corrupt `episodeUrlsJson` row must not make a
     * character unviewable while the API is sitting there able to serve it. The
     * cost is that when the device is both offline *and* the row is unreadable,
     * the user sees the network error rather than the storage one; that is the
     * more actionable of the two anyway.
     *
     * The fall-through is **silent**, and that is a real limitation: a
     * permanently corrupt row means every open of that favourite does a network
     * round trip forever, and the user is never told their saved-for-offline
     * character is not actually readable. There is no logging seam to surface it
     * through yet.
     */
    override suspend fun getCharacter(id: Int): Result<Character, DataError> =
        when (val local = readLocal(id)) {
            is Result.Success -> local.data
                ?.let { Result.Success(it) }
                ?: remoteDataSource.fetchCharacter(id)

            is Result.Error -> remoteDataSource.fetchCharacter(id)
        }

    /**
     * Retries before giving up on storage, so the fall-through above only ever
     * handles a *permanent* failure.
     *
     * Without this, a `SQLITE_BUSY` from a concurrent write — favouriting on Home
     * while Detail opens, which `SPEC.md` requires to work — is indistinguishable
     * from a corrupt row and costs the offline guarantee for a row that is
     * perfectly fine and would have read on a second attempt. Same bound as the
     * observe chains in `RoomFavoriteCharacterLocalDataSource`.
     */
    private suspend fun readLocal(id: Int): Result<Character?, DataError.Local> {
        var result = localDataSource.getFavorite(id)
        repeat(TRANSIENT_RETRIES) {
            if (result is Result.Success) return result
            result = localDataSource.getFavorite(id)
        }
        return result
    }

    private companion object {
        const val TRANSIENT_RETRIES = 2
    }
}
