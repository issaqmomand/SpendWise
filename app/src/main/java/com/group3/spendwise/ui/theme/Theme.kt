package com.group3.spendwise.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Colours based on the SpendWise wireframes
val SpendBackground = Color(0xFFF3F4EE)
val SpendGreen = Color(0xFF4F7261)
val SpendLightGreen = Color(0xFFE3E9DF)
val SpendNavy = Color(0xFF243241)
val SpendMuted = Color(0xFF5C665F)
val SpendBorder = Color(0xFFDDE1D6)
val SpendError = Color(0xFFB3261E)
val SpendWarning = Color(0xFFB36B00)

private val SpendColorScheme = lightColorScheme(
    primary = SpendGreen,
    onPrimary = Color.White,
    secondaryContainer = SpendLightGreen,
    onSecondaryContainer = SpendGreen,
    background = SpendBackground,
    onBackground = SpendNavy,
    surface = Color.White,
    onSurface = SpendNavy,
    error = SpendError
)

@Composable
fun SpendWiseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SpendColorScheme,
        content = content
    )
}