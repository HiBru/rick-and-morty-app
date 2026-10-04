package de.shinz.rickandmortyshowcase.features.characterlist.presentation.assembler

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.model.testCharacter
import de.shinz.rickandmortyshowcase.core.ui.UiText
import de.shinz.rickandmortyshowcase.core.ui.toUiText
import de.shinz.rickandmortyshowcase.features.characterlist.domain.model.CharacterListData
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListPageLoad
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListUiState
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListViewModelState
import de.shinz.rickandmortyshowcase.generated.resources.Res
import de.shinz.rickandmortyshowcase.generated.resources.favorite_remove_message
import kotlin.test.Test

/**
 * One test per branch, and no coroutines anywhere — which is the point of
 * keeping the assembler pure. These run on both platforms.
 *
 * Two conditions to keep in view while reading: every loading and error state is
 * placed by whether the character buffer is empty, and the removal dialog
 * resolves its name out of that same buffer.
 */
class CharacterListUiStateAssemblerTest {

    private val assembler = CharacterListUiStateAssembler()

    private fun assemble(
        vmState: CharacterListViewModelState,
        favoriteIds: Set<Int> = emptySet(),
    ) = assembler.assemble(CharacterListData(favoriteIds), vmState)

    /* ---------- rows ---------- */

    /** `containsExactly` also pins the order: the API's, not favourites-first. */
    @Test
    fun marksARowFavoriteBySetMembership() {
        val uiState = assemble(
            vmState = CharacterListViewModelState(
                characters = listOf(testCharacter(3), testCharacter(1), testCharacter(2)),
                pageLoad = CharacterListPageLoad.Idle,
            ),
            favoriteIds = setOf(2),
        )

        assertThat(uiState.characters.map { it.id to it.isFavorite })
            .containsExactly(3 to false, 1 to false, 2 to true)
    }

    /* ---------- loading ---------- */

    /**
     * The default `CharacterListViewModelState()` — which is literally what the
     * screen paints first, since `stateIn`'s initial value is assembled from it.
     */
    @Test
    fun anEmptyListLoadingIsTheFullScreenIndicator() {
        val uiState = assemble(CharacterListViewModelState())

        assertThat(uiState).prop(CharacterListUiState::isInitialLoading).isTrue()
        assertThat(uiState).prop(CharacterListUiState::isNextPageLoading).isFalse()
        assertThat(uiState).prop(CharacterListUiState::initialLoadError).isNull()
        assertThat(uiState).prop(CharacterListUiState::nextPageError).isNull()
    }

    @Test
    fun loadingBeneathCharactersIsTheInlineIndicator() {
        val uiState = assemble(
            CharacterListViewModelState(
                characters = listOf(testCharacter(1)),
                pageLoad = CharacterListPageLoad.Loading,
            ),
        )

        assertThat(uiState).prop(CharacterListUiState::isNextPageLoading).isTrue()
        assertThat(uiState).prop(CharacterListUiState::isInitialLoading).isFalse()
        // The corner CharacterListPageLoad exists to make unrepresentable: a
        // spinner and a leftover error row at the same time.
        assertThat(uiState).prop(CharacterListUiState::nextPageError).isNull()
        assertThat(uiState).prop(CharacterListUiState::initialLoadError).isNull()
        // SPEC: a page in flight never discards what is already shown.
        assertThat(uiState.characters).hasSize(1)
    }

    @Test
    fun idleWithCharactersShowsNeitherIndicatorNorError() {
        val uiState = assemble(
            CharacterListViewModelState(
                characters = listOf(testCharacter(1)),
                pageLoad = CharacterListPageLoad.Idle,
            ),
        )

        assertThat(uiState).prop(CharacterListUiState::isInitialLoading).isFalse()
        assertThat(uiState).prop(CharacterListUiState::isNextPageLoading).isFalse()
        assertThat(uiState).prop(CharacterListUiState::initialLoadError).isNull()
        assertThat(uiState).prop(CharacterListUiState::nextPageError).isNull()
    }

