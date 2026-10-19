package tr.ailetv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import tr.ailetv.data.CategoryIds
import tr.ailetv.ui.AppViewModel
import tr.ailetv.ui.Screen
import tr.ailetv.ui.components.AppIcons
import tr.ailetv.ui.components.ChannelCard
import tr.ailetv.ui.components.FilterPill
import tr.ailetv.ui.components.HintKey
import tr.ailetv.ui.components.KeyHintRow
import tr.ailetv.ui.components.LogoMark
import tr.ailetv.ui.components.PillButton
import tr.ailetv.ui.components.ResumeCard
import tr.ailetv.ui.theme.Palette
import tr.ailetv.ui.theme.Type

/*
 * Ana ekran (Guidelines §5–§7, PROMPTLAR Prompt 3 + 4, App.tsx HomeScreen).
 *
 * Vertical layout on the 960×540 canvas (text size 1): top bar 28–68, resume card 88–192,
 * chips 216–256, first grid row at 280. The chip row and the grid are full-width lazy layouts
 * with 58 dp side padding so the 1.05× chip and the 1.1× card (plus its 3 dp ring) never clip.
 *
 * Focus moves between zones explicitly (top bar → resume card → selected chip → first card and
 * back up); inside the chip row and the grid the platform focus search is used, with every
 * edge consumed so nothing wraps or jumps to another zone.
 *
 * No focusRestorer on the chip row or the grid: a restorer also redirects every programmatic
 * request into its zone (to the selected chip, the first card, the watched channel's card) to
 * the child it saved. Each zone has an onEnter instead that lands on the element the spec names.
 */

/** Set when "Ayarlar" opens Settings so focus lands on that button again when Home comes back. */
private var focusSettingsOnReturn = false

private enum class AfterFilter { FirstItem, Chip }

/** The element to give focus back to after the "İnternet yok" overlay (see HomeScreen). */
private class OfflineHandBack {
    /** The element that got focus last (set on every focus gain, never cleared). */
    var last: FocusRequester? = null
    var lastIsCard = false
    /** What the overlay took focus from, until it has been given back. */
    var target: FocusRequester? = null
    var targetIsCard = false

    fun focused(req: FocusRequester, card: Boolean = false) {
        last = req
        lastIsCard = card
    }
}

private val GlideSpec = tween<Float>(150, easing = EaseOut)

/** The grid scrolls itself (row above the focused row stays on top), so focus must not auto-scroll it. */
private object NoAutoScroll : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(vm: AppViewModel, editFavorites: Boolean) {
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val online by vm.online.collectAsStateWithLifecycle()
    val offlineShown by vm.offlineShown.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val activeFilter = settings.activeFilter
    val textScale = settings.textScale
    val categories = catalog.categories
    val cols = when {
        textScale >= 1.5f -> 3
        textScale >= 1.25f -> 4
        else -> 5
    }
    val favSet = remember(favorites) { favorites.toHashSet() }
    // In edit mode on the favourites filter the list is frozen: un-starring a card must not pull it
    // (and the focus) out of the grid. It is recomputed when the filter or the mode changes.
    val favKey: Any = if (editFavorites && activeFilter == CategoryIds.FAVORITES) Unit else favorites
    val channels = remember(catalog, activeFilter, favKey, editFavorites) { vm.channelsFor(activeFilter) }
    val lastChannel = remember(catalog, settings.lastChannelId) { vm.lastChannel() }
    val resumeChannel = if (editFavorites) null else lastChannel
    val selectedChip = remember(categories, activeFilter) {
        val i = categories.indexOfFirst { it.id == activeFilter }
        if (i >= 0) i else categories.indexOfFirst { it.id == CategoryIds.ALL }
    }
    val favoritesEmpty = channels.isEmpty() && activeFilter == CategoryIds.FAVORITES

