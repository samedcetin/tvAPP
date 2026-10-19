package tr.ailetv.ui.screens

import android.app.ActivityManager
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import tr.ailetv.data.Settings
import tr.ailetv.player.PlaybackState
import tr.ailetv.system.DisplayModes
import tr.ailetv.ui.AppViewModel
import tr.ailetv.ui.Screen
import tr.ailetv.ui.components.AppIcons
import tr.ailetv.ui.components.DialogScaffold
import tr.ailetv.ui.components.LogoMark
import tr.ailetv.ui.components.SettingsRow
import tr.ailetv.ui.theme.LocalUiScale
import tr.ailetv.ui.theme.Palette
import tr.ailetv.ui.theme.Space
import tr.ailetv.ui.theme.Type
import tr.ailetv.ui.theme.UiScale

// ---------------------------------------------------------------------------------------------
// Shared helpers
// ---------------------------------------------------------------------------------------------

private val IsoDate = Regex("""^(\d{4})-(\d{2})-(\d{2})""")

/** "2026-10-19" (or an ISO timestamp) → "19.10.2026" (Guidelines §4). */
private fun formatDate(raw: String): String {
    val m = IsoDate.find(raw.trim()) ?: return raw.ifBlank { "—" }
    val (y, mo, d) = m.destructured
    return "$d.$mo.$y"
}

/** The 1.5 text size has its own widths in the prototype (index.css `data-text-scale="1.5"`). */
private val UiScale.isLargest: Boolean get() = factor > 1.4f

// ---------------------------------------------------------------------------------------------
// Ayarlar
// ---------------------------------------------------------------------------------------------

private const val ROW_TEXT_SIZE = 0
private const val ROW_AUTO_START = 1
private const val ROW_DIRECTION = 2
private const val ROW_BANNER = 3
private const val ROW_50HZ = 4
private const val ROW_REFRESH = 5
private const val ROW_GUIDE = 6
private const val ROW_ABOUT = 7
private const val ROW_COUNT = 8

private val TEXT_SCALES = listOf(1f, 1.25f, 1.5f)
private val TEXT_SCALE_LABELS = listOf("Normal", "Büyük", "Çok büyük")
private val DIRECTION_LABELS = listOf("▲ sonraki kanal", "▲ önceki kanal")
private val BANNER_SECONDS = listOf(3, 5, 10)
private val BANNER_LABELS = listOf("3 saniye", "5 saniye", "10 saniye")

/**
 * Row to focus when Settings is shown again after Guide/About (the root rebuilds the screen,
 * so remembered state is gone). Null = fresh entry from Home → first row.
 */
private var settingsReturnRow: Int? = null

private enum class Choice { TextSize, Direction, Banner }

private fun textScaleIndex(scale: Float): Int =
    TEXT_SCALES.indices.minBy { kotlin.math.abs(TEXT_SCALES[it] - scale) }

private fun bannerIndex(seconds: Int): Int =
    BANNER_SECONDS.indexOf(seconds).let { if (it < 0) BANNER_SECONDS.indexOf(5) else it }

