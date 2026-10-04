package de.shinz.rickandmortyshowcase.core.format

import kotlin.time.Instant

/**
 * The one entry point for formatting a date the app displays.
 *
 * No `DateTimeFormatter` or `NSDateFormatter` anywhere else, and no format
 * pattern in a composable — a hardcoded `"dd.MM.yyyy"` is a reimplementation of
 * CLDR with one locale in it.
 *
 * A `class` rather than the skill's `object`, even though the language is fixed
 * for the process. An assembler takes it as a constructor dependency — the one
 * kind of dependency the MVI skill allows an assembler to have, because it is
 * synchronous and pure — and a preview provider has to be able to construct one.
 * Neither works with a global.
 *
 * It formats what it is handed and never asks what time it is, so there is no
 * `Clock` here: nothing in this app shows a relative date.
 */
class AppDateTimeManager {

    private val formatter = PlatformDateTimeFormatter()

    /**
     * A character's creation date as "Nov 4, 2017", in the device's time zone and
     * always in English — the app has no other language, so a date rendered in
     * the phone's language would disagree with the text beside it.
     */
    fun formatDate(instant: Instant): String = formatter.date(instant)
}
