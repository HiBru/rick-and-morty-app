package de.shinz.rickandmortyshowcase.core.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.Result
import io.ktor.client.engine.darwin.DarwinHttpRequestException
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSError
import platform.Foundation.NSURLErrorBadURL
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorNotConnectedToInternet
import kotlin.test.Test

/**
 * The offline path on Darwin.
 *
 * `ktor-client-darwin` maps only `NSURLErrorTimedOut`; every other `NSError` is
 * wrapped in a [DarwinHttpRequestException], so the cause has to be read off the
 * `NSURLError` code. Before this was classified, airplane mode on iOS reported
 * `UNKNOWN`.
 */
class NetworkErrorClassifierIosTest {

    @Test
    fun notConnectedToInternetIsNoInternet() {
        assertThat(darwinError(NSURLErrorNotConnectedToInternet).asNetworkErrorOrNull())
            .isEqualTo(DataError.Network.NO_INTERNET)
    }

    @Test
    fun anUnrelatedUrlErrorIsNotClassified() {
        // A malformed url is a bug, not a connectivity problem, so it must fall
        // through to UNKNOWN rather than telling the user to check their signal.
        assertThat(darwinError(NSURLErrorBadURL).asNetworkErrorOrNull()).isNull()
    }

    @Test
    fun anExceptionThatIsNotADarwinFailureIsNotClassified() {
        assertThat(IllegalStateException("bug").asNetworkErrorOrNull()).isNull()
    }

    @Test
    fun safeCallReportsAnOfflineFailureAsNoInternetEndToEnd() = runTest {
        val client = HttpClientFactory.create(
            MockEngine { throw darwinError(NSURLErrorNotConnectedToInternet) },
        )

        val result = client.getResult<String, String>(route = "/character") { it }

        assertThat((result as Result.Error).error).isEqualTo(DataError.Network.NO_INTERNET)
    }

    private fun darwinError(code: Long) = DarwinHttpRequestException(
        origin = NSError.errorWithDomain(
            domain = NSURLErrorDomain,
            code = code,
            userInfo = null,
        ),
    )
}
