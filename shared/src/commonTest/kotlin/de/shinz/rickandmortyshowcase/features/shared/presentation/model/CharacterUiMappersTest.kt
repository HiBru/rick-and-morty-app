package de.shinz.rickandmortyshowcase.features.shared.presentation.model

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import de.shinz.rickandmortyshowcase.core.domain.model.Character
import de.shinz.rickandmortyshowcase.core.domain.model.CharacterStatus
import de.shinz.rickandmortyshowcase.core.ui.UiText
import kotlin.test.Test
import kotlin.time.Instant

class CharacterUiMappersTest {

    private val character = Character(
        id = 1,
        name = "Rick Sanchez",
        status = CharacterStatus.ALIVE,
        species = "Human",
        type = "",
        gender = "Male",
        originName = "Earth (C-137)",
        originUrl = "https://rickandmortyapi.com/api/location/1",
        locationName = "Citadel of Ricks",
        locationUrl = "https://rickandmortyapi.com/api/location/3",
        imageUrl = "https://rickandmortyapi.com/api/character/avatar/1.jpeg",
        episodeUrls = listOf("https://rickandmortyapi.com/api/episode/1"),
        url = "https://rickandmortyapi.com/api/character/1",
        created = Instant.parse("2017-11-04T18:48:46.250Z"),
    )

    @Test
    fun carriesTheFieldsARowRenders() {
        val ui = character.toCharacterUi(isFavorite = false)

        assertThat(ui).prop(CharacterUi::id).isEqualTo(1)
        assertThat(ui).prop(CharacterUi::name).isEqualTo("Rick Sanchez")
        assertThat(ui).prop(CharacterUi::imageUrl)
            .isEqualTo("https://rickandmortyapi.com/api/character/avatar/1.jpeg")
        assertThat(ui).prop(CharacterUi::isFavorite).isEqualTo(false)
        assertThat(character.toCharacterUi(isFavorite = true))
            .prop(CharacterUi::isFavorite).isEqualTo(true)
    }

    /**
     * The status stays a domain enum rather than becoming a colour or a word.
     * `AppStatusDot` is what turns it into a colour, and it cannot be handed one
     * — so a mapper that helpfully resolved this would break the dot.
     */
    @Test
    fun keepsTheStatusAsADomainEnum() {
        val statuses = CharacterStatus.entries.map { status ->
            character.copy(status = status).toCharacterUi(isFavorite = false).status
        }

        assertThat(statuses).containsExactly(
            CharacterStatus.ALIVE,
            CharacterStatus.DEAD,
            CharacterStatus.UNKNOWN,
        )
    }

    @Test
    fun subtitleIsTheStatusLabelThenTheSpecies() {
        val subtitle = character.toCharacterUi(isFavorite = false).subtitle

        assertThat(subtitle).isInstanceOf<UiText.Joined>()
        val parts = (subtitle as UiText.Joined).parts

        assertThat(parts.map { it.describe() })
            .containsExactly("res:status_alive", "dyn:Human")
    }

    /**
     * One label per status, and each its own.
     *
     * Pairwise over the whole enum rather than a spot check: two resources
     * swapped would compile, render, and label every dead character "Alive".
     */
    @Test
    fun everyStatusGetsItsOwnLabel() {
        val labels = CharacterStatus.entries.map { status ->
            val subtitle = character.copy(status = status)
                .toCharacterUi(isFavorite = false).subtitle as UiText.Joined
            status to subtitle.parts.first().describe()
        }

        assertThat(labels).containsExactly(
            CharacterStatus.ALIVE to "res:status_alive",
            CharacterStatus.DEAD to "res:status_dead",
            CharacterStatus.UNKNOWN to "res:status_unknown",
        )
    }

    /**
     * A blank species still goes in as a part. Dropping it is
     * [UiText.Joined]'s job at resolution time, not the mapper's — the mapper
     * cannot know what the other parts resolve to, and a special case here would
     * be a second place for the same rule to live.
     */
    @Test
    fun keepsABlankSpeciesAsAPartForJoinedToDrop() {
        val subtitle = character.copy(species = "")
            .toCharacterUi(isFavorite = false).subtitle as UiText.Joined

        assertThat(subtitle.parts.map { it.describe() })
            .containsExactly("res:status_alive", "dyn:")
    }

    @Test
    fun favoriteDescriptionSwitchesAndNamesTheCharacter() {
        val notFavorite = character.toCharacterUi(isFavorite = false)
        val favorite = character.toCharacterUi(isFavorite = true)

        assertThat(notFavorite.favoriteContentDescription.describe())
            .isEqualTo("res:cd_add_favorite")
        assertThat(favorite.favoriteContentDescription.describe())
            .isEqualTo("res:cd_remove_favorite")

        // The name is interpolated, which is the only reason a list of twenty
        // rows does not announce twenty identical buttons.
        assertThat((favorite.favoriteContentDescription as UiText.StringResourceText).args)
            .containsExactly("Rick Sanchez")
    }
}

/**
 * A `UiText` as a comparable string.
 *
 * The resource *key* rather than the `StringResource` itself, because that class
 * has no `toString()` — a mismatch would otherwise be reported as
 * `StringResource@1a2b` instead of a name. Resolved *text* is not asserted
 * anywhere here on purpose: `getString` needs `Resources.getSystem()` on the
 * JVM, so a common test cannot reach it. `ErrorStringsTest` in `iosTest` is
 * where resource wording is checked.
 */
private fun UiText.describe(): String = when (this) {
    is UiText.StringResourceText -> "res:${id.key}"
    is UiText.DynamicString -> "dyn:$value"
    is UiText.Joined -> parts.joinToString(separator) { it.describe() }
}