    /* ---------- errors ---------- */

    @Test
    fun aFailureWithNothingLoadedIsTheFullScreenError() {
        val uiState = assemble(
            CharacterListViewModelState(
                pageLoad = CharacterListPageLoad.Failed(DataError.Network.NO_INTERNET),
            ),
        )

        assertThat(uiState).prop(CharacterListUiState::initialLoadError)
            .isEqualTo(DataError.Network.NO_INTERNET.toUiText())
        assertThat(uiState).prop(CharacterListUiState::nextPageError).isNull()
        assertThat(uiState).prop(CharacterListUiState::isInitialLoading).isFalse()
    }

    /**
     * The rule `SPEC.md` is most explicit about: a failed page keeps what is
     * already on screen, and the error goes inline rather than over the top.
     */
    @Test
    fun aFailureWithCharactersLoadedIsInlineAndKeepsTheRows() {
        val uiState = assemble(
            CharacterListViewModelState(
                characters = listOf(testCharacter(1), testCharacter(2)),
                pageLoad = CharacterListPageLoad.Failed(DataError.Network.REQUEST_TIMEOUT),
            ),
        )

        assertThat(uiState).prop(CharacterListUiState::nextPageError)
            .isEqualTo(DataError.Network.REQUEST_TIMEOUT.toUiText())
        assertThat(uiState).prop(CharacterListUiState::initialLoadError).isNull()
        assertThat(uiState.characters).hasSize(2)
    }

    /**
     * A tripwire, not coverage — the two tests above already pin both corners by
     * name. It exists so that reintroducing two independently-computed
     * conditions, which is exactly what `CharacterListPageLoad` was created to
     * prevent, fails here rather than on a device.
     */
    @Test
    fun anErrorIsNeverBothFullScreenAndInline() {
        listOf(emptyList(), listOf(testCharacter(1))).forEach { characters ->
            val uiState = assemble(
                CharacterListViewModelState(
                    characters = characters,
                    pageLoad = CharacterListPageLoad.Failed(DataError.Network.SERVER_ERROR),
                ),
            )

            assertThat(
                listOfNotNull(uiState.initialLoadError, uiState.nextPageError),
                name = "${characters.size} characters",
            ).hasSize(1)
        }
    }

    /* ---------- removal dialog ---------- */

    @Test
    fun theRemovalDialogNamesTheCharacter() {
        val uiState = assemble(
            CharacterListViewModelState(
                characters = listOf(
                    testCharacter(1, name = "Rick Sanchez"),
                    testCharacter(2, name = "Morty Smith"),
                ),
                pageLoad = CharacterListPageLoad.Idle,
                pendingRemovalId = 2,
            ),
        )

        assertThat(uiState).prop(CharacterListUiState::removeDialogText).isEqualTo(
            UiText.StringResourceText(
                id = Res.string.favorite_remove_message,
                args = listOf("Morty Smith"),
            ),
        )
    }

    @Test
    fun noPendingRemovalMeansNoDialog() {
        val uiState = assemble(
            CharacterListViewModelState(
                characters = listOf(testCharacter(1)),
                pageLoad = CharacterListPageLoad.Idle,
            ),
        )

        assertThat(uiState).prop(CharacterListUiState::removeDialogText).isNull()
    }

    /**
     * An id the buffer cannot resolve shows nothing rather than an "are you
     * sure" with a blank name in it.
     */
    @Test
    fun anUnresolvableIdMeansNoDialog() {
        val uiState = assemble(
            CharacterListViewModelState(
                characters = listOf(testCharacter(1)),
                pageLoad = CharacterListPageLoad.Idle,
                pendingRemovalId = 99,
            ),
        )

        assertThat(uiState).prop(CharacterListUiState::removeDialogText).isNull()
    }
}
