package tr.ailetv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import tr.ailetv.ui.AppViewModel
import tr.ailetv.ui.components.AppIcons
import tr.ailetv.ui.components.LoadingLine
import tr.ailetv.ui.components.LogoMark
import tr.ailetv.ui.components.PillButton
import tr.ailetv.ui.theme.LocalUiScale
import tr.ailetv.ui.theme.Palette
import tr.ailetv.ui.theme.Space
import tr.ailetv.ui.theme.Type

// ---------------------------------------------------------------------------------------------
// AÇILIŞ (#acilis) — PROMPTLAR Prompt 2 (1). No focus, keys do nothing.
// ---------------------------------------------------------------------------------------------

/** The splash stays at least this long so it never just flickers. */
private const val SPLASH_MIN_MS = 600L

/** After this the "Kanallar hazırlanıyor…" line appears. */
private const val SPLASH_SLOW_MS = 1000L

/** Back is swallowed on the splash; if it is somehow still showing after this, Back works again (root: exit). */
private const val SPLASH_BACK_LOCK_MS = 5000L

/** Navigation keys the invisible key sink eats. Volume/media/Back keys are left alone. */
private val SplashSwallowedKeys = setOf(
    Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight,
    Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Spacebar,
    Key.ChannelUp, Key.ChannelDown, Key.PageUp, Key.PageDown, Key.Tab,
)

@Composable
fun SplashScreen(vm: AppViewModel) {
    var slow by remember { mutableStateOf(false) }
    var backLocked by remember { mutableStateOf(true) }
    var finished by remember { mutableStateOf(false) }
    val keySink = remember { FocusRequester() }

    // "Tuşlar etkisiz": Back does nothing while the splash is up (only a safety unlock if it hangs).
    BackHandler(enabled = backLocked) { }

    LaunchedEffect(Unit) {
        runCatching { keySink.requestFocus() }
        delay(SPLASH_MIN_MS)
        vm.settings.first { it.loaded }
        if (!finished) {
            finished = true
            vm.finishSplash()
        }
    }
    LaunchedEffect(Unit) {
        delay(SPLASH_SLOW_MS)
        slow = true
        delay(SPLASH_BACK_LOCK_MS - SPLASH_SLOW_MS)
        backLocked = false
    }

    Box(Modifier.fillMaxSize().background(Palette.Bg)) {
        // Invisible key sink: keeps exactly one focused element and makes D-pad/OK do nothing.
        Box(
            Modifier
                .size(1.dp)
                .focusRequester(keySink)
                .onPreviewKeyEvent { it.key in SplashSwallowedKeys }
                .focusable(),
        )
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LogoMark(72.dp)
            Spacer(Modifier.height(16.dp))
            Text("Aile TV", style = Type.headline(), color = Palette.Text, maxLines = 1)
        }
        if (slow) {
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp)) {
                LoadingLine("Kanallar hazırlanıyor…")
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// KUMANDA REHBERİ (#kumanda-rehberi) — PROMPTLAR Prompt 2 (2), index.css .guide-*/.remote*
// ---------------------------------------------------------------------------------------------

private enum class GuideKey { UpDown, Ok, LeftRight, Back }

private class GuideRowData(val key: GuideKey, val title: String, val text: String)

private val GuideRows = listOf(
    GuideRowData(GuideKey.UpDown, "Kanal değiştir", "Yayın izlerken önceki veya sonraki kanala geçer."),
    GuideRowData(GuideKey.Ok, "Seç ve kanal listesi", "Seçtiğinizi açar. Yayın sırasında kanal listesini gösterir."),
    GuideRowData(GuideKey.LeftRight, "Menüde gezin", "Seçenekler arasında sağa ve sola gider."),
    GuideRowData(GuideKey.Back, "Geri", "Bir önceki ekrana döner. Arka arkaya basınca ana ekrana çıkarsınız."),
)

/**
 * Layout per text size. 1×: left column x 58–420, right column x 460–902 (Guidelines/PROMPTLAR).
 * 1.25× / 1.5×: like the prototype, the remote column narrows (and the remote shrinks) so the
 * copy gets ~680 dp; at 1.5× title and description sit side by side so everything fits 540 dp.
 */
private class GuideDims(
    val leftWidth: Dp,
    val gap: Dp,
    val remoteScale: Float,
    val sideBySide: Boolean,
    val subtitleTop: Dp,
    val subtitleBottom: Dp,
    val rowGap: Dp,
    val buttonTop: Dp,
)

private fun guideDims(factor: Float): GuideDims = when {
    factor <= 1.1f -> GuideDims(362.dp, 40.dp, 1f, false, 8.dp, 18.dp, 16.dp, 18.dp)
    factor <= 1.35f -> GuideDims(150.dp, 20.dp, 0.92f, false, 5.dp, 12.dp, 8.dp, 10.dp)
    else -> GuideDims(136.dp, 16.dp, 0.84f, true, 4.dp, 10.dp, 6.dp, 8.dp)
}

/** Title column width of the side-by-side (1.5×) row. */
private val SideTitleWidth = 210.dp

private val PillShape = RoundedCornerShape(percent = 50)

@Composable
fun GuideScreen(vm: AppViewModel, fromSettings: Boolean) {
    val factor = LocalUiScale.current.factor
    val dims = remember(factor) { guideDims(factor) }
    val startFocus = remember { FocusRequester() }
    var finished by remember { mutableStateOf(false) }
    val finish: () -> Unit = {
        if (!finished) {
            finished = true
            vm.finishGuide(fromSettings)
        }
    }

    // OK (on the button) or Back → guide seen → continue (or back to Settings).
    BackHandler(onBack = finish)
    LaunchedEffect(Unit) { runCatching { startFocus.requestFocus() } }

    Row(
        Modifier
            .fillMaxSize()
            .background(Palette.Bg)
            .padding(horizontal = Space.SafeX, vertical = Space.SafeY),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(dims.leftWidth).fillMaxHeight(), contentAlignment = Alignment.Center) {
            RemoteDrawing(dims.remoteScale)
        }
        Spacer(Modifier.width(dims.gap))
        FitHeight(Modifier.weight(1f).fillMaxHeight()) {
            Column(Modifier.fillMaxWidth()) {
                Text("Kumandada bu tuşlar yeter", style = Type.headline(), color = Palette.Text)
                Spacer(Modifier.height(dims.subtitleTop))
                Text(
                    "Aile TV'yi yalnızca yön tuşları, OK ve Geri ile kullanırsınız.",
                    style = Type.body(),
                    color = Palette.Text2,
                )
                Spacer(Modifier.height(dims.subtitleBottom))
                Column(verticalArrangement = Arrangement.spacedBy(dims.rowGap)) {
                    GuideRows.forEach { GuideRow(it, dims.sideBySide) }
                }
                Spacer(Modifier.height(dims.buttonTop))
                // Left edge aligned with the copy above (Column start).
                PillButton("Anladım, başla", onClick = finish, focusRequester = startFocus)
            }
        }
    }
}

