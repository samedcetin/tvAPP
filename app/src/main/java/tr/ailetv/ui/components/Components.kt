package tr.ailetv.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import tr.ailetv.data.Channel
import tr.ailetv.ui.theme.LocalUiScale
import tr.ailetv.ui.theme.Palette
import tr.ailetv.ui.theme.Type

private val Pill = RoundedCornerShape(999.dp)

/** Guidelines §7 Button. [wide] = dialog button (48 dp, full width, left aligned). */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    wide: Boolean = false,
    focusRequester: FocusRequester? = null,
) {
    val s = LocalUiScale.current
    Surface(
        onClick = onClick,
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .then(if (wide) Modifier.fillMaxWidth().heightIn(min = s.dp(48)) else Modifier.heightIn(min = s.dp(40))),
        shape = ClickableSurfaceDefaults.shape(Pill),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Palette.Surface2,
            contentColor = Palette.Text,
            focusedContainerColor = Palette.FocusBg,
            focusedContentColor = Palette.FocusText,
            pressedContainerColor = Palette.FocusBg,
            pressedContentColor = Palette.FocusText,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
    ) {
        Row(
            modifier = Modifier
                .then(if (wide) Modifier.fillMaxWidth() else Modifier)
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .align(if (wide) Alignment.CenterStart else Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(if (wide) 20.dp else 18.dp))
            Text(text, style = Type.labelL(), maxLines = 1)
        }
    }
}

/** Guidelines §7 Chip (category filter). Selected ≠ focused: selected shows ✓ on a blue-grey fill. */
@Composable
fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    star: Boolean = false,
    onFocused: () -> Unit = {},
) {
    val s = LocalUiScale.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(s.dp(40))
            .onFocusChanged { if (it.isFocused) onFocused() },
        shape = ClickableSurfaceDefaults.shape(Pill),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Palette.SelectedBg else Palette.Surface2,
            contentColor = if (selected) Palette.SelectedText else Palette.Text,
            focusedContainerColor = Palette.FocusBg,
            focusedContentColor = Palette.FocusText,
            pressedContainerColor = Palette.FocusBg,
            pressedContentColor = Palette.FocusText,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        interactionSource = interaction,
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp).align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (selected) Icon(AppIcons.Check, null, Modifier.size(16.dp))
            if (star) Icon(AppIcons.StarFilled, null, Modifier.size(16.dp), tint = if (focused) Palette.FocusText else Palette.Star)
            Text(label, style = Type.labelL(), maxLines = 1)
        }
    }
}

