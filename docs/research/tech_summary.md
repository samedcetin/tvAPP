# tech
## SUMMARY
Recommended stack: Kotlin 2.4.20 with Jetpack Compose for TV (Compose BOM 2026.09.00 and androidx.tv:tv-material 1.1.0), and Media3/ExoPlayer 1.11.1 for HLS live playback. Build on Android Studio Rabbit 1 (2026.2.1, stable since Oct 1 2026), AGP 9.4.0 and Gradle 9.6.0+. Set compileSdk 37, targetSdk 36 and minSdk 23. Two constraints force some of these numbers: Compose 1.12 needs compileSdk 37 and AGP 9, and Media3 and AndroidX already require minSdk 23.

Google officially recommends Compose for TV and has deprecated Leanback. Flutter and React Native have only community-maintained TV support, mainly for D-pad focus, so native Kotlin is the lowest-risk choice for a solo developer on a 2 GB, Cortex-A55, Mali-G31 box.

The manifest must include:
- the LEANBACK_LAUNCHER intent filter (without it a sideloaded app does not appear on the TV home screen)
- a 320x180 banner
- touchscreen and leanback declared as required=false
- a network-security-config that allows cleartext only for the specific HTTP stream domains

Remote handling:
- Use onPreviewKeyEvent/onKeyEvent with KeyEventType checks, BackHandler, and focusRestorer/FocusRequester.
- Zapping must work with DPAD_UP/DOWN. The stock Mi Box remote is not confirmed to have CH+/- or number keys. Handle KEYCODE_CHANNEL_UP/DOWN as well for other remotes.

Playback: use HLS with adaptive bitrate (ABR) and recover from BehindLiveWindow by calling seekToDefaultPosition()+prepare(). Retry network errors with backoff. Set headers per channel through DefaultHttpDataSource or OkHttpDataSource. Turn on decoder fallback. Leave tunneling off by default and expose it as an option.

Data: fetch a remote channels.json and cache it. Store favorites and the last channel in DataStore. Room is only needed if EPG is added. For EPG, iptv-org/epg must be self-run because it hosts no ready-made files; Turkish sources digiturk, dsmart, tvplus and turksatkablo are all listed as working. Load images with Coil 3.6.3 at small sizes. Use MVVM with manual DI; Hilt (2.60.1) is optional.

Release:
- Build a signed APK with a self-managed keystore (back it up: losing it means no more updates), R8 and resource shrinking on, via ./gradlew assembleRelease.
- Install with adb connect <ip>:5555 + adb install -r (Wireless-debugging pairing needs Android 13+ on TV), or with Downloader / Send Files to TV after enabling developer mode.
- Self-update: version.json plus an APK download, installed through a PackageInstaller session with REQUEST_INSTALL_PACKAGES and the per-app "install unknown apps" grant.

Google's developer verification does not affect TV sideloading in 2026: the Sept 2026 wave covers phones and tablets in BR/ID/SG/TH only. It expands globally in 2027, so registering a free limited-distribution account (up to 20 devices) is advisable before then.

