package de.shinz.rickandmortyshowcase.features.characterlist.presentation.assembler

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import de.shinz.rickandmortyshowcase.core.domain.model.testCharacter
import de.shinz.rickandmortyshowcase.core.ui.resolve
import de.shinz.rickandmortyshowcase.features.characterlist.domain.model.CharacterListData
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListPageLoad
import de.shinz.rickandmortyshowcase.features.characterlist.presentation.model.CharacterListViewModelState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * Reads what the removal dialog actually says.
 *
 * `CharacterListUiStateAssemblerTest` compares `UiText` *values*, which is what
 * an assembler test should do — and is blind to the one way this string can be
 * broken. `favorite_remove_message` interpolates a name, and Compose Resources
 * substitutes only indexed placeholders: a bare `%s` is emitted literally, with
 * no error and no failing assertion anywhere. The user would be asked to confirm
 * removing "%s".
 *
 * In `iosTest` because resource text is only readable there — on the JVM
 * `getString` resolves through `Resources.getSystem()`.
 */
class CharacterListStringsTest {

    private val assembler = CharacterListUiStateAssembler()

    private val rick = testCharacter(id = 1, name = "Rick Sanchez")

    @Test
    fun theRemovalDialogMessageNamesTheCharacter() = runTest {
        val uiState = assembler.assemble(
            data = CharacterListData.EMPTY,
            vmState = CharacterListViewModelState(
                characters = listOf(rick),
                pageLoad = CharacterListPageLoad.Idle,
                pendingRemovalId = 1,
            ),
        )

        assertThat(uiState.removeDialogText).isNotNull()
        assertThat(uiState.removeDialogText!!.resolve())
            .isEqualTo("Rick Sanchez will no longer be saved for offline viewing.")
    }
}
