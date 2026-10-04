package de.shinz.rickandmortyshowcase.core.format

import kotlin.time.Instant

/**
 * The platform's CLDR date formatting.
 *
 * An `expect class` rather than an `expect fun` so each platform builds its
 * formatter once — an `NSDateFormatter` costs real time to construct, and a
 * `java.time.DateTimeFormatter` is immutable and reusable.
 *
 * Two deliberate departures from `android-date-time-manager`:
 *
 * The seam takes a [kotlin.time.Instant] rather than a `kotlinx.datetime.LocalDate`.
 * The skill's principle 4 lists `Instant` among the acceptable typed values, and
 * the stdlib one covers everything this app needs — see the dependency table in
 * `docs/IMPLEMENTATION_PLAN.md` for why `kotlinx-datetime` is not declared.
 *
 * There is no `locale` constructor parameter. The app is English only, so the
 * formatter pins English instead of following the device. That is the skill's own
 * reasoning applied to a single-language app: date language and text language
 * then agree by construction, including on a phone set to a third language.
 */
internal expect class PlatformDateTimeFormatter() {

    /**
     * A medium-style date — "Nov 4, 2017".
     *
     * Rendered in the device's time zone, not UTC. The reference implementation
     * pins UTC, but it is formatting zone-less `LocalDate`s, where the `NSDate`
     * standing in for one has to be read back in the zone it was written in.
     * This formats a real moment, which belongs in the reader's zone.
     */
    fun date(instant: Instant): String
}
