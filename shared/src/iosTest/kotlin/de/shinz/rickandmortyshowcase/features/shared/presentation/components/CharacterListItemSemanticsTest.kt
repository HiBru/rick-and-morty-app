package de.shinz.rickandmortyshowcase.features.shared.presentation.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import de.shinz.rickandmortyshowcase.core.designsystem.theme.AppTheme
import de.shinz.rickandmortyshowcase.core.domain.model.testCharacter
import de.shinz.rickandmortyshowcase.features.shared.presentation.model.toCharacterUi
import kotlin.test.Test

/**
 * What a screen reader can actually reach on a character row.
 *
 * A **regression guard for behaviour that was always correct**, not a fix.
 * Task 13 recorded that the row's clickable card exposed no accessible name;
 * dumping the real semantics tree here showed otherwise — `Modifier.clickable`
 * sets `MergeDescendants` itself, so the card already carries the name and
 * subtitle, and the favourite button survives as its own node because
 * `IconButton` is a merging node and merging stops there. The Task 13 reading
 * came from `adb shell uiautomator dump`, which reports the **unmerged** tree.
 * What is worth guarding is that both halves stay true.
 *
 * iOS-only, and that is a platform limit: `runComposeUiTest` needs a real Compose
 * host, which on Android means instrumentation. Semantics are
 * platform-independent, so the tree asserted here is the tree Android builds.
 *
 * The tests are a set on purpose: a merge that gave the card a name by absorbing
 * the favourite button would pass the first and fail the others.
 */
@OptIn(ExperimentalTestApi::class)
class CharacterListItemSemanticsTest {

    private val rick = testCharacter(id = 1, name = "Rick Sanchez").toCharacterUi(isFavorite = false)

    @Test
    fun theRowAnnouncesTheCharacterItOpens() = runComposeUiTest {
        setContent {
            AppTheme {
                CharacterListItem(character = rick, onClick = {}, onFavoriteClick = {})
            }
        }

        // The name has to be reachable on a node that is itself clickable — not
        // merely present somewhere inside an unnamed clickable container. And
        // the subtitle has to be merged in with it: a `clearAndSetSemantics`
        // anywhere below would still leave the name, which is what makes the
        // exact-text assertion worth more than a presence check.
        onNodeWithText("Rick Sanchez")
            .assertHasClickAction()
            .assertTextEquals("Rick Sanchez", "Alive · Human")
    }

    /** Tapping the row must not be the same node as tapping the heart. */
    @Test
    fun theRowAndTheHeartAreDifferentTargets() = runComposeUiTest {
        var rowClicks = 0
        var favoriteClicks = 0
        setContent {
            AppTheme {
                CharacterListItem(
                    character = rick,
                    onClick = { rowClicks++ },
                    onFavoriteClick = { favoriteClicks++ },
                )
            }
        }

        onNodeWithContentDescription("Add Rick Sanchez to favorites").performClick()

        assertThat(favoriteClicks).isEqualTo(1)
        assertThat(rowClicks).isEqualTo(0)
    }
}
