package de.shinz.rickandmortyshowcase.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/* ============================================================
   Colors — cool teal on slate
   Neutral surfaces carry the layout; a single teal accent carries everything
   interactive. The character artwork is the only other colour on screen, which
   is why the UI stays this quiet.

   The raw values are private: call sites read semantic names off
   AppTheme.colors, never a palette constant. Material 3 components read the
   ColorSchemes below, which are built from the same raw values so the two can
   never drift.
   ============================================================ */

private val BackgroundLight = Color(0xFFFAFAFA)
private val BackgroundDark = Color(0xFF0F1113)
private val SurfaceLight = Color(0xFFFFFFFF)
private val SurfaceDark = Color(0xFF181B1E)
private val SurfaceVariantLight = Color(0xFFF1F3F5)
private val SurfaceVariantDark = Color(0xFF21252A)
private val OnSurfaceLight = Color(0xFF1A1D20)
private val OnSurfaceDark = Color(0xFFE8EAED)
// #6B7280 was the obvious choice and fails WCAG AA at 4.35:1 on surfaceVariant,
// which is where captions inside a filled card sit. Darkened to 5.46:1.
private val OnSurfaceMutedLight = Color(0xFF5B636E)
private val OnSurfaceMutedDark = Color(0xFF9AA3AD)
private val AccentLight = Color(0xFF0F766E)
private val AccentDark = Color(0xFF2DD4BF)
private val OnAccentLight = Color(0xFFFFFFFF)
private val OnAccentDark = Color(0xFF0F1113)
private val BorderLight = Color(0xFFE4E7EB)
private val BorderDark = Color(0xFF2A2F35)
/*
 * The status colours tint a label as well as its dot, so the bar is 4.5:1, not
 * the 3:1 a non-text graphic would get. The light values are all darker than the
 * first choices for that reason: #16A34A measured 2.96:1 on surfaceVariant and
 * #DC2626 measured 4.34:1.
 *
 * Unknown deliberately *is* the muted foreground — an unknown status should read
 * as de-emphasised rather than as its own colour. The light/dark pair was also
 * transposed at first, which left a light grey on white at 2.54:1.
 */
private val StatusAliveLight = Color(0xFF15803D)
private val StatusAliveDark = Color(0xFF4ADE80)
private val StatusDeadLight = Color(0xFFB91C1C)
private val StatusDeadDark = Color(0xFFF87171)
private val StatusUnknownLight = OnSurfaceMutedLight
private val StatusUnknownDark = OnSurfaceMutedDark

/*
 * The snackbar's surface: deliberately the *opposite* side of the theme, so a
 * transient message reads as sitting on top of the app rather than in it. These
 * are their own tokens rather than a reuse of `onSurface`/`background`, even
 * though the values coincide today: those two are tuned for text contrast on a
 * surface, and retuning either would otherwise move the snackbar with them.
 */
private val InverseSurfaceLight = Color(0xFF1A1D20)
private val InverseSurfaceDark = Color(0xFFE8EAED)
private val OnInverseSurfaceLight = Color(0xFFFAFAFA)
private val OnInverseSurfaceDark = Color(0xFF181B1E)

/** A dim behind dialogs. Identical in both themes — it darkens what is there. */
private val Scrim = Color(0x99000000)

/**
 * Every colour the app draws with.
 *
 * Read via `AppTheme.colors`. The three `status*` values exist because a
 * character's status is a domain fact with a colour, not an app error — mapping
 * them lives in `AppStatusDot`, so no screen branches on status itself.
 */
@Immutable
data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    /** Secondary text: species, locations, captions. Never body copy. */
    val onSurfaceMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val border: Color,
    /** The snackbar's background — the theme inverted. Not a surface variant. */
    val inverseSurface: Color,
    val onInverseSurface: Color,
    val statusAlive: Color,
    val statusDead: Color,
    val statusUnknown: Color,
)

