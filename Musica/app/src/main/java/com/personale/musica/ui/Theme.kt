package com.personale.musica.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF000000)
val Surface1 = Color(0xFF121212)
val Surface2 = Color(0xFF1E1E1E)
val TextMain = Color(0xFFFFFFFF)
val TextDim = Color(0xFFAAAAAA)
val Accent = Color(0xFFFF3B3B)

@Composable
fun MusicaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = TextMain,
            onPrimary = Bg,
            secondary = Accent,
            background = Bg,
            onBackground = TextMain,
            surface = Surface1,
            onSurface = TextMain,
            surfaceVariant = Surface2,
            onSurfaceVariant = TextDim,
        )
    ) {
        // Colore predefinito di icone e testi: bianco (prima era nero su nero)
        CompositionLocalProvider(LocalContentColor provides TextMain, content = content)
    }
}
