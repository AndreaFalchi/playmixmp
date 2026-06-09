package com.falchi.playmixmp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// --- COLORI DELLO STATO DELLA RIPRODUZIONE (COLORATI) ---
val ColorPlayingDefault = Color(0xFF10B981) // Verde smeraldo
val ColorPausedDefault = Color(0xFF3B82F6)  // Blu moderno
val ColorTraktorDefault = Color(0xFFEF4444) // Rosso corallo
val ColorCuePointDefault = Color(0xFFF59E0B) // Ambra

// --- COLORI DELLO STATO DELLA RIPRODUZIONE (GRAYSCALE) ---
val ColorPlayingGrayscale = Color(0xFFFFFFFF) // Bianco
val ColorPausedGrayscale = Color(0xFFBDBDBD)  // Grigio chiaro
val ColorTraktorGrayscale = Color(0xFF757575) // Grigio medio
val ColorCuePointGrayscale = Color(0xFF9E9E9E) // Grigio

// Variabili globali che cambiano in base al tema
var ColorPlaying = ColorPlayingDefault
var ColorPaused = ColorPausedDefault
var ColorTraktor = ColorTraktorDefault
var ColorCuePoint = ColorCuePointDefault

// --- COLORI DEGLI ELEMENTI UI FISSI ---
val ColorLockOverlay = Color(0xFF121212).copy(alpha = 0.8f) // Nero antracite trasparente
val ColorDivider = Color(0xFF94A3B8).copy(alpha = 0.25f)    // Grigio freddo

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),   // Blu moderno
    secondary = Color(0xFF10B981), // Verde smeraldo
    tertiary = Color(0xFFF59E0B)   // Ambra
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),   // Blu leggermente più scuro
    secondary = Color(0xFF059669), // Verde leggermente più scuro
    tertiary = Color(0xFFD97706)   // Ambra leggermente più scura
)

private val GrayscaleDarkColorScheme = darkColorScheme(
    primary = Color(0xFFEEEEEE),   // Quasi bianco
    secondary = Color(0xFFBDBDBD), // Grigio
    tertiary = Color(0xFF757575),  // Grigio scuro
    surface = Color(0xFF121212),
    background = Color(0xFF000000)
)

private val GrayscaleLightColorScheme = lightColorScheme(
    primary = Color(0xFF212121),   // Quasi nero
    secondary = Color(0xFF757575), // Grigio
    tertiary = Color(0xFFBDBDBD),  // Grigio chiaro
    surface = Color(0xFFFFFFFF),
    background = Color(0xFFF5F5F5)
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isGrayscale: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        isGrayscale && darkTheme -> GrayscaleDarkColorScheme
        isGrayscale -> GrayscaleLightColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Aggiorna i colori funzionali in base al tema scelto
    if (isGrayscale) {
        ColorPlaying = ColorPlayingGrayscale
        ColorPaused = ColorPausedGrayscale
        ColorTraktor = ColorTraktorGrayscale
        ColorCuePoint = ColorCuePointGrayscale
    } else {
        ColorPlaying = ColorPlayingDefault
        ColorPaused = ColorPausedDefault
        ColorTraktor = ColorTraktorDefault
        ColorCuePoint = ColorCuePointDefault
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
