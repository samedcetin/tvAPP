package tr.ailetv.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Lucide-style stroke icons (24×24, stroke 2) drawn in black; tint them with Icon(tint = …).
 * Kept local instead of pulling in material-icons-extended (~10 MB).
 */
object AppIcons {
    private fun stroke(name: String, vararg paths: String, filled: Boolean = false): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        for (d in paths) {
            b.addPath(
                pathData = addPathNodes(d),
                fill = if (filled) SolidColor(Color.Black) else null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    private const val STAR = "M12 3.5l2.6 5.3 5.9.9-4.25 4.1 1 5.8L12 16.9l-5.25 2.7 1-5.8L3.5 9.7l5.9-.9z"
    private const val CIRCLE = "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z"

    val Tv = stroke("tv", "M4 7h16a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2z", "M17 2l-5 5-5-5")
    val Star = stroke("star", STAR)
    val StarFilled = stroke("star-filled", STAR, filled = true)
    val StarOff = stroke("star-off", STAR, "M3 3l18 18")
    val Sliders = stroke("sliders", "M4 7h10M18 7h2M4 17h4M12 17h8", "M14 7a2 2 0 1 0 4 0a2 2 0 1 0-4 0z", "M8 17a2 2 0 1 0 4 0a2 2 0 1 0-4 0z")
    val Check = stroke("check", "M5 12.5l4.5 4.5L19 7.5")
    val ChevronLeft = stroke("chevron-left", "M15 6l-6 6 6 6")
    val ChevronRight = stroke("chevron-right", "M9 6l6 6-6 6")
    val ChevronUp = stroke("chevron-up", "M6 15l6-6 6 6")
    val ChevronDown = stroke("chevron-down", "M6 9l6 6 6-6")
    val Back = stroke("back", "M9 10l-5 5 5 5", "M20 4v7a4 4 0 0 1-4 4H4")
    val Play = stroke("play", "M7 5.5v13l11-6.5z", filled = true)
    val Info = stroke("info", CIRCLE, "M12 16v-4M12 8h.01")
    val ListIcon = stroke("list", "M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01")
    val Refresh = stroke(
        "refresh",
        "M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8", "M21 3v5h-5",
        "M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16", "M8 16H3v5",
    )
    val Home = stroke("home", "M3 10.5L12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z")
    val Alert = stroke("alert", CIRCLE, "M12 8v4M12 16h.01")
    val WifiOff = stroke(
        "wifi-off",
        "M12 20h.01", "M8.5 16.43a5 5 0 0 1 7 0", "M2 8.82a15 15 0 0 1 4.17-2.65",
        "M10.66 5c4.01-.36 8.14.9 11.34 3.76", "M16.85 11.25a10 10 0 0 1 2.22 1.68",
        "M5 13a10 10 0 0 1 5.24-2.76", "M2 2l20 20",
    )
    val MonitorPlay = stroke(
        "monitor-play",
        "M4 3h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z",
        "M10 7.75v4.5l4-2.25z", "M12 17v4M8 21h8",
    )
    val Globe = stroke("globe", CIRCLE, "M3 12h18", "M12 3a14 14 0 0 1 0 18a14 14 0 0 1 0-18z")
    val Close = stroke("close", "M18 6L6 18M6 6l12 12")
}
