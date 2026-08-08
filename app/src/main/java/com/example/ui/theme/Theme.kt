package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeInfo(
    val darkColor: Color,
    val lightColor: Color,
    val title: String
) {
    MULBERRY_MINT(Color(0xFF3A2036), Color(0xFFDCE8DB), "Mulberry & Mint"),
    MIDNIGHT_EMBER(Color(0xFF0D1117), Color(0xFFFF6B35), "Midnight & Ember"),
    CRIMSON_CHALK(Color(0xFFDC143C), Color(0xFFF2EFE7), "Crimson & Chalk"),
    ESPRESSO_CREAM(Color(0xFF382A21), Color(0xFFF0E5D3), "Espresso & Cream"),
    SLATE_BLUSH(Color(0xFF2E3F4F), Color(0xFFF5C5B8), "Slate & Blush"),
    PETROL_SAND(Color(0xFF326586), Color(0xFFF4E9D4), "Petrol & Sand"),
    PINE_CREAM(Color(0xFF455B51), Color(0xFFFFF0A4), "Pine & Cream"),
    ONYX_CORAL(Color(0xFF2C2C2C), Color(0xFFE8B59E), "Onyx & Coral"),
    PINE_STONE(Color(0xFF36A372), Color(0xFFF0EBE9), "Pine & Stone"),
    JADE_CITRUS(Color(0xFF042D22), Color(0xFFE6FF55), "Jade & Citrus"),
    GRAPE_LIME(Color(0xFF6D28D9), Color(0xFFD7FF00), "Grape & Lime"),
    BLUEBERRY_CREAM(Color(0xFF243B8F), Color(0xFFFFF0C9), "Blueberry & Cream"),
    QUANTUM_ICE(Color(0xFF2457FF), Color(0xFFDFF7FF), "Quantum & Ice")
}

@Composable
fun MyApplicationTheme(
    appThemeInfo: AppThemeInfo = AppThemeInfo.MULBERRY_MINT,
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) {
        androidx.compose.material3.darkColorScheme(
            primary = appThemeInfo.lightColor,
            onPrimary = appThemeInfo.darkColor,
            background = appThemeInfo.darkColor,
            onBackground = appThemeInfo.lightColor,
            surface = Color.Black.copy(alpha = 0.6f),
            onSurface = appThemeInfo.lightColor,
            error = appThemeInfo.lightColor,
            surfaceVariant = appThemeInfo.lightColor.copy(alpha = 0.15f),
            onSurfaceVariant = appThemeInfo.lightColor.copy(alpha = 0.7f)
        )
    } else {
        lightColorScheme(
            primary = appThemeInfo.darkColor,
            onPrimary = appThemeInfo.lightColor,
            background = appThemeInfo.lightColor,
            onBackground = appThemeInfo.darkColor,
            surface = Color.White.copy(alpha = 0.6f),
            onSurface = appThemeInfo.darkColor,
            error = appThemeInfo.darkColor,
            surfaceVariant = appThemeInfo.darkColor.copy(alpha = 0.1f),
            onSurfaceVariant = appThemeInfo.darkColor.copy(alpha = 0.7f)
        )
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
