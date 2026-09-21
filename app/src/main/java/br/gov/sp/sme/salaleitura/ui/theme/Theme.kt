package br.gov.sp.sme.salaleitura.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** A reading-room identity; no official insignia or external fonts are bundled. */
object ReadingRoomColors {
    val Wine = Color(0xFF763F4A)
    val Ivory = Color(0xFFFFF8EE)
    val Ocher = Color(0xFFD8AE69)
    val Green = Color(0xFF3E6752)
    val Blue = Color(0xFF46667D)
    val Ink = Color(0xFF262522)
}

private val ReadingRoomScheme = lightColorScheme(
    primary = ReadingRoomColors.Wine,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF4E0E2),
    onPrimaryContainer = Color(0xFF3A1920),
    secondary = ReadingRoomColors.Blue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4EDF1),
    onSecondaryContainer = Color(0xFF1F3E52),
    tertiary = ReadingRoomColors.Green,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE2EDE4),
    onTertiaryContainer = Color(0xFF1C3C28),
    background = ReadingRoomColors.Ivory,
    onBackground = ReadingRoomColors.Ink,
    surface = Color(0xFFFFFDF8),
    onSurface = ReadingRoomColors.Ink,
    surfaceVariant = Color(0xFFF3EADD),
    onSurfaceVariant = Color(0xFF50443B),
    outline = Color(0xFF86786F),
    error = Color(0xFFAD2735),
    onError = Color.White
)

private val ReadingRoomTypography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 23.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
)

@Composable
fun SalaLeituraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ReadingRoomScheme, typography = ReadingRoomTypography, content = content)
}
