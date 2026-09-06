package de.faction.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * FACTION-Palette. Die App ist bewusst nur dunkel: sie wird neben einem dunklen
 * Spiel benutzt, und ein heller Modus würde die Kartenränder aus Gold zerstören.
 */
object FactionColors {
    val Night = Color(0xFF0A0F14)
    val Surface = Color(0xFF131A22)
    val SurfaceRaised = Color(0xFF1A232D)
    val Border = Color(0xFF2A3441)
    val Gold = Color(0xFFC9A227)
    val GoldSoft = Color(0xFFD9BE6E)
    val Teal = Color(0xFF4FD1C5)
    val TextPrimary = Color(0xFFE8EDF2)
    val TextSecondary = Color(0xFF93A1B0)

    /** Rahmen der Karten — von Gold nach transparent, wie im Entwurf. */
    val CardEdge = Brush.verticalGradient(
        listOf(Color(0x66C9A227), Color(0x1AC9A227)),
    )

    val buildNow = Color(0xFF5FBF7A)
    val buildLater = Color(0xFF6BA8E5)
    val keep = Color(0xFF9BA8B5)
    val food = Color(0xFFE0705F)

    fun rarity(name: String): Color = when (name) {
        "Legendär" -> Color(0xFFE8B23A)
        "Episch" -> Color(0xFFB47BE0)
        "Selten" -> Color(0xFF4FD1C5)
        "Ungewöhnlich" -> Color(0xFF7FC06A)
        else -> Color(0xFF9BA8B5)
    }
}

private val FactionScheme = darkColorScheme(
    primary = FactionColors.Gold,
    onPrimary = Color(0xFF1A1305),
    secondary = FactionColors.Teal,
    onSecondary = Color(0xFF04211F),
    background = FactionColors.Night,
    onBackground = FactionColors.TextPrimary,
    surface = FactionColors.Surface,
    onSurface = FactionColors.TextPrimary,
    surfaceVariant = FactionColors.SurfaceRaised,
    onSurfaceVariant = FactionColors.TextSecondary,
    outline = FactionColors.Border,
)

private val FactionTypography = Typography(
    displaySmall = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Light, letterSpacing = 3.sp),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Normal),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelSmall = TextStyle(fontSize = 11.sp, letterSpacing = 1.sp),
)

private val FactionShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)

@Composable
fun FactionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FactionScheme,
        typography = FactionTypography,
        shapes = FactionShapes,
        content = content,
    )
}
