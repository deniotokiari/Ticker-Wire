package pl.deniotokiari.tickerwire.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import tickerwire.composeapp.generated.resources.FiraCode_Bold
import tickerwire.composeapp.generated.resources.FiraCode_Medium
import tickerwire.composeapp.generated.resources.FiraCode_Regular
import tickerwire.composeapp.generated.resources.FiraCode_SemiBold
import tickerwire.composeapp.generated.resources.Res

@Composable
fun rememberFiraCodeFontFamily(): FontFamily {
    val regularFont = Font(Res.font.FiraCode_Regular, FontWeight.Normal, FontStyle.Normal)
    val mediumFont = Font(Res.font.FiraCode_Medium, FontWeight.Medium, FontStyle.Normal)
    val semiBoldFont = Font(Res.font.FiraCode_SemiBold, FontWeight.SemiBold, FontStyle.Normal)
    val boldFont = Font(Res.font.FiraCode_Bold, FontWeight.Bold, FontStyle.Normal)
    
    return remember(regularFont, mediumFont, semiBoldFont, boldFont) {
        FontFamily(
            regularFont,
            mediumFont,
            semiBoldFont,
            boldFont
        )
    }
}

