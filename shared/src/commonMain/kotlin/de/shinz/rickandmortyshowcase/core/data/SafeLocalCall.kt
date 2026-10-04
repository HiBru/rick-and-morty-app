package de.shinz.rickandmortyshowcase.core.data

import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.Result
import kotlin.coroutines.cancellation.CancellationException

/**
 * The database counterpart of [safeCall]: turns a storage failure into a
 * [DataError.Local] instead of letting it escape as an exception.
 */
suspend fun <T> safeLocalCall(block: suspend () -> T): Result<T, DataError.Local> = try {
    Result.Success(block())
} catch (e: Exception) {
    if (e is CancellationException) throw e
    Result.Error(e.asLocalError())
}

/**
 * Best-effort recognition of a full disk, across both storage layers.
 *
 * Message matching is not a choice — `androidx.sqlite.SQLiteException` carries
 * **only** a message, with no result code exposed, so the text is the sole
 * signal available on either platform. "database or disk is full" is SQLite's
 * own canonical `errstr` for `SQLITE_FULL`, which is why it is stable enough to
 * match on. Anything unrecognised is [DataError.Local.UNKNOWN], whose message is
 * a safe fallback, so a miss costs precision rather than correctness.
 */
internal fun Throwable.asLocalError(): DataError.Local {
    val text = message?.lowercase()

    return if (
        text != null && (
            // SQLite's errstr, and androidx.sqlite's "Error code: N, message: …" wrapper.
            text.contains("disk is full") ||
                text.contains("error code: 13") ||
                // DataStore writes go through okio, which surfaces ENOSPC as
                // strerror text on both platforms — no SQLite wording involved.
                text.contains("no space left on device")
            )
    ) {
        DataError.Local.DISK_FULL
    } else {
        DataError.Local.UNKNOWN
    }
}
