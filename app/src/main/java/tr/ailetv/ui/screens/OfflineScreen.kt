package tr.ailetv.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import tr.ailetv.ui.AppViewModel
import tr.ailetv.ui.components.AppIcons
import tr.ailetv.ui.components.PillButton
import tr.ailetv.ui.theme.Palette
import tr.ailetv.ui.theme.Type

// İNTERNET YOK (#internet-yok) — PROMPTLAR Prompt 7 (5), index.css .offline-screen.

private val OfflineChecks = listOf(
    "Modem açık mı?",
    "Televizyon kutusu Wi-Fi'a bağlı mı?",
    "Modemi kapatıp 10 saniye sonra tekrar açmayı deneyin.",
)

/**
 * Full-canvas overlay drawn above the current screen while there is no internet. It owns focus
 * (focus cannot leave it, and it takes focus back if the screen underneath grabs it). It goes
 * away by itself when `vm.online` becomes true (AppRoot). Back → [onExit] (leave to Google TV).
 */
@Composable
fun OfflineScreen(vm: AppViewModel, onExit: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val retry = remember { FocusRequester() }
    val network = remember { FocusRequester() }
    var lastFocused by remember { mutableIntStateOf(0) }
    var hasFocus by remember { mutableStateOf(false) }
    var backDown by remember { mutableStateOf(false) }

    BackHandler(onBack = onExit)

    // Initial focus "Tekrar dene"; re-take focus whenever something underneath steals it.
    LaunchedEffect(hasFocus) {
        if (hasFocus) return@LaunchedEffect
        val target = if (lastFocused == 1) network else retry
        runCatching { target.requestFocus() }
        withFrameNanos { }
        if (!hasFocus) {
            // A focus trap underneath (e.g. an open dialog) refused to let go: clear it, then take focus.
            focusManager.clearFocus(force = true)
            runCatching { target.requestFocus() }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Bg)
            // Back is handled here first so a screen underneath can never take it from the overlay.
            .onPreviewKeyEvent { e ->
                if (e.key == Key.Back) {
                    when (e.type) {
                        KeyEventType.KeyDown -> backDown = true
                        KeyEventType.KeyUp -> if (backDown) {
                            backDown = false
                            onExit()
                        }
                    }
                    true
                } else {
                    false
                }
            }
            .onFocusChanged { hasFocus = it.hasFocus }
            .focusProperties { this.onExit = { cancelFocusChange() } }
            .focusGroup(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(AppIcons.WifiOff, contentDescription = null, modifier = Modifier.size(56.dp), tint = Palette.Text2)
            Spacer(Modifier.height(14.dp))
            Text("İnternet bağlantısı yok", style = Type.headline(), color = Palette.Text)
            Spacer(Modifier.height(14.dp))
            Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OfflineChecks.forEach { line ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = Palette.Accent)
                        Spacer(Modifier.width(10.dp))
                        Text(line, style = Type.body(), color = Palette.Text)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                PillButton(
                    "Tekrar dene",
                    onClick = { vm.showToast("Bağlantı kontrol ediliyor…") },
                    modifier = Modifier.onFocusChanged { if (it.isFocused) lastFocused = 0 },
                    icon = AppIcons.Refresh,
                    focusRequester = retry,
                )
                PillButton(
                    "Ağ ayarlarını aç",
                    onClick = { if (!openNetworkSettings(context)) vm.showToast("Ağ ayarları açılamadı") },
                    modifier = Modifier.onFocusChanged { if (it.isFocused) lastFocused = 1 },
                    icon = AppIcons.Sliders,
                    focusRequester = network,
                )
            }
        }
    }
}

/** Opens the TV's Wi-Fi settings, or the main settings if that screen does not exist. */
private fun openNetworkSettings(context: Context): Boolean {
    for (action in arrayOf(Settings.ACTION_WIFI_SETTINGS, Settings.ACTION_SETTINGS)) {
        val intent = Intent(action)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val opened = try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
        if (opened) return true
    }
    return false
}
