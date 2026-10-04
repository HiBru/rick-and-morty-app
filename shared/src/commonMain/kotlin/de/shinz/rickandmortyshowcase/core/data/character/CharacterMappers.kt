package de.shinz.rickandmortyshowcase.core.data.character

import de.shinz.rickandmortyshowcase.core.domain.model.Character
import de.shinz.rickandmortyshowcase.core.domain.model.CharacterPage
import de.shinz.rickandmortyshowcase.core.domain.model.CharacterStatus
import kotlin.time.Instant

/**
 * Wire shape to domain shape.
 *
 * `Instant.parse` can throw on a malformed timestamp, and that is handled rather
 * than guarded: these mappers run inside `safeCall`, which turns the throw into
 * `DataError.Network.SERIALIZATION` — the honest answer, since a timestamp the
 * API should never send is a response that did not match what we expected.
 */
fun CharacterDto.toCharacter(): Character = Character(
    id = id,
    name = name,
    status = status.toCharacterStatus(),
    species = species,
    type = type,
    gender = gender,
    originName = origin.name,
    originUrl = origin.url,
    locationName = location.name,
    locationUrl = location.url,
    imageUrl = image,
    episodeUrls = episode,
    url = url,
    created = Instant.parse(created),
)

fun CharacterPageDto.toCharacterPage(): CharacterPage = CharacterPage(
    characters = results.map { it.toCharacter() },
    // The API answers paging with a url, not a count: `next` is absent on the
    // last page. The list screen stops asking when this goes false.
    hasMore = info.next != null,
)

/**
 * The API sends `"Alive"`, `"Dead"` or `"unknown"` — capitalised inconsistently,
 * which is why this compares case-insensitively. Anything unrecognised becomes
 * [CharacterStatus.UNKNOWN] rather than throwing: a new status value should grey
 * out a dot, not fail a whole page.
 */
private fun String.toCharacterStatus(): CharacterStatus = when (lowercase()) {
    "alive" -> CharacterStatus.ALIVE
    "dead" -> CharacterStatus.DEAD
    else -> CharacterStatus.UNKNOWN
}