/** Channel name drawn as a "logo" placeholder until real logos exist. */
@Composable
fun ChannelLogo(name: String, modifier: Modifier = Modifier, large: Boolean = false) {
    val size = when {
        large -> 22
        name.length <= 5 -> 22
        name.length <= 8 -> 19
        else -> 16
    }
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(Palette.SurfaceTile), contentAlignment = Alignment.Center) {
        Text(
            name,
            color = Palette.Text,
            fontSize = size.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

/**
 * Guidelines §7 ChannelCard: 16:9 (or 1:1) logo tile + "number name" label.
 * Focus: whole card scales 1.1, 3 dp white ring around the tile, drawn above neighbours.
 */
@Composable
fun ChannelCard(
    channel: Channel,
    favorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    editing: Boolean = false,
    square: Boolean = false,
    onFocused: () -> Unit = {},
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.1f else 1f, tween(150), label = "cardScale")
    val tileShape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (square) 1f else 16f / 9f)
                .onFocusChanged { if (it.isFocused) onFocused() },
            shape = ClickableSurfaceDefaults.shape(tileShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Palette.SurfaceTile,
                contentColor = Palette.Text,
                focusedContainerColor = Palette.SurfaceTile,
                focusedContentColor = Palette.Text,
                pressedContainerColor = Palette.SurfaceTile,
                pressedContentColor = Palette.Text,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(BorderStroke(3.dp, Palette.FocusRing), shape = tileShape),
            ),
            interactionSource = interaction,
        ) {
            ChannelLogo(channel.name, Modifier.fillMaxSize())
            if (favorite || editing) {
                val badge = if (editing) 28.dp else 22.dp
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(badge)
                        .clip(CircleShape)
                        .background(Color(0xC70F1013))
                        .then(if (editing && !favorite) Modifier.border(1.5.dp, Palette.KeyOutline, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (favorite) AppIcons.StarFilled else AppIcons.Star,
                        contentDescription = if (favorite) "Favori" else null,
                        tint = if (favorite) Palette.Star else Palette.KeyOutline,
                        modifier = Modifier.size(badge * 0.6f),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(channel.number.toString(), style = Type.labelL(), color = Palette.Text2, modifier = Modifier.widthIn(min = 18.dp))
            Text(channel.name, style = Type.labelL(), color = Palette.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** "Kaldığın yerden" card on Home. */
@Composable
fun ResumeCard(channel: Channel, onClick: () -> Unit, modifier: Modifier = Modifier, focusRequester: FocusRequester? = null) {
    val s = LocalUiScale.current
    val shape = RoundedCornerShape(16.dp)
    Surface(
        onClick = onClick,
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .fillMaxWidth()
            .heightIn(min = s.dp(104)),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Palette.Surface1,
            contentColor = Palette.Text,
            focusedContainerColor = Palette.Surface1,
            focusedContentColor = Palette.Text,
            pressedContainerColor = Palette.Surface1,
            pressedContentColor = Palette.Text,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(3.dp, Palette.FocusRing), shape = shape)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 24.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ChannelLogo(channel.name, Modifier.width(128.dp).height(72.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Kaldığın yerden", style = Type.label(), color = Palette.Text2)
                Text("${channel.number} · ${channel.name}", style = Type.titleL(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            KeyHint(HintKey.Ok, "İzlemeye devam et")
        }
    }
}

/** Guidelines §7 ListRow (in-player channel list, search results). */
@Composable
fun ChannelRow(
    channel: Channel,
    favorite: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
) {
    val s = LocalUiScale.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = s.dp(52))
            .onFocusChanged { if (it.isFocused) onFocused() },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (playing) Palette.Surface1 else Color.Transparent,
            contentColor = Palette.Text,
            focusedContainerColor = Palette.FocusBg,
            focusedContentColor = Palette.FocusText,
            pressedContainerColor = Palette.FocusBg,
            pressedContentColor = Palette.FocusText,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        interactionSource = interaction,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp).align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                channel.number.toString(),
                style = Type.labelL(),
                color = if (focused) Palette.FocusText else Palette.Text2,
                modifier = Modifier.width(s.dp(28)),
            )
            Text(channel.name, style = Type.row(), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (playing) {
                Icon(AppIcons.Play, null, Modifier.size(16.dp), tint = if (focused) Palette.FocusText else Palette.Accent)
                Text("Şu an", style = Type.label())
            }
            if (favorite) Icon(AppIcons.StarFilled, "Favori", Modifier.size(16.dp), tint = if (focused) Palette.FocusText else Palette.Star)
        }
    }
}

/** Guidelines §7 SettingsRow. [switchOn] non-null draws a switch instead of a chevron. */
@Composable
fun SettingsRow(
    label: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    switchOn: Boolean? = null,
    focusRequester: FocusRequester? = null,
) {
    val s = LocalUiScale.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Surface(
        onClick = onClick,
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .fillMaxWidth()
            .heightIn(min = s.dp(60)),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Palette.Text,
            focusedContainerColor = Palette.FocusBg,
            focusedContentColor = Palette.FocusText,
            pressedContainerColor = Palette.FocusBg,
            pressedContentColor = Palette.FocusText,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        interactionSource = interaction,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = Type.row())
                if (!value.isNullOrEmpty()) {
                    Text(value, style = Type.bodyM(), color = if (focused) Palette.FocusText else Palette.Text2)
                }
            }
            if (switchOn != null) ToggleSwitch(switchOn) else Icon(AppIcons.ChevronRight, null, Modifier.size(20.dp))
        }
    }
}

@Composable
fun ToggleSwitch(on: Boolean) {
    Box(
        Modifier
            .width(44.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (on) Palette.Accent else Palette.Outline)
            .padding(3.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.size(18.dp).clip(CircleShape).background(if (on) Palette.Bg else Palette.Text2))
    }
}

enum class HintKey { UpDown, Ok, LeftRight, Left, Right, Back }

