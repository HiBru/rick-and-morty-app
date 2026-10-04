package de.shinz.rickandmortyshowcase.core.data.character

import de.shinz.rickandmortyshowcase.core.data.getResult
import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.Result
import de.shinz.rickandmortyshowcase.core.domain.datasource.CharacterRemoteDataSource
import de.shinz.rickandmortyshowcase.core.domain.model.Character
import de.shinz.rickandmortyshowcase.core.domain.model.CharacterPage
import io.ktor.client.HttpClient

/**
 * The Rick and Morty API over Ktor.
 *
 * Named for what it wraps rather than suffixed `Impl`, per the data-layer naming
 * rule. It holds no state and no logic beyond naming the two routes — status
 * codes, deserialization and mapping all live in `getResult`.
 */
class KtorCharacterRemoteDataSource(
    private val httpClient: HttpClient,
) : CharacterRemoteDataSource {

    override suspend fun fetchCharacterPage(
        page: Int,
    ): Result<CharacterPage, DataError.Network> =
        httpClient.getResult<CharacterPageDto, CharacterPage>(
            route = CHARACTER_ROUTE,
            parameters = mapOf("page" to page),
        ) { it.toCharacterPage() }

    override suspend fun fetchCharacter(id: Int): Result<Character, DataError.Network> =
        httpClient.getResult<CharacterDto, Character>(
            route = "$CHARACTER_ROUTE/$id",
        ) { it.toCharacter() }

    private companion object {
        const val CHARACTER_ROUTE = "/character"
    }
}
