package de.shinz.rickandmortyshowcase.core.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Builds the one `HttpClient` the app uses.
 *
 * The engine is a parameter rather than being chosen here: production passes the
 * platform engine from DI (OkHttp on Android, Darwin on iOS) and tests pass a
 * `MockEngine`, which is the only reason the data source is testable without a
 * network.
 *
 * No `Auth` plugin, unlike the skill's example — the API is public, so there is
 * no token to load or refresh.
 *
 * Logging defaults to [LogLevel.NONE] and the logger is a parameter, because
 * Ktor's `Logger.DEFAULT` is wrong on both of our platforms: on the JVM it goes
 * through slf4j, which has no binding declared here and so silently discards
 * every message, and on iOS it `println`s unconditionally — including in release
 * builds. DI decides what, if anything, gets logged.
 */
object HttpClientFactory {

    fun create(
        engine: HttpClientEngine,
        logLevel: LogLevel = LogLevel.NONE,
        logger: Logger = Logger.SIMPLE,
    ): HttpClient = HttpClient(engine) {
        // Load-bearing and easy to "tidy" into a bug: with expectSuccess = true
        // Ktor throws a ResponseException (an IllegalStateException) for every
        // non-2xx, which would collapse responseToResult's whole status table
        // into UNKNOWN without a single test failing.
        expectSuccess = false

        install(ContentNegotiation) {
            json(
                Json {
                    // The API can add fields without breaking us. Without this, a
                    // new key in the response is a hard deserialization failure.
                    ignoreUnknownKeys = true
                },
            )
        }

        install(HttpTimeout) {
            // Staggered on purpose. Equal values make the request and socket
            // timers race for the common "server accepted and went silent" case,
            // so which message the user sees becomes non-deterministic. The
            // request timeout is the outer bound.
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
            socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        }

        install(Logging) {
            this.logger = logger
            this.level = logLevel
        }
    }

    private const val CONNECT_TIMEOUT_MILLIS = 10_000L
    private const val SOCKET_TIMEOUT_MILLIS = 10_000L
    private const val REQUEST_TIMEOUT_MILLIS = 20_000L
}
