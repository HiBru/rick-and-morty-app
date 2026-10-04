package de.shinz.rickandmortyshowcase.core.data

import de.shinz.rickandmortyshowcase.core.domain.DataError
import io.ktor.client.engine.darwin.DarwinHttpRequestException
import platform.Foundation.NSURLErrorCannotConnectToHost
import platform.Foundation.NSURLErrorCannotFindHost
import platform.Foundation.NSURLErrorDNSLookupFailed
import platform.Foundation.NSURLErrorDataNotAllowed
import platform.Foundation.NSURLErrorNetworkConnectionLost
import platform.Foundation.NSURLErrorNotConnectedToInternet

/**
 * Darwin's offline shapes.
 *
 * `ktor-client-darwin` maps only `NSURLErrorTimedOut` to a
 * `SocketTimeoutException`; every other `NSError` is wrapped in a
 * [DarwinHttpRequestException], so the real cause has to be read off its
 * `NSURLError` code. The constants come from `platform.Foundation` rather than
 * being written as literals.
 */
internal actual fun Throwable.asNetworkErrorOrNull(): DataError.Network? =
    (this as? DarwinHttpRequestException)?.origin?.code?.let { code ->
        when (code) {
            NSURLErrorNotConnectedToInternet,
            NSURLErrorCannotFindHost,
            NSURLErrorCannotConnectToHost,
            NSURLErrorNetworkConnectionLost,
            NSURLErrorDNSLookupFailed,
            NSURLErrorDataNotAllowed,
            -> DataError.Network.NO_INTERNET

            else -> null
        }
    }
