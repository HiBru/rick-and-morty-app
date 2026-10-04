package de.shinz.rickandmortyshowcase.core.data

import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.ContentConvertException
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.serialization.SerializationException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Turns the exceptions a network call can throw into a [DataError.Network].
 *
 * Unlike the skill's version the block returns the finished [Result] rather than
 * an `HttpResponse`, so deserialization happens inside the protected region too —
 * `body<T>()` can fail on a response that arrived perfectly well, and "the
 * response did not match what we expected" is [DataError.Network.SERIALIZATION]
 * rather than an exception for a caller to discover at runtime.
 *
 * **The three timeout types are siblings, not subtypes**, and each needs its own
 * branch: `HttpRequestTimeoutException` comes from the `HttpTimeout` plugin and
 * extends `IOException`; `ConnectTimeoutException` extends `ConnectException`;
 * only `SocketTimeoutException` is what OkHttp's own mapping produces. Miss any
 * one and a timeout is reported as `UNKNOWN`.
 *
 * The `CancellationException` rethrow is not optional. Cancellation travels as an
 * exception, so swallowing it would turn a cancelled scope into a spurious error
 * and keep the coroutine running. It is an `IllegalStateException` on both
 * platforms, so it reaches the final branch rather than an earlier one.
 */
suspend fun <T> safeCall(
    block: suspend () -> Result<T, DataError.Network>,
): Result<T, DataError.Network> = try {
    block()
} catch (e: HttpRequestTimeoutException) {
    Result.Error(DataError.Network.REQUEST_TIMEOUT)
} catch (e: ConnectTimeoutException) {
    Result.Error(DataError.Network.REQUEST_TIMEOUT)
} catch (e: SocketTimeoutException) {
    Result.Error(DataError.Network.REQUEST_TIMEOUT)
} catch (e: UnresolvedAddressException) {
    // Only CIO/ktor-network throws this. Kept because it costs nothing and would
    // cover a future engine swap; the engines we ship are handled below.
    Result.Error(DataError.Network.NO_INTERNET)
} catch (e: SerializationException) {
    Result.Error(DataError.Network.SERIALIZATION)
} catch (e: ContentConvertException) {
    // Ktor's ContentNegotiation does not let a kotlinx SerializationException
    // through — it wraps it in JsonConvertException, a ContentConvertException,
    // which is not a SerializationException at all. Without this branch every
    // malformed response reports UNKNOWN.
    Result.Error(DataError.Network.SERIALIZATION)
} catch (e: Exception) {
    if (e is CancellationException) throw e
    // The offline case has no common type: OkHttp raises UnknownHostException,
    // Darwin wraps an NSURLError. See NetworkErrorClassifier.
    Result.Error(e.asNetworkErrorOrNull() ?: DataError.Network.UNKNOWN)
}

/**
 * Maps an HTTP status onto the error vocabulary, deserializing on success.
 *
 * Reachable only because `expectSuccess` is left `false` on the client — see
 * [HttpClientFactory]. Codes this origin does not produce fall through to
 * [DataError.Network.UNKNOWN] rather than getting a branch that cannot be hit.
 */
suspend inline fun <reified T> responseToResult(
    response: HttpResponse,
): Result<T, DataError.Network> = when (response.status.value) {
    in 200..299 -> try {
        Result.Success(response.body<T>())
    } catch (e: NoTransformationFoundException) {
        // A 2xx whose body is not what we asked for — an HTML error page from a
        // proxy, most often. Same class of problem as malformed JSON.
        Result.Error(DataError.Network.SERIALIZATION)
    }

    404 -> Result.Error(DataError.Network.NOT_FOUND)
    408 -> Result.Error(DataError.Network.REQUEST_TIMEOUT)
    429 -> Result.Error(DataError.Network.TOO_MANY_REQUESTS)
    in 500..599 -> Result.Error(DataError.Network.SERVER_ERROR)
    else -> Result.Error(DataError.Network.UNKNOWN)
}

/**
 * A GET that answers with a domain value or a [DataError.Network].
 *
 * Named `getResult` rather than the skill's `get`: an extension called `get` on
 * `HttpClient` sits beside Ktor's own `get(urlString)` and makes every call site
 * depend on named arguments to resolve.
 *
 * @param transform maps the DTO to the domain model. It is wrapped in its own
 *   `IllegalArgumentException` catch rather than relying on [safeCall], so that a
 *   value the mapper rejects — `Instant.parse` on a timestamp the API should
 *   never have sent — is reported as `SERIALIZATION` *without* also labelling a
 *   stray `require(...)` elsewhere in the request pipeline as a response problem.
 */
suspend inline fun <reified Dto, Domain> HttpClient.getResult(
    route: String,
    parameters: Map<String, Any?> = emptyMap(),
    crossinline transform: (Dto) -> Domain,
): Result<Domain, DataError.Network> = safeCall {
    val response = get(constructRoute(route)) {
        // `parameter` already skips a null value.
        parameters.forEach { (key, value) -> parameter(key, value) }
    }

    when (val dto = responseToResult<Dto>(response)) {
        is Result.Success -> try {
            Result.Success(transform(dto.data))
        } catch (e: IllegalArgumentException) {
            Result.Error(DataError.Network.SERIALIZATION)
        }

        is Result.Error -> dto
    }
}
