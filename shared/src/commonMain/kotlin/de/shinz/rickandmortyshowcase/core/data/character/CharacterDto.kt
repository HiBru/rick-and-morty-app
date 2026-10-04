package de.shinz.rickandmortyshowcase.core.data.character

import kotlinx.serialization.Serializable

/**
 * The `/character` list response.
 *
 * The API wraps results in an `info` envelope. Only `next` is carried: `count`,
 * `pages` and `prev` are never read, and `ignoreUnknownKeys` drops them for free,
 * so declaring them would only invite someone to believe they matter.
 */
@Serializable
data class CharacterPageDto(
    val info: PageInfoDto,
    val results: List<CharacterDto>,
)

@Serializable
data class PageInfoDto(
    /**
     * Absolute url of the next page. The API sends an explicit `null` on the last
     * page — this is the only genuinely nullable field in the whole response, and
     * it is what [toCharacterPage] reduces to `hasMore`.
     */
    val next: String? = null,
)

/**
 * One character, exactly as the API shapes it.
 *
 * Field names mirror the wire format rather than the domain — `image` not
 * `imageUrl`, `episode` not `episodeUrls` — because renaming here would need
 * `@SerialName` on half the class for no gain. The mapper does the renaming.
 *
 * **No field has a default, deliberately.** All twelve keys are present on every
 * character the API serves; `type` and an unknown origin's `url` arrive as `""`,
 * which is a *value*, not an absent key. A default would therefore not add
 * robustness, it would hide a contract change: `image = ""` renders a blank
 * avatar with no error, `episode = emptyList()` shows "0 episodes", and Task 7
 * would then persist that half-record as the user's favourite — defeating the
 * offline guarantee in `SPEC.md` silently and permanently. Missing fields should
 * fail loudly as `SERIALIZATION` instead.
 */
@Serializable
data class CharacterDto(
    val id: Int,
    val name: String,
    val status: String,
    val species: String,
    /** Subspecies or variant. Usually `""` — present but empty. */
    val type: String,
    val gender: String,
    val origin: LocationRefDto,
    val location: LocationRefDto,
    val image: String,
    val episode: List<String>,
    val url: String,
    /** ISO-8601 with milliseconds, e.g. `2017-11-04T18:48:46.250Z`. */
    val created: String,
)

/** A named reference to a location. An unknown origin's `url` is `""`. */
@Serializable
data class LocationRefDto(
    val name: String,
    val url: String,
)
