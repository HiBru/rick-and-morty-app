package de.shinz.rickandmortyshowcase.core.data.character

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import de.shinz.rickandmortyshowcase.core.data.HttpClientFactory
import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.Result
import de.shinz.rickandmortyshowcase.core.domain.model.CharacterStatus
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * Drives the real data source over a `MockEngine`, which is the whole reason
 * `HttpClientFactory.create` takes an engine instead of choosing one.
 *
 * These cover the status-code table and the request the data source actually
 * builds — the parts no mapper test can reach.
 */
class KtorCharacterRemoteDataSourceTest {

    @Test
    fun fetchCharacterPageParsesAPageAndSendsThePageParameter() = runTest {
        var requestedUrl: String? = null
        val dataSource = dataSourceReturning(PAGE_JSON) { requestedUrl = it }

        val result = dataSource.fetchCharacterPage(page = 2)

        val page = (result as Result.Success).data
        assertThat(page.characters.map { it.name }).isEqualTo(listOf("Rick Sanchez"))
        assertThat(page.characters.single().status).isEqualTo(CharacterStatus.ALIVE)
        assertThat(page.hasMore).isTrue()
        // The route is prefixed with the base url and the page arrives as a query
        // parameter, not baked into the path.
        assertThat(requestedUrl)
            .isEqualTo("https://rickandmortyapi.com/api/character?page=2")
    }

    @Test
    fun fetchCharacterPutsTheIdInThePath() = runTest {
        var requestedUrl: String? = null
        val dataSource = dataSourceReturning(CHARACTER_JSON) { requestedUrl = it }

        val result = dataSource.fetchCharacter(id = 1)

        assertThat((result as Result.Success).data.name).isEqualTo("Rick Sanchez")
        assertThat(requestedUrl).isEqualTo("https://rickandmortyapi.com/api/character/1")
    }

    @Test
    fun mapsEachStatusCodeToItsError() = runTest {
        // One case per branch of responseToResult, including the fall-through.
        assertThat(errorFor(HttpStatusCode.NotFound)).isEqualTo(DataError.Network.NOT_FOUND)
        assertThat(errorFor(HttpStatusCode.RequestTimeout))
            .isEqualTo(DataError.Network.REQUEST_TIMEOUT)
        assertThat(errorFor(HttpStatusCode.TooManyRequests))
            .isEqualTo(DataError.Network.TOO_MANY_REQUESTS)
        assertThat(errorFor(HttpStatusCode.InternalServerError))
            .isEqualTo(DataError.Network.SERVER_ERROR)
        assertThat(errorFor(HttpStatusCode.ServiceUnavailable))
            .isEqualTo(DataError.Network.SERVER_ERROR)
        // Unauthorized has no branch of its own — the API never produces it.
        assertThat(errorFor(HttpStatusCode.Unauthorized)).isEqualTo(DataError.Network.UNKNOWN)
    }

    @Test
    fun fetchCharacterReportsAMissingIdAsNotFound() = runTest {
        // The detail screen's case: /character/9999 really does 404.
        val engine = MockEngine { respondError(HttpStatusCode.NotFound) }
        val dataSource = KtorCharacterRemoteDataSource(HttpClientFactory.create(engine))

        val result = dataSource.fetchCharacter(id = 9999)

        assertThat((result as Result.Error).error).isEqualTo(DataError.Network.NOT_FOUND)
    }

    @Test
    fun malformedJsonIsASerializationError() = runTest {
        val dataSource = dataSourceReturning("""{"info":{},"results":[{"id":"not-a-number"}]}""")

        val result = dataSource.fetchCharacterPage(page = 1)

        assertThat((result as Result.Error).error).isEqualTo(DataError.Network.SERIALIZATION)
    }

    @Test
    fun anUnparseableTimestampIsASerializationErrorRatherThanAThrow() = runTest {
        // The mapper's Instant.parse runs inside safeCall, so a timestamp the API
        // should never send degrades instead of escaping as an exception.
        val dataSource = dataSourceReturning(CHARACTER_JSON.replace("2017-11-04T18:48:46.250Z", "yesterday"))

        val result = dataSource.fetchCharacter(id = 1)

        assertThat((result as Result.Error).error).isEqualTo(DataError.Network.SERIALIZATION)
    }

    private fun dataSourceReturning(
        json: String,
        onRequest: (String) -> Unit = {},
    ): KtorCharacterRemoteDataSource {
        val engine = MockEngine { request ->
            onRequest(request.url.toString())
            respond(
                content = json,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return KtorCharacterRemoteDataSource(HttpClientFactory.create(engine))
    }

    private suspend fun errorFor(status: HttpStatusCode): DataError.Network {
        val engine = MockEngine { respondError(status) }
        val dataSource = KtorCharacterRemoteDataSource(HttpClientFactory.create(engine))

        return (dataSource.fetchCharacterPage(page = 1) as Result.Error).error
    }

    private companion object {
        val CHARACTER_JSON = """
            {
              "id": 1,
              "name": "Rick Sanchez",
              "status": "Alive",
              "species": "Human",
              "type": "",
              "gender": "Male",
              "origin": { "name": "Earth (C-137)", "url": "https://rickandmortyapi.com/api/location/1" },
              "location": { "name": "Citadel of Ricks", "url": "https://rickandmortyapi.com/api/location/3" },
              "image": "https://rickandmortyapi.com/api/character/avatar/1.jpeg",
              "episode": ["https://rickandmortyapi.com/api/episode/1"],
              "url": "https://rickandmortyapi.com/api/character/1",
              "created": "2017-11-04T18:48:46.250Z"
            }
        """.trimIndent()

        val PAGE_JSON = """
            {
              "info": {
                "count": 826,
                "pages": 42,
                "next": "https://rickandmortyapi.com/api/character?page=3",
                "prev": "https://rickandmortyapi.com/api/character?page=1"
              },
              "results": [$CHARACTER_JSON]
            }
        """.trimIndent()
    }
}
