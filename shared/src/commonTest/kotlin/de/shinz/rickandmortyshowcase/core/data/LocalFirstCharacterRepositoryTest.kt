package de.shinz.rickandmortyshowcase.core.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.Result
import de.shinz.rickandmortyshowcase.core.domain.datasource.FakeCharacterRemoteDataSource
import de.shinz.rickandmortyshowcase.core.domain.datasource.FakeFavoriteCharacterLocalDataSource
import de.shinz.rickandmortyshowcase.core.domain.model.Character
import de.shinz.rickandmortyshowcase.core.domain.model.CharacterStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Instant

/**
 * All three branches of the local-first read, plus the thing that makes it worth
 * having: a favourited character must resolve without touching the network.
 */
class LocalFirstCharacterRepositoryTest {

    private val local = FakeFavoriteCharacterLocalDataSource()
    private val remote = FakeCharacterRemoteDataSource()
    private val repository = LocalFirstCharacterRepository(local, remote)

    @Test
    fun aFavouritedCharacterComesFromStorageEvenWhenTheApiHasItToo() = runTest {
        // Registering a *different* value under the same id states the
        // preference rather than inferring it from an unavailable remote.
        local.seed(MORTY)
        remote.characters[MORTY.id] = MORTY.copy(name = "stale from the API")

        val result = repository.getCharacter(MORTY.id)

        assertThat(result).isEqualTo(Result.Success(MORTY))
    }

    @Test
    fun aFavouritedCharacterNeverReachesTheNetwork() = runTest {
        // The offline guarantee in SPEC.md: opening a favourite with no
        // connection has to work. Asserting the returned value alone would pass
        // even if the repository hit the API first and ignored the answer.
        local.seed(MORTY)
        remote.error = DataError.Network.NO_INTERNET

        val result = repository.getCharacter(MORTY.id)

        assertThat(result).isEqualTo(Result.Success(MORTY))
        assertThat(remote.fetchCharacterCallCount).isEqualTo(0)
    }

    @Test
    fun aCharacterThatIsNotFavouritedComesFromTheApi() = runTest {
        remote.characters[RICK.id] = RICK

        val result = repository.getCharacter(RICK.id)

        assertThat(result).isEqualTo(Result.Success(RICK))
        assertThat(remote.fetchCharacterCallCount).isEqualTo(1)
    }

    @Test
    fun aLocalFailureFallsThroughToTheApiRatherThanFailing() = runTest {
        // A corrupt favourites row must not make a character unviewable while
        // the API can serve it. This is the branch that decision lives in.
        local.readError = DataError.Local.UNKNOWN
        remote.characters[RICK.id] = RICK

        val result = repository.getCharacter(RICK.id)

        assertThat(result).isEqualTo(Result.Success(RICK))
        assertThat(remote.fetchCharacterCallCount).isEqualTo(1)
    }

    @Test
    fun theNetworkErrorSurfacesWhenTheCharacterIsNotStoredEither() = runTest {
        remote.error = DataError.Network.NO_INTERNET

        val result = repository.getCharacter(RICK.id)

        assertThat(result).isEqualTo(Result.Error(DataError.Network.NO_INTERNET))
    }

    @Test
    fun theNetworkErrorSurfacesWhenBothSourcesFail() = runTest {
        // Offline *and* an unreadable row. The network error is the one the user
        // can act on, so it is the one that wins.
        local.readError = DataError.Local.UNKNOWN
        remote.error = DataError.Network.NO_INTERNET

        val result = repository.getCharacter(RICK.id)

        assertThat(result).isEqualTo(Result.Error(DataError.Network.NO_INTERNET))
    }

    @Test
    fun aTransientStorageFailureIsRetriedRatherThanFallingThroughToTheNetwork() = runTest {
        // A lock from a concurrent favourite write, not a corrupt row. Falling
        // through here would cost the offline guarantee for a readable row.
        local.seed(MORTY)
        local.transientReadFailures = 1
        remote.error = DataError.Network.NO_INTERNET

        val result = repository.getCharacter(MORTY.id)

        assertThat(result).isEqualTo(Result.Success(MORTY))
        assertThat(remote.fetchCharacterCallCount).isEqualTo(0)
    }

    @Test
    fun storageIsConsultedExactlyOncePerRead() = runTest {
        remote.characters[RICK.id] = RICK

        repository.getCharacter(RICK.id)

        assertThat(local.getFavoriteCallCount).isEqualTo(1)
    }

    private companion object {
        val MORTY = Character(
            id = 2,
            name = "Morty Smith",
            status = CharacterStatus.ALIVE,
            species = "Human",
            type = "",
            gender = "Male",
            originName = "unknown",
            originUrl = "",
            locationName = "Citadel of Ricks",
            locationUrl = "loc/3",
            imageUrl = "avatar/2.jpeg",
            episodeUrls = listOf("ep/1"),
            url = "character/2",
            created = Instant.parse("2017-11-04T18:50:21.651Z"),
        )

        val RICK = MORTY.copy(id = 1, name = "Rick Sanchez")
    }
}