## RECOMMENDATIONS
- Framework: native Kotlin 2.4.20 + Jetpack Compose for TV (androidx.tv:tv-material 1.1.0) + Media3 1.11.1. Do not add Leanback; skip Flutter and React Native for this device and team size.
- Toolchain: Android Studio Rabbit 1 (2026.2.1) with its bundled JDK 21 (AGP needs 17+). AGP 9.4.0 with built-in Kotlin, so no org.jetbrains.kotlin.android. Gradle wrapper 9.6.0 (or 9.7.1). Add kotlin-gradle-plugin 2.4.20 to the buildscript classpath and org.jetbrains.kotlin.plugin.compose 2.4.20. Use KSP 2.3.12 only if Room or Hilt are used.
- SDK levels: compileSdk 37 (required by Compose 1.12), targetSdk 36 set explicitly (AGP 9 otherwise defaults it to compileSdk), minSdk 23 (Media3, Dagger and AndroidX all require 23).
- Dependencies (version catalog): compose-bom 2026.09.00 (ui, foundation, ui-tooling); tv-material 1.1.0 (no material3 theme mixing); activity-compose 1.13.0; lifecycle-viewmodel-compose and runtime-compose 2.11.0; navigation3 1.2.0 (or a simple sealed-class screen state, since the app has under 6 screens); media3 1.11.1 exoplayer, exoplayer-hls, ui-compose (custom overlay) or ui-compose-material3, datasource-okhttp, optional session; okhttp 5.x; kotlinx-serialization-json 1.11.0; datastore-preferences 1.2.1; coil-compose + coil-network-okhttp 3.6.3; optional room 2.8.5 (EPG only) and work 2.12.0 (background EPG/channel refresh).
- Architecture: single-activity MVVM. Repository layer: ChannelRepository (remote channels.json, then a cached file, then a bundled fallback asset), PrefsRepository (DataStore: favorites, last channel, last category, settings), optional EpgRepository (Room). Use manual DI through an AppContainer in the Application class to keep builds simple. Hilt 2.60.1 + androidx.hilt 1.4.0 only if you prefer it.
- channels.json schema suggestion: version, updatedAt, categories[] (id, name_tr, order) and channels[]. Each channel has id, number, name, logoUrl, categoryIds[], streams[] (url, type=hls|dash, headers{User-Agent, Referer, Origin}, priority), epgId and isActive. Support several stream URLs per channel and fall back to the next on error. Bundle a copy in assets so the app works offline on first launch.
- Manifest:
- INTERNET and ACCESS_NETWORK_STATE; REQUEST_INSTALL_PACKAGES (+ UPDATE_PACKAGES_WITHOUT_USER_ACTION) only if self-update ships.
- uses-feature leanback and touchscreen (and faketouch) with required=false.
- application android:banner=@drawable/banner (320x180 xhdpi, with the app name as text) and networkSecurityConfig, with a domain-config cleartext allowlist for the http:// stream hosts only.
- One MainActivity: exported=true, MAIN + LEANBACK_LAUNCHER (add LAUNCHER for phone or emulator testing), screenOrientation=landscape, launchMode=singleTask.
- configChanges including keyboard|keyboardHidden|navigation|screenSize|smallestScreenSize|screenLayout|uiMode|density, so a remote reconnecting doesn't recreate the activity and stop playback (common practice, not from the official docs).
- Remote mapping (player screen, via a root onPreviewKeyEvent on KeyDown):
- DPAD_UP / CHANNEL_UP: previous / next channel. Debounce about 300-500 ms and show the channel banner immediately.
- DPAD_DOWN / CHANNEL_DOWN: the opposite direction.
- DPAD_CENTER / ENTER: show or hide the info and channel-list overlay.
- DPAD_LEFT: open the category/channel side list.
- Digits (if present): numeric entry with a 1.5 s commit.
- BACK: close the overlay; if none is open, return to the channel grid; at the root, exit to Home.

Back must never toggle. Use nativeKeyEvent.keyCode comparisons, repeatCount for long-press fast scrolling, and BackHandler for in-app back.
- Focus: give the grid or list a FocusRequester and request it in LaunchedEffect. Use Modifier.focusRestorer(fallbackRequester) on the LazyVerticalGrid/LazyRow so returning from the player lands on the last-watched channel. Wrap the category chip row in focusGroup(). Use tv-material Surface/Card/FilterChip for built-in focus scale and borders; use high-contrast focus colours and large (24sp+) text for elderly users. Use BringIntoViewSpec to keep the focused row at a fixed pivot.
- Player setup:
- One ExoPlayer instance reused for every zap.
- DefaultRenderersFactory(ctx).setEnableDecoderFallback(true).
- setAudioAttributes(AudioAttributes(USAGE_MEDIA, CONTENT_TYPE_MOVIE), handleAudioFocus = true) and setHandleAudioBecomingNoisy(true).
- OkHttpDataSource.Factory with a default User-Agent; per-channel headers via a ResolvingDataSource or a factory per channel; setAllowCrossProtocolRedirects(true) when using DefaultHttpDataSource.
- Modest LoadControl buffers (keep under about 100 MB on this 2 GB box), and trackSelectionParameters max video size 1920x1080 plus preferred audio language 'tr'.
- Modifier.keepScreenOn() on the player surface.
- On BEHIND_LIVE_WINDOW: seekToDefaultPosition()+prepare(). On IO/network errors: exponential-backoff retry (e.g. 1s, 2s, 4s, max 3), then try the next stream URL, then show a friendly Turkish error with 'Tekrar dene' and 'Başka kanal'.
- Release in onStop. Tunneling off by default, as a hidden setting to test on the device.
- Performance on the low-end SoC: judge performance only on release builds (R8 on). Enable isMinifyEnabled and isShrinkResources with proguard-android-optimize.txt (AGP 9 strict keep rules). Add a Baseline Profile (androidx.baselineprofile / benchmark 1.5.0); profileinstaller applies it to sideloaded installs. Avoid blur and heavy animations. Request logos at their displayed size (e.g. 160x90) via Coil, or bundle pre-sized WebP logos in the APK. Use colour placeholders.
- EPG (phase 2, optional): run the iptv-org/epg grabber on a schedule (GitHub Actions cron, or Docker ghcr.io/iptv-org/epg:master on a small server) for tvplus.com.tr / turksatkablo.com.tr / digiturk.com.tr. Publish a gzipped XMLTV or a trimmed JSON (now/next per channel) to static hosting. The app fetches it with WorkManager, parses it into Room, and shows now/next in the overlay. Keep it now/next only to limit memory.
- Signing and release:
- Create the release keystore once (keytool -genkeypair -v -keystore tvapp-release.jks -keyalg RSA -keysize 4096 -validity 10000 -alias tvapp) and keep two offline backups plus a password manager entry.
- Put the credentials in keystore.properties (gitignored) and wire signingConfigs.release.
- versionCode: monotonic integer MAJOR*10000 + MINOR*100 + PATCH (1.2.3 = 10203), never reused; versionName '1.2.3'.
- Build with ./gradlew assembleRelease and rename the output to tvapp-<versionName>.apk.
- Read the version at runtime via PackageManager longVersionCode, or enable buildFeatures.buildConfig (off by default since AGP 8).
- Install on the box:
1. Enable developer mode (tap 'Android TV OS build' 7 times).
2. Turn on USB/Network debugging.
3. Run adb connect <box-ip>:5555, accept the RSA prompt, then adb install -r tvapp.apk.

