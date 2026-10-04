package de.shinz.rickandmortyshowcase.core.domain.datasource

import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.EmptyResult
import de.shinz.rickandmortyshowcase.core.domain.Result
import de.shinz.rickandmortyshowcase.core.domain.model.Character
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * In-memory favourites, backed by a `MutableStateFlow` so the observe functions
 * behave like the real ones: a write shows up in every active collector.
 *
 * Shared from Task 9 onwards — the repository test and every ViewModel test that
 * touches favourites use it.
 *
 * **It mirrors the real source's operator chain deliberately.** `observeFavoriteIds`
 * applies `distinctUntilChanged` because `RoomFavoriteCharacterLocalDataSource`
 * does; without it the fake emits a duplicate id set whenever an already-stored
 * character is re-favourited, and a downstream test would be written to consume
 * an emission the shipped app never produces.
 *
 * `error` is split in two because the observe functions have **no error channel**
 * by contract while the other three do — and because a toggle test needs "reads
 * fine, the write fails", which one shared switch cannot express.
 */
class FakeFavoriteCharacterLocalDataSource : FavoriteCharacterLocalDataSource {

    /** Insertion order: newest favourited **last**. */
    private val entries = MutableStateFlow<List<Character>>(emptyList())

    /** Fails [getFavorite] persistently. The observe functions ignore it, as the contract says. */
    var readError: DataError.Local? = null

    /**
     * Fails the next N [getFavorite] calls and then recovers — a lock, not a
     * corrupt row. Consumed one per call.
     */
    var transientReadFailures: Int = 0

    /** Fails [addFavorite] and [removeFavorite]. */
    var writeError: DataError.Local? = null

    /**
     * Replaces the degraded path's upstream, for the one behaviour a
     * `MutableStateFlow` cannot reproduce: the real chain emits its fallback and
     * then *completes*.
     */
    var observeFavoritesOverride: Flow<List<Character>>? = null

    var getFavoriteCallCount: Int = 0
        private set

    /**
     * Counts deletes, which is the only way to tell "removed once" from
     * "removed twice" — removing an absent id is a documented no-op, so the
     * stored contents look identical either way.
     */
    var removeFavoriteCallCount: Int = 0
        private set

    /** Current contents, newest favourited **last** — for assertions. */
    val stored: List<Character> get() = entries.value

    /**
     * Arranges favourites in the order they were favourited, so the last
     * argument is the newest. Use this rather than assigning a list: the
     * newest-last invariant is what makes [observeFavorites] come out in the
     * right order, and a positional assignment silently inverts it.
     */
    fun seed(vararg characters: Character) {
        entries.value = characters.toList()
    }

    override fun observeFavorites(): Flow<List<Character>> =
        observeFavoritesOverride ?: entries.map { it.reversed() }

    override fun observeFavoriteIds(): Flow<Set<Int>> = entries
        .map { characters -> characters.map { it.id }.toSet() }
        .distinctUntilChanged()

    override suspend fun getFavorite(id: Int): Result<Character?, DataError.Local> {
        getFavoriteCallCount++
        readError?.let { return Result.Error(it) }
        if (transientReadFailures > 0) {
            transientReadFailures--
            return Result.Error(DataError.Local.UNKNOWN)
        }

        return Result.Success(entries.value.firstOrNull { it.id == id })
    }

    override suspend fun addFavorite(character: Character): EmptyResult<DataError.Local> {
        writeError?.let { return Result.Error(it) }
        // Moves an already-stored character to the end, matching the real
        // @Upsert stamping a fresh addedAt and so moving the row to the front of
        // `ORDER BY addedAtEpochMillis DESC`.
        entries.value = entries.value.filterNot { it.id == character.id } + character

        return Result.Success(Unit)
    }

    override suspend fun removeFavorite(id: Int): EmptyResult<DataError.Local> {
        removeFavoriteCallCount++
        writeError?.let { return Result.Error(it) }
        entries.value = entries.value.filterNot { it.id == id }

        return Result.Success(Unit)
    }
}
