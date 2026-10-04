package de.shinz.rickandmortyshowcase.core.format

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.Instant

/**
 * `java.time` + CLDR. Available natively at `minSdk 26`, so no desugaring.
 *
 * The formatter carries the zone because [DateTimeFormatter.format] needs one to
 * resolve date fields from an instant, and it is built once — `DateTimeFormatter`
 * is immutable and thread-safe.
 */
internal actual class PlatformDateTimeFormatter actual constructor() {

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.ENGLISH)
            .withZone(ZoneId.systemDefault())

    actual fun date(instant: Instant): String = dateFormatter.format(
        java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds()),
    )
}