    // ---------- focus requesters ----------
    val favReq = remember { FocusRequester() }
    val settingsReq = remember { FocusRequester() }
    val doneReq = remember { FocusRequester() }
    val resumeReq = remember { FocusRequester() }
    val emptyAllReq = remember { FocusRequester() }
    val emptyEditReq = remember { FocusRequester() }
    val chipReqs = remember(categories.size) { List(categories.size) { FocusRequester() } }
    val cardReqs = remember { HashMap<String, FocusRequester>() }
    fun cardReq(id: String): FocusRequester = cardReqs.getOrPut(id) { FocusRequester() }

    // ---------- returning from the player ----------
    val returnId = remember { vm.homeFocusChannelId }
    val initialIndex = remember {
        if (returnId != null && !editFavorites && channels.isNotEmpty()) {
            channels.indexOfFirst { it.id == returnId }.coerceAtLeast(0)
        } else {
            -1
        }
    }

    // ---------- scroll states ----------
    val chipState = rememberLazyListState()
    val gridStart = remember { intArrayOf(if (initialIndex >= 0) (initialIndex / cols - 1).coerceAtLeast(0) * cols else 0) }
    // A fresh grid state per filter: a new filter always starts at the top.
    val gridState = remember(activeFilter) { LazyGridState(gridStart[0]).also { gridStart[0] = 0 } }

    // ---------- focus tracking ----------
    var screenFocused by remember { mutableStateOf(false) }
    var gridFocused by remember { mutableStateOf(false) }
    // The focused card, or the card an explicit entry is about to focus (the grid's onEnter target).
    var gridIndex by remember { mutableIntStateOf(initialIndex.coerceAtLeast(0)) }
    var assumeGrid by remember { mutableStateOf(initialIndex >= 0) }
    var emptyFocused by remember { mutableStateOf(false) }
    var chipsFocused by remember { mutableStateOf(false) }
    var focusedChipId by remember { mutableStateOf<String?>(null) }
    var afterFilter by remember { mutableStateOf<Pair<String, AfterFilter>?>(null) }
    val started = remember { booleanArrayOf(false) }
    val handBack = remember { OfflineHandBack() }
    // Leaving edit mode puts focus on "Favorileri düzenle": keep the header up until it got there.
    val pinHeader = remember(editFavorites) { mutableStateOf(!editFavorites && started[0]) }

    // Top bar + resume card slide away once focus is past the first grid row. With the largest text
    // even the first row does not fit under them, so there they go as soon as the grid has focus.
    // The edit-mode bar (title + key hints) always stays: the edit grid fits below it.
    val hasChannels = channels.isNotEmpty()
    val collapsedState = remember(editFavorites, textScale, cols, hasChannels, pinHeader) {
        val collapseFromRow = if (textScale >= 1.5f) 0 else 1
        derivedStateOf {
            when {
                editFavorites || pinHeader.value -> false
                (gridFocused || assumeGrid) && hasChannels -> gridIndex / cols >= collapseFromRow
                emptyFocused -> textScale > 1f
                else -> false
            }
        }
    }
    val collapsed by collapsedState

    // ---------- focus helpers ----------
    fun tryFocus(req: FocusRequester): Boolean =
        runCatching { req.requestFocus(FocusDirection.Enter) }.getOrDefault(false)

    fun focusSelectedChip() {
        val i = selectedChip
        if (i !in chipReqs.indices) return
        if (!tryFocus(chipReqs[i])) {
            scope.launch {
                chipState.scrollToItem(i)
                chipReqs[i].awaitFocus()
            }
        }
    }

    fun focusFirstItem() {
        val first = channels.firstOrNull()
        when {
            first != null -> {
                gridIndex = 0
                val atTop = gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
                if (!atTop || !tryFocus(cardReq(first.id))) {
                    scope.launch {
                        gridState.scrollToItem(0)
                        cardReq(first.id).awaitFocus()
                    }
                }
            }
            favoritesEmpty -> tryFocus(emptyAllReq)
        }
    }

