package de.shinz.rickandmortyshowcase.features.characterdetail.presentation.assembler

import de.shinz.rickandmortyshowcase.core.domain.model.Character
import de.shinz.rickandmortyshowcase.core.format.AppDateTimeManager
import de.shinz.rickandmortyshowcase.core.ui.UiText
import de.shinz.rickandmortyshowcase.core.ui.toUiText
import de.shinz.rickandmortyshowcase.features.characterdetail.domain.model.CharacterDetailData
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailFieldUi
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailLoad
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailUi
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailUiState
import de.shinz.rickandmortyshowcase.features.characterdetail.presentation.model.CharacterDetailViewModelState
import de.shinz.rickandmortyshowcase.features.shared.presentation.model.favoriteContentDescription
import de.shinz.rickandmortyshowcase.features.shared.presentation.model.statusLabel
import de.shinz.rickandmortyshowcase.generated.resources.Res
import de.shinz.rickandmortyshowcase.generated.resources.detail_created
import de.shinz.rickandmortyshowcase.generated.resources.detail_episodes
import de.shinz.rickandmortyshowcase.generated.resources.detail_gender
import de.shinz.rickandmortyshowcase.generated.resources.detail_location
import de.shinz.rickandmortyshowcase.generated.resources.detail_origin
import de.shinz.rickandmortyshowcase.generated.resources.detail_type
import de.shinz.rickandmortyshowcase.generated.resources.favorite_remove_message
import org.jetbrains.compose.resources.StringResource

/**
 * Turns the resolved character and its favourite flag into what the detail
 * screen renders.
 *
 * Takes [dateTimeManager] as its only constructor dependency — the one kind the
 * MVI skill allows an assembler, because it is synchronous and pure. It is the
 * reason the screen never sees an `Instant`: a format pattern in a composable is
 * a reimplementation of CLDR with one locale in it.
 *
 * Like the list's assembler, it is a pure projection: it never invents an input.
 */
class CharacterDetailUiStateAssembler(
    private val dateTimeManager: AppDateTimeManager,
) {

    fun assemble(
        data: CharacterDetailData,
        vmState: CharacterDetailViewModelState,
    ): CharacterDetailUiState {
        val failure = (vmState.load as? CharacterDetailLoad.Failed)?.error?.toUiText()
        val character = vmState.character

        return CharacterDetailUiState(
            isLoading = vmState.load is CharacterDetailLoad.Loading,
            error = failure,
            // Keyed on `Idle`, not on "no error": that makes the three top-level
            // states exclusive by construction rather than by two conditions
            // agreeing. A character beside an error would show stale content
            // under a message saying it failed; one beside a spinner would put
            // the spinner over it.
            character = character?.takeIf { vmState.load is CharacterDetailLoad.Idle }
                ?.toCharacterDetailUi(isFavorite = data.isFavorite),
            removeDialogText = if (vmState.isRemovalPending && character != null) {
                UiText.StringResourceText(
                    id = Res.string.favorite_remove_message,
                    args = listOf(character.name),
                )
            } else {
                null
            },
        )
    }

    private fun Character.toCharacterDetailUi(isFavorite: Boolean) = CharacterDetailUi(
        name = name,
        imageUrl = imageUrl,
        status = status,
        subtitle = UiText.Joined(
            listOf(UiText.StringResourceText(status.statusLabel()), UiText.DynamicString(species)),
        ),
        isFavorite = isFavorite,
        favoriteContentDescription = favoriteContentDescription(name, isFavorite),
        fields = fields(),
    )

    /**
     * The labelled rows, in the order `SPEC.md` lists them.
     *
     * **`type` is dropped when blank**, which is the one conditional field and
     * the reason this is a list rather than six properties — the API sends `""`
     * for most characters, and a row reading "Type:" with nothing after it is
     * worse than no row.
     *
     * `unknown` values are *kept*: the API says "unknown" for an origin it does
     * not know, which is information. Hiding it would leave the user unable to
     * tell "not known" from "not shown".
     */
    private fun Character.fields(): List<CharacterDetailFieldUi> = buildList {
        if (type.isNotBlank()) add(field(Res.string.detail_type, type))
        add(field(Res.string.detail_gender, gender))
        add(field(Res.string.detail_origin, originName))
        add(field(Res.string.detail_location, locationName))
        // The count, not the names: resolving those is one request per episode
        // and out of scope per SPEC. A bare number beside the "Episodes" label
        // also sidesteps pluralising a string the app has no plurals file for.
        add(field(Res.string.detail_episodes, episodeUrls.size.toString()))
        add(field(Res.string.detail_created, dateTimeManager.formatDate(created)))
    }
}

private fun field(label: StringResource, value: String) =
    CharacterDetailFieldUi(label = UiText.StringResourceText(label), value = value)
