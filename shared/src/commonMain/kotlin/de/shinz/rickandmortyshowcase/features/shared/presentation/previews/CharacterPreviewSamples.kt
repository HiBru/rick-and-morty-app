package de.shinz.rickandmortyshowcase.features.shared.presentation.previews

import de.shinz.rickandmortyshowcase.core.domain.model.Character
import de.shinz.rickandmortyshowcase.core.domain.model.CharacterStatus
import kotlin.time.Instant

/**
 * Sample characters for every preview in the app.
 *
 * One set rather than one per provider, because the character list, Favorites
 * and the detail screen all preview the same thing and three hand-written Ricks
 * would differ in exactly the fields a preview exists to show.
 *
 * A verbatim slice of `GET /character`, image urls included — so a sample can be
 * dropped into a running app to check a layout against data the API will really
 * send, which is how this row was verified on both platforms.
 *
 * **In an IDE preview the portraits will not load.** No `ImageLoader` is
 * configured there (`App()` never runs) and the renderer blocks network access,
 * so `AsyncImage` falls through to the flat placeholder fill. That is a property
 * of previews, not of these urls; Coil's `LocalAsyncImagePreviewHandler` could
 * substitute a painter, but it can only supply a solid colour here — which is
 * what the placeholder already draws.
 */
internal object CharacterPreviewSamples {

    /** The ordinary case: alive, a short name, a portrait that loads. */
    val rick = Character(
        id = 1,
        name = "Rick Sanchez",
        status = CharacterStatus.ALIVE,
        species = "Human",
        type = "",
        gender = "Male",
        originName = "Earth (C-137)",
        originUrl = "https://rickandmortyapi.com/api/location/1",
        locationName = "Citadel of Ricks",
        locationUrl = "https://rickandmortyapi.com/api/location/3",
        imageUrl = "https://rickandmortyapi.com/api/character/avatar/1.jpeg",
        episodeUrls = List(51) { "https://rickandmortyapi.com/api/episode/${it + 1}" },
        url = "https://rickandmortyapi.com/api/character/1",
        created = Instant.parse("2017-11-04T18:48:46.250Z"),
    )

    /** A dead character, so the status dot's second colour is on screen. */
    val abadango = Character(
        id = 6,
        name = "Abadango Cluster Princess",
        status = CharacterStatus.DEAD,
        species = "Alien",
        type = "",
        gender = "Female",
        originName = "Abadango",
        originUrl = "https://rickandmortyapi.com/api/location/2",
        locationName = "Abadango",
        locationUrl = "https://rickandmortyapi.com/api/location/2",
        imageUrl = "https://rickandmortyapi.com/api/character/avatar/6.jpeg",
        episodeUrls = listOf("https://rickandmortyapi.com/api/episode/27"),
        url = "https://rickandmortyapi.com/api/character/6",
        created = Instant.parse("2017-11-04T19:50:28.250Z"),
    )

    /**
     * The awkward one: unknown status, a name long enough to truncate, and a
     * **blank species** — which the API really does send, and which is the case
     * `UiText.Joined` exists to handle. A row showing "Unknown · " is the bug
     * this sample catches.
     */
    val longNameNoSpecies = Character(
        id = 331,
        name = "Mr. Nimbus, King of the Oceans and Rick's Nemesis",
        status = CharacterStatus.UNKNOWN,
        species = "",
        type = "Mythological creature",
        gender = "Male",
        originName = "unknown",
        originUrl = "",
        locationName = "Earth (Replacement Dimension)",
        locationUrl = "https://rickandmortyapi.com/api/location/20",
        imageUrl = "https://rickandmortyapi.com/api/character/avatar/331.jpeg",
        episodeUrls = listOf("https://rickandmortyapi.com/api/episode/41"),
        url = "https://rickandmortyapi.com/api/character/331",
        created = Instant.parse("2021-07-26T17:04:39.962Z"),
    )
}
