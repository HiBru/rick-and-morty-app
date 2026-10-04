package de.shinz.rickandmortyshowcase.features.characterdetail.domain.usecase

import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.Result
import de.shinz.rickandmortyshowcase.core.domain.model.Character
import de.shinz.rickandmortyshowcase.core.domain.repository.CharacterRepository

/**
 * The character behind an id, wherever it comes from.
 *
 * A one-shot `suspend` read rather than a `Flow`: the detail screen is handed an
 * id and loads once. The favourite *state* on that screen is observed
 * separately, so nothing here needs to stay subscribed.
 *
 * A pass-through, and still worth existing — the boundary is the point. The
 * detail ViewModel depends on this rather than on `CharacterRepository`, so the
 * local-first strategy can change without the presentation layer noticing.
 */
class GetCharacterUseCase(
    private val characterRepository: CharacterRepository,
) {
    suspend operator fun invoke(id: Int): Result<Character, DataError> =
        characterRepository.getCharacter(id)
}
