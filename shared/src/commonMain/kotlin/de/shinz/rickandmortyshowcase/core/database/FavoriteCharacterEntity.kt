package de.shinz.rickandmortyshowcase.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A favourited character, flattened for storage.
 *
 * Carries every value the API returns, because `SPEC.md` requires a favourited
 * character to render completely offline and this row is the only copy.
 *
 * Two columns are not part of the API record:
 *
 * [addedAtEpochMillis] is when the *user* favourited it, and exists because
 * nothing in the API record can order the Favorites screen —
 * `Character.created` is the API's own bookkeeping timestamp and has no relation
 * to the user's actions. It is deliberately absent from the domain model: no
 * screen shows it, and only the database needs it.
 *
 * [episodeUrlsJson] holds the episode list as a JSON array string rather than
 * through a `TypeConverter`. The entity mapper already exists and already owns
 * every other shape change, so a converter would only split that knowledge
 * across two places.
 */
@Entity(tableName = "favorite_characters")
internal data class FavoriteCharacterEntity(
    @PrimaryKey val id: Int,
    val name: String,
    /** [de.shinz.rickandmortyshowcase.core.domain.model.CharacterStatus] by name. */
    val status: String,
    val species: String,
    val type: String,
    val gender: String,
    val originName: String,
    val originUrl: String,
    val locationName: String,
    val locationUrl: String,
    val imageUrl: String,
    val episodeUrlsJson: String,
    val url: String,
    /** The API's `created`, as epoch millis — its precision is milliseconds. */
    val createdEpochMillis: Long,
    val addedAtEpochMillis: Long,
)