private fun requestRow(rows: List<FocusRequester>, index: Int): Boolean =
    runCatching { rows[index].requestFocus() }.isSuccess

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val s = LocalUiScale.current
    val rows = remember { List(ROW_COUNT) { FocusRequester() } }
    val listState = rememberLazyListState()
    var choice by remember { mutableStateOf<Choice?>(null) }
    var refocusRow by remember { mutableStateOf<Int?>(null) }
    val startRow = remember {
        val r = settingsReturnRow?.coerceIn(0, ROW_COUNT - 1) ?: 0
        settingsReturnRow = null
        r
    }

    // Initial focus: first row, or the row we left from (Guide / About).
    LaunchedEffect(Unit) {
        withFrameNanos { }
        if (listState.layoutInfo.visibleItemsInfo.none { it.index == startRow }) {
            listState.scrollToItem(startRow)
            withFrameNanos { }
        }
        if (!requestRow(rows, startRow)) requestRow(rows, 0)
    }
    // The "İnternet yok" overlay (AppRoot) takes the remote: close an open selection dialog under it. When the
    // overlay goes away the framework enters the list, whose restorer puts focus back on the row (the opener),
    // and a dialog left open would have nothing focused. The overlay covers the canvas, so this is not seen.
    val offlineShown by vm.offlineShown.collectAsStateWithLifecycle()
    LaunchedEffect(offlineShown) { if (offlineShown) choice = null }
    // A closed selection dialog gives focus back to the row that opened it.
    LaunchedEffect(choice) {
        val target = refocusRow
        if (choice == null && target != null) {
            withFrameNanos { }
            if (!requestRow(rows, target)) requestRow(rows, 0)
            refocusRow = null
        }
    }

    fun open(c: Choice, row: Int) {
        refocusRow = row
        choice = c
    }

    val updated = remember(catalog.updatedAt) { "Son güncelleme: ${formatDate(catalog.updatedAt)}" }
    val version = remember { "Sürüm ${vm.appVersion()}" }

    Box(Modifier.fillMaxSize()) {
        Row(
            // The list sits 8 dp outside the safe area (and pads 8 dp inside) so the 1.02 focus scale is never clipped.
            Modifier.fillMaxSize().padding(horizontal = Space.SafeX, vertical = Space.SafeY - 8.dp),
            horizontalArrangement = Arrangement.spacedBy(40.dp),
        ) {
            Column(Modifier.width(if (s.isLargest) 250.dp else 300.dp).padding(top = 8.dp)) {
                Text("Ayarlar", style = Type.headline(), color = Palette.Text)
                Spacer(Modifier.height(8.dp))
                Text("Değişiklikler hemen uygulanır.", style = Type.body(), color = Palette.Text2)
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .width(if (s.isLargest) 554.dp else 504.dp)
                    .fillMaxHeight()
                    // No fallback requester: with one, entering the list before it saved a row (a fresh screen
                    // after Guide/About asking for the row it left from) is redirected to the first row.
                    // Without it, it only restores the saved row: after a dialog and after the offline overlay.
                    .focusRestorer(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item(key = "yazi-boyutu") {
                    SettingsRow(
                        label = "Yazı boyutu",
                        value = TEXT_SCALE_LABELS[textScaleIndex(settings.textScale)],
                        onClick = { open(Choice.TextSize, ROW_TEXT_SIZE) },
                        focusRequester = rows[ROW_TEXT_SIZE],
                    )
                }
                item(key = "acilis") {
                    SettingsRow(
                        label = "Açılışta son kanalı başlat",
                        value = if (settings.autoStartLastChannel) "Açık" else "Kapalı",
                        onClick = {
                            val next = !settings.autoStartLastChannel
                            vm.setAutoStart(next)
                            vm.showToast(if (next) "Açılışta son kanal açılacak" else "Ana ekran açılacak")
                        },
                        switchOn = settings.autoStartLastChannel,
                        focusRequester = rows[ROW_AUTO_START],
                    )
                }
                item(key = "kanal-yonu") {
                    SettingsRow(
                        label = "Kanal değiştirme yönü",
                        value = DIRECTION_LABELS[if (settings.upIsNext) 0 else 1],
                        onClick = { open(Choice.Direction, ROW_DIRECTION) },
                        focusRequester = rows[ROW_DIRECTION],
                    )
                }
                item(key = "bant-suresi") {
                    SettingsRow(
                        label = "Bilgi bandı süresi",
                        value = BANNER_LABELS[bannerIndex(settings.bannerSeconds)],
                        onClick = { open(Choice.Banner, ROW_BANNER) },
                        focusRequester = rows[ROW_BANNER],
                    )
                }
                item(key = "50hz") {
                    SettingsRow(
                        label = "Görüntü akıcılığı (50 Hz)",
                        value = "Türk yayınları için önerilir",
                        onClick = { vm.setMatch50Hz(!settings.match50Hz) },
                        switchOn = settings.match50Hz,
                        focusRequester = rows[ROW_50HZ],
                    )
                }
                item(key = "guncelle") {
                    SettingsRow(
                        label = "Kanal listesini güncelle",
                        value = updated,
                        onClick = { vm.refreshChannelList() },
                        focusRequester = rows[ROW_REFRESH],
                    )
                }
                item(key = "rehber") {
                    SettingsRow(
                        label = "Kumanda rehberini göster",
                        value = null,
                        onClick = {
                            settingsReturnRow = ROW_GUIDE
                            vm.navigate(Screen.Guide(fromSettings = true))
                        },
                        focusRequester = rows[ROW_GUIDE],
                    )
                }
                item(key = "hakkinda") {
                    SettingsRow(
                        label = "Hakkında",
                        value = version,
                        onClick = {
                            settingsReturnRow = ROW_ABOUT
                            vm.navigate(Screen.About)
                        },
                        focusRequester = rows[ROW_ABOUT],
                    )
                }
            }
        }

        choice?.let { c ->
            ChoiceDialog(
                choice = c,
                settings = settings,
                vm = vm,
                onClose = { choice = null },
            )
        }
    }
}

