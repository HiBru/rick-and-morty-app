package de.shinz.rickandmortyshowcase.core.data.character

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import de.shinz.rickandmortyshowcase.core.domain.model.CharacterStatus
import kotlin.test.Test
import kotlin.time.Instant

class CharacterMappersTest {

    @Test
    fun mapsEveryFieldOntoTheDomainModel() {
        val character = RICK_DTO.toCharacter()

        assertThat(character.id).isEqualTo(2)
        assertThat(character.name).isEqualTo("Morty Smith")
        assertThat(character.status).isEqualTo(CharacterStatus.ALIVE)
        assertThat(character.species).isEqualTo("Human")
        assertThat(character.gender).isEqualTo("Male")
        // The nested objects get flattened, and the urls come along because
        // favouriting has to persist the whole record.
        assertThat(character.originName).isEqualTo("unknown")
        assertThat(character.originUrl).isEqualTo("")
        assertThat(character.locationName).isEqualTo("Citadel of Ricks")
        assertThat(character.locationUrl).isEqualTo("https://rickandmortyapi.com/api/location/3")
        // Renamed on the way through: image -> imageUrl, episode -> episodeUrls.
        assertThat(character.imageUrl).isEqualTo("https://rickandmortyapi.com/api/character/avatar/2.jpeg")
        assertThat(character.episodeUrls).isEqualTo(
            listOf(
                "https://rickandmortyapi.com/api/episode/1",
                "https://rickandmortyapi.com/api/episode/2",
            ),
        )
        assertThat(character.url).isEqualTo("https://rickandmortyapi.com/api/character/2")
        assertThat(character.created).isEqualTo(Instant.parse("2017-11-04T18:50:21.651Z"))
    }

    @Test
    fun keepsABlankTypeBlankRatherThanInventingAPlaceholder() {
        // The API sends "" for most characters. The detail screen decides to omit
        // the row; the mapper does not get to decide that.
        assertThat(RICK_DTO.toCharacter().type).isEqualTo("")
    }

    @Test
    fun readsStatusCaseInsensitively() {
        // The API capitalises Alive and Dead but not unknown.
        assertThat(RICK_DTO.copy(status = "Alive").toCharacter().status)
            .isEqualTo(CharacterStatus.ALIVE)
        assertThat(RICK_DTO.copy(status = "alive").toCharacter().status)
            .isEqualTo(CharacterStatus.ALIVE)
        assertThat(RICK_DTO.copy(status = "Dead").toCharacter().status)
            .isEqualTo(CharacterStatus.DEAD)
        assertThat(RICK_DTO.copy(status = "unknown").toCharacter().status)
            .isEqualTo(CharacterStatus.UNKNOWN)
    }

    @Test
    fun treatsAnUnrecognisedStatusAsUnknownInsteadOfThrowing() {
        // A status the API adds later should grey out one dot, not fail a page.
        assertThat(RICK_DTO.copy(status = "Schrodinger").toCharacter().status)
            .isEqualTo(CharacterStatus.UNKNOWN)
    }

    @Test
    fun aPageWithANextUrlHasMore() {
        val page = CharacterPageDto(
            info = PageInfoDto(next = "$BASE/character?page=2"),
            results = listOf(RICK_DTO),
        ).toCharacterPage()

        assertThat(page.hasMore).isTrue()
        // A field, not `listOf(RICK_DTO.toCharacter())` — comparing the mapper's
        // output to the mapper's output passes for any implementation.
        assertThat(page.characters.single().id).isEqualTo(2)
    }

    @Test
    fun theLastPageHasNoMore() {
        // This is what stops the list screen asking for page 43.
        val page = CharacterPageDto(
            info = PageInfoDto(next = null),
            results = listOf(RICK_DTO),
        ).toCharacterPage()

        assertThat(page.hasMore).isFalse()
    }

    @Test
    fun anEmptyPageMapsToAnEmptyList() {
        val page = CharacterPageDto(info = PageInfoDto(), results = emptyList()).toCharacterPage()

        assertThat(page.characters).isEmpty()
        assertThat(page.hasMore).isFalse()
    }

    private companion object {
        const val BASE = "https://rickandmortyapi.com/api"

        val RICK_DTO = CharacterDto(
            id = 2,
            name = "Morty Smith",
            status = "Alive",
            species = "Human",
            type = "",
            gender = "Male",
            origin = LocationRefDto(name = "unknown", url = ""),
            location = LocationRefDto(name = "Citadel of Ricks", url = "$BASE/location/3"),
            image = "$BASE/character/avatar/2.jpeg",
            episode = listOf("$BASE/episode/1", "$BASE/episode/2"),
            url = "$BASE/character/2",
            created = "2017-11-04T18:50:21.651Z",
        )
    }
}