@Composable
private fun GuideRow(row: GuideRowData, sideBySide: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // index.css: grid 48px 1fr, gap 14 → badge 40 in a 48 slot.
        Box(Modifier.width(48.dp)) { KeyBadge(row.key) }
        Spacer(Modifier.width(14.dp))
        if (sideBySide) {
            Text(row.title, style = Type.titleM(), color = Palette.Text, modifier = Modifier.width(SideTitleWidth))
            Spacer(Modifier.width(12.dp))
            Text(row.text, style = Type.bodyM(), color = Palette.Text2, modifier = Modifier.weight(1f))
        } else {
            Column(Modifier.weight(1f)) {
                Text(row.title, style = Type.titleM(), color = Palette.Text)
                Spacer(Modifier.height(2.dp))
                Text(row.text, style = Type.bodyM(), color = Palette.Text2)
            }
        }
    }
}

/** 40×40 key badge, radius 9, 2 dp key-outline border. */
@Composable
private fun KeyBadge(key: GuideKey) {
    val shape = RoundedCornerShape(9.dp)
    Box(
        Modifier.size(40.dp).border(2.dp, Palette.KeyOutline, shape),
        contentAlignment = Alignment.Center,
    ) {
        when (key) {
            GuideKey.UpDown -> ChevronPair(AppIcons.ChevronUp, AppIcons.ChevronDown)
            GuideKey.Ok -> Text("OK", style = Type.label(), color = Palette.Text, maxLines = 1, softWrap = false)
            GuideKey.LeftRight -> ChevronPair(AppIcons.ChevronLeft, AppIcons.ChevronRight)
            GuideKey.Back -> Icon(AppIcons.Back, contentDescription = null, modifier = Modifier.size(18.dp), tint = Palette.Text)
        }
    }
}

