package de.shinz.rickandmortyshowcase.core.domain.repository

import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.Result
import de.shinz.rickandmortyshowcase.core.domain.model.Character

/**
 * The one place in this app that genuinely coordinates two sources, which is why
 * it is the only thing here called a repository — everything else is a data
 * source.
 *
 * It exists for exactly one read. The detail screen is handed a character id and
 * nothing else, so something has to decide where that character comes from:
 * the local row if it is favourited, the API otherwise. Because it spans both
 * sources its error type widens to plain [DataError].
 */
interface CharacterRepository {

    /**
     * The character behind [id] — local if it is favourited, remote if not.
     *
     * The local hit is what makes a favourited character open with no network,
     * and the remote fall-through is what makes the same screen work for the
     * other 800-odd characters.
     */
    suspend fun getCharacter(id: Int): Result<Character, DataError>
}
