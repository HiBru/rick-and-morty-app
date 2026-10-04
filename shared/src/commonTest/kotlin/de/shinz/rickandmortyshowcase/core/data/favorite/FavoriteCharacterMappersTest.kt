package de.shinz.rickandmortyshowcase.core.data.favorite

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import de.shinz.rickandmortyshowcase.core.domain.model.Character
import de.shinz.rickandmortyshowcase.core.domain.model.CharacterStatus
import kotlin.test.Test
import kotlin.time.Instant

class FavoriteCharacterMappersTest {

    @Test
    fun aCharacterSurvivesTheRoundTripIntact() {
        // The offline guarantee in SPEC.md rests on this: whatever goes into the
        // row has to come back out, urls and episode list included, because the
        // row is the only copy a favourited character has.
        val restored = MORTY.toFavoriteEntity(addedAt = ADDED_AT).toCharacter()

        assertThat(restored).isEqualTo(MORTY)
    }

    @Test
    fun storesTheEpisodeListAsAJsonArray() {
        val entity = MORTY.toFavoriteEntity(addedAt = ADDED_AT)

        assertThat(entity.episodeUrlsJson).isEqualTo("""["ep/1","ep/2"]""")
    }

    @Test
    fun anEmptyEpisodeListRoundTrips() {
        val character = MORTY.copy(episodeUrls = emptyList())

        val restored = character.toFavoriteEntity(addedAt = ADDED_AT).toCharacter()

        assertThat(restored.episodeUrls).isEmpty()
    }

    @Test
    fun keepsMillisecondPrecisionOnCreated() {
        // The API sends milliseconds; storing seconds would quietly shift the
        // date for anything near midnight.
        val entity = MORTY.toFavoriteEntity(addedAt = ADDED_AT)

        assertThat(entity.createdEpochMillis).isEqualTo(1_509_821_326_250L)
        assertThat(entity.toCharacter().created)
            .isEqualTo(Instant.parse("2017-11-04T18:48:46.250Z"))
    }

    @Test
    fun recordsWhenTheUserFavouritedItSeparatelyFromTheApiTimestamp() {
        // The two are unrelated, and only addedAt can order the Favorites screen.
        val entity = MORTY.toFavoriteEntity(addedAt = ADDED_AT)

        assertThat(entity.addedAtEpochMillis).isEqualTo(ADDED_AT.toEpochMilliseconds())
        assertThat(entity.addedAtEpochMillis).isEqualTo(1_700_000_000_000L)
    }

    @Test
    fun storesStatusByName() {
        assertThat(MORTY.copy(status = CharacterStatus.DEAD).toFavoriteEntity(ADDED_AT).status)
            .isEqualTo("DEAD")
    }

    @Test
    fun anUnreadableStoredStatusBecomesUnknownRatherThanThrowing() {
        // The stored string came from an older build of this app. Renaming a
        // constant must not make the user's existing favourites unreadable.
        val entity = MORTY.toFavoriteEntity(ADDED_AT).copy(status = "MOSTLY_ALIVE")

        assertThat(entity.toCharacter().status).isEqualTo(CharacterStatus.UNKNOWN)
    }

    private companion object {
        val ADDED_AT = Instant.fromEpochMilliseconds(1_700_000_000_000L)

        val MORTY = Character(
            id = 2,
            name = "Morty Smith",
            status = CharacterStatus.ALIVE,
            species = "Human",
            type = "",
            gender = "Male",
            originName = "unknown",
            originUrl = "",
            locationName = "Citadel of Ricks",
            locationUrl = "loc/3",
            imageUrl = "avatar/2.jpeg",
            episodeUrls = listOf("ep/1", "ep/2"),
            url = "character/2",
            created = Instant.parse("2017-11-04T18:48:46.250Z"),
        )
    }
}
