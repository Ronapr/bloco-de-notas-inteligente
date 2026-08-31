package com.securenotes.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.securenotes.app.domain.settings.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Accent80,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Accent40
)

// Lava / Obsidiana — rocha real com pequenas fissuras incandescentes
private val LavaColorScheme = darkColorScheme(
    primary = LavaRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A0A00),
    onPrimaryContainer = Color(0xFFFFDAD1),
    secondary = LavaOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A1600),
    onSecondaryContainer = Color(0xFFFFDCC2),
    tertiary = LavaYellow,
    onTertiary = Color(0xFF1A0A00),
    tertiaryContainer = Color(0xFF4A2A00),
    onTertiaryContainer = Color(0xFFFFE8B0),
    background = Color(0xE6050505),
    onBackground = LavaTextPrimary,
    surface = Color(0xE60B0B0B),
    onSurface = LavaTextPrimary,
    surfaceVariant = Color(0xE6171717),
    onSurfaceVariant = LavaTextSecondary,
    surfaceContainer = Color(0xE60F0F0F),
    surfaceContainerHigh = Color(0xE61E1E1E),
    outline = Color(0xFF8B2500),
    outlineVariant = Color(0xFF5A1A00),
    error = LavaRed,
    onError = Color.White,
    errorContainer = Color(0xFF4A0A00),
    onErrorContainer = Color(0xFFFFDAD1)
)

private val VolcanicColorScheme = LavaColorScheme

// Floral Rosa — painéis bem translúcidos para buquês aparecerem atrás, como na referência
private val FloralColorScheme = lightColorScheme(
    primary = FloralStrong,
    onPrimary = Color.White,
    primaryContainer = FloralLight,
    onPrimaryContainer = FloralDark,
    secondary = FloralRose,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD6E2),
    onSecondaryContainer = FloralDark,
    tertiary = FloralDark,
    onTertiary = Color.White,
    tertiaryContainer = FloralPink,
    onTertiaryContainer = FloralDark,
    background = Color(0xB0FFF4F8),
    onBackground = FloralTextPrimary,
    surface = Color(0x88FFFFFF),
    onSurface = FloralTextPrimary,
    surfaceVariant = Color(0x99FFE8F0),
    onSurfaceVariant = FloralTextSecondary,
    surfaceContainer = Color(0x88FFE8F0),
    surfaceContainerHigh = Color(0x99FFE0EB),
    outline = FloralPink,
    outlineVariant = Color(0xFFFFD6E2),
    error = FloralDark,
    onError = Color.White,
    errorContainer = FloralLight,
    onErrorContainer = FloralDark,
    scrim = Color(0xFFFFF4F8)
)

// Foto da galeria — foto do usuário como fundo, com overlay escuro para legibilidade
private val GalleryColorScheme = lightColorScheme(
    primary = FloralStrong,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE8F0),
    onPrimaryContainer = Color(0xFF5A3542),
    secondary = FloralRose,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD6E2),
    onSecondaryContainer = Color(0xFF5A3542),
    tertiary = FloralDark,
    onTertiary = Color.White,
    tertiaryContainer = FloralPink,
    onTertiaryContainer = Color.White,
    background = Color(0xCCFFFFFF),
    onBackground = Color(0xFF5A3542),
    surface = Color(0xCCFFFFFF),
    onSurface = Color(0xFF5A3542),
    surfaceVariant = Color(0xE0FFE8F0),
    onSurfaceVariant = Color(0xFF8A6672),
    surfaceContainer = Color(0xCCFFFFFF),
    surfaceContainerHigh = Color(0xE0FFE0EB),
    outline = FloralPink,
    outlineVariant = Color(0xFFFFD6E2),
    error = FloralDark,
    onError = Color.White,
    errorContainer = FloralLight,
    onErrorContainer = FloralDark,
    scrim = Color(0xFF5A3542)
)

@Composable
fun SecureNotesTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    customPhotoUri: String = "",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.DARK -> DarkColorScheme
        ThemeMode.VOLCANIC -> LavaColorScheme
        ThemeMode.FLORAL -> FloralColorScheme
        ThemeMode.GALLERY -> GalleryColorScheme
        ThemeMode.LIGHT -> LightColorScheme
        ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            when (themeMode) {
                ThemeMode.VOLCANIC -> Box(Modifier.fillMaxSize()) {
                    VolcanicBackground(Modifier.fillMaxSize())
                    content()
                }
                ThemeMode.FLORAL -> Box(Modifier.fillMaxSize()) {
                    FloralBackground(Modifier.fillMaxSize())
                    content()
                }
                ThemeMode.GALLERY -> Box(Modifier.fillMaxSize()) {
                    CustomPhotoBackground(
                        photoUri = customPhotoUri,
                        modifier = Modifier.fillMaxSize(),
                        overlayAlpha = 0.38f
                    )
                    content()
                }
                else -> content()
            }
        }
    )
}
