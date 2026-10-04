package de.shinz.rickandmortyshowcase.core.ui

import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.generated.resources.Res
import de.shinz.rickandmortyshowcase.generated.resources.error_disk_full
import de.shinz.rickandmortyshowcase.generated.resources.error_local_unknown
import de.shinz.rickandmortyshowcase.generated.resources.error_no_internet
import de.shinz.rickandmortyshowcase.generated.resources.error_not_found
import de.shinz.rickandmortyshowcase.generated.resources.error_request_timeout
import de.shinz.rickandmortyshowcase.generated.resources.error_serialization
import de.shinz.rickandmortyshowcase.generated.resources.error_server
import de.shinz.rickandmortyshowcase.generated.resources.error_too_many_requests
import de.shinz.rickandmortyshowcase.generated.resources.error_unknown

/**
 * Turns a data-layer failure into something a screen can show.
 *
 * Lives in `core/ui` because it is where an `Error` meets a string resource —
 * the mapping is UI, the error is not. It is a plain function and not
 * `@Composable`, which is what lets both a ViewModel (for an event) and an
 * assembler (for a rendered error state) call it.
 *
 * `when` over both enums is exhaustive on purpose: adding a `DataError` constant
 * should fail to compile here rather than silently fall through to "something
 * went wrong".
 */
fun DataError.toUiText(): UiText = UiText.StringResourceText(
    when (this) {
        DataError.Network.NO_INTERNET -> Res.string.error_no_internet
        DataError.Network.REQUEST_TIMEOUT -> Res.string.error_request_timeout
        DataError.Network.NOT_FOUND -> Res.string.error_not_found
        DataError.Network.TOO_MANY_REQUESTS -> Res.string.error_too_many_requests
        DataError.Network.SERVER_ERROR -> Res.string.error_server
        DataError.Network.SERIALIZATION -> Res.string.error_serialization
        DataError.Network.UNKNOWN -> Res.string.error_unknown

        DataError.Local.DISK_FULL -> Res.string.error_disk_full
        // Not a "couldn't save" message: Local.UNKNOWN also comes from reads —
        // getFavorite, and the local-first getCharacter behind the detail screen.
        DataError.Local.UNKNOWN -> Res.string.error_local_unknown
    },
)
