package de.shinz.rickandmortyshowcase.core.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.Result
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException

/**
 * The offline path on OkHttp.
 *
 * `ktor-client-okhttp` maps only `SocketTimeoutException` and rethrows everything
 * else as the raw `java.net` exception, so these are the types that actually
 * arrive when the device has no connection. Before this was classified, airplane
 * mode reported `UNKNOWN` and `error_no_internet` was unreachable.
 */
class NetworkErrorClassifierAndroidTest {

    @Test
    fun `an unknown host is no internet`() {
        // Airplane mode and DNS failure both land here.
        assertThat(UnknownHostException("rickandmortyapi.com").asNetworkErrorOrNull())
            .isEqualTo(DataError.Network.NO_INTERNET)
    }

    @Test
    fun `a refused connection is no internet`() {
        assertThat(ConnectException("Connection refused").asNetworkErrorOrNull())
            .isEqualTo(DataError.Network.NO_INTERNET)
    }

    @Test
    fun `an unrelated failure is not classified`() {
        // Returning null is what lets safeCall fall back to UNKNOWN rather than
        // calling every IO problem a connectivity problem.
        assertThat(IOException("stream closed").asNetworkErrorOrNull()).isNull()
        assertThat(IllegalStateException("bug").asNetworkErrorOrNull()).isNull()
    }

    @Test
    fun `safeCall reports an offline failure as no internet end to end`() = runTest {
        val client = HttpClientFactory.create(
            MockEngine { throw UnknownHostException("rickandmortyapi.com") },
        )

        val result = client.getResult<String, String>(route = "/character") { it }

        assertThat((result as Result.Error).error).isEqualTo(DataError.Network.NO_INTERNET)
    }
}