    suspend fun awaitFirstItem() {
        val first = channels.firstOrNull()
        when {
            first != null -> {
                gridIndex = 0
                gridState.scrollToItem(0)
                cardReq(first.id).awaitFocus()
            }
            favoritesEmpty -> emptyAllReq.awaitFocus()
        }
    }

    fun focusAboveChips() {
        when {
            resumeChannel != null -> tryFocus(resumeReq)
            editFavorites -> tryFocus(doneReq)
            else -> tryFocus(favReq)
        }
    }

    fun focusBelowTopBar() {
        if (resumeChannel != null) tryFocus(resumeReq) else focusSelectedChip()
    }

    fun chipDown(id: String) {
        if (id != activeFilter) {
            // Focus rested on this chip for less than 300 ms: apply it now and land on its first card.
            focusedChipId = null
            afterFilter = id to AfterFilter.FirstItem
            vm.setFilter(id)
        } else {
            focusFirstItem()
        }
    }

    suspend fun enterEditMode() {
        if (favoritesEmpty) {
            // Nothing to edit on an empty favourites list: show every channel instead.
            afterFilter = CategoryIds.ALL to AfterFilter.FirstItem
            vm.setFilter(CategoryIds.ALL)
        } else {
            awaitFirstItem()
        }
    }

    // ---------- initial focus, entering / leaving edit mode ----------
    LaunchedEffect(editFavorites) {
        if (!started[0]) {
            started[0] = true
            vm.homeFocusChannelId = null
            try {
                when {
                    initialIndex >= 0 -> cardReq(channels[initialIndex].id).awaitFocus()
                    returnId != null && !editFavorites && favoritesEmpty -> emptyAllReq.awaitFocus()
                    editFavorites -> enterEditMode()
                    focusSettingsOnReturn -> settingsReq.awaitFocus()
                    resumeChannel != null -> resumeReq.awaitFocus()
                    selectedChip in chipReqs.indices -> chipReqs[selectedChip].awaitFocus()
                    else -> favReq.awaitFocus()
                }
            } finally {
                focusSettingsOnReturn = false
                assumeGrid = false
            }
            return@LaunchedEffect
        }
        if (editFavorites) {
            enterEditMode()
        } else {
            // Back / "Bitti": focus returns to "Favorileri düzenle" (pinHeader keeps the bar shown).
            try {
                favReq.awaitFocus()
            } finally {
                pinHeader.value = false
            }
            gridState.glideTo(0)
        }
    }

    // A filter applied from ▼ on a chip or from "Tüm kanallara git": move focus once it is in effect.
    LaunchedEffect(activeFilter) {
        val pending = afterFilter ?: return@LaunchedEffect
        if (pending.first != activeFilter) return@LaunchedEffect
        afterFilter = null
        when (pending.second) {
            AfterFilter.FirstItem -> awaitFirstItem()
            AfterFilter.Chip -> chipReqs.getOrNull(selectedChip)?.awaitFocus()
        }
    }

    // Chip focus applies its filter after 300 ms of rest (read via snapshotFlow: no recomposition per move).
    LaunchedEffect(Unit) {
        snapshotFlow { focusedChipId }.collectLatest { id ->
            if (id != null) {
                delay(300)
                vm.setFilter(id)
            }
        }
    }

    // The row above the focused row stays at the top of the grid; row 0 means "scrolled to the top".
    // In edit mode (bar stays) with the largest text two rows do not fit: the focused row goes on top.
    LaunchedEffect(gridState, cols, editFavorites) {
        snapshotFlow { if (gridFocused) gridIndex else -1 }.collectLatest { index ->
            if (index < 0) return@collectLatest
            val row = index / cols
            val topRow = when {
                row == 0 -> 0
                !editFavorites || gridState.fitsTwoRows() -> row - 1
                else -> row
            }
            gridState.glideTo(topRow * cols)
        }
    }