@Composable
private fun ChevronPair(first: ImageVector, second: ImageVector) {
    Row {
        Icon(first, contentDescription = null, modifier = Modifier.size(16.dp), tint = Palette.Text)
        Icon(second, contentDescription = null, modifier = Modifier.size(16.dp), tint = Palette.Text)
    }
}

/**
 * Plain remote drawing (no brand logos): 150×380 body, radius 42, D-pad ring with OK,
 * Back + Home keys, four unnamed shortcut pills. [k] shrinks it at larger text sizes.
 */
@Composable
private fun RemoteDrawing(k: Float) {
    val body = RoundedCornerShape((42 * k).dp)
    Column(
        Modifier
            .size((150 * k).dp, (380 * k).dp)
            .background(Palette.Surface1, body)
            .border(2.dp, Palette.Outline, body)
            // index.css: padding 32 18 inside a 2 dp border (border-box).
            .padding(horizontal = (20 * k).dp, vertical = (34 * k).dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy((20 * k).dp),
    ) {
        DPadRing(k)
        Row(horizontalArrangement = Arrangement.spacedBy((18 * k).dp)) {
            RoundRemoteKey(AppIcons.Back, k)
            RoundRemoteKey(AppIcons.Home, k)
        }
        repeat(4) {
            Box(Modifier.size((70 * k).dp, (12 * k).dp).background(Palette.Outline, PillShape))
        }
    }
}

@Composable
private fun DPadRing(k: Float) {
    val icon = (16 * k).dp
    // Chevrons centred in the 25 dp outer tracks of the 25/58/25 grid (inside the 2 dp ring).
    val edge = (6.5f * k).dp
    Box(Modifier.size((108 * k).dp).border(2.dp, Palette.Accent, CircleShape)) {
        Icon(AppIcons.ChevronUp, null, Modifier.align(Alignment.TopCenter).padding(top = edge).size(icon), tint = Palette.Accent)
        Icon(AppIcons.ChevronLeft, null, Modifier.align(Alignment.CenterStart).padding(start = edge).size(icon), tint = Palette.Accent)
        Icon(AppIcons.ChevronRight, null, Modifier.align(Alignment.CenterEnd).padding(end = edge).size(icon), tint = Palette.Accent)
        Icon(AppIcons.ChevronDown, null, Modifier.align(Alignment.BottomCenter).padding(bottom = edge).size(icon), tint = Palette.Accent)
        Box(
            Modifier.align(Alignment.Center).size((52 * k).dp).border(2.dp, Palette.Accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("OK", style = Type.labelL(), fontWeight = FontWeight.Bold, color = Palette.Text, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
private fun RoundRemoteKey(icon: ImageVector, k: Float) {
    Box(
        Modifier.size((36 * k).dp).border(2.dp, Palette.Accent, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size((16 * k).dp), tint = Palette.Text)
    }
}

/**
 * Centres its content vertically. If the content is taller than the space (very long copy at
 * 1.5× text), it is scaled down from the top-left so nothing is ever cut off below 540 dp.
 */
@Composable
private fun FitHeight(modifier: Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0, maxHeight = Constraints.Infinity)
        val placeables = measurables.map { it.measure(loose) }
        val w = if (constraints.hasBoundedWidth) constraints.maxWidth else placeables.maxOfOrNull { it.width } ?: 0
        val h = if (constraints.hasBoundedHeight) constraints.maxHeight else placeables.maxOfOrNull { it.height } ?: 0
        layout(w, h) {
            placeables.forEach { p ->
                if (p.height > h && p.height > 0) {
                    val k = h.toFloat() / p.height
                    p.placeWithLayer(0, 0) {
                        scaleX = k
                        scaleY = k
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                } else {
                    p.place(0, (h - p.height) / 2)
                }
            }
        }
    }
}
