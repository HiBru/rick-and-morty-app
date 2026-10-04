package de.shinz.rickandmortyshowcase.core.data

import de.shinz.rickandmortyshowcase.core.domain.DataError
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.PortUnreachableException
import java.net.UnknownHostException

/**
 * OkHttp's offline shapes.
 *
 * `ktor-client-okhttp` maps only `SocketTimeoutException`; everything else is
 * rethrown as the raw `java.net` exception, so these are what actually arrive.
 * `UnknownHostException` is the airplane-mode and DNS-failure case.
 */
internal actual fun Throwable.asNetworkErrorOrNull(): DataError.Network? = when (this) {
    is UnknownHostException,
    is ConnectException,
    is NoRouteToHostException,
    is PortUnreachableException,
    -> DataError.Network.NO_INTERNET

    else -> null
}
