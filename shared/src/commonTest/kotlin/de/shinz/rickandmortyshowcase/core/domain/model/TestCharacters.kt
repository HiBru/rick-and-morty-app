package de.shinz.rickandmortyshowcase.core.domain.model

import kotlin.time.Instant

/**
 * A character for a test to vary one field of.
 *
 * `Character` has fourteen fields and almost every test cares about two of them,
 * so hand-writing the constructor is fourteen lines of noise around the one
 * value that matters — and four copies of it had accumulated across the list,
 * mapper and assembler tests before this existed.
 *
 * Beside the model rather than inside one test class, for the same reason the
 * fakes live beside their interfaces: every feature's tests need it.
 *
 * Deliberately **not** `CharacterPreviewSamples`, which lives in `commonMain`
 * for the previews. Tests asserting against preview data means retuning a
 * preview breaks a test that has nothing to do with it.
 */
internal fun testCharacter(
    id: Int = 1,
    name: String = "Character $id",
    status: CharacterStatus = CharacterStatus.ALIVE,
    species: String = "Human",
    type: String = "",
    gender: String = "Male",
    originName: String = "Earth (C-137)",
    locationName: String = "Citadel of Ricks",
    episodeUrls: List<String> = emptyList(),
    created: Instant = Instant.parse("2017-11-04T18:48:46.250Z"),
) = Character(
    id = id,
    name = name,
    status = status,
    species = species,
    type = type,
    gender = gender,
    originName = originName,
    originUrl = "https://rickandmortyapi.com/api/location/1",
    locationName = locationName,
    locationUrl = "https://rickandmortyapi.com/api/location/3",
    imageUrl = "https://rickandmortyapi.com/api/character/avatar/$id.jpeg",
    episodeUrls = episodeUrls,
    url = "https://rickandmortyapi.com/api/character/$id",
    created = created,
)