    // While the chips are not focused the selected chip (with its ✓) stays in view.
    LaunchedEffect(selectedChip, chipsFocused) {
        if (chipsFocused || selectedChip < 0) return@LaunchedEffect
        withFrameNanos { }
        val info = chipState.layoutInfo
        if (info.visibleItemsInfo.isEmpty()) return@LaunchedEffect
        val item = info.visibleItemsInfo.firstOrNull { it.index == selectedChip }
        val end = info.viewportSize.width - info.beforeContentPadding - info.afterContentPadding
        if (item == null || item.offset < 0 || item.offset + item.size > end) {
            chipState.animateScrollToItem(selectedChip)
        }
    }

    // Safety net: if the focused item disappears (list refresh) or an overlay gave focus back, refocus.
    LaunchedEffect(screenFocused, online) {
        if (screenFocused || !online) return@LaunchedEffect
        delay(400)
        when {
            resumeChannel != null -> resumeReq.awaitFocus()
            selectedChip in chipReqs.indices -> {
                chipState.scrollToItem(selectedChip)
                chipReqs[selectedChip].awaitFocus()
            }
            editFavorites -> doneReq.awaitFocus()
            else -> favReq.awaitFocus()
        }
    }

    // The "İnternet yok" overlay (AppRoot) takes focus from whatever had it. When it goes away the framework
    // focuses a leaf by itself (a D-pad search from the top left: the resume card or a chip), and nothing
    // outside the screen can restore a card or a chip, so Home gives focus back to that element itself, once.
    LaunchedEffect(offlineShown) {
        if (offlineShown) {
            // An earlier episode whose hand-back did not finish keeps its target.
            if (handBack.target == null) {
                handBack.target = handBack.last
                handBack.targetIsCard = handBack.lastIsCard
            }
            // The header stays as it was (it would expand as the grid loses focus): the grid keeps its
            // height, so the card stays composed and nothing moves when the overlay goes away.
            if (handBack.targetIsCard) assumeGrid = true
            return@LaunchedEffect
        }
        val target = handBack.target ?: return@LaunchedEffect
        // Retries for a few frames: the overlay may still be on screen (it keeps focus until it is gone).
        target.awaitFocus()
        handBack.target = null
        if (handBack.targetIsCard) assumeGrid = false
    }