If the box is on Android TV 14, adb pair also works. Family-friendly alternative: host the APK at a short URL and install via Downloader (AFTVnews), allowing it in Install unknown apps; if Play Protect blocks it, choose Install anyway. Disable debugging afterwards.
- Self-update (phase 2):
- Host version.json ({versionCode, versionName, apkUrl, sha256, minSupportedVersionCode, changelog_tr}) and the APK on GitHub Releases/Pages or similar HTTPS hosting.
- Check on app start, at most once a day.
- Show a simple Turkish dialog ('Yeni sürüm var — Şimdi güncelle / Sonra').
- Download to cacheDir with OkHttp, verify the SHA-256, then install through a PackageInstaller session with USER_ACTION_NOT_REQUIRED where allowed.
- Handle PENDING_USER_ACTION.
- If canRequestPackageInstalls() is false, show a one-time guide screen with the exact Settings path, wrapping the ACTION_MANAGE_UNKNOWN_APP_SOURCES intent in a try/catch.
- Testing:
- Develop on a Google TV/Android TV AVD (x86_64, WHPX on Windows 11, since AEHD sunsets on Dec 31, 2026). Match the box's API level if an image exists (31 for Android TV 12, 34 for 14).
- Test D-pad-only navigation from a keyboard's arrow keys and Esc.
- Validate every release build on the real box over ADB Wi-Fi: cold start time, zapping latency, memory in Android Studio Profiler, and a 2-hour playback soak.
- Add Firebase Crashlytics or ACRA before giving the app to the family, or rely on logcat for an MVP.
- Plan for 2027 developer verification: before any global enforcement, create a free limited-distribution account in the Android Developer Console and register the package name (up to 20 devices). Keep ADB installs as the fallback path.
- Optional CI: a GitHub Actions workflow with the keystore stored as a base64 secret. It runs ./gradlew assembleRelease, computes the SHA-256, uploads the APK to a GitHub Release, and updates version.json, so updates become push-button.

## OPEN QUESTIONS
- Which Android TV OS version is the family's box actually running (11, 12 or 14)? Check Settings > System > About > Android TV OS. This decides the emulator image and whether adb pair (Wireless debugging) works.
- Does the stock remote have CH+/CH- or number keys? If not, the D-pad-only zapping scheme is mandatory and number entry can be dropped.
- Do the chosen Turkish channel streams need custom headers (Referer/Origin/User-Agent), expiring tokens or a geo-IP check? This decides whether static URLs in channels.json are enough or a token-refresh step is needed. That is a separate research topic.
- Where will channels.json, version.json and the APK be hosted (GitHub Pages/Releases, Firebase Hosting, a personal server)? Who updates channels.json when a stream URL breaks?
- Is EPG (now/next) wanted in the MVP? It adds a scheduled grabber job and Room storage.
- Crash reporting: is a Firebase/Google account acceptable, or should the app stay Google-free (ACRA or none)?
- Does Google's 2027 global developer verification cover Android TV/Google TV sideloads, and does the limited-distribution account work on TV devices? I could not confirm either from official pages.
- Which Google TV emulator system-image API levels are currently offered in Android Studio Rabbit? I could not verify this.
- Is OkHttp 5.4.0 the latest stable on Maven Central? I saw it only in secondary sources. Is the AGP 9.4 KGP runtime dependency still 2.2.10?

