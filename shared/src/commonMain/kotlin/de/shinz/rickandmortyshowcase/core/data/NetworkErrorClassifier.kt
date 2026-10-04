package de.shinz.rickandmortyshowcase.core.data

import de.shinz.rickandmortyshowcase.core.domain.DataError

/**
 * Recognises "the device could not reach the server" from a platform exception.
 *
 * This has to be a platform seam because **no type in `commonMain` names the
 * offline case for the engines this app ships.** `UnresolvedAddressException`
 * looks like it should, but it is thrown only by CIO/`ktor-network`: OkHttp
 * raises `UnknownHostException` or `ConnectException`, and Darwin wraps every
 * non-timeout failure in `DarwinHttpRequestException` carrying an `NSURLError`
 * code. Without this, airplane mode — the failure users hit most — reports
 * `UNKNOWN` and [DataError.Network.NO_INTERNET] is never produced at all.
 *
 * @return [DataError.Network.NO_INTERNET] when the throwable means the server was
 *   unreachable, or `null` to let the caller fall back to `UNKNOWN`.
 */
internal expect fun Throwable.asNetworkErrorOrNull(): DataError.Network?
