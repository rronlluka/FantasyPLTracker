package com.fpl.tracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val AthleticColorScheme = darkColorScheme(
    primary = PrimaryA1D494,
    onPrimary = OnPrimary0A3909,
    primaryContainer = PrimaryContainer2D5A27,
    secondary = SecondaryFFE083,
    onSecondary = OnSecondary3C2F00,
    tertiary = TertiaryFFB3AD,
    tertiaryContainer = TertiaryContainerA40217,
    background = Background131313,
    surface = Surface131313,
    surfaceVariant = SurfaceVariant353535,
    onBackground = OnSurfaceE5E2E1,
    onSurface = OnSurfaceE5E2E1,
    onSurfaceVariant = OnSurfaceVariantC2C9BB,
    outlineVariant = OutlineVariant42493E,
    error = ErrorFFB4AB,
    onError = OnError690005
)

@Composable
fun FantasyLiveTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AthleticColorScheme,
        typography = Typography,
        content = content
    )
}
