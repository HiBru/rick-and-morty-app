package de.shinz.rickandmortyshowcase.core.domain

/**
 * The shared failure vocabulary of the data layer.
 *
 * Hoisted into one type because this app genuinely has two sources that fail in
 * their own ways — the Rick and Morty API and the favourites database. A data
 * source narrows to [Network] or [Local]; only a repository coordinating both
 * widens to plain `DataError`.
 *
 * Deliberately shorter than the canonical list in `android-error-handling`:
 * every constant here is reachable, and every one costs a branch plus a string
 * resource in `toUiText()`. There is no `UNAUTHORIZED`, `FORBIDDEN` or
 * `PAYLOAD_TOO_LARGE` because the API is public and read-only — the app never
 * authenticates and never uploads, so those would be dead branches explaining a
 * login the user does not have. Anything genuinely unexpected is [Network.UNKNOWN].
 */
sealed interface DataError : Error {

    enum class Network : DataError {
        /** No usable connection. The one failure the user can act on. */
        NO_INTERNET,
        REQUEST_TIMEOUT,

        /** A character id or page that does not exist. */
        NOT_FOUND,

        /** The API rate-limits, so this one is real rather than defensive. */
        TOO_MANY_REQUESTS,

        /** Any 5xx. */
        SERVER_ERROR,

        /** The response arrived but did not match the DTOs — our bug, not theirs. */
        SERIALIZATION,
        UNKNOWN,
    }

    enum class Local : DataError {
        /** Saving a favourite failed because the device is out of space. */
        DISK_FULL,
        UNKNOWN,
        /*
         * No NOT_FOUND: a character that is not favourited is the normal case,
         * not a failure, so the local source returns `Character?` and reserves
         * Result.Error for things that actually went wrong. Deleting a row that
         * is already gone is a no-op for the same reason.
         */
    }
}
