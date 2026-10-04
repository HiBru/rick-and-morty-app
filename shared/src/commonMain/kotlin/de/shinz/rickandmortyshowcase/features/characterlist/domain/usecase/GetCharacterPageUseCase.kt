package de.shinz.rickandmortyshowcase.features.characterlist.domain.usecase

import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.Result
import de.shinz.rickandmortyshowcase.core.domain.datasource.CharacterRemoteDataSource
import de.shinz.rickandmortyshowcase.core.domain.model.CharacterPage

/**
 * One page of characters, with the API's end-of-list quirk already absorbed.
 *
 * Not a pass-through, unlike most read use cases here — it exists to answer one
 * question the ViewModel should not have to: **what does a 404 on a page mean?**
 *
 * The API serves 20 characters per page and answers a page past the last one
 * with `404`, not with an empty list. Left alone, that would surface as
 * `DataError.Network.NOT_FOUND`, whose message reads "We couldn't find what you
 * were looking for" — wrong twice over at the bottom of a list. The user
 * scrolled rather than searched, and there is nothing to retry: either the
 * client asked for a page that was never going to exist, or the dataset shrank
 * between two requests. Both mean the same thing to the screen — stop paging.
 *
 * **The first page is the exception, and it has to be.** A 404 on page 1 is not
 * "no characters exist"; it is the API being broken or having moved, and turning
 * it into an empty success would render a blank screen with no error and no
 * retry. So only pages after the first collapse into end-of-list.
 */
class GetCharacterPageUseCase(
    private val remoteCharacters: CharacterRemoteDataSource,
) {
    suspend operator fun invoke(page: Int): Result<CharacterPage, DataError.Network> =
        when (val result = remoteCharacters.fetchCharacterPage(page)) {
            is Result.Success -> result
            is Result.Error ->
                if (page > FIRST_PAGE && result.error == DataError.Network.NOT_FOUND) {
                    Result.Success(END_OF_LIST)
                } else {
                    result
                }
        }

    companion object {
        /** The API counts pages from 1. The list's own page counter starts here. */
        const val FIRST_PAGE: Int = 1

        private val END_OF_LIST = CharacterPage(characters = emptyList(), hasMore = false)
    }
}
