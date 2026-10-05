package de.shinz.rickandmortyshowcase.core.designsystem

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

/**
 * The one piece of geometry three screens share, and the designated place to fix
 * a landscape or cutout inset — so the place an RTL mistake would hide.
 *
 * It is a plain function precisely so this test can exist: reading
 * `LocalLayoutDirection` inside would have made it `@Composable` and pushed the
 * check onto a simulator.
 */
class ContentPaddingTest {

    private val window = PaddingValues(start = 10.dp, top = 20.dp, end = 30.dp, bottom = 40.dp)

    @Test
    fun addsTheInsetToEverySide() {
        val result = window.plus(LayoutDirection.Ltr, horizontal = 5.dp, vertical = 7.dp)

        assertThat(result.calculateStartPadding(LayoutDirection.Ltr)).isEqualTo(15.dp)
        assertThat(result.calculateEndPadding(LayoutDirection.Ltr)).isEqualTo(35.dp)
        assertThat(result.calculateTopPadding()).isEqualTo(27.dp)
        assertThat(result.calculateBottomPadding()).isEqualTo(47.dp)
    }

    /**
     * Start stays start under RTL rather than becoming left.
     *
     * The failure this guards is silent: mixing a direction-relative read with an
     * absolute write looks correct in LTR and mirrors the gutters in RTL, which
     * is exactly the sort of thing nobody notices without an RTL device.
     */
    @Test
    fun keepsPaddingDirectionRelativeUnderRtl() {
        val result = window.plus(LayoutDirection.Rtl, horizontal = 5.dp)

        assertThat(result.calculateStartPadding(LayoutDirection.Rtl)).isEqualTo(15.dp)
        assertThat(result.calculateEndPadding(LayoutDirection.Rtl)).isEqualTo(35.dp)
    }

    /** The defaults are the identity, so a caller can add on one axis only. */
    @Test
    fun addsNothingWhenGivenNothing() {
        val result = window.plus(LayoutDirection.Ltr)

        assertThat(result.calculateStartPadding(LayoutDirection.Ltr)).isEqualTo(10.dp)
        assertThat(result.calculateBottomPadding()).isEqualTo(40.dp)
    }
}
