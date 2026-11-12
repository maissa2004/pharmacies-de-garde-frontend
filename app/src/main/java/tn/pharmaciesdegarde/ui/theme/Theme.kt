package tn.pharmaciesdegarde.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = GreenPharmacy
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = GreenPharmacy,
    background = BackgroundLight,
    surface = androidx.compose.ui.graphics.Color.White,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onBackground = TextInk,
    onSurface = TextInk
)

@Composable
fun PharmaciesDeGardeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Désactivé pour garder vos couleurs Figma
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Assurez-vous que ceci existe dans votre Type.kt
        content = content
    )
}