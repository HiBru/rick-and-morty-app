package de.shinz.rickandmortyshowcase.features.characterdetail.presentation.assembler

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.model.CharacterStatus
import de.shinz.rickandmortyshowcase.core.domain.model.testCharacter
import de.shinz.rickandmortyshowcase.core.format.AppDateTimeManager
import de.shinz.rickandmortyshowcase.core.ui.UiText
import de.shinz.rickandmortyshowcase.core.ui.toUiText
import de.shinz.rickandmortyshowcase.features.characterdetail.domain.model.CharacterDetailData
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailFieldUi
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailLoad
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailUi
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailUiState
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailViewModelState
import kotlin.test.Test

/**
 * One test per branch, no coroutines — the point of a pure assembler. Runs on
 * both platforms.
 *
 * Field *labels* are compared by resource key rather than by resolved text, for
 * the usual reason: `getString` needs `Resources.getSystem()` on the JVM.
 * `CharacterDetailStringsTest` in `iosTest` reads the words.
 */
class CharacterDetailUiStateAssemblerTest {

    private val assembler = CharacterDetailUiStateAssembler(AppDateTimeManager())

    private fun assemble(
        vmState: CharacterDetailViewModelState,
        isFavorite: Boolean = false,
    ) = assembler.assemble(CharacterDetailData(isFavorite), vmState)

    private val rick = testCharacter(
        id = 1,
        name = "Rick Sanchez",
        gender = "Male",
        originName = "Earth (C-137)",
        locationName = "Citadel of Ricks",
        episodeUrls = List(51) { "https://rickandmortyapi.com/api/episode/${it + 1}" },
    )

    /* ---------- the three top-level states ---------- */

    /** The default state — what the screen paints before anything resolves. */
    @Test
    fun loadingShowsNeitherCharacterNorError() {
        val uiState = assemble(CharacterDetailViewModelState())

        assertThat(uiState).prop(CharacterDetailUiState::isLoading).isTrue()
        assertThat(uiState).prop(CharacterDetailUiState::character).isNull()
        assertThat(uiState).prop(CharacterDetailUiState::error).isNull()
    }

    @Test
    fun aFailureBecomesARetryableError() {
        val uiState = assemble(
            CharacterDetailViewModelState(
                load = CharacterDetailLoad.Failed(DataError.Network.NO_INTERNET),
            ),
        )

        assertThat(uiState).prop(CharacterDetailUiState::error)
            .isEqualTo(DataError.Network.NO_INTERNET.toUiText())
        assertThat(uiState).prop(CharacterDetailUiState::isLoading).isFalse()
        assertThat(uiState).prop(CharacterDetailUiState::character).isNull()
    }

    /**
     * The character is shown **only** when the load is `Idle`.
     *
     * The ViewModel cannot currently produce either of these pairings — it only
     * stores a character together with `Idle`, and retry is guarded on `Failed`.
     * They are still worth pinning here, because an assembler is a pure function
     * of its inputs and this is the contract the screen is written against: one
     * of loading, error and content, never two. The day a refresh path makes
     * them reachable, the screen does not have to change.
     */
    @Test
    fun aCharacterIsShownOnlyWhenTheLoadIsIdle() {
        val failed = assemble(
            CharacterDetailViewModelState(
                character = rick,
                load = CharacterDetailLoad.Failed(DataError.Local.UNKNOWN),
            ),
        )
        assertThat(failed).prop(CharacterDetailUiState::character).isNull()
        assertThat(failed).prop(CharacterDetailUiState::error).isNotNull()

        val loading = assemble(
            CharacterDetailViewModelState(character = rick, load = CharacterDetailLoad.Loading),
        )
        assertThat(loading).prop(CharacterDetailUiState::character).isNull()
        assertThat(loading).prop(CharacterDetailUiState::isLoading).isTrue()
    }

    /* ---------- the resolved character ---------- */

    @Test
    fun aResolvedCharacterCarriesItsHeader() {
        val uiState = assemble(
            CharacterDetailViewModelState(character = rick, load = CharacterDetailLoad.Idle),
        )
        val character = uiState.character!!

        assertThat(character).prop(CharacterDetailUi::name).isEqualTo("Rick Sanchez")
        assertThat(character).prop(CharacterDetailUi::status).isEqualTo(CharacterStatus.ALIVE)
        assertThat(character).prop(CharacterDetailUi::isFavorite).isFalse()
        assertThat((character.subtitle as UiText.Joined).parts.map { it.describe() })
            .containsExactly("res:status_alive", "dyn:Human")
    }

