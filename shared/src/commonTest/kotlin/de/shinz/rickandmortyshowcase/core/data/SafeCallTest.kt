package de.shinz.rickandmortyshowcase.core.data

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import de.shinz.rickandmortyshowcase.core.data.character.CharacterPageDto
import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.Result
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test

/**
 * The exception half of `safeCall`, which the data-source tests cannot reach
 * because they only vary the *response*.
 *
 * Timeouts are testable in `commonTest` because their types live in Ktor. The
 * offline case is not: no common type names it, so it is covered per platform in
 * `androidHostTest` and `iosTest` against the exceptions the real engines throw.
 */
class SafeCallTest {

    @Test
    fun aRequestTimeoutIsATimeoutNotUnknown() = runTest {
        // HttpRequestTimeoutException extends IOException, not
        // SocketTimeoutException - so it needs its own branch or the 20s request
        // timeout silently reports "something went wrong".
        assertThat(errorFrom { request -> throw HttpRequestTimeoutException(request) })
            .isEqualTo(DataError.Network.REQUEST_TIMEOUT)
    }

    @Test
    fun aConnectTimeoutIsATimeoutNotUnknown() = runTest {
        // ConnectTimeoutException extends ConnectException, a sibling of
        // SocketTimeoutException rather than a subtype.
        assertThat(errorFrom { throw ConnectTimeoutException("connect timed out") })
            .isEqualTo(DataError.Network.REQUEST_TIMEOUT)
    }

    @Test
    fun anUnrecognisedFailureIsUnknown() = runTest {
        assertThat(errorFrom { throw IllegalStateException("something odd") })
            .isEqualTo(DataError.Network.UNKNOWN)
    }

    @Test
    fun cancellationIsRethrownRatherThanReportedAsAnError() = runTest {
        // Swallowing this would turn a cancelled scope into a spurious error and
        // leave the coroutine running. Reordering the catch branches so an
        // earlier one intercepts it would break exactly this.
        val client = clientThrowing { throw CancellationException("cancelled") }

        assertFailure {
            client.getResult<String, String>(route = "/character") { it }
        }.isInstanceOf<CancellationException>()
    }

    @Test
    fun aMapperRejectingAValueIsASerializationError() = runTest {
        val client = MockEngine {
            respond(
                content = """"ok"""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }.let { HttpClientFactory.create(it) }

        val result = client.getResult<String, String>(route = "/character") {
            require(false) { "mapper rejected it" }
            it
        }

        assertThat((result as Result.Error).error).isEqualTo(DataError.Network.SERIALIZATION)
    }

    @Test
    fun aTwoHundredWithTheWrongContentTypeIsASerializationError() = runTest {
        // A proxy's HTML error page behind a 200. The requested type has to be a
        // real DTO for this to be a meaningful probe: Ktor's default transformer
        // can produce a String from any body at all, so asking for String here
        // would succeed and prove nothing.
        val client = MockEngine {
            respond(
                content = "<html>gateway</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }.let { HttpClientFactory.create(it) }

        val result = client.getResult<CharacterPageDto, Int>(route = "/character") {
            it.results.size
        }

        assertThat((result as Result.Error).error).isEqualTo(DataError.Network.SERIALIZATION)
    }

    private fun clientThrowing(block: (HttpRequestData) -> Nothing) =
        HttpClientFactory.create(MockEngine { request -> block(request) })

    private suspend fun errorFrom(block: (HttpRequestData) -> Nothing): DataError.Network {
        val result = clientThrowing(block).getResult<String, String>(route = "/character") { it }

        return (result as Result.Error).error
    }
}
