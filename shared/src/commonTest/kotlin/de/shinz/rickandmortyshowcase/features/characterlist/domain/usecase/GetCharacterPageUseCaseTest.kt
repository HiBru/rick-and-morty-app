package de.shinz.rickandmortyshowcase.features.characterlist.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.Result
import de.shinz.rickandmortyshowcase.core.domain.datasource.FakeCharacterRemoteDataSource
import de.shinz.rickandmortyshowcase.core.domain.model.CharacterPage
import de.shinz.rickandmortyshowcase.core.domain.model.testCharacter
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * What this API's page responses actually mean.
 *
 * Two translations, both about page 1 being different from the rest, and both
 * invisible when wrong. Collapsing every `NOT_FOUND` would turn a broken first
 * page into a blank screen with no error; collapsing none would end a long
 * scroll with "We couldn't find what you were looking for" and a retry that can
 * never succeed. Treating an empty *successful* first page as fine would leave
 * the screen with nothing to show and nothing to scroll.
 */
class GetCharacterPageUseCaseTest {

    private val remote = FakeCharacterRemoteDataSource()
    private val getCharacterPage = GetCharacterPageUseCase(remote)

    @Test
    fun servesAPageTheApiKnows() = runTest {
        val page = CharacterPage(characters = listOf(testCharacter(1)), hasMore = true)
        remote.pages[1] = page

        assertThat(getCharacterPage(1))
            .isInstanceOf<Result.Success<CharacterPage>>()
            .prop(Result.Success<CharacterPage>::data).isEqualTo(page)
    }

    /**
     * A page past the last one is the end of the list, not a failure. The API
     * answers it with `404` rather than an empty list, which is the whole reason
     * this use case is not a pass-through.
     *
     * The `NOT_FOUND` is **named**, not arranged by leaving page 2 unregistered
     * and leaning on the fake's miss default. Those produce the same result
     * today, and the lazy version would keep passing if the fake's default ever
     * softened to `Success(empty, hasMore = false)` — with the collapse branch
     * never executing and the only real logic in this task silently untested.
     */
    @Test
    fun aMissingPageAfterTheFirstIsTheEndOfTheList() = runTest {
        remote.pageErrors[2] = DataError.Network.NOT_FOUND

        assertThat(getCharacterPage(2))
            .isInstanceOf<Result.Success<CharacterPage>>()
            .prop(Result.Success<CharacterPage>::data)
            .isEqualTo(CharacterPage(characters = emptyList(), hasMore = false))
    }

    /**
     * Page 1 is not "no characters exist" — it is the API being broken or having
     * moved. Collapsing it would render an empty list with no error and no
     * retry, which is the one outcome the user cannot escape.
     */
    @Test
    fun aMissingFirstPageStaysAnError() = runTest {
        remote.pageErrors[GetCharacterPageUseCase.FIRST_PAGE] = DataError.Network.NOT_FOUND

        assertThat(getCharacterPage(GetCharacterPageUseCase.FIRST_PAGE))
            .isInstanceOf<Result.Error<DataError.Network>>()
            .prop(Result.Error<DataError.Network>::error)
            .isEqualTo(DataError.Network.NOT_FOUND)
    }

    /**
     * A `200` carrying no characters at all is the server misbehaving, not an
     * empty catalogue — this API answers an out-of-range page with `404`, so it
     * has no reason to serve an empty first page.
     *
     * Reported here rather than left for the screen to notice, because a
     * *successful* page advances the page counter: by the time the list could
     * see the emptiness, a retry would ask for page 2 and show characters 21-40
     * as the start of the list. A failure is never consumed, so retry asks for
     * page 1 again.
     */
    @Test
    fun anEmptyFirstPageIsAFailure() = runTest {
        remote.pages[GetCharacterPageUseCase.FIRST_PAGE] =
            CharacterPage(characters = emptyList(), hasMore = false)

        assertThat(getCharacterPage(GetCharacterPageUseCase.FIRST_PAGE))
            .isInstanceOf<Result.Error<DataError.Network>>()
            .prop(Result.Error<DataError.Network>::error)
            .isEqualTo(DataError.Network.SERVER_ERROR)
    }

    /**
     * The end-of-list collapse must survive it. A later page legitimately comes
     * back empty — that is what the `404` branch above turns a missing page into
     * — so the empty check has to be first-page-only or paging could never stop.
     */
    @Test
    fun anEmptyLaterPageStaysTheEndOfTheList() = runTest {
        remote.pages[2] = CharacterPage(characters = emptyList(), hasMore = false)

        assertThat(getCharacterPage(2))
            .isInstanceOf<Result.Success<CharacterPage>>()
            .prop(Result.Success<CharacterPage>::data)
            .isEqualTo(CharacterPage(characters = emptyList(), hasMore = false))
    }

    /**
     * Only `NOT_FOUND` means end-of-list. Every other failure stays a failure on
     * **both** page positions — going offline mid-scroll must offer a retry
     * rather than silently ending the list, and going offline on the first load
     * is the app's own acceptance item 2.
     */
    @Test
    fun otherFailuresSurviveOnEveryPage() = runTest {
        val others = DataError.Network.entries - DataError.Network.NOT_FOUND

        listOf(GetCharacterPageUseCase.FIRST_PAGE, 7).forEach { page ->
            others.forEach { error ->
                remote.pageErrors[page] = error

                assertThat(getCharacterPage(page), name = "page $page, $error")
                    .isInstanceOf<Result.Error<DataError.Network>>()
                    .prop(Result.Error<DataError.Network>::error)
                    .isEqualTo(error)
            }
        }
    }
}
