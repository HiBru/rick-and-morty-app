package de.shinz.rickandmortyshowcase.core.domain.datasource

import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.Result
import de.shinz.rickandmortyshowcase.core.domain.model.Character
import de.shinz.rickandmortyshowcase.core.domain.model.CharacterPage

/**
 * The Rick and Morty API. One source, so the error type stays narrowed to
 * [DataError.Network].
 */
interface CharacterRemoteDataSource {

    /** @param page 1-based, as the API counts. */
    suspend fun fetchCharacterPage(page: Int): Result<CharacterPage, DataError.Network>

    suspend fun fetchCharacter(id: Int): Result<Character, DataError.Network>
}