## VERIFY CORRECTIONS
- CLAIM: Kotlin 2.4.20 is the latest stable Kotlin (released Sept 7, 2026); recommend kotlin-gradle-plugin and the Compose compiler plugin at 2.4.20.
  PROBLEM: Out of date. Kotlin 2.4.21 shipped on Oct 8, 2026 as a bug-fix release, so it is now the latest stable. The 2.4.20 date (Sept 7, 2026) and its Gradle 9.7 support are correct.
  CORRECTED: Use Kotlin 2.4.21 for both kotlin-gradle-plugin and org.jetbrains.kotlin.plugin.compose (they must match). Release line: 2.4.0 on Jun 3, 2.4.10 on Jul 14, 2.4.20 on Sep 7, 2.4.21 on Oct 8, 2026. The 2.4.20 release supports Gradle up to 9.7.0, so pairing it with Gradle 9.6.0 or 9.7.1 is still the safe choice. Support for Gradle 9.8 is still unconfirmed.
  SRC: https://kotlinlang.org/docs/releases.html ; https://blog.jetbrains.com/kotlin/2026/09/kotlin-2-4-20-released/
- CLAIM: OkHttp 5.4.0 (Jun 8, 2026), seen only in Renovate PRs and not on Maven Central.
  PROBLEM: Wrong on both points. Maven Central lists 5.4.0, and a newer 5.5.0 is published there as the latest and release version (metadata lastUpdated 2026-08-16).
  CORRECTED: OkHttp 5.5.0 (com.squareup.okhttp3:okhttp) is the latest stable on Maven Central.
  SRC: https://repo1.maven.org/maven2/com/squareup/okhttp3/okhttp/maven-metadata.xml
- CLAIM: Firebase BoM 34.17.0 (for Crashlytics).
  PROBLEM: Out of date. The latest Firebase Android BoM is 35.0.2 (Oct 15, 2026). Because the major version changed, read the 35.0.0 breaking-change notes before adopting it.
  CORRECTED: Firebase BoM 35.0.2, if Crashlytics is chosen.
  SRC: https://firebase.google.com/support/release-notes/android
- CLAIM: Google Play's 2026 target-API rule for TV apps is API 34+ (from Aug 31, 2026).
  PROBLEM: Wrong date. The Android TV row (API 34+) has been in effect since Aug 31, 2025, and Google did not change it for 2026. The Aug 31, 2026 deadline applies to phones and tablets (API 36), which can get an extension to Nov 1, 2026.
  CORRECTED: Android TV apps on Play must target API 34+ (effective Aug 31, 2025, no 2026 change). Phones and tablets must target API 36 by Aug 31, 2026. None of this binds a sideloaded APK; targetSdk 36 is still fine.
  SRC: https://support.google.com/googleplay/android-developer/answer/11926878?hl=en
- CLAIM: The box's Android TV version is uncertain (11 to 14). Reddit users report an Android TV 14 OTA with a 2026-03-01 security patch. The emulator should match API 31 (Android TV 12) or API 34 (Android TV 14).
  PROBLEM: The Android TV 14 OTA claim is unconfirmed, and the plan leaves out the most likely real target, Android TV 11 (API 30).
- At launch: a hands-on review (smartzone.de) and androidpctv both say Google TV on Android TV 11, and androidpctv says 32-bit.
- Latest OTA I could find: June 2025, build RTT0.211222.001.774 with the April 2025 security patch. The 'R' prefix means an Android 11 base.
- A 2025 Reddit thread says the box is still on Android TV 11.
- I found no Xiaomi or press source for an Android TV 12 or 14 rollout. androidtv-guide's '11 → 14' has no dates or evidence.
  CORRECTED: Plan around Android TV 11 (API 30, 32-bit userspace) as the baseline, and treat 12 or 14 only as a possibility to check on the family's box (Settings > System > About). Use an API 30 TV AVD as the primary emulator image, if Android Studio still offers one (unverified). Features gated on API 31+ will not run on this box, such as the PackageInstaller USER_ACTION_NOT_REQUIRED silent update (see the self-update entry). Change the device-spec confidence: SoC Amlogic S905X4 is confirmed by several sources; the OS version stays low confidence.
  SRC: https://www.smartzone.de/testbericht-xiaomi-tv-box-s-2-gen/ ; https://androidpctv.com/?p=36896 ; https://tech-ish.com/2025/06/18/xiaomi-mi-box-s-2nd-gen-april-2025-security-update/ ; https://www.androidtv-guide.com/?p=10762
- CLAIM: Confirmed stock remote buttons are D-pad/OK, Back, Home, Google Assistant mic, volume, and Netflix/Prime Video shortcuts.
  PROBLEM: Incomplete. Several sources describe more buttons:
