package de.shinz.rickandmortyshowcase.core.ui

import assertk.all
import assertk.assertAll
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import de.shinz.rickandmortyshowcase.core.domain.DataError
import kotlin.test.Test

class DataErrorToUiTextTest {

    private val allErrors: List<DataError> =
        DataError.Network.entries + DataError.Local.entries

    /**
     * The whole table, in one assertion.
     *
     * Asserting the mapping pairwise rather than spot-checking two of them is
     * what catches the real failure mode here: two constants swapped, or one
     * pointed at a neighbour's message. Both compile, and both show the user the
     * wrong text. The resource *key* is compared instead of the `StringResource`
     * object because the latter has no `toString()`, so a mismatch would report
     * `StringResource@1a2b` instead of the name.
     */
    @Test
    fun everyErrorMapsToItsOwnMessage() {
        val mapping = allErrors.map { it to (it.toUiText() as UiText.StringResourceText).id.key }

        assertThat(mapping).containsExactly(
            DataError.Network.NO_INTERNET to "error_no_internet",
            DataError.Network.REQUEST_TIMEOUT to "error_request_timeout",
            DataError.Network.NOT_FOUND to "error_not_found",
            DataError.Network.TOO_MANY_REQUESTS to "error_too_many_requests",
            DataError.Network.SERVER_ERROR to "error_server",
            DataError.Network.SERIALIZATION to "error_serialization",
            DataError.Network.UNKNOWN to "error_unknown",
            DataError.Local.DISK_FULL to "error_disk_full",
            DataError.Local.UNKNOWN to "error_local_unknown",
        )
    }

    @Test
    fun noErrorCarriesFormatArguments() {
        // assertAll so one bad constant does not hide the rest, and `name` so the
        // failure says which one.
        assertAll {
            allErrors.forEach { error ->
                assertThat(error.toUiText(), name = error.toString()).all {
                    isInstanceOf<UiText.StringResourceText>()
                        .prop(UiText.StringResourceText::args)
                        .isEmpty()
                }
            }
        }
    }
}
