package de.shinz.rickandmortyshowcase.features.characterdetail.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.model.testCharacter
import de.shinz.rickandmortyshowcase.core.format.AppDateTimeManager
import de.shinz.rickandmortyshowcase.features.characterdetail.domain.model.CharacterDetailData
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.assembler.CharacterDetailUiStateAssembler
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailLoad
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailUiAction
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailViewModelState
import kotlin.test.Test

/**
 * What each of the detail screen's controls dispatches.
 *
 * The assembler owns what is rendered and the ViewModel owns what each action
 * *does*; this covers the wiring between them, which neither can reach. States
 * come from the real assembler so a test cannot drift from the screen.
 *
 * iOS-only for the same platform reason as every other Compose UI test here.
 */
@OptIn(ExperimentalTestApi::class)
class CharacterDetailScreenTest {

    private val assembler = CharacterDetailUiStateAssembler(AppDateTimeManager())
    private val rick = testCharacter(id = 1, name = "Rick Sanchez")

    private fun loaded(isFavorite: Boolean = false, isRemovalPending: Boolean = false) =
        assembler.assemble(
            data = CharacterDetailData(isFavorite),
            vmState = CharacterDetailViewModelState(
                character = rick,
                load = CharacterDetailLoad.Idle,
                isRemovalPending = isRemovalPending,
            ),
        )

    private fun failed() = assembler.assemble(
        data = CharacterDetailData.EMPTY,
        vmState = CharacterDetailViewModelState(
            load = CharacterDetailLoad.Failed(DataError.Network.NO_INTERNET),
        ),
    )

    @Test
    fun tappingTheHeartReportsIt() = runComposeUiTest {
        CharacterDetailRobot(this)
            .setContent(loaded())
            .tapFavorite("Add Rick Sanchez to favorites")
            .assertDispatched(CharacterDetailUiAction.OnFavoriteClick)
    }

    /** Back is a callback, not an action — the ViewModel has no opinion about it. */
    @Test
    fun backCallsItsOwnCallbackAndDispatchesNoAction() = runComposeUiTest {
        CharacterDetailRobot(this)
            .setContent(loaded())
            .tapBack()
            .assertNavigatedBack(times = 1)
            .assertDispatched()
    }

    /**
     * Back has to work over the error state too — this screen has no bottom bar,
     * so it is the only way out, and it is drawn as a sibling of the branch that
     * fills the window.
     */
    @Test
    fun backIsReachableWhileTheScreenIsShowingAnError() = runComposeUiTest {
        CharacterDetailRobot(this)
            .setContent(failed())
            .tapBack()
            .assertNavigatedBack(times = 1)
    }

    /** The retry button is wired to the retry action, not merely present. */
    @Test
    fun theErrorStateRetryAsksForAnotherResolve() = runComposeUiTest {
        CharacterDetailRobot(this)
            .setContent(failed())
            .tap("Retry")
            .assertDispatched(CharacterDetailUiAction.OnRetryClick)
    }

    @Test
    fun confirmingTheDialogReportsIt() = runComposeUiTest {
        CharacterDetailRobot(this)
            .setContent(loaded(isFavorite = true, isRemovalPending = true))
            .tap("Remove")
            .assertDispatched(CharacterDetailUiAction.OnConfirmRemoveFavorite)
    }

    /** Cancel must dismiss, not remove — the one path only the device check saw. */
    @Test
    fun cancellingTheDialogDismissesIt() = runComposeUiTest {
        CharacterDetailRobot(this)
            .setContent(loaded(isFavorite = true, isRemovalPending = true))
            .tap("Cancel")
            .assertDispatched(CharacterDetailUiAction.OnDismissRemoveFavorite)
    }

    /**
     * A field reads as one thing, not two. Without the merge, "Gender" and
     * "Male" are separate nodes with nothing tying them together — confirmed by
     * dumping the tree before the merge was added.
     */
    @Test
    fun aFieldAnnouncesItsLabelAndValueTogether() = runComposeUiTest {
        CharacterDetailRobot(this)
            .setContent(loaded())
            .assertFieldReadsAsOneNode(label = "Gender", value = "Male")
    }
}
