package de.shinz.rickandmortyshowcase.features.characterdetail.presentation.assembler

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import de.shinz.rickandmortyshowcase.core.domain.model.testCharacter
import de.shinz.rickandmortyshowcase.core.format.AppDateTimeManager
import de.shinz.rickandmortyshowcase.core.ui.resolve
import de.shinz.rickandmortyshowcase.features.characterdetail.domain.model.CharacterDetailData
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailLoad
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailViewModelState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Instant

/**
 * What the detail screen's labels actually say.
 *
 * `CharacterDetailUiStateAssemblerTest` compares resource *keys*, which is right
 * for an assembler test and blind to the words themselves — a swapped pair of
 * labels would leave every assertion green while the screen called a gender an
 * origin.
 *
 * In `iosTest` because resource text is only readable there: on the JVM
 * `getString` resolves through `Resources.getSystem()`.
 */
class CharacterDetailStringsTest {

    private val assembler = CharacterDetailUiStateAssembler(AppDateTimeManager())

    private val rick = testCharacter(
        id = 1,
        name = "Rick Sanchez",
        gender = "Male",
        originName = "Earth (C-137)",
        locationName = "Citadel of Ricks",
        type = "Genetic experiment",
        episodeUrls = List(51) { "https://rickandmortyapi.com/api/episode/${it + 1}" },
        created = Instant.parse("2017-11-04T18:48:46.250Z"),
    )

    private fun detail() = assembler.assemble(
        data = CharacterDetailData(isFavorite = false),
        vmState = CharacterDetailViewModelState(
            character = rick,
            load = CharacterDetailLoad.Idle,
        ),
    ).character!!

    @Test
    fun everyFieldLabelReadsAsItself() = runTest {
        assertThat(detail().fields.map { it.label.resolve() }).containsExactly(
            "Type",
            "Gender",
            "Origin",
            "Last known location",
            "Episodes",
            "Added to the API",
        )
    }

    @Test
    fun theHeaderReadsAsOneLine() = runTest {
        assertThat(detail().subtitle.resolve()).isEqualTo("Alive · Human")
    }

    /**
     * The date goes through the platform formatter, so this asserts the *shape*
     * rather than an exact string — the exact wording is CLDR's and the zone is
     * the device's, which an exact assertion would pin to one machine.
     */
    @Test
    fun theCreatedDateIsFormattedNotRaw() = runTest {
        val created = detail().fields.single { it.label.resolve() == "Added to the API" }.value

        assertThat(created.contains("2017")).isEqualTo(true)
        // Not the ISO-8601 the API sent, which is what a missing formatter looks
        // like on screen.
        assertThat(created.contains("T")).isEqualTo(false)
    }
}