- a YouTube shortcut (Notebookcheck, Xataka, log.com.tr; smartzone counts '4 quick-access keys')
- a Xiaomi TV+ key (androidpctv, log.com.tr)
- an Apps key (androidpctv, log.com.tr)
- a power key, with built-in IR so power and volume can control the TV or soundbar (Xiaomi product page)
Xiaomi says remote appearance and functions vary by market. No source shows number keys or CH+/CH-, so the D-pad-only zapping conclusion holds.
  CORRECTED: Stock remote (BT + IR): Power, Google Assistant mic, D-pad + OK, Back, Home, Apps, Xiaomi TV+, Netflix, Prime Video, YouTube, Vol +/- (market-dependent). There are no digit or channel keys. The shortcut, Home and Apps keys are system-handled (unconfirmed whether a foreground app can see their keycodes). Zapping must be DPAD_UP/DOWN. Add a hidden key-logger debug screen and record the real remote's keycodes during the first device test.
  SRC: https://www.xatakahome.com/centro-multimedia/xiaomi-tv-box-s-2nd-gen-caracteristicas-precio-ficha-tecnica/amp ; https://androidpctv.com/?p=36896 ; https://www.log.com.tr/xiaomi-tv-box-s-2-nesil-incelemesi/2/ ; https://www.smartzone.de/testbericht-xiaomi-tv-box-s-2-gen/
- CLAIM: ADB: 'Wireless debugging' pairing needs Android 13+ on TV. Android TV 11/12 boxes use Developer options > USB debugging plus adb connect <ip>:5555. The legacy alternative is adb tcpip 5555 over USB.
  PROBLEM: No official source supports either point.
- 'Android 13+ for pairing': I found no official Google source.
- 'adb connect' on Android TV 11/12: Google's Android-TV-with-Cast debugging doc says standard retail Android TV devices do not listen for ADB automatically and need USB plus 'adb tcpip 5555' first. Whether this box opens port 5555 when USB debugging is enabled is unconfirmed.
- The USB fallback is very likely impossible: the box has only a USB-A 2.0 host port and no Ethernet (smartzone, androidpctv). The fallback is my inference from the hardware.
  CORRECTED: Treat Wi-Fi ADB on this box as must-test-on-device.
- First try enabling Developer options > USB/Network debugging, then 'adb connect <ip>:5555'.
- If Developer options shows 'Wireless debugging', use adb pair.
- Do not rely on a USB-cable fallback.
- Make the primary family install path non-ADB: Downloader (AFTVnews) or Send Files to TV, plus a per-app 'Install unknown apps' grant.
  SRC: https://developers.google.com/cast/docs/android_tv_receiver/debugging ; https://www.smartzone.de/testbericht-xiaomi-tv-box-s-2-gen/ ; https://androidpctv.com/?p=36896
- CLAIM: Self-update: for a silent update (API 31+), use setRequireUserAction(USER_ACTION_NOT_REQUIRED) with UPDATE_PACKAGES_WITHOUT_USER_ACTION.
  PROBLEM: Accurate as an API statement but misleading for this device. setRequireUserAction exists only on API 31+, and the box most likely runs Android 11 (API 30). AOSP commits show the conditions:
- the installer is the installer of record, or the update owner when that is enforced, or the app is updating itself
- the installer holds the permission
- the new APK targets API 29+, a threshold that rises on newer platform versions
  CORRECTED: On this box, assume every self-update shows the system install-confirmation dialog. Design the update UX around one extra 'Yükle' (Install) confirmation the family must press. Call the silent-update path only behind Build.VERSION.SDK_INT >= 31.
  SRC: https://android.googlesource.com/platform/frameworks/base/+/c17bbf82ce4b%5E%21/ ; https://developer.android.com/reference/android/content/pm/PackageInstaller
- CLAIM: Recommendation: media3 'ui-compose (custom overlay) or ui-compose-material3', together with tv-material and 'no material3 theme mixing'.
  PROBLEM: Internally inconsistent. media3-ui-compose-material3, which provides the Player composable, ErrorText and PlayerDefaults, is built on androidx.compose.material3. The Compose for TV guide warns that mixing Compose Material with tv-material can cause unexpected behaviour, because each has its own MaterialTheme. Media3's TV page recommends the material3 Player for automatic D-pad support, so the two official recommendations pull in different directions.
  CORRECTED: Default to media3-ui-compose (PlayerSurface plus state holders such as ErrorState and CurrentMediaItemState) with your own tv-material overlay. Use media3-ui-compose-material3's Player only if you accept a second theme and test focus and colours on the device.
  SRC: https://developer.android.com/training/tv/playback/compose ; https://developer.android.com/media/media3/ui/androidtv ; https://developer.android.com/jetpack/androidx/releases/media3
- CLAIM: Since compose-ui 1.9.0-alpha01, an unattached FocusRequester logs a warning instead of crashing.
  PROBLEM: Not supported. The compose-ui release notes I read have no such entry. The only related 1.9.0-alpha01 item is a lint check that a FocusRequester is remembered. Forum threads still show the 'FocusRequester is not initialized' IllegalStateException.
  CORRECTED: Treat this as unconfirmed. Request focus only after composition (LaunchedEffect or onGloballyPositioned), keep the requester attached, and wrap the call in try/catch or guard it until this is verified.
  SRC: https://developer.android.com/jetpack/androidx/releases/compose-ui
