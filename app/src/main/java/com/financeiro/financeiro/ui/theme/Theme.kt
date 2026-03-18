package com.financeiro.financeiro.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = FinanceBlueLight,
    secondary = FinanceAccent,
    tertiary = FinanceBlue,
    background = FinanceDarkSurface,
    surface = FinanceDarkSurfaceAlt,
    onPrimary = Color(0xFF0F172A),
    onSecondary = Color(0xFF1F2937),
    onTertiary = Color.White,
    onBackground = FinanceDarkText,
    onSurface = FinanceDarkText
)

private val LightColorScheme = lightColorScheme(
    primary = FinanceBlue,
    secondary = FinanceAccent,
    tertiary = FinanceBlueLight,
    background = Color(0xFFF3F6FC),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color(0xFF2B2B2B),
    onTertiary = Color(0xFF16306E),
    onBackground = Color(0xFF1F2937),
    onSurface = Color(0xFF1F2937)
)

@Composable
fun FinanceiroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
        typography = Typography,
        content = content
    )
}
