package de.shinz.rickandmortyshowcase.features.characterlist.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.Result
import de.shinz.rickandmortyshowcase.core.domain.datasource.FakeCharacterRemoteDataSource
import de.shinz.rickandmortyshowcase.core.domain.model.Character
import de.shinz.rickandmortyshowcase.core.domain.model.CharacterPage
import de.shinz.rickandmortyshowcase.core.domain.model.CharacterStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Instant

/**
 * The one piece of real logic in Task 14: what a `404` on a page means.
 *
 * Worth testing precisely because it is a *translation* rather than a
 * pass-through — and because getting it wrong is invisible. Collapsing every
 * `NOT_FOUND` would turn a broken first page into a blank screen with no error;
 * collapsing none would end a long scroll with "We couldn't find what you were
 * looking for" and a retry button that can never succeed.
 */
class GetCharacterPageUseCaseTest {

    private val remote = FakeCharacterRemoteDataSource()
    private val getCharacterPage = GetCharacterPageUseCase(remote)

    private fun character(id: Int) = Character(
        id = id,
        name = "Character $id",
        status = CharacterStatus.ALIVE,
        species = "Human",
        type = "",
        gender = "Male",
        originName = "Earth",
        originUrl = "",
        locationName = "Earth",
        locationUrl = "",
        imageUrl = "https://rickandmortyapi.com/api/character/avatar/$id.jpeg",
        episodeUrls = emptyList(),
        url = "",
        created = Instant.parse("2017-11-04T18:48:46.250Z"),
    )

    @Test
    fun servesAPageTheApiKnows() = runTest {
        val page = CharacterPage(characters = listOf(character(1)), hasMore = true)
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