/** Guidelines §7 Dialog (480 wide) with radio rows; OK selects and closes, Back cancels (DialogScaffold). */
@Composable
private fun ChoiceDialog(choice: Choice, settings: Settings, vm: AppViewModel, onClose: () -> Unit) {
    val s = LocalUiScale.current
    val title: String
    val options: List<String>
    val current: Int
    when (choice) {
        Choice.TextSize -> {
            title = "Yazı boyutu"
            options = TEXT_SCALE_LABELS
            current = textScaleIndex(settings.textScale)
        }
        Choice.Direction -> {
            title = "Kanal değiştirme yönü"
            options = DIRECTION_LABELS
            current = if (settings.upIsNext) 0 else 1
        }
        Choice.Banner -> {
            title = "Bilgi bandı süresi"
            options = BANNER_LABELS
            current = bannerIndex(settings.bannerSeconds)
        }
    }
    // Captured once: the dialog keeps the value it opened with even if settings change underneath.
    val opened = remember(choice) { current }
    val requesters = remember(choice) { List(options.size) { FocusRequester() } }
    var focusedIndex by remember(choice) { mutableIntStateOf(opened) }

    fun select(index: Int) {
        when (choice) {
            Choice.TextSize -> vm.setTextScale(TEXT_SCALES[index])
            Choice.Direction -> vm.setUpIsNext(index == 0)
            Choice.Banner -> vm.setBannerSeconds(BANNER_SECONDS[index])
        }
        onClose()
    }

    DialogScaffold(
        title = title,
        onDismiss = onClose,
        initialFocus = requesters[opened],
        modifier = Modifier.heightIn(max = 484.dp),
        width = if (s.isLargest) 620.dp else 480.dp,
    ) {
        Column(
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            options.forEachIndexed { i, label ->
                RadioRow(
                    label = label,
                    selected = i == opened,
                    onClick = { select(i) },
                    requester = requesters[i],
                    onFocused = { focusedIndex = i },
                )
            }
            if (choice == Choice.TextSize) TextSizePreview(TEXT_SCALES[focusedIndex])
        }
    }
}

@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    requester: FocusRequester,
    onFocused: () -> Unit,
) {
    val s = LocalUiScale.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = s.dp(52))
            .focusRequester(requester)
            .onFocusChanged { if (it.isFocused) onFocused() },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Palette.SelectedBg else Color.Transparent,
            contentColor = if (selected) Palette.SelectedText else Palette.Text,
            focusedContainerColor = Palette.FocusBg,
            focusedContentColor = Palette.FocusText,
            pressedContainerColor = Palette.FocusBg,
            pressedContentColor = Palette.FocusText,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f, pressedScale = 1.02f),
        interactionSource = interaction,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp).align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val ink = LocalContentColor.current
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .then(
                        if (selected) Modifier.background(ink)
                        else Modifier.border(2.dp, Palette.KeyOutline, CircleShape),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        AppIcons.Check,
                        contentDescription = "Seçili",
                        modifier = Modifier.size(14.dp),
                        tint = if (focused) Palette.FocusBg else Palette.SelectedBg,
                    )
                }
            }
            Text(label, style = Type.body())
        }
    }
}

