package de.shinz.rickandmortyshowcase.core.data

/**
 * The Rick and Morty API root.
 *
 * A plain constant rather than `BuildConfig.BASE_URL`, which the error-handling
 * skill's `constructRoute` reads: `BuildConfig` does not exist in `commonMain`,
 * and this API is public and read-only, so there is no secret to keep out of the
 * binary.
 */
const val BASE_URL: String = "https://rickandmortyapi.com/api"

/**
 * Turns a route into an absolute url, accepting either form.
 *
 * `startsWith` rather than the skill's `contains`: `contains` also matches the
 * base url sitting inside a *query value*, which would pass the whole string
 * through unprefixed — and Ktor resolves a relative url against its default
 * origin, so the request would quietly go to `localhost` instead of failing.
 *
 * The pass-through branch is forward compatibility, not a live path: nothing
 * currently feeds back an absolute url. It exists because the API hands them out
 * (`info.next`, a character's own `url`) and prefixing one twice is the kind of
 * bug that produces a confusing 404 rather than an error.
 */
fun constructRoute(route: String): String = when {
    route.startsWith(BASE_URL) -> route
    route.startsWith("/") -> BASE_URL + route
    else -> "$BASE_URL/$route"
}