    @Test
    fun theFavoriteFlagAndItsDescriptionFollowTheFavourites() {
        val uiState = assemble(
            vmState = CharacterDetailViewModelState(
                character = rick,
                load = CharacterDetailLoad.Idle,
            ),
            isFavorite = true,
        )

        assertThat(uiState.character!!).prop(CharacterDetailUi::isFavorite).isTrue()
        assertThat(uiState.character!!.favoriteContentDescription.describe())
            .isEqualTo("res:cd_remove_favorite")
    }

    /* ---------- the field list ---------- */

    /**
     * Order and membership together, because the list *is* the layout: the
     * screen draws whatever arrives, in the order it arrives.
     */
    @Test
    fun theFieldsAreTheSpecsOrderWithoutABlankType() {
        val uiState = assemble(
            CharacterDetailViewModelState(character = rick, load = CharacterDetailLoad.Idle),
        )

        // Labels *paired with* their values, because the failure that matters
        // is a swap: `field(detail_gender, locationName)` beside
        // `field(detail_location, gender)` keeps both lists correct on their own.
        assertThat(uiState.character!!.fields.map { it.label.describe() to it.value })
            .containsExactly(
                "res:detail_gender" to "Male",
                "res:detail_origin" to "Earth (C-137)",
                "res:detail_location" to "Citadel of Ricks",
                "res:detail_episodes" to "51",
                "res:detail_created" to AppDateTimeManager().formatDate(rick.created),
            )
    }

    /** The one conditional field `SPEC.md` names, and it leads when present. */
    @Test
    fun aNonBlankTypeIsIncludedFirst() {
        val uiState = assemble(
            CharacterDetailViewModelState(
                character = rick.copy(type = "Genetic experiment"),
                load = CharacterDetailLoad.Idle,
            ),
        )

        val first = uiState.character!!.fields.first()
        assertThat(first.label.describe()).isEqualTo("res:detail_type")
        assertThat(first).prop(CharacterDetailFieldUi::value).isEqualTo("Genetic experiment")
    }

    @Test
    fun theEpisodeCountIsTheNumberOfUrls() {
        val uiState = assemble(
            CharacterDetailViewModelState(character = rick, load = CharacterDetailLoad.Idle),
        )

        assertThat(uiState.character!!.fields.single { it.label.describe() == "res:detail_episodes" })
            .prop(CharacterDetailFieldUi::value).isEqualTo("51")
    }

    /**
     * An `unknown` origin is shown, not hidden — the API saying it does not know
     * is information, and dropping it would leave "not known" and "not shown"
     * looking identical.
     */
    @Test
    fun anUnknownOriginIsStillShown() {
        val uiState = assemble(
            CharacterDetailViewModelState(
                character = rick.copy(originName = "unknown"),
                load = CharacterDetailLoad.Idle,
            ),
        )

        assertThat(uiState.character!!.fields.single { it.label.describe() == "res:detail_origin" })
            .prop(CharacterDetailFieldUi::value).isEqualTo("unknown")
    }

    /* ---------- the removal dialog ---------- */

    @Test
    fun theRemovalDialogNamesTheCharacter() {
        val uiState = assemble(
            CharacterDetailViewModelState(
                character = rick,
                load = CharacterDetailLoad.Idle,
                isRemovalPending = true,
            ),
        )

        assertThat(uiState.removeDialogText!!.describe()).isEqualTo("res:favorite_remove_message")
        assertThat((uiState.removeDialogText as UiText.StringResourceText).args)
            .containsExactly("Rick Sanchez")
    }

    @Test
    fun noPendingRemovalMeansNoDialog() {
        val uiState = assemble(
            CharacterDetailViewModelState(character = rick, load = CharacterDetailLoad.Idle),
        )

        assertThat(uiState).prop(CharacterDetailUiState::removeDialogText).isNull()
    }

    /** Nothing to confirm removing before the character has resolved. */
    @Test
    fun aPendingRemovalWithoutACharacterShowsNoDialog() {
        val uiState = assemble(CharacterDetailViewModelState(isRemovalPending = true))

        assertThat(uiState).prop(CharacterDetailUiState::removeDialogText).isNull()
    }
}

private fun UiText.describe(): String = when (this) {
    is UiText.StringResourceText -> "res:${id.key}"
    is UiText.DynamicString -> "dyn:$value"
    is UiText.Joined -> parts.joinToString(separator) { it.describe() }
}
