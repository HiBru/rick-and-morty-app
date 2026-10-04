package de.shinz.rickandmortyshowcase.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/* ============================================================
   Typography — five styles, and the type scale lives here
   There is no AppFontSize group: a size is never chosen independently of the
   weight and line height it belongs with, so the style *is* the token. A
   `fontSize =` or `fontWeight =` override at a call site means a style is
   missing from this file — add it here instead.

   The system font family, deliberately: no custom font means no @Composable
   Font() call, which is what lets these be plain top-level vals rather than
   something remembered inside composition.
   Read via AppTheme.typography (see Theme.kt).
   ============================================================ */

@Immutable
data class AppTypography(
    /** The detail screen's character name. One per screen at most. */
    val displayTitle: TextStyle,
    /** A screen's own title, and the empty-state headline. */
    val screenTitle: TextStyle,
    /** A character's name in a list row; a settings option's label. */
    val cardTitle: TextStyle,
    /** Body copy, and a detail screen's field values. */
    val body: TextStyle,
    /** Species, status, field labels. Pair with colors.onSurfaceMuted. */
    val caption: TextStyle,
)

private val DisplayTitle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 28.sp,
    lineHeight = 34.sp,
    // Negative tracking at display sizes: default spacing reads loose once the
    // glyphs are this large.
    letterSpacing = (-0.4).sp,
)

private val ScreenTitle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
    letterSpacing = (-0.2).sp,
)

private val CardTitle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 17.sp,
    lineHeight = 22.sp,
)

private val Body = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 21.sp,
    letterSpacing = 0.1.sp,
)

private val Caption = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    lineHeight = 17.sp,
    letterSpacing = 0.2.sp,
)

val appTypography = AppTypography(
    displayTitle = DisplayTitle,
    screenTitle = ScreenTitle,
    cardTitle = CardTitle,
    body = Body,
    caption = Caption,
)

/**
 * The same five styles mapped onto the Material 3 slots.
 *
 * Material components — `AlertDialog`, `NavigationBar` labels, `Button` — read
 * `MaterialTheme.typography` and never `AppTheme.typography`. Without this they
 * render at stock M3 sizes, so a dialog would not match the screen behind it.
 *
 * Each slot takes the app style closest in *role*, which is not always within
 * 1sp of the M3 default:
 *  - `headlineSmall` is the `AlertDialog` title slot (M3 24/Regular → 22/SemiBold).
 *    Leaving it stock was the one gap that defeated the point of this mapping.
 *  - `headlineMedium` and `titleLarge` keep the M3 size and only add weight.
 *  - `titleMedium`, `bodyLarge`, `bodyMedium` and `labelMedium` are within 1sp at
 *    the same weight — the skill's reuse criterion — and are mapped only so the
 *    call site reads `AppTheme.typography.cardTitle` instead of `titleMedium`.
 *  - `labelLarge` and `labelSmall` are left **stock**: M3's 14/11 Medium already
 *    sit inside this scale, and overriding `labelLarge` with `cardTitle` made
 *    every dialog button 21% larger than intended.
 */
val appMaterialTypography = Typography(
    headlineMedium = DisplayTitle,
    headlineSmall = ScreenTitle,
    titleLarge = ScreenTitle,
    titleMedium = CardTitle,
    bodyLarge = Body,
    bodyMedium = Body,
    bodySmall = Caption,
    labelMedium = Caption,
)

val LocalAppTypography = staticCompositionLocalOf { appTypography }
