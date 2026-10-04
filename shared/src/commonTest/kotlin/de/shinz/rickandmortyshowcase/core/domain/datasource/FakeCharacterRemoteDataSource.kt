package de.shinz.rickandmortyshowcase.core.domain.datasource

import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.Result
import de.shinz.rickandmortyshowcase.core.domain.model.Character
import de.shinz.rickandmortyshowcase.core.domain.model.CharacterPage

/**
 * A fake, not a mock — an in-memory stand-in whose behaviour is set by
 * assignment rather than by recording expectations.
 *
 * Shared across the repository test and every ViewModel test from Task 14 on,
 * which is why it lives beside the interface rather than nested in one test.
 *
 * **An unregistered page or id fails with `NOT_FOUND`, exactly as the real API
 * does.** Serving a successful empty page instead would be the more forgiving
 * default and a trap: a paging bug that asks for the wrong page number would get
 * `Success(empty, hasMore = false)`, conclude "end of list", and pass — and the
 * plan's obligation to treat a page `NOT_FOUND` as end-of-list would have no
 * reachable branch to test.
 */
class FakeCharacterRemoteDataSource : CharacterRemoteDataSource {

    /** Characters the API knows about, by id. */
    val characters: MutableMap<Int, Character> = mutableMapOf()

    /** Pages the API will serve, by 1-based page number. */
    val pages: MutableMap<Int, CharacterPage> = mutableMapOf()

    /** When set, every call fails with this. */
    var error: DataError.Network? = null

    /**
     * Per-page failures, for the case a global switch cannot express: page 1
     * succeeds and page 2 fails, which is what `SPEC.md`'s "a failed page load
     * never discards pages already shown" needs in order to be arranged at all.
     */
    val pageErrors: MutableMap<Int, DataError.Network> = mutableMapOf()

    /** Counts calls, so a test can assert the network was *not* reached. */
    var fetchCharacterCallCount: Int = 0
        private set
    var fetchCharacterPageCallCount: Int = 0
        private set

    override suspend fun fetchCharacterPage(
        page: Int,
    ): Result<CharacterPage, DataError.Network> {
        fetchCharacterPageCallCount++
        error?.let { return Result.Error(it) }
        pageErrors[page]?.let { return Result.Error(it) }

        return pages[page]
            ?.let { Result.Success(it) }
            ?: Result.Error(DataError.Network.NOT_FOUND)
    }

    override suspend fun fetchCharacter(id: Int): Result<Character, DataError.Network> {
        fetchCharacterCallCount++
        error?.let { return Result.Error(it) }

        return characters[id]
            ?.let { Result.Success(it) }
            ?: Result.Error(DataError.Network.NOT_FOUND)
    }
}
