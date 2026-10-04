package de.shinz.rickandmortyshowcase.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import de.shinz.rickandmortyshowcase.core.domain.model.ThemeMode

/* ============================================================
   App theme
   Combines the Material 3 ColorScheme and type scale with the six extended
   token groups (colors, spacing, radius, size, border, typography) into one
   wrapper.

   `AppTheme` is deliberately both a composable and an object — the same shape
   Material 3 itself uses for `MaterialTheme`. `AppTheme { }` wraps; `AppTheme.colors`
   reads.
   ============================================================ */

val LocalAppColors = staticCompositionLocalOf { appLightColors }

/*
 * None of these vary by theme, so they are allocated once at class init rather
 * than on every composition of AppTheme. Typography is in this list because the
 * app uses the system font: with no @Composable Font() call to build a
 * FontFamily, there is nothing to remember inside composition.
 */
private val SpacingTokens = AppSpacing()
private val RadiusTokens = AppRadius()
private val SizeTokens = AppSize()
private val BorderTokens = AppBorder()

/**
 * Defaults to [ThemeMode.SYSTEM], and must keep doing so: `@PreviewLightDark`
 * renders both themes from one preview function by varying the preview's
 * `uiMode`, which only works if the theme follows the system when told nothing.
 * No preview should ever pass `themeMode` explicitly.
 */
@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = themeMode.isDark()

    CompositionLocalProvider(
        LocalAppColors provides if (darkTheme) appDarkColors else appLightColors,
        LocalAppSpacing provides SpacingTokens,
        LocalAppRadius provides RadiusTokens,
        LocalAppSize provides SizeTokens,
        LocalAppBorder provides BorderTokens,
        LocalAppTypography provides appTypography,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = appMaterialTypography,
            content = content,
        )
    }
}

/**
 * [ThemeMode.SYSTEM] is the only mode that has to ask anyone — and because
 * `isSystemInDarkTheme()` is observed, it keeps asking.
 */
@Composable
@ReadOnlyComposable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

/**
 * Every design value the app draws with. Inside [AppTheme], prefer these over
 * `MaterialTheme.*` in app code — Material components read the M3 scheme
 * themselves, and `AppTheme` is where the semantic names live.
 *
 * Alias [size] at the call site as `sizes`: a local named `size` shadows
 * `DrawScope.size`.
 */
object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    /** Gaps and paddings only. */
    val spacing: AppSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalAppSpacing.current

    /** Corner radii. */
    val radius: AppRadius
        @Composable
        @ReadOnlyComposable
        get() = LocalAppRadius.current

    /** Icon, control and component dimensions. */
    val size: AppSize
        @Composable
        @ReadOnlyComposable
        get() = LocalAppSize.current

    /** Stroke widths — not the border *colour*, which is `colors.border`. */
    val border: AppBorder
        @Composable
        @ReadOnlyComposable
        get() = LocalAppBorder.current

    /** The five text styles. The type scale lives here, not in a size group. */
    val typography: AppTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTypography.current
}
