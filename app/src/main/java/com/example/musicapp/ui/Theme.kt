package com.example.musicapp.ui

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

val LocalDark = compositionLocalOf { false }

/**
 * Colores que cambian con el tema.
 * Cream = fondo, Ink = texto principal, Muted = texto secundario,
 * Line = líneas finas, Card = superficies, Accent = color de acento.
 */
object Palette {
    val Cream: Color @Composable get() = if (LocalDark.current) Color(0xFF121110) else Color(0xFFF4F1EA)
    val Ink: Color @Composable get() = if (LocalDark.current) Color(0xFFF4F1EA) else Color(0xFF141414)
    val Muted: Color @Composable get() = if (LocalDark.current) Color(0xFF9A958A) else Color(0xFF8A857B)
    val Line: Color @Composable get() = if (LocalDark.current) Color(0xFF2C2A27) else Color(0xFFDAD5C9)
    val Card: Color @Composable get() = if (LocalDark.current) Color(0xFF1F1D1B) else Color(0xFFEAE5DA)
    val Accent = Color(0xFFFF5A1F)
}

val SerifFont = FontFamily.Serif

/** Preferencia de tema guardada en el dispositivo. null = seguir al sistema. */
object ThemeSettings {
    private var prefs: SharedPreferences? = null
    private val _dark = MutableStateFlow<Boolean?>(null)
    val dark: StateFlow<Boolean?> = _dark

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        prefs = p
        _dark.value = if (p.contains("dark")) p.getBoolean("dark", false) else null
    }

    fun set(value: Boolean) {
        _dark.value = value
        prefs?.edit()?.putBoolean("dark", value)?.apply()
    }
}

private val LightScheme = lightColorScheme(
    primary = Color(0xFFFF5A1F),
    onPrimary = Color.White,
    secondary = Color(0xFF141414),
    background = Color(0xFFF4F1EA),
    onBackground = Color(0xFF141414),
    surface = Color(0xFFF4F1EA),
    onSurface = Color(0xFF141414)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFFF5A1F),
    onPrimary = Color.White,
    secondary = Color(0xFFF4F1EA),
    background = Color(0xFF121110),
    onBackground = Color(0xFFF4F1EA),
    surface = Color(0xFF1F1D1B),
    onSurface = Color(0xFFF4F1EA)
)

@Composable
fun MusicTheme(content: @Composable () -> Unit) {
    val pref by ThemeSettings.dark.collectAsStateWithLifecycle()
    val dark = pref ?: isSystemInDarkTheme()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bg = if (dark) 0xFF121110.toInt() else 0xFFF4F1EA.toInt()
            window.statusBarColor = bg
            window.setBackgroundDrawable(ColorDrawable(bg))
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }

    CompositionLocalProvider(LocalDark provides dark) {
        MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, content = content)
    }
}

@Composable
fun minimalFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Palette.Ink,
    unfocusedIndicatorColor = Palette.Line,
    cursorColor = Palette.Accent,
    focusedLabelColor = Palette.Ink,
    unfocusedLabelColor = Palette.Muted,
    focusedTextColor = Palette.Ink,
    unfocusedTextColor = Palette.Ink,
    focusedPlaceholderColor = Palette.Muted,
    unfocusedPlaceholderColor = Palette.Muted
)
