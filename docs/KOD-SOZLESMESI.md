# Aile TV — code contract for screen authors

Read this before writing a screen. The project builds today (`./gradlew :app:assembleDebug`). Stack: Kotlin 2.4.21, AGP 9.4.1 (built-in Kotlin), Compose BOM 2026.09.00 (Compose 1.12), **androidx.tv:tv-material 1.1.0** (`androidx.tv.material3.*`), Media3 1.11.1, minSdk 24, device = Google TV on Android 11 (API 30), 2 GB RAM.

## Build
```bash
source /c/Users/varca/.tvbox-toolchain/env.sh && cd /c/Users/varca/TvBoxApp && ./gradlew :app:compileDebugKotlin --console=plain -q
```
Other people edit other screen files at the same time. If the build fails only because of errors in files you do not own, ignore those errors. If Gradle reports a lock/timeout, wait 30 s and retry.

## Design sources (follow them exactly)
- `docs/figma-make/Guidelines.md` — tokens, sizes, focus rules, key mapping, components, Turkish copy rules.
- `docs/figma-make/PROMPTLAR.md` — per-screen spec with the exact Turkish texts.
- `design/figma-make/src/App.tsx` + `src/index.css` — the approved Figma Make prototype (same screens, real CSS values).
- All sizes are dp on a 960×540 canvas (the app forces this canvas in `ui/AppRoot.kt`). Safe area: 58 dp left/right, 28 dp top/bottom.

## Shared code you use (do NOT edit these files; if you need a change, say so in your final report)
- `ui/theme/Theme.kt` — `Palette.*` colours, `Type.display()/headline()/titleL()/titleM()/labelL()/body()/bodyM()/row()/label()` text styles (already scaled by the user's text size), `LocalUiScale.current.dp(x)` / `.sp(x)` for text-bound sizes, `Space.SafeX/SafeY`.
- `ui/components/Components.kt` — `PillButton`, `FilterPill`, `ChannelLogo`, `ChannelCard`, `ResumeCard`, `ChannelRow`, `SettingsRow`, `ToggleSwitch`, `KeyHint`/`KeyHintRow`/`HintKey`, `Spinner`, `LogoMark`, `DialogScaffold` (scrim + focus trap + Back), `Toast`, `LoadingLine`.
- `ui/components/Icons.kt` — `AppIcons.*` (Tv, Star, StarFilled, StarOff, Sliders, Check, ChevronLeft/Right/Up/Down, Back, Play, Info, ListIcon, Refresh, Home, Alert, WifiOff, MonitorPlay, Globe, Close). Use `androidx.tv.material3.Icon(AppIcons.X, contentDescription, tint = …)`.
- `ui/AppViewModel.kt` — the only API screens call. State: `catalog`, `settings`, `favorites`, `online`, `keyLog`, `playback`, `toast`; functions: `navigate(Screen)`, `back()`, `finishSplash()`, `finishGuide(fromSettings)`, `channelsFor(filter)`, `lastChannel()`, `neighbor(channel, ±1)`, `openChannel(channel)`, `playChannel(channel)`, `retryPlayback()`, `isFavorite(id)`, `toggleFavorite(id)` (shows its own toast), `setFilter(id)`, `setTextScale/setAutoStart/setUpIsNext/setBannerSeconds/setMatch50Hz`, `refreshChannelList()`, `showToast(msg)`, `appVersion()`, `homeFocusChannelId` (read & clear), `engine` (PlaybackEngine: `player`, `videoInfo()`, `onExternalReady(session)`, `onExternalError(session)`), `container.network.transport()`.
- `player/PlaybackEngine.kt` — `PlaybackState` = Idle | Opening(channel, source, sourceIndex, sourceCount) | PlayingHls(channel, source) | YouTube(channel, source, videoId, youtubeChannelId, ready, session) | Web(channel, source, url, ready, session) | Failed(channel, reason).
- `ui/player/VideoViews.kt` — `VideoSurface(player)`, `YouTubeEmbed(videoId, youtubeChannelId, visible, onReady, onError, onBuffering, onResumed)`, `WebPagePlayer(url, onReady, onError, onBuffering, onResumed)` (wire onBuffering/onResumed to `engine.onExternalBuffering/onExternalResumed(session)`).
- `data/Models.kt` — `Channel(id, number, name, category, defaultFavorite, sources)`, `ChannelCatalog(categories, enabledChannels, channel(id), categoryLabel(id), updatedAt, version)`, `CategoryIds.FAVORITES/ALL`.

## Rules
1. Only `androidx.tv.material3` widgets (Surface/Text/Icon) and the shared components. Never `androidx.compose.material3` (mixing MaterialThemes breaks colours).
2. Lazy layouts: `androidx.compose.foundation.lazy.*` (`LazyRow`, `LazyColumn`, `lazy.grid.LazyVerticalGrid`). TvLazy* no longer exists.
3. Focus: every screen sets its initial focus with a `FocusRequester` in a `LaunchedEffect` (wrap `requestFocus()` in `runCatching`, items may not be composed yet). Do not give `Modifier.focusRestorer()` a fallback requester: the fallback overrides explicit focus requests into the group. Prefer explicit focus targets (`focusProperties { onEnter = … }` to the selected/last item). Never leave the screen without a focused element.
4. Back: register `androidx.activity.compose.BackHandler(enabled = …)` only for the layer you own (close an overlay, leave edit mode…). The root already handles "pop screen / exit app"; do not duplicate it.
5. Keys: `Modifier.onPreviewKeyEvent { it.type == KeyEventType.KeyDown && … }` with `it.key == Key.DirectionUp/DirectionDown/DirectionLeft/DirectionRight/DirectionCenter/Enter/ChannelUp/ChannelDown`. Return true only when you consumed the key.
6. Text: Turkish copy from PROMPTLAR.md word for word. No `uppercase()` (Turkish İ/ı). Times with `SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr-TR"))`.
7. No new dependencies, no experimental APIs without `@OptIn`. Explicit imports (no wildcard imports). Keep files self-contained: private helpers stay private in your file.
8. Performance (2 GB box, Mali-G31): no blur, no translucency stacks, no per-frame allocations, `remember` derived lists, keys on lazy items (`key = { it.id }`).
