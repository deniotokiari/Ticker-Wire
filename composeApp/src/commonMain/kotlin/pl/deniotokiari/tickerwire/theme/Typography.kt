package pl.deniotokiari.tickerwire.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun appTypography(): Typography {
    val firaCodeFontFamily = rememberFiraCodeFontFamily()
    
    return Typography(
    displayLarge = TextStyle(
        fontFamily = firaCodeFontFamily,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 40.sp
    ),
    displayMedium = TextStyle(
        fontFamily = firaCodeFontFamily,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 36.sp
    ),
    displaySmall = TextStyle(
        fontFamily = firaCodeFontFamily,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 32.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = firaCodeFontFamily,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 28.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = firaCodeFontFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 26.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = firaCodeFontFamily,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = firaCodeFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = firaCodeFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 20.sp
    ),
    titleSmall = TextStyle(
        fontFamily = firaCodeFontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = firaCodeFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = firaCodeFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = firaCodeFontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = firaCodeFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = firaCodeFontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = firaCodeFontFamily,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 14.sp
    )
    )
}