    // Back inside the grid, or on the empty-favourites block that stands in for it: scroll to the top
    // and go to the selected chip. Elsewhere the root handles it.
    BackHandler(enabled = !editFavorites && (gridFocused || emptyFocused)) {
        val inGrid = gridFocused
        focusSelectedChip()
        // Without a grid on screen its state is never laid out and scrolling it would hang.
        if (inGrid) scope.launch { gridState.glideTo(0) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .onFocusChanged { screenFocused = it.hasFocus }
            .padding(top = 24.dp),
    ) {
        AnimatedVisibility(
            visible = !collapsed,
            enter = expandVertically(tween(150, easing = EaseOut), expandFrom = Alignment.Top) +
                slideInVertically(tween(150, easing = EaseOut)) { -it },
            exit = shrinkVertically(tween(150, easing = EaseOut), shrinkTowards = Alignment.Top) +
                slideOutVertically(tween(150, easing = EaseOut)) { -it },
        ) {
            Column(Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(4.dp))
                if (editFavorites) {
                    EditTopBar(
                        doneReq = doneReq,
                        onDone = { vm.back() },
                        onDown = { focusBelowTopBar() },
                        onFocused = { handBack.focused(it) },
                    )
                } else {
                    MainTopBar(
                        favReq = favReq,
                        settingsReq = settingsReq,
                        onFavorites = { vm.navigate(Screen.Home(editFavorites = true)) },
                        onSettings = {
                            focusSettingsOnReturn = true
                            vm.navigate(Screen.Settings)
                        },
                        onDown = { focusBelowTopBar() },
                        onFocused = { handBack.focused(it) },
                    )
                }
                if (resumeChannel != null) {
                    Spacer(Modifier.height(20.dp))
                    ResumeCard(
                        channel = resumeChannel,
                        onClick = { vm.openChannel(resumeChannel) },
                        modifier = Modifier
                            .padding(horizontal = 58.dp)
                            .onFocusGained(resumeReq) { handBack.focused(it) }
                            .dpad(
                                up = { tryFocus(favReq); true },
                                down = { focusSelectedChip(); true },
                                left = { true },
                                right = { true },
                            ),
                        focusRequester = resumeReq,
                    )
                    Spacer(Modifier.height(20.dp))
                } else {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // ---------- category chips ----------
        LazyRow(
            state = chipState,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    chipsFocused = it.hasFocus
                    if (!it.hasFocus) focusedChipId = null
                }
                // Entering the row always lands on the selected chip (never on a chip passed over).
                .focusProperties { onEnter = { chipReqs.getOrNull(selectedChip)?.let { tryFocus(it) } } },
            contentPadding = PaddingValues(horizontal = 58.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(categories, key = { _, c -> c.id }) { i, category ->
                FilterPill(
                    label = category.label,
                    selected = category.id == activeFilter,
                    onClick = {
                        focusedChipId = null
                        vm.setFilter(category.id)
                    },
                    modifier = Modifier
                        .focusRequester(chipReqs[i])
                        .dpad(
                            up = { focusAboveChips(); true },
                            down = { chipDown(category.id); true },
                            left = { i == 0 },
                            right = { i == categories.lastIndex },
                        ),
                    star = category.id == CategoryIds.FAVORITES,
                    onFocused = {
                        focusedChipId = category.id
                        handBack.focused(chipReqs[i])
                    },
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // ---------- channel grid / empty state ----------
        Box(Modifier.fillMaxWidth().weight(1f)) {
            when {
                channels.isNotEmpty() -> CompositionLocalProvider(LocalBringIntoViewSpec provides NoAutoScroll) {
                    // Same for the grid (a list refresh can empty it).
                    DisposableEffect(Unit) { onDispose { gridFocused = false } }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(cols),
                        state = gridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .onFocusChanged { gridFocused = it.hasFocus }
                            // Entering the grid lands on card gridIndex: explicit entries (first card,
                            // watched channel) set it before they request focus, so they are kept; any other
                            // entry (the offline hand-back, the framework's own search) gets the last card.
                            .focusProperties {
                                onEnter = { channels.getOrNull(gridIndex)?.let { tryFocus(cardReq(it.id)) } }
                            },
                        contentPadding = PaddingValues(start = 58.dp, end = 58.dp, top = 14.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        itemsIndexed(channels, key = { _, ch -> ch.id }) { i, channel ->
                            val size = channels.size
                            ChannelCard(
                                channel = channel,
                                favorite = channel.id in favSet,
                                onClick = {
                                    if (editFavorites) {
                                        vm.toggleFavorite(channel.id)
                                    } else {
                                        focusSettingsOnReturn = false
                                        vm.openChannel(channel)
                                    }
                                },
                                modifier = Modifier
                                    .focusRequester(cardReq(channel.id))
                                    .dpad(
                                        up = {
                                            if (i < cols) {
                                                focusSelectedChip()
                                                true
                                            } else {
                                                false
                                            }
                                        },
                                        down = {
                                            when {
                                                // Last row: ▼ does nothing (no wrap).
                                                i / cols == (size - 1) / cols -> true
                                                // Nothing directly below in the shorter last row: its last
                                                // card. If that is not composed yet the platform search
                                                // picks the nearest card of that row.
                                                i + cols >= size -> tryFocus(cardReq(channels[size - 1].id))
                                                else -> false
                                            }
                                        },
                                        left = { i % cols == 0 },
                                        right = { i % cols == cols - 1 || i == size - 1 },
                                    ),
                                editing = editFavorites,
                                onFocused = {
                                    gridIndex = i
                                    handBack.focused(cardReq(channel.id), card = true)
                                },
                            )
                        }
                    }
                }
                favoritesEmpty -> EmptyFavorites(
                    editing = editFavorites,
                    largeText = textScale > 1f,
                    allReq = emptyAllReq,
                    editReq = emptyEditReq,
                    onAll = {
                        afterFilter = CategoryIds.ALL to AfterFilter.Chip
                        vm.setFilter(CategoryIds.ALL)
                    },
                    onEdit = { vm.navigate(Screen.Home(editFavorites = true)) },
                    onUp = { focusSelectedChip() },
                    onFocusChanged = { emptyFocused = it },
                    onFocused = { handBack.focused(it) },
                    tryFocus = { tryFocus(it) },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
    }
}

// ---------- top bars ----------

@Composable
private fun MainTopBar(
    favReq: FocusRequester,
    settingsReq: FocusRequester,
    onFavorites: () -> Unit,
    onSettings: () -> Unit,
    onDown: () -> Unit,
    onFocused: (FocusRequester) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .padding(horizontal = 58.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LogoMark(32.dp)
        Spacer(Modifier.width(12.dp))
        Text("Aile TV", style = Type.titleL(), color = Palette.Text, maxLines = 1)
        Spacer(Modifier.weight(1f))
        PillButton(
            text = "Favorileri düzenle",
            onClick = onFavorites,
            icon = AppIcons.Star,
            focusRequester = favReq,
            modifier = Modifier.onFocusGained(favReq, onFocused).dpad(
                up = { true },
                down = { onDown(); true },
                left = { true },
                right = { runCatching { settingsReq.requestFocus(FocusDirection.Enter) }; true },
            ),
        )
        Spacer(Modifier.width(12.dp))
        PillButton(
            text = "Ayarlar",
            onClick = onSettings,
            icon = AppIcons.Sliders,
            focusRequester = settingsReq,
            modifier = Modifier.onFocusGained(settingsReq, onFocused).dpad(
                up = { true },
                down = { onDown(); true },
                left = { runCatching { favReq.requestFocus(FocusDirection.Enter) }; true },
                right = { true },
            ),
        )
        Spacer(Modifier.width(20.dp))
        Clock()
    }
}

@Composable
private fun EditTopBar(
    doneReq: FocusRequester,
    onDone: () -> Unit,
    onDown: () -> Unit,
    onFocused: (FocusRequester) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .padding(horizontal = 58.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(AppIcons.Star, contentDescription = null, modifier = Modifier.size(22.dp), tint = Palette.Star)
        Spacer(Modifier.width(12.dp))
        Text("Favorileri düzenle", style = Type.titleL(), color = Palette.Text, maxLines = 1)
        Spacer(Modifier.weight(1f))
        KeyHintRow(HintKey.Ok to "★ ekle veya çıkar", HintKey.Back to "Bitti")
        Spacer(Modifier.weight(1f))
        PillButton(
            text = "Bitti",
            onClick = onDone,
            focusRequester = doneReq,
            modifier = Modifier.onFocusGained(doneReq, onFocused).dpad(
                up = { true },
                down = { onDown(); true },
                left = { true },
                right = { true },
            ),
        )
    }
}

@Composable
private fun Clock() {
    val format = remember { SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr-TR")) }
    var time by remember { mutableStateOf(format.format(Date())) }
    // Wakes just after each minute boundary, like the player's banner clock, so both flip together.
    LaunchedEffect(Unit) {
        while (true) {
            time = format.format(Date()) // also covers a minute that turned before this effect started
            delay(60_000L - System.currentTimeMillis() % 60_000L + 50L)
        }
    }
    Text(time, style = Type.titleM(), color = Palette.Text2, maxLines = 1)
}

// ---------- empty favourites ----------

@Composable
private fun EmptyFavorites(
    editing: Boolean,
    largeText: Boolean,
    allReq: FocusRequester,
    editReq: FocusRequester,
    onAll: () -> Unit,
    onEdit: () -> Unit,
    onUp: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onFocused: (FocusRequester) -> Unit,
    tryFocus: (FocusRequester) -> Boolean,
    modifier: Modifier = Modifier,
) {
    // The block goes away while focused ("Tüm kanallara git"): its flag must not outlive it (Back, header).
    DisposableEffect(Unit) { onDispose { onFocusChanged(false) } }
    Column(
        modifier
            .padding(top = if (largeText) 24.dp else 60.dp)
            .width(520.dp)
            .onFocusChanged { onFocusChanged(it.hasFocus) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(AppIcons.Star, contentDescription = null, modifier = Modifier.size(40.dp), tint = Palette.Text2)
        Spacer(Modifier.height(14.dp))
        Text("Henüz favori kanalınız yok.", style = Type.titleL(), color = Palette.Text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Sık izlediğiniz kanalları favorilere ekleyin. Kanal değiştirirken yalnızca onlar arasında dolaşırsınız.",
            style = Type.body(),
            color = Palette.Text2,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PillButton(
                text = "Tüm kanallara git",
                onClick = onAll,
                focusRequester = allReq,
                modifier = Modifier.onFocusGained(allReq, onFocused).dpad(
                    up = { onUp(); true },
                    down = { true },
                    left = { true },
                    right = {
                        if (!editing) tryFocus(editReq)
                        true
                    },
                ),
            )
            if (!editing) {
                PillButton(
                    text = "Favorileri düzenle",
                    onClick = onEdit,
                    focusRequester = editReq,
                    modifier = Modifier.onFocusGained(editReq, onFocused).dpad(
                        up = { onUp(); true },
                        down = { true },
                        left = { tryFocus(allReq); true },
                        right = { true },
                    ),
                )
            }
        }
    }
}

// ---------- helpers ----------

/** Reports [req] to [onFocused] each time this element gains focus. */
private fun Modifier.onFocusGained(req: FocusRequester, onFocused: (FocusRequester) -> Unit): Modifier =
    onFocusChanged { if (it.isFocused) onFocused(req) }

/** D-pad handling for one element. Each lambda returns true when it consumed the key. */
private fun Modifier.dpad(
    up: () -> Boolean = { false },
    down: () -> Boolean = { false },
    left: () -> Boolean = { false },
    right: () -> Boolean = { false },
): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) {
        false
    } else {
        when (event.key) {
            Key.DirectionUp -> up()
            Key.DirectionDown -> down()
            Key.DirectionLeft -> left()
            Key.DirectionRight -> right()
            else -> false
        }
    }
}

/** Requests focus, retrying for a few frames while lazy items are still being composed. */
private suspend fun FocusRequester.awaitFocus(maxFrames: Int = 20): Boolean {
    repeat(maxFrames) {
        if (runCatching { requestFocus(FocusDirection.Enter) }.getOrDefault(false)) return true
        withFrameNanos { }
    }
    return false
}

/** True when two card rows (plus the gap) fit in the grid viewport. */
private fun LazyGridState.fitsTwoRows(): Boolean {
    val info = layoutInfo
    val rowHeight = info.visibleItemsInfo.firstOrNull()?.size?.height ?: return true
    val available = info.viewportSize.height - info.beforeContentPadding - info.afterContentPadding
    return 2 * rowHeight + info.mainAxisItemSpacing <= available
}

/** Puts item [index]'s row at the top of the grid (150 ms ease-out when it is on screen). */
private suspend fun LazyGridState.glideTo(index: Int) {
    val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (item == null) {
        scrollToItem(index)
        return
    }
    val delta = item.offset.y.toFloat()
    if (abs(delta) >= 1f) animateScrollBy(delta, GlideSpec)
}