/** Guidelines §7 KeyHint: key badge + label. */
@Composable
fun KeyHint(key: HintKey, label: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .widthIn(min = 26.dp)
                .height(24.dp)
                .border(1.5.dp, Palette.KeyOutline, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            val tint = Palette.Text
            when (key) {
                HintKey.Ok -> Text("OK", style = Type.label(), color = tint)
                HintKey.Back -> Icon(AppIcons.Back, null, Modifier.size(14.dp), tint = tint)
                HintKey.Left -> Icon(AppIcons.ChevronLeft, null, Modifier.size(14.dp), tint = tint)
                HintKey.Right -> Icon(AppIcons.ChevronRight, null, Modifier.size(14.dp), tint = tint)
                HintKey.UpDown -> Row {
                    Icon(AppIcons.ChevronUp, null, Modifier.size(14.dp), tint = tint)
                    Icon(AppIcons.ChevronDown, null, Modifier.size(14.dp), tint = tint)
                }
                HintKey.LeftRight -> Row {
                    Icon(AppIcons.ChevronLeft, null, Modifier.size(14.dp), tint = tint)
                    Icon(AppIcons.ChevronRight, null, Modifier.size(14.dp), tint = tint)
                }
            }
        }
        Text(label, style = Type.label(), color = Palette.Text2, maxLines = 1)
    }
}

@Composable
fun KeyHintRow(vararg hints: Pair<HintKey, String>, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
        hints.forEach { (k, l) -> KeyHint(k, l) }
    }
}

/**
 * Guidelines §7 Spinner: a [size] ring (40, small 24) of 4 dp, `--outline` with the top quarter in `--accent`,
 * one turn per 0.9 s. Like the prototype's border-box CSS ring, the stroke lies inside the box.
 */
@Composable
fun Spinner(size: Dp = 40.dp, strokeWidth: Dp = 4.dp) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "angle",
    )
    // The stroke and ring geometry are built once per size (drawWithCache); the angle is only read in the draw
    // phase, so a frame redraws without recomposing or allocating.
    Spacer(
        Modifier.size(size).drawWithCache {
            val w = strokeWidth.toPx()
            // A stroke is centred on its path: inset the ring by half of it so it stays inside the box.
            val topLeft = Offset(w / 2f, w / 2f)
            val ring = Size(this.size.width - w, this.size.height - w)
            val stroke = Stroke(w) // butt ends, like the CSS border-top segment
            onDrawBehind {
                drawArc(Palette.Outline, 0f, 360f, false, topLeft = topLeft, size = ring, style = stroke)
                // The top border quarter (225°–315°) at the start of a turn, as in the prototype.
                drawArc(Palette.Accent, 225f + angle, 90f, false, topLeft = topLeft, size = ring, style = stroke)
            }
        },
    )
}

@Composable
fun LogoMark(size: Dp = 32.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.28f)).background(Palette.Accent),
        contentAlignment = Alignment.Center,
    ) {
        Icon(AppIcons.Tv, null, Modifier.size(size * 0.62f), tint = Palette.Bg)
    }
}

/**
 * Guidelines §7 Dialog: scrim + centred panel. Focus cannot leave the panel; Back calls [onDismiss].
 * Attach [initialFocus] to the control that should be focused when the dialog opens.
 */
@Composable
fun DialogScaffold(
    title: String,
    onDismiss: () -> Unit,
    initialFocus: FocusRequester,
    modifier: Modifier = Modifier,
    width: Dp = 440.dp,
    icon: ImageVector? = null,
    iconTint: Color = Palette.Accent,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(onBack = onDismiss)
    Box(Modifier.fillMaxSize().background(Palette.Scrim), contentAlignment = Alignment.Center) {
        Column(
            modifier
                .width(width)
                .clip(RoundedCornerShape(24.dp))
                .background(Palette.SurfaceDialog)
                .padding(28.dp)
                .focusProperties { onExit = { cancelFocusChange() } }
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (icon != null) {
                Icon(icon, null, Modifier.size(40.dp), tint = iconTint)
                Spacer(Modifier.height(4.dp))
            }
            Text(title, style = Type.titleL(), color = Palette.Text)
            if (description != null) Text(description, style = Type.body(), color = Palette.Text2)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
    LaunchedEffect(Unit) { runCatching { initialFocus.requestFocus() } }
}

@Composable
fun Toast(message: String, modifier: Modifier = Modifier, icon: ImageVector = AppIcons.Check) {
    Row(
        modifier
            .height(44.dp)
            .clip(Pill)
            .background(Palette.FocusBg)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = Palette.FocusText)
        Text(message, style = Type.labelL(), color = Palette.FocusText)
    }
}

/** Shows a centred spinner + text; used by loading states. */
@Composable
fun LoadingLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Spinner(24.dp)
        Text(text, style = Type.body(), color = Palette.Text2)
    }
}

/** Content colour of the enclosing tv Surface (white normally, dark when focused). */
val currentContent: Color @Composable get() = LocalContentColor.current