val appLightColors = AppColors(
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurface = OnSurfaceLight,
    onSurfaceMuted = OnSurfaceMutedLight,
    accent = AccentLight,
    onAccent = OnAccentLight,
    border = BorderLight,
    inverseSurface = InverseSurfaceLight,
    onInverseSurface = OnInverseSurfaceLight,
    statusAlive = StatusAliveLight,
    statusDead = StatusDeadLight,
    statusUnknown = StatusUnknownLight,
)

val appDarkColors = AppColors(
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurface = OnSurfaceDark,
    onSurfaceMuted = OnSurfaceMutedDark,
    accent = AccentDark,
    onAccent = OnAccentDark,
    border = BorderDark,
    inverseSurface = InverseSurfaceDark,
    onInverseSurface = OnInverseSurfaceDark,
    statusAlive = StatusAliveDark,
    statusDead = StatusDeadDark,
    statusUnknown = StatusUnknownDark,
)

/* ============================================================
   Material 3 ColorSchemes
   Scaffold, NavigationBar, AlertDialog and friends read these, not AppColors —
   so they are mapped from the same raw values rather than left on the M3
   defaults, which would put stock purple in the bottom bar.

   One accent, so primary, secondary and tertiary are the same teal. `error`
   reuses the dead-status red rather than introducing a second one. They *can*
   share a screen — the remove-confirmation dialog tints its destructive action
   while status dots sit behind the scrim — which is an argument for one red
   rather than against it.

   Every slot the app's components actually read is mapped, because an unmapped
   one is not a fallback — it is stock M3 purple. The three that bite:
     - secondaryContainer / onSecondaryContainer → the NavigationBar's selected
       pill and icon, on screen for all three tabs.
     - surfaceContainerHigh → every AlertDialog background, and SPEC puts a
       confirmation dialog on three screens.
     - inverseSurface / inversePrimary → Snackbar background and action label.
   The whole surfaceContainer family maps to `surface` so dialogs, cards and the
   bottom bar all sit a shade above the screen background; `surfaceVariant` is
   kept for deliberately filled areas via surfaceContainerHighest.
   ============================================================ */

val LightColorScheme = lightColorScheme(
    primary = AccentLight,
    onPrimary = OnAccentLight,
    primaryContainer = AccentLight,
    onPrimaryContainer = OnAccentLight,
    secondary = AccentLight,
    onSecondary = OnAccentLight,
    secondaryContainer = AccentLight,
    onSecondaryContainer = OnAccentLight,
    tertiary = AccentLight,
    onTertiary = OnAccentLight,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceMutedLight,
    surfaceContainerLowest = SurfaceLight,
    surfaceContainerLow = SurfaceLight,
    surfaceContainer = SurfaceLight,
    surfaceContainerHigh = SurfaceLight,
    surfaceContainerHighest = SurfaceVariantLight,
    surfaceDim = SurfaceVariantLight,
    surfaceBright = SurfaceLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = OnInverseSurfaceLight,
    inversePrimary = AccentDark,
    outline = BorderLight,
    outlineVariant = BorderLight,
    error = StatusDeadLight,
    onError = OnAccentLight,
    scrim = Scrim,
)

val DarkColorScheme = darkColorScheme(
    primary = AccentDark,
    onPrimary = OnAccentDark,
    primaryContainer = AccentDark,
    onPrimaryContainer = OnAccentDark,
    secondary = AccentDark,
    onSecondary = OnAccentDark,
    secondaryContainer = AccentDark,
    onSecondaryContainer = OnAccentDark,
    tertiary = AccentDark,
    onTertiary = OnAccentDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceMutedDark,
    surfaceContainerLowest = SurfaceDark,
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = SurfaceDark,
    surfaceContainerHighest = SurfaceVariantDark,
    surfaceDim = SurfaceVariantDark,
    surfaceBright = SurfaceDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = OnInverseSurfaceDark,
    inversePrimary = AccentLight,
    outline = BorderDark,
    outlineVariant = BorderDark,
    error = StatusDeadDark,
    onError = OnAccentDark,
    scrim = Scrim,
)