/** "Yazı boyutu" preview, drawn at the focused option's scale. */
@Composable
private fun TextSizePreview(scale: Float) {
    CompositionLocalProvider(LocalUiScale provides UiScale(scale)) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Palette.SurfacePanel)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("1 · TRT 1", style = Type.titleL(), color = Palette.Text)
            Text("Kanal değiştir", style = Type.body(), color = Palette.Text2)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Hakkında
// ---------------------------------------------------------------------------------------------

@Composable
fun AboutScreen(vm: AppViewModel) {
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val s = LocalUiScale.current
    val versionFocus = remember { FocusRequester() }
    var presses by remember { mutableIntStateOf(0) }
    val version = remember { vm.appVersion() }
    val listInfo = remember(catalog) { "${formatDate(catalog.updatedAt)} · ${catalog.enabledChannels.size} kanal" }

    LaunchedEffect(Unit) { runCatching { versionFocus.requestFocus() } }

    Box(
        Modifier.fillMaxSize().padding(horizontal = Space.SafeX, vertical = Space.SafeY),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.width(if (s.isLargest) 650.dp else 560.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            LogoMark(56.dp)
            Spacer(Modifier.height(12.dp))
            Text("Aile TV", style = Type.headline(), color = Palette.Text)
            Spacer(Modifier.height(18.dp))
            VersionRow(
                value = version,
                focusRequester = versionFocus,
                onClick = {
                    // Hidden shortcut (Guidelines §6): 5 × OK opens Tanı. No feedback per press.
                    val next = presses + 1
                    if (next >= 5) {
                        presses = 0
                        vm.navigate(Screen.Diagnostics)
                    } else {
                        presses = next
                    }
                },
            )
            AboutRow("Kanal listesi", listInfo)
            AboutRow("Yayınlar", "Kanalların kendi resmi yayınları")
            Spacer(Modifier.height(18.dp))
            Text(
                "Bu uygulama aile içi kullanım içindir. Kanal yayınları ve logoları ilgili yayıncılara aittir.",
                style = Type.bodyM(),
                color = Palette.Text2,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun VersionRow(value: String, focusRequester: FocusRequester, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).focusRequester(focusRequester),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Palette.Text,
            focusedContainerColor = Palette.FocusBg,
            focusedContentColor = Palette.FocusText,
            pressedContainerColor = Palette.FocusBg,
            pressedContentColor = Palette.FocusText,
        ),
        // Pressed looks exactly like focused: the 5-press shortcut stays invisible.
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f, pressedScale = 1.02f),
        interactionSource = interaction,
    ) {
        AboutRowContent(
            label = "Sürüm",
            value = value,
            labelColor = if (focused) Palette.FocusText else Palette.Text2,
            modifier = Modifier.align(Alignment.CenterStart),
        )
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Box(Modifier.fillMaxWidth().heightIn(min = 52.dp), contentAlignment = Alignment.CenterStart) {
        AboutRowContent(label = label, value = value, labelColor = Palette.Text2, valueColor = Palette.Text)
    }
}

@Composable
private fun AboutRowContent(
    label: String,
    value: String,
    labelColor: Color,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(label, style = Type.body(), color = labelColor)
        Text(
            value,
            style = Type.body().copy(fontWeight = FontWeight.Bold),
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Tanı ekranı (hidden, developer only)
// ---------------------------------------------------------------------------------------------

@Composable
fun DiagnosticsScreen(vm: AppViewModel) {
    // Developer screen: always laid out at text size 1 so both cards fit on the 960×540 canvas.
    CompositionLocalProvider(LocalUiScale provides UiScale(1f)) {
        DiagnosticsContent(vm)
    }
}

@Composable
private fun DiagnosticsContent(vm: AppViewModel) {
    val keys by vm.keyLog.collectAsStateWithLifecycle()
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val playback by vm.playback.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val focus = remember { FocusRequester() }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            tick++
        }
    }

    val recentKeys = remember(keys) { keys.take(8) }
    val androidVersion = remember { "${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}" }
    val model = remember { "${Build.MANUFACTURER} ${Build.MODEL}" }
    val abis = remember { Build.SUPPORTED_ABIS.joinToString(", ") }
    val memory = remember(context) {
        val am = context.getSystemService(ActivityManager::class.java)
        if (am == null) "?" else "${am.memoryClass} MB" + if (am.isLowRamDevice) " · düşük RAM" else ""
    }
    val network = remember(tick) { runCatching { vm.container.network.transport() }.getOrDefault("?") }
    val display = remember(tick, activity) {
        activity?.let { runCatching { DisplayModes.describe(it) }.getOrNull() } ?: "?"
    }
    val video = remember(tick, playback) { runCatching { vm.engine.videoInfo() }.getOrNull() }
    val source = remember(playback, video) { describeSource(playback, video) }
    val listInfo = remember(catalog) { "${formatDate(catalog.updatedAt)} · sürüm ${catalog.version}" }

    Column(Modifier.fillMaxSize().padding(horizontal = Space.SafeX, vertical = Space.SafeY)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Tanı ekranı", style = Type.headline(), color = Palette.Text)
            Text(
                "Geliştirici",
                style = Type.label(),
                color = Palette.SelectedText,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Palette.SelectedBg)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            // The only focusable element: keeps focus on the screen; keys are only logged here.
            DiagCard(
                "Son basılan tuşlar",
                Modifier.weight(1f).fillMaxHeight().focusRequester(focus).focusable(),
            ) {
                recentKeys.forEach { e -> DiagLine("${e.time} · ${e.key} · ${e.android}") }
            }
            DiagCard("Cihaz", Modifier.weight(1f).fillMaxHeight()) {
                DiagPair("Android TV sürümü", androidVersion)
                DiagPair("Model", model)
                DiagPair("İşlemci mimarisi", abis)
                DiagPair("Bellek", memory)
                DiagPair("Ağ", network)
                DiagPair("Ekran", display)
                DiagPair("Kanal listesi", listInfo)
                DiagPair("Oynatılan kaynak", source)
            }
        }
    }
}

