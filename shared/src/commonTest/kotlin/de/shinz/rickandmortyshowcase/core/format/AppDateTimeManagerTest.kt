package de.shinz.rickandmortyshowcase.core.format

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotEqualTo
import kotlin.test.Test
import kotlin.time.Instant

/**
 * Properties rather than exact strings, deliberately.
 *
 * The output is produced by the platform's CLDR data in the device's time zone,
 * so an exact assertion would be pinned to one CLDR version and one machine's
 * zone — and would differ between Android ICU and `NSDateFormatter` even when
 * both are right. The instant below is mid-June at midday UTC so that no real
 * zone offset (−12 to +14) can move it into another *year*, which is the one
 * thing worth asserting about the formatted text.
 *
 * What the format actually looks like is verified by looking at the detail screen
 * on both platforms in Task 19.
 */
class AppDateTimeManagerTest {

    private val manager = AppDateTimeManager()

    @Test
    fun formatsADateContainingItsYear() {
        val formatted = manager.formatDate(MIDYEAR_2017)

        assertThat(formatted).isNotEmpty()
        assertThat(formatted).contains("2017")
    }

    @Test
    fun isDeterministicForTheSameInstant() {
        // The formatter is built once and reused; this catches it picking up
        // mutable state between calls.
        assertThat(manager.formatDate(MIDYEAR_2017))
            .isEqualTo(manager.formatDate(MIDYEAR_2017))
    }

    @Test
    fun twoSeparateManagersAgree() {
        assertThat(AppDateTimeManager().formatDate(MIDYEAR_2017))
            .isEqualTo(AppDateTimeManager().formatDate(MIDYEAR_2017))
    }

    @Test
    fun distinguishesDifferentDays() {
        assertThat(manager.formatDate(MIDYEAR_2017))
            .isNotEqualTo(manager.formatDate(MIDYEAR_2018))
    }

    @Test
    fun dropsTheTimeOfDay() {
        // A medium date style, not a date-time: two moments on the same day in
        // every zone must render identically.
        val morning = Instant.parse("2017-06-15T11:00:00Z")
        val noon = Instant.parse("2017-06-15T12:00:00Z")

        assertThat(manager.formatDate(morning)).isEqualTo(manager.formatDate(noon))
    }

    @Test
    fun handlesTheApiTimestampFormat() {
        // The exact shape the API sends, milliseconds and all.
        val created = Instant.parse("2017-11-04T18:48:46.250Z")

        assertThat(manager.formatDate(created)).contains("2017")
    }

    private companion object {
        val MIDYEAR_2017 = Instant.parse("2017-06-15T12:00:00Z")
        val MIDYEAR_2018 = Instant.parse("2018-06-15T12:00:00Z")
    }
}
