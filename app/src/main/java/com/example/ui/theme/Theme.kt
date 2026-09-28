package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================
// Brand "Red" Material 3 Color Schemes
// ============================================================================

/**
 * Material 3 Light Color Scheme adhering strictly to Brand Red specifications:
 * Vibrant crimson primary, rich ruby accents, crisp surfaces, and clear contrast.
 */
val BrandRedLightColorScheme = lightColorScheme(
    primary = PrimaryRed,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    inversePrimary = Color(0xFFFFB4AB),
    secondary = BrandRedAccent,
    onSecondary = Color.White,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = AccentGreen,
    onTertiary = Color.White,
    tertiaryContainer = AccentGreenContainer,
    onTertiaryContainer = Color(0xFF1B5E20),
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    surfaceTint = PrimaryRed,
    inverseSurface = SurfaceDark,
    inverseOnSurface = TextPrimaryDark,
    outline = BorderLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorLight,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = Color(0xFF410002)
)

/**
 * Material 3 Dark Color Scheme adhering strictly to Brand Red specifications:
 * OLED deep dark surfaces, high-contrast radiant red primary, and warm gold accents.
 */
val BrandRedDarkColorScheme = darkColorScheme(
    primary = PrimaryRedDarkTheme,
    onPrimary = Color(0xFF5C0003),
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    inversePrimary = PrimaryRed,
    secondary = AccentGoldDark,
    onSecondary = Color(0xFF3F2E00),
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = AccentGreenLight,
    onTertiary = Color(0xFF00390E),
    tertiaryContainer = AccentGreenContainerDark,
    onTertiaryContainer = Color(0xFFA5D6A7),
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceTint = PrimaryRedDarkTheme,
    inverseSurface = SurfaceLight,
    inverseOnSurface = TextPrimaryLight,
    outline = BorderDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorDark,
    onError = Color(0xFF690005),
    errorContainer = ErrorContainerDark,
    onErrorContainer = Color(0xFFFFDAD6)
)

// ============================================================================
// Dynamic Theme Controller & CompositionLocal
// ============================================================================

/**
 * Controller allowing any composable in the hierarchy to inspect and dynamically
 * toggle between Light and Dark mode.
 */
@Stable
interface ThemeController {
    val isDark: Boolean
    fun toggleTheme()
    fun setDarkTheme(enabled: Boolean)
}

val LocalThemeController = staticCompositionLocalOf<ThemeController> {
    object : ThemeController {
        override val isDark: Boolean = false
        override fun toggleTheme() {}
        override fun setDarkTheme(enabled: Boolean) {}
    }
}

// ============================================================================
// Custom Theme Wrapper Composables
// ============================================================================

/**
 * Custom Material 3 Theme Wrapper with dynamic toggle between Light and Dark mode,
 * styled with the brand's 'Red' theme requirement.
 *
 * @param initialDarkTheme Optional override for initial dark mode state. Defaults to system preference.
 * @param onThemeChanged Callback invoked whenever the theme is dynamically toggled.
 * @param content The composable subtree wrapped with Brand M3 theme and [LocalThemeController].
 */
@Composable
fun BrandThemeWrapper(
    initialDarkTheme: Boolean? = null,
    onThemeChanged: ((Boolean) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    var isDarkState by remember(initialDarkTheme) {
        mutableStateOf(initialDarkTheme ?: systemDark)
    }

    val themeController = remember(isDarkState) {
        object : ThemeController {
            override val isDark: Boolean get() = isDarkState

            override fun toggleTheme() {
                val nextState = !isDarkState
                isDarkState = nextState
                onThemeChanged?.invoke(nextState)
            }

            override fun setDarkTheme(enabled: Boolean) {
                if (isDarkState != enabled) {
                    isDarkState = enabled
                    onThemeChanged?.invoke(enabled)
                }
            }
        }
    }

    val colorScheme = if (isDarkState) BrandRedDarkColorScheme else BrandRedLightColorScheme

    CompositionLocalProvider(LocalThemeController provides themeController) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

/**
 * Application Theme wrapper (alias for [BrandThemeWrapper]).
 */
@Composable
fun PollPosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    onThemeChanged: ((Boolean) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    BrandThemeWrapper(
        initialDarkTheme = darkTheme,
        onThemeChanged = onThemeChanged,
        content = content
    )
}

// ============================================================================
// Dynamic Theme Toggle Components (Reusable UI)
// ============================================================================

/**
 * Reusable dynamic Theme Toggle Button featuring animated icon transitions,
 * glowing brand red accents, and smooth feedback.
 */
@Composable
fun ThemeToggleButton(
    modifier: Modifier = Modifier,
    controller: ThemeController = LocalThemeController.current
) {
    val isDark = controller.isDark
    val rotation by animateFloatAsState(
        targetValue = if (isDark) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "ThemeIconRotation"
    )

    IconButton(
        onClick = { controller.toggleTheme() },
        modifier = modifier
    ) {
        Icon(
            imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
            contentDescription = if (isDark) "Beralih ke Tema Terang" else "Beralih ke Tema Gelap",
            tint = if (isDark) Color(0xFF90CAF9) else Color(0xFFF57F17),
            modifier = Modifier.rotate(rotation)
        )
    }
}

/**
 * Sleek dual-pill theme switch showing "Terang" / "Gelap" with brand red active indicator.
 */
@Composable
fun ThemeToggleSwitch(
    modifier: Modifier = Modifier,
    controller: ThemeController = LocalThemeController.current
) {
    val isDark = controller.isDark
    val bgIndicatorColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primary,
        label = "ThemeSwitchBg"
    )

    Surface(
        modifier = modifier.clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Light Option
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (!isDark) bgIndicatorColor else Color.Transparent)
                    .clickable { controller.setDarkTheme(false) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LightMode,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (!isDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Terang",
                        fontSize = 11.sp,
                        fontWeight = if (!isDark) FontWeight.Bold else FontWeight.Normal,
                        color = if (!isDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Dark Option
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) bgIndicatorColor else Color.Transparent)
                    .clickable { controller.setDarkTheme(true) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DarkMode,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Gelap",
                        fontSize = 11.sp,
                        fontWeight = if (isDark) FontWeight.Bold else FontWeight.Normal,
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
