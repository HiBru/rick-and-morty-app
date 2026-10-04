package de.shinz.rickandmortyshowcase.core.domain.model

/**
 * One page of the character list — 20 characters, per the API's page size.
 *
 * [hasMore] rather than a next-page number or url: the API answers paging with
 * `info.next`, which is a url or null, and the list ViewModel already tracks
 * which page it asked for. Handing it a url it would have to parse, or a number
 * it already knows, would be giving the domain layer a job the screen is doing.
 */
data class CharacterPage(
    val characters: List<Character>,
    val hasMore: Boolean,
)
