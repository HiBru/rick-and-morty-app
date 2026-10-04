package de.shinz.rickandmortyshowcase.features.shared.presentation.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import de.shinz.rickandmortyshowcase.core.domain.model.CharacterStatus
import de.shinz.rickandmortyshowcase.core.domain.model.testCharacter
import de.shinz.rickandmortyshowcase.core.ui.resolve
import de.shinz.rickandmortyshowcase.generated.resources.Res
import de.shinz.rickandmortyshowcase.generated.resources.status_alive
import de.shinz.rickandmortyshowcase.generated.resources.status_dead
import de.shinz.rickandmortyshowcase.generated.resources.status_unknown
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.getString
import kotlin.test.Test

/**
 * Reads what a row's strings actually say.
 *
 * `CharacterUiMappersTest` in `commonTest` compares resource *keys*, which is
 * the right thing for a mapper test — and is also structurally blind to the one
 * failure this file exists for. `cd_add_favorite` is the app's first
 * **interpolated** string, and Compose Resources substitutes only indexed
 * placeholders: a bare `%s` is emitted literally, with no error and no failing
 * test anywhere. The description a blind user hears would read
 * "Add %s to favorites" and every other check would stay green.
 *
 * In `iosTest` for the same reason as `ErrorStringsTest`: on the JVM `getString`
 * resolves through `Resources.getSystem()`, which an unmocked host test cannot
 * provide.
 */
class CharacterUiStringsTest {

    private val character = testCharacter(id = 1, name = "Rick Sanchez")

    @Test
    fun theFavoriteDescriptionInterpolatesTheName() = runTest {
        assertThat(character.toCharacterUi(isFavorite = false).favoriteContentDescription.resolve())
            .isEqualTo("Add Rick Sanchez to favorites")

        assertThat(character.toCharacterUi(isFavorite = true).favoriteContentDescription.resolve())
            .isEqualTo("Remove Rick Sanchez from favorites")
    }

    @Test
    fun theStatusWordsAreTheApisOwn() = runTest {
        assertThat(getString(Res.string.status_alive)).isEqualTo("Alive")
        assertThat(getString(Res.string.status_dead)).isEqualTo("Dead")
        assertThat(getString(Res.string.status_unknown)).isEqualTo("Unknown")
    }

    /**
     * The whole subtitle, resolved — including the case `UiText.Joined` exists
     * for. A blank species must take its separator with it, or every such row
     * reads "Unknown · " with a dangling middot.
     */
    @Test
    fun aBlankSpeciesTakesItsSeparatorWithIt() = runTest {
        assertThat(character.toCharacterUi(isFavorite = false).subtitle.resolve())
            .isEqualTo("Alive · Human")

        assertThat(
            character.copy(species = "", status = CharacterStatus.UNKNOWN)
                .toCharacterUi(isFavorite = false).subtitle.resolve(),
        ).isEqualTo("Unknown")
    }
}