private fun describeSource(state: PlaybackState, video: String?): String {
    fun src(type: String, origin: String) = if (origin.isBlank()) type else "$type · $origin"
    val base = when (state) {
        PlaybackState.Idle -> return "—"
        is PlaybackState.Opening ->
            "${state.channel.name} · ${src(state.source.type, state.source.origin)} · açılıyor (${state.sourceIndex + 1}/${state.sourceCount})"
        is PlaybackState.PlayingHls -> "${state.channel.name} · ${src(state.source.type, state.source.origin)}"
        is PlaybackState.YouTube -> "${state.channel.name} · ${src(state.source.type, state.source.origin)}"
        is PlaybackState.Web -> "${state.channel.name} · ${src(state.source.type, state.source.origin)}"
        is PlaybackState.Failed -> return "${state.channel.name} · açılamadı"
    }
    return if (video != null) "$base · $video" else base
}

@Composable
private fun DiagCard(title: String, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Surface1)
            .padding(20.dp),
    ) {
        Text(title, style = Type.titleL(), color = Palette.Text)
        Spacer(Modifier.height(4.dp))
        content()
    }
}

private val DividerModifier = Modifier.drawBehind {
    val w = 1.dp.toPx()
    val y = size.height - w / 2
    drawLine(Palette.Outline, Offset(0f, y), Offset(size.width, y), strokeWidth = w)
}

@Composable
private fun DiagLine(text: String) {
    Text(
        text,
        style = Type.body(),
        color = Palette.Text2,
        modifier = Modifier.fillMaxWidth().then(DividerModifier).padding(vertical = 8.dp),
    )
}

@Composable
private fun DiagPair(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().then(DividerModifier).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = Type.body(), color = Palette.Text2)
        Text(
            value,
            style = Type.body().copy(fontWeight = FontWeight.Bold),
            color = Palette.Text2,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}
