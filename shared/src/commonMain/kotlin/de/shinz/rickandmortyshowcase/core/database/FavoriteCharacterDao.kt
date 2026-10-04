package de.shinz.rickandmortyshowcase.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface FavoriteCharacterDao {

    /** Most recently favourited first — see `addedAtEpochMillis`. */
    @Query("SELECT * FROM favorite_characters ORDER BY addedAtEpochMillis DESC")
    fun observeAll(): Flow<List<FavoriteCharacterEntity>>

    /**
     * Ids only, for marking rows elsewhere.
     *
     * The narrow projection does **not** on its own stop this re-emitting: Room
     * invalidates per table, so any write to `favorite_characters` re-runs the
     * query. What it buys is a payload cheap enough to compare, which is why the
     * data source can drop the duplicates with `distinctUntilChanged()`.
     */
    @Query("SELECT id FROM favorite_characters")
    fun observeIds(): Flow<List<Int>>

    @Query("SELECT * FROM favorite_characters WHERE id = :id")
    suspend fun getById(id: Int): FavoriteCharacterEntity?

    /**
     * Upsert, not insert: favouriting a character that is somehow already stored
     * should refresh the row rather than fail on the primary key.
     */
    @Upsert
    suspend fun upsert(entity: FavoriteCharacterEntity)

    /** A no-op when the row is already gone, which the contract allows. */
    @Query("DELETE FROM favorite_characters WHERE id = :id")
    suspend fun deleteById(id: Int)
}