- CLAIM: The official Compose-for-TV setup snippet uses compose-bom 2026.09.00 with activity-compose 1.13.0 (implying the snippet matches tv-material 1.1.0).
  PROBLEM: Minor. The same official snippet still pins androidx.tv:tv-material:1.0.0, even though 1.1.0 has been stable since May 6, 2026. Its compatibility text ('Android 5.0 / API 21, Compose 1.3.0, Kotlin 1.7.10') is also stale. Copying the snippet as-is would pull the old version.
  CORRECTED: Pin tv-material 1.1.0 and tv-foundation 1.0.0 explicitly. Don't copy the docs snippet versions.
  SRC: https://developer.android.com/training/tv/playback/compose ; https://developer.android.com/jetpack/androidx/releases/tv
- CLAIM: Manifest: declare wifi required=false for Ethernet-only boxes. iptv-org/epg community Turkish feed is an option.
  PROBLEM: The wifi=false line is irrelevant for this device: the Xiaomi TV Box S 2nd Gen is Wi-Fi only, with no RJ45 port (smartzone, androidpctv; one Turkish review wrongly lists Ethernet). On EPG, the iptv-org GUIDES.md shared-host list has no Turkish guide, and all listed hosts show 0 channels and an offline status. Self-hosting the grabber is therefore the only real option.
  CORRECTED: The box is Wi-Fi 5 only, so streaming reliability depends on Wi-Fi: recommend 5 GHz, and add connectivity monitoring and auto-retry. EPG requires running the iptv-org grabber yourself; no ready-made Turkish guide exists.
  SRC: https://www.smartzone.de/testbericht-xiaomi-tv-box-s-2-gen/ ; https://github.com/iptv-org/epg/blob/master/GUIDES.md

## VERIFY MISSING
- Stream sourcing and legality, the biggest gap. The report verifies no channel, stream URL, format, token or geo restriction, and makes no legal statement about rebroadcasting.
- A complete plan needs a vetted list of Turkish national channels (with 2024-2026 renames and closures) and which ones offer official free live HLS.
- It needs to know which streams are tokenized, geo-locked to Turkey or Widevine-DRM protected; ExoPlayer can play DRM streams only with a licence server, which official sites won't give a third-party app.
- It needs a legal check under RTÜK Law 6112 and FSEK Law 5846, which covers retransmission rights. Keep the app private, for family use only, and do not publish an APK that bundles third-party stream URLs.
- Practical fallback: for channels whose streams can't be played directly, deep-link to the official app (e.g. tabii/TRT, or a broadcaster's own app) with a package intent instead of playing the stream.
- 32-bit userspace. androidpctv reports 'Google TV 11 (32-bit)' on a 64-bit-capable S905X4 (single source, medium confidence). Any native .so (FFmpeg extension, some crash SDKs) must ship armeabi-v7a, so never set abiFilters to arm64-v8a only. The emulator ABI also differs from the device's.
- Real resource headroom: about 1 GB free RAM and about 3.5 GB free storage on a fresh box (androidpctv). Keep the APK small, cap Coil disk/memory caches and the ExoPlayer buffer, and expect the process to be killed in the background. Save the last channel or screen so the app restores quickly.
- Remote convenience for elderly users:
- Teach the family to pin the app to the Google TV 'Your apps' row (manual step for the install guide).
- Optionally remap a shortcut key such as Xiaomi TV+ or Apps to launch our app with an accessibility-based button remapper. Whether these keys can be remapped on this box is unverified.
- Auto-start on boot. If the family wants the app to open at power-on, note that Android 10+ blocks starting an activity from the background (a BOOT_COMPLETED receiver cannot normally launch one). This is likely not possible without special permissions, so plan for the Home-row approach. Not verified for this device.
- Android 11 specifics for the install-permission flow. From memory, unverified: granting 'Install unknown apps' to an app restarts its process on Android 11. The permission-helper screen should survive a restart and re-check canRequestPackageInstalls() in onResume.
- TV layout safe areas (about 5% overscan margins) and 10-foot typography are not in the tech plan. They need tokens in the design system before the Figma screens are drawn.
- Turkish locale handling: use Locale("tr") for case-insensitive channel filtering and search (İ/ı/I/i), put all strings in Turkish resources, and use a Turkish-text 320x180 banner. With the default locale, a filter like 'İ' vs 'i' breaks.
- Frame-rate and format testing: Turkish broadcast HLS is typically 25/50 fps, and possibly interlaced or with AC-3 audio. On a 60 Hz output this can judder. Consider ExoPlayer's video frame-rate-change strategy (Surface.setFrameRate exists from API 30) and test hardware decoding and deinterlacing on the device. Behaviour on this device is unverified.
- Optional MediaSession, so HDMI-CEC play/pause from the TV's own remote and Assistant voice 'pause' work. The report mentions the MediaSession key rule but does not plan for the session itself.
- Developer verification details the report left out:
- The 'advanced flow' for power users, for installing apps from unverified developers after a one-time setup, launched globally in Aug 2026.
- ADB stays available for your own devices.
- Google's pages disagree on the September 2026 countries: the main page lists Brazil, Indonesia, Singapore and Thailand; the FAQ text lists Indonesia, Singapore and Thailand.
- The limited-distribution account needs 2-Step Verification and a linked payments profile. No page says whether it works for Android TV.
- channels.json operations: who updates broken URLs, a stream-health check (e.g. a CI job that HEAD-checks every stream URL daily), and a per-channel 'resolver' field for streams that need a token fetch. Without this, the app silently degrades as URLs rotate.
- Accessibility: system font-scale support, TalkBack contentDescription on channel cards, and very high-contrast focus states. These matter for elderly users and are not covered on the tech side.
- Verification limits: the web-search budget ran out before I could check:
- whether Android TV 11 kills the app when the 'install unknown apps' grant changes
- whether Compose's Key.ChannelUp and related constants exist; the reference page didn't render. The android.view.KeyEvent values in the report match the standard API.
- which API levels the TV emulator images offer
- whether this box opens port 5555 for network ADB
All of these remain open.

