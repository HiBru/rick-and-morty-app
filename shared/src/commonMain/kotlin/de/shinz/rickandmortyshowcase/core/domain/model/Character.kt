package de.shinz.rickandmortyshowcase.core.domain.model

import kotlin.time.Instant

/**
 * A Rick and Morty character.
 *
 * Carries every value the API returns, including the urls and the episode list
 * that no screen displays. That is deliberate: `SPEC.md` requires favouriting to
 * persist the whole record so a favourited character renders completely offline,
 * and the only path into the database is through this model.
 */
data class Character(
    val id: Int,
    val name: String,
    val status: CharacterStatus,
    val species: String,
    /** Subspecies or variant. Usually blank — the API sends `""`, not null. */
    val type: String,
    val gender: String,
    val originName: String,
    val originUrl: String,
    val locationName: String,
    val locationUrl: String,
    val imageUrl: String,
    /**
     * One url per episode the character appears in. The detail screen shows only
     * the count; resolving the names would cost one request per episode and is
     * out of scope per `SPEC.md`.
     */
    val episodeUrls: List<String>,
    /** This character's own API endpoint. */
    val url: String,
    val created: Instant,
)
