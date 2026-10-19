package tr.ailetv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/** Design tokens from docs/figma-make/Guidelines.md §3 (all sizes are dp on a 960×540 canvas). */
object Palette {
    val Bg = Color(0xFF0F1013)
    val Surface1 = Color(0xFF1C1D21)
    val SurfaceTile = Color(0xFF24262B)
    val Surface2 = Color(0xFF2A2C31)
    val SurfacePanel = Color(0xFF17181C)
    val SurfaceDialog = Color(0xFF202226)
    val Outline = Color(0xFF3A3C42)
    val KeyOutline = Color(0xFF6C6F77)
    val Text = Color(0xFFEDEEF0)
    val Text2 = Color(0xFFB8BBC2)
    val TextDisabled = Color(0xFF7D8088)
    val FocusBg = Color(0xFFF1F3F4)
    val FocusText = Color(0xFF15161A)
    val SelectedBg = Color(0xFF33405A)
    val SelectedText = Color(0xFFD3E3FD)
    val Accent = Color(0xFF8AB4F8)
    val Star = Color(0xFFFDD663)
    val Danger = Color(0xFFF28B82)
    /** rgba(8,9,11,0.72) */
    val Scrim = Color(0xB808090B)
    val BannerBg = Color(0xF0141519)
    val VideoBg = Color(0xFF16181C)
    val VideoText = Color(0xFF4A4D55)
    val FocusRing = Color(0xFFFFFFFF)
}

/** User's "Yazı boyutu" (1 / 1.25 / 1.5). Text and text-bound heights scale with it. */
@Immutable
data class UiScale(val factor: Float) {
    fun sp(v: Number): TextUnit = (v.toFloat() * factor).sp
    fun dp(v: Number): Dp = (v.toFloat() * factor).dp
}

val LocalUiScale = staticCompositionLocalOf { UiScale(1f) }

object Type {
    val s: UiScale @Composable @ReadOnlyComposable get() = LocalUiScale.current

    @Composable @ReadOnlyComposable
    fun display() = TextStyle(fontFamily = FontFamily.Default, fontSize = s.sp(30), lineHeight = s.sp(36), fontWeight = FontWeight.Bold)

    @Composable @ReadOnlyComposable
    fun headline() = TextStyle(fontFamily = FontFamily.Default, fontSize = s.sp(28), lineHeight = s.sp(34), fontWeight = FontWeight.Medium)

    @Composable @ReadOnlyComposable
    fun titleL() = TextStyle(fontFamily = FontFamily.Default, fontSize = s.sp(22), lineHeight = s.sp(28), fontWeight = FontWeight.Medium)

    @Composable @ReadOnlyComposable
    fun titleM() = TextStyle(fontFamily = FontFamily.Default, fontSize = s.sp(18), lineHeight = s.sp(24), fontWeight = FontWeight.Medium)

    @Composable @ReadOnlyComposable
    fun labelL() = TextStyle(fontFamily = FontFamily.Default, fontSize = s.sp(16), lineHeight = s.sp(22), fontWeight = FontWeight.Medium)

    @Composable @ReadOnlyComposable
    fun body() = TextStyle(fontFamily = FontFamily.Default, fontSize = s.sp(16), lineHeight = s.sp(22), fontWeight = FontWeight.Normal)

    @Composable @ReadOnlyComposable
    fun bodyM() = TextStyle(fontFamily = FontFamily.Default, fontSize = s.sp(15), lineHeight = s.sp(20), fontWeight = FontWeight.Normal)

    @Composable @ReadOnlyComposable
    fun row() = TextStyle(fontFamily = FontFamily.Default, fontSize = s.sp(17), lineHeight = s.sp(22), fontWeight = FontWeight.Medium)

    /** 14 sp floor (Guidelines §4). */
    @Composable @ReadOnlyComposable
    fun label() = TextStyle(fontFamily = FontFamily.Default, fontSize = s.sp(14), lineHeight = s.sp(18), fontWeight = FontWeight.Medium)
}

object Space {
    /** Guidelines §2 safe area. */
    val SafeX = 58.dp
    val SafeY = 28.dp
    val ContentWidth = 844.dp
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AileTvTheme(scale: Float, content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = Palette.Accent,
        onPrimary = Palette.Bg,
        background = Palette.Bg,
        onBackground = Palette.Text,
        surface = Palette.Surface1,
        onSurface = Palette.Text,
        surfaceVariant = Palette.Surface2,
        onSurfaceVariant = Palette.Text2,
        border = Palette.FocusRing,
        inverseSurface = Palette.FocusBg,
        inverseOnSurface = Palette.FocusText,
        error = Palette.Danger,
    )
    CompositionLocalProvider(LocalUiScale provides UiScale(scale)) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}