## VERIFIED KEY FACTS
- Android Studio Rabbit 1 (2026.2.1) shipped stable on Oct 1, 2026, and supports AGP 7.1-9.4. Rabbit 2 Canary 5 came out on Oct 15, 2026.
- AGP 9.4.0 is the latest stable (September 2026): minimum and default Gradle 9.6.0, JDK 17, SDK Build Tools 36.0.0, maximum API 37. AGP 9.5 is in preview. android.newDsl.optOut exists ahead of AGP 10.
- AGP minimum Gradle versions: 9.4 needs 9.6.0, 9.3 needs 9.5.0, 9.2 needs 9.4.1, 9.1 needs 9.3.1, 9.0 needs 9.1.0.
- AGP 9.0 default changes are as the report says:
- builtInKotlin=true, with a runtime dependency on KGP 2.2.10
- kotlin-kapt incompatible; use KSP or com.android.legacy-kapt
- targetSdk defaults to compileSdk when unset
- R8 strictFullModeForKeepRules=true and optimizedResourceShrinking=true
- proguard-android.txt disallowed (use proguard-android-optimize.txt)
- useAndroidx=true, uniquePackageNames=true, resValues default false
- kotlinOptions replaced by kotlin { compilerOptions {} }
- Gradle releases: 9.8.1 (Oct 7, 2026), 9.8.0 (Sep 24), 9.7.1 (Aug 19), 9.7.0 (Aug 6), 9.6.1 (Jun 26). Kotlin 2.4.20 officially supports Gradle up to 9.7.0.
- Compose BOM 2026.09.00 maps to ui, foundation and runtime 1.12.1 and material3 1.4.0. Compose UI 1.12.0 shipped Aug 12, 2026 and 1.12.1 on Sep 9, 2026. 1.12.0-alpha01 raised compileSdk to API 37, which requires AGP 9.2.0 or later. 1.13.0-alpha03 moved to compileSdk 37.1. 1.13.0-beta01 (Oct 7, 2026) requires kotlinc 2.2. The focusRestorer fix landed in 1.12.0-alpha03. Modifier.keepScreenOn arrived in 1.9.0-alpha01. Compose's default minSdk became 23 in 1.10.0-alpha01.
- tv-material 1.1.0 and tv-foundation 1.0.0 are both stable (May 6, 2026). TvLazy* layouts were deprecated in alpha11 (Jul 2024) and removed in alpha12 (Jan 2025). FocusProperties.enter/exit became onEnter/onExit in tv-material 1.1.0-alpha01.
- The Compose for TV guide calls Compose the recommended way to build for Android TV. It maps tv-material 'instead of' material3 and warns against mixing them because each has its own MaterialTheme. The docs include a page titled 'Migrate from the deprecated Leanback UI toolkit'. Leanback 1.2.0 (Apr 2025) is still the last stable.
- Media3 1.11.1 (Sep 10, 2026) is the latest stable. minSdk has been 23 since 1.9.0 (Dec 17, 2025).
- 1.10.0 (Mar 26, 2026): Player composable in media3-ui-compose-material3, and HLS location fallback.
- 1.10.1 (May 12, 2026): tunneling IllegalStateException race fix.
- 1.11.0 (Aug 5, 2026): HLS Content Steering, PlayerDefaults Top/Center/Bottom controls and ErrorOverlay, FocusRequester in Player, ErrorState, CurrentMediaItemState, PlayerPool/rememberPooledPlayer, dynamic scheduling on by default.
- Media3's Android TV page: the material3 Player and PlayerDefaults support D-pad by default through an automatic FocusRequester. A PlayerView inside AndroidView needs Modifier.focusable().onKeyEvent { playerView.dispatchKeyEvent(it.nativeKeyEvent) }.
- Media3 live streaming:
- On ERROR_CODE_BEHIND_LIVE_WINDOW, call seekToDefaultPosition() then prepare().
- Live configuration priority: per-MediaItem setLiveConfiguration, then the DefaultMediaSourceFactory.setLiveTargetOffsetMs global, then the media itself.
- Setting min and max speed to 1.0f disables speed adjustment.
- isCurrentWindowLive stays true after the stream ends.
- AndroidX stable versions:
- activity 1.13.0, lifecycle 2.11.0
- navigation 2.10.2, navigation3 1.2.0 (Sep 23, 2026)
- datastore 1.2.1, room 2.8.5, work 2.12.0, core 1.19.1
- androidx.hilt 1.4.0, benchmark 1.5.0, profileinstaller 1.4.1
- Third-party versions:
- Coil 3.6.3 (Sep 18, 2026)
- Dagger/Hilt 2.60.1 (Jul 6): Hilt Gradle plugin needs AGP 9 since 2.59; Hilt minSdk 23
- KSP 2.3.12 (Sep 9)
- kotlinx.serialization 1.11.0 stable, 1.12.0-RC on Sep 4
- ACRA 5.14.2 (Maven Central, updated Oct 1, 2026)
- Flutter dpad: 3.0.0 rewrite (Jun 13, 2026); latest is 3.1.1 (Oct 7, 2026)
- TV manifest rules are confirmed from the docs:
- Without LEANBACK_LAUNCHER, an app installed with dev tools does not appear in the TV UI.
- leanback required=false.
- touchscreen required=false is mandatory.
- The banner is xhdpi 320x180 px with text, localised per language.
- Custom SplashScreen animations are unsupported on TV from Android 12.
- Cleartext is disabled by default from API 28. A domain-config with cleartextTrafficPermitted="true" allows specific hosts, and base-config cleartext should be avoided.
- TV key semantics are confirmed:
- Back: KEYCODE_BACK, BUTTON_B. Select: DPAD_CENTER, ENTER, NUMPAD_ENTER, BUTTON_A, BUTTON_SELECT.
- Back must never toggle, and repeated Back must reach the Home screen.
- Don't override MEDIA_PLAY/PAUSE when using a MediaSession.
- The official page does not cover channel or number keys.
KeyEvent values match the standard API: DPAD_UP 19, DPAD_DOWN 20, DPAD_CENTER 23, BACK 4, CHANNEL_UP 166, CHANNEL_DOWN 167, MEDIA_PLAY_PAUSE 85, GUIDE 172, INFO 165, 0-9 = 7-16, LAST_CHANNEL 229.
- TV memory guidance:
- 1 GB devices: at most 280 MB total, under 200 MB recommended (anon+swap plus graphics).
- Devices with 2 GB or more: 2160p video and 1080p UI.
- Media buffer for 2 GB / 2160p: 100-120 MB.
- Background jobs under 30 MB, with setRequiresDeviceIdle(true).
- Always check isLowRamDevice.
- Use baseline profiles.
- Device hardware is confirmed across several sources: Amlogic S905X4 (quad Cortex-A55, 12 nm), Mali-G31 MP2, 2 GB RAM, 8 GB eMMC, Google TV, HDMI 2.1 (2.0 bandwidth), USB-A 2.0, no Ethernet, Wi-Fi 5, BT 5.2, AV1/VP9/HEVC decode, Widevine L1, Dolby Vision/HDR10+.
- Developer verification:
- APIs, limited-distribution accounts and the power-user advanced flow launched in Aug 2026.
- Enforcement starts Sep 30, 2026, in Brazil, Indonesia, Singapore and Thailand, on certified Android 7+ devices.
- The FAQ says September 2026 covers 'mobile and tablet form factors'.
- Global rollout follows in 2027.
- ADB stays available.
- Limited distribution: free, no ID, 2SV plus payments profile, up to 20 devices via QR/link consent.
- Emulator 37.2.12 (Oct 1, 2026). AEHD will sunset on Dec 31, 2026, so Windows users should move to WHPX. The TV-emulator hover crash was fixed in 36.4.10 (Mar 9, 2026).
- iptv-org/epg generates guide.xml locally (npm run grab, or Docker ghcr.io/iptv-org/epg:master, cron at 00:00 UTC by default). Turkish sites listed as working (green): digiturk.com.tr 112, dsmart.com.tr 100, tvplus.com.tr 150, turksatkablo.com.tr 177. There is no TRT source.
- Google TV sideloading flow: enable developer mode (Settings > System > About > tap 'Android TV OS build'), then give Downloader the per-app 'Install unknown apps' permission. AFTVnews says a 'Unknown Sources is disabled' message means developer mode was not enabled correctly.