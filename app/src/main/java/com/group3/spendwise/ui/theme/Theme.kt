package com.group3.spendwise.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Original SpendWise palette, preserved for light mode.
val SpendBackground = Color(0xFFF3F4EE)
val SpendGreen = Color(0xFF4F7261)
val SpendLightGreen = Color(0xFFE3E9DF)
val SpendNavy = Color(0xFF243241)
val SpendMuted = Color(0xFF5C665F)
val SpendBorder = Color(0xFFDDE1D6)
val SpendError = Color(0xFFB3261E)
val SpendWarning = Color(0xFFB36B00)

// Explicitly define the Material 3 surface/secondary roles.
// Otherwise, unconfigured roles use Material's default lavender/purple palette
// in DropdownMenu, Switch, DatePicker, dialogs, and other controls.
private val LightColors = lightColorScheme(
    primary = SpendGreen,
    onPrimary = Color.White,
    primaryContainer = SpendLightGreen,
    onPrimaryContainer = SpendNavy,
    inversePrimary = Color(0xFFB5D6C2),

    secondary = SpendGreen,
    onSecondary = Color.White,
    secondaryContainer = SpendLightGreen,
    onSecondaryContainer = SpendGreen,

    tertiary = SpendNavy,
    onTertiary = Color.White,
    tertiaryContainer = SpendNavy, // Original Current Balance card
    onTertiaryContainer = Color.White,

    background = SpendBackground,
    onBackground = SpendNavy,
    surface = Color.White,
    onSurface = SpendNavy,
    surfaceVariant = Color(0xFFEDF2EB),
    onSurfaceVariant = SpendMuted,
    surfaceTint = SpendGreen,

    surfaceDim = Color(0xFFE6ECE5),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFCFDFB),
    surfaceContainer = Color(0xFFF7F9F5),
    surfaceContainerHigh = Color(0xFFEEF3EC),
    surfaceContainerHighest = Color(0xFFE6EEE5),

    inverseSurface = SpendNavy,
    inverseOnSurface = Color(0xFFF3F4EE),
    outline = SpendBorder,
    outlineVariant = Color(0xFFE7ECE6),
    scrim = Color.Black,

    error = SpendError,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFADD3BD),
    onPrimary = Color(0xFF183229),
    primaryContainer = Color(0xFF345547),
    onPrimaryContainer = Color(0xFFDCEBE1),
    inversePrimary = SpendGreen,

    secondary = Color(0xFFADD3BD),
    onSecondary = Color(0xFF183229),
    secondaryContainer = Color(0xFF334A40),
    onSecondaryContainer = Color(0xFFDBEBE0),

    tertiary = Color(0xFFBDD0D9),
    onTertiary = Color(0xFF20313B),
    tertiaryContainer = Color(0xFF344655), // Original dark Current Balance card
    onTertiaryContainer = Color.White,

    background = Color(0xFF121B21),
    onBackground = Color(0xFFF0F5F2),
    surface = Color(0xFF202D36),
    onSurface = Color(0xFFF0F5F2),
    surfaceVariant = Color(0xFF2B3B43),
    onSurfaceVariant = Color(0xFFB8C5C0),
    surfaceTint = Color(0xFFADD3BD),

    surfaceDim = Color(0xFF121B21),
    surfaceBright = Color(0xFF3A4D55),
    surfaceContainerLowest = Color(0xFF152028),
    surfaceContainerLow = Color(0xFF1B2931),
    surfaceContainer = Color(0xFF25343D),
    surfaceContainerHigh = Color(0xFF2D3E45),
    surfaceContainerHighest = Color(0xFF344851),

    inverseSurface = Color(0xFFF0F5F2),
    inverseOnSurface = Color(0xFF243241),
    outline = Color(0xFF445861),
    outlineVariant = Color(0xFF374952),
    scrim = Color.Black,

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

// Material 3 menus read the extra-small shape token. This gives every
// dropdown softer corners without changing any screen's padding/spacing.
private val SpendShapes = Shapes(
    extraSmall = RoundedCornerShape(16.dp)
)

@Composable
fun SpendWiseTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = SpendShapes,
        content = content
    )
}
