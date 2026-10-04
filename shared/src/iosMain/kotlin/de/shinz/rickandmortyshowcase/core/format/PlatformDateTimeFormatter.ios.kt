package de.shinz.rickandmortyshowcase.core.format

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import kotlin.time.Instant

/**
 * `NSDateFormatter` + CLDR.
 *
 * Built once, because constructing an `NSDateFormatter` costs real time. The
 * `timeZone` is deliberately left unset so it defaults to the device's — the
 * counterpart of `ZoneId.systemDefault()` on Android.
 */
internal actual class PlatformDateTimeFormatter actual constructor() {

    private val dateFormatter: NSDateFormatter = NSDateFormatter().apply {
        locale = NSLocale(localeIdentifier = EN_US)
        dateStyle = NSDateFormatterMediumStyle
        timeStyle = NSDateFormatterNoStyle
    }

    actual fun date(instant: Instant): String = dateFormatter.stringFromDate(
        NSDate.dateWithTimeIntervalSince1970(
            instant.toEpochMilliseconds() / MILLIS_PER_SECOND,
        ),
    )

    private companion object {
        const val EN_US = "en_US"
        const val MILLIS_PER_SECOND = 1_000.0
    }
}
