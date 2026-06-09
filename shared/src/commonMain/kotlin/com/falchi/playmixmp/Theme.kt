package com.falchi.playmixmp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// --- COLORI DELLO STATO DELLA RIPRODUZIONE ---
val ColorPlaying = Color(0xFF10B981) // Verde smeraldo: Titolo canzone in PLAY
val ColorPaused = Color(0xFF3B82F6)  // Blu moderno: Titolo canzone in PAUSA
val ColorTraktor = Color(0xFFEF4444) // Rosso corallo: Brani, BPM e KEY corrispondenti a Traktor

// --- COLORI DEGLI ELEMENTI UI ---
val ColorCuePoint = Color(0xFFF59E0B) // Ambra: Pulsanti degli Hotcue
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

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
