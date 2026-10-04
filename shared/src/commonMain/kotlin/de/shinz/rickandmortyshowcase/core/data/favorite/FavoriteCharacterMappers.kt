package de.shinz.rickandmortyshowcase.core.data.favorite

import de.shinz.rickandmortyshowcase.core.database.FavoriteCharacterEntity
import de.shinz.rickandmortyshowcase.core.domain.model.Character
import de.shinz.rickandmortyshowcase.core.domain.model.CharacterStatus
import kotlinx.serialization.json.Json
import kotlin.time.Instant

private val episodeJson = Json

/**
 * Domain to row.
 *
 * @param addedAt when the user favourited this character. Supplied by the data
 *   source from an injected `Clock` rather than read here, because a mapper that
 *   asks what time it is cannot be tested.
 */
internal fun Character.toFavoriteEntity(addedAt: Instant): FavoriteCharacterEntity =
    FavoriteCharacterEntity(
        id = id,
        name = name,
        status = status.name,
        species = species,
        type = type,
        gender = gender,
        originName = originName,
        originUrl = originUrl,
        locationName = locationName,
        locationUrl = locationUrl,
        imageUrl = imageUrl,
        episodeUrlsJson = episodeJson.encodeToString(episodeUrls),
        url = url,
        createdEpochMillis = created.toEpochMilliseconds(),
        addedAtEpochMillis = addedAt.toEpochMilliseconds(),
    )

/** Row to domain. `addedAt` is dropped — no screen shows it. */
internal fun FavoriteCharacterEntity.toCharacter(): Character = Character(
    id = id,
    name = name,
    status = status.toCharacterStatus(),
    species = species,
    type = type,
    gender = gender,
    originName = originName,
    originUrl = originUrl,
    locationName = locationName,
    locationUrl = locationUrl,
    imageUrl = imageUrl,
    episodeUrls = episodeJson.decodeFromString(episodeUrlsJson),
    url = url,
    created = Instant.fromEpochMilliseconds(createdEpochMillis),
)

/**
 * Not `CharacterStatus.valueOf`, which throws.
 *
 * The stored string came from an older build of this app, so renaming a constant
 * would otherwise make every existing favourite unreadable — and the user's own
 * saved data is the last thing that should break on an upgrade.
 */
private fun String.toCharacterStatus(): CharacterStatus =
    CharacterStatus.entries.firstOrNull { it.name == this } ?: CharacterStatus.UNKNOWN
