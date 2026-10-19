- Açılış (Splash) [screen/mvp]: Brief brand screen while the bundled or remote channel list and the last-watched state load. Never part of the back stack.
  ELEMENTS: Android 12+ SplashScreen API with the app logo on a dark background; No text unless loading takes more than 1 s ('Kanallar hazırlanıyor…'); Routes to the first-run guide, Home, or Home + auto-started Player
  REMOTE: No focusable elements; keys ignored; Back exits the app
- Kumanda Rehberi (first-run remote guide) [overlay/mvp]: One-time, one-page explanation of the only keys parents need; can be reopened from Ayarlar.
  ELEMENTS: Illustration of the Xiaomi remote with highlighted keys; 4 big rows: ▲▼ Kanal değiştir · OK Kanal listesi · ◀▶ Menü/Bilgi · Geri Geri dön/Ana ekran; Single primary button 'Anladım, başla' (initially focused)
  REMOTE: OK on the button closes the guide and goes to Home (or starts the last channel); Back does the same as 'Anladım'
- Ana Ekran – Kanallar (Home / channel browser) [screen/mvp]: Start destination. Resume the last channel in one press and browse or filter all national channels.
  ELEMENTS: Header: app name, clock, 'Favorileri düzenle' and 'Ayarlar' buttons (top-right); 'Kaldığın yerden' resume card: last channel logo + name + 'İzlemeye devam et' (initial focus); Single-select FilterChip row: ★ Favoriler, Tümü, Genel, Haber, Spor, Çocuk, Belgesel, Müzik; LazyVerticalGrid of channel cards, 5 columns (4 in Büyük, 3 in Çok büyük), 16:9 logo surface with number + name always visible below; Badges: ★ favorite, '▶ Son izlenen' on the last channel; Focus: scale 1.05 + 3 dp white/amber border; focusRestorer on chips and grid; Inline empty state for empty favorites or filters
  REMOTE: D-pad moves focus; Up from the top grid row goes to chips, Up from chips to the header; Focusing a chip applies the filter (debounced); Down enters the grid; OK on a card opens the Player on that channel; OK on the resume card plays the last channel; Long-press OK on a card is a shortcut to Kanal seçenekleri (also reachable via 'Favorileri düzenle'); Back in the grid scrolls to top and focuses the selected chip; Back on chips or header exits the app (no confirmation); Returning from the Player restores focus to the card of the channel that was playing
- Favorileri Düzenle modu [overlay/mvp]: Visible, non-hidden way to add or remove favorites directly on the Home grid.
  ELEMENTS: Banner at top: 'Favori düzenleme: OK ile ★ ekle/çıkar · Bitince Geri'; Each card shows an empty or filled ★ toggle state; 'Bitti' button in the header
  REMOTE: D-pad moves between cards; OK toggles favorite on the focused card with immediate visual feedback; Back or 'Bitti' leaves edit mode
- Canlı Yayın Oynatıcı (fullscreen player) [screen/mvp]: Fullscreen live playback with frictionless zapping.
  ELEMENTS: Media3 ExoPlayer (HLS) surface, 16:9, black bars only for video; Keep-screen-on during playback (blocks Ambient Mode); No persistent chrome; overlays appear on key press; Single reused player instance for fast zapping
  REMOTE: ▲ / ▼ (and CH+/CH− if present): previous/next channel in the active filter, wrapping around; shows the info banner; OK or ◀: open the channel list panel; ▶ (or INFO key): show the info banner with now/next; Back: closes any overlay, otherwise returns to Home with focus on the current channel; Play/Pause media key: pause/resume; Digit keys (if delivered via HDMI-CEC): direct channel number entry with a 2 s commit; Home key: system; app stops or pauses playback in onStop
- Kanal Bilgi Bandı (info banner) [overlay/mvp]: Confirms which channel is on and what is airing; teaches the keys.
  ELEMENTS: Bottom gradient scrim within the safe area; Channel logo, channel number, channel name (28–32 sp); Now/next program titles with times and a progress bar (only if EPG data exists; otherwise the category label); Clock, ★ if favorite, optional sleep-timer chip; Key hint row: '▲▼ Kanal değiştir · OK Kanal listesi · Geri Ana ekran'; Auto-hides after 5 s (configurable 3/5/10 s)
  REMOTE: Appears on every zap and on ▶/INFO; ▲▼ keeps zapping while it is visible; OK opens the channel list panel; Back hides the banner
- Kanal Listesi Paneli (in-player channel list) [overlay/mvp]: Pick another channel or category without leaving playback.
  ELEMENTS: Left side panel (~1/3 width) over video with scrim; video keeps playing; Category chips or pill tabs at top (same filters as Home, current one selected); LazyColumn of tv ListItems: number, logo, name, one-line now program; ▶ icon for the current channel, ★ icon for favorites, trailing '›' options hint; Initial focus on the currently playing channel; Footer hint: 'OK İzle · ▶ Seçenekler · Geri Kapat'
  REMOTE: ▲▼ moves focus in the list (no zapping until OK); OK tunes to the focused channel and closes the panel; ▶ on a row opens Kanal seçenekleri; ▲ at the top of the list, or ◀, moves to category chips; ◀/▶ on chips changes category; Back closes the panel; Auto-closes only after a long idle (~30 s), not quickly
- Kanal Seçenekleri (channel options) [dialog/mvp]: Per-channel actions with big, explicit buttons.
  ELEMENTS: Title: channel logo + name; Buttons: '★ Favorilere ekle' / 'Favorilerden çıkar', 'Yayını yeniden yükle', 'Kapat'; Compose Dialog with a tv-material Surface and Buttons (no TV AlertDialog confirmed)
  REMOTE: ▲▼ moves between buttons; the first is initially focused; OK runs the action and closes the dialog with visual confirmation (e.g., a toast 'Favorilere eklendi'); Back closes without changes
- Yükleniyor / Arabellek (loading state) [overlay/mvp]: Shows the channel is opening so the user doesn't think the box froze.
  ELEMENTS: Channel logo + name + circular progress; Text 'Yayın açılıyor…'; Silent automatic retry or backup URL; after ~10 s switch to the error overlay
  REMOTE: ▲▼ still zaps to another channel; OK opens the channel list; Back returns to Home
- Yayın Açılamadı (stream error) [overlay/mvp]: Plain-language recovery when a channel can't be played.
  ELEMENTS: Icon + message 'TRT 1 şu anda açılamıyor.'; Sub-text: 'Biraz sonra tekrar deneyin veya başka bir kanal seçin.'; Horizontal buttons: 'Tekrar dene' (initial focus), 'Sonraki kanal', 'Ana ekran'
  REMOTE: ◀▶ moves between buttons, OK activates; ▲▼ still zaps prev/next channel; Back returns to Home
- İnternet Bağlantısı Yok (no internet) [screen/mvp]: Clear guidance when the box is offline; recovers automatically.
  ELEMENTS: Large icon + 'İnternet bağlantısı yok'; Simple checklist: 'Modem açık mı? Kablo/Wi-Fi bağlı mı?'; Buttons: 'Tekrar dene' (focused), 'Ağ ayarlarını aç' (system Wi-Fi settings intent, if available); Auto-dismiss when ConnectivityManager reports the network is back
  REMOTE: ◀▶ or ▲▼ moves between buttons, OK activates; Back exits to the Google TV home
- Boş Durum (empty favorites / empty filter) [overlay/mvp]: Inline Home state that explains what to do instead of showing an empty grid.
  ELEMENTS: Message 'Henüz favori kanalınız yok.'; How-to: 'Favorileri düzenle'ye girip bir kanalda OK'a basın.'; Buttons: 'Tüm kanallara git' (focused), 'Favorileri düzenle'
  REMOTE: OK on 'Tüm kanallara git' selects the Tümü chip; Up moves to the chip row; Back behaves as on Home
- Ayarlar (settings) [screen/mvp]: A few family-relevant preferences in one flat list.
  ELEMENTS: Vertical list of tv ListItems with trailing value or Switch; Yazı boyutu (Normal / Büyük / Çok büyük) with live preview; Açılışta son kanalı başlat (Switch, default on); Kanal değiştirme yönü (▲ = sonraki / ▲ = önceki); Bilgi bandı süresi (3 / 5 / 10 sn); Kanal listesini güncelle; Kumanda rehberini göster; Hakkında (app version, channel-list date); Later: Uyku zamanlayıcı, Ebeveyn kilidi, Favori sıralama, Güncellemeleri denetle
  REMOTE: ▲▼ moves between rows; OK toggles a Switch or opens Seçim diyaloğu; Back returns to Home with focus on the 'Ayarlar' button
- Seçim Diyaloğu (generic option picker) [dialog/mvp]: Choose one value for a setting (text size, banner duration, zapping direction).
  ELEMENTS: Title + RadioButton list items; the current value has a check/radio icon (not color only); For text size: live sample text that rescales on focus
  REMOTE: ▲▼ moves between options; OK selects and closes; Back cancels without change
- Uyku Zamanlayıcı (sleep timer) [dialog/later]: Stop playback automatically after a chosen time; useful for parents who fall asleep in front of the TV.
  ELEMENTS: Options: Kapalı / 30 dk / 60 dk / 90 dk / 120 dk; Remaining-time chip in the info banner when active
  REMOTE: Opened from Ayarlar or Kanal seçenekleri; ▲▼ selects, OK confirms, Back cancels
- Hâlâ İzliyor musunuz? (still-watching check) [dialog/later]: Shown at sleep-timer end or after several hours without key presses; prevents all-night streaming.
  ELEMENTS: Message + 60 s countdown; Buttons: 'Evet, izlemeye devam' (focused), 'Kapat'; On timeout: stop playback, return to Home, allow Ambient Mode
  REMOTE: OK (any key) continues watching; Selecting 'Kapat' stops playback; Back equals continue
- Ebeveyn Kilidi – PIN (parental lock) [dialog/later]: Optional 4-digit PIN to protect settings or specific channels.
  ELEMENTS: 4 PIN dots; On-screen 3x4 numeric keypad (the remote likely has no number keys); 'Sil' and 'Vazgeç' keys; error text 'PIN yanlış, tekrar deneyin'
  REMOTE: D-pad moves on the keypad, OK enters a digit; Digit keys accepted if delivered; Back deletes the last digit, then cancels
- Kanal Arama (channel search) [screen/later]: Find a channel by name; low priority because chips plus a 50–80 channel list cover browsing.
  ELEMENTS: Search field + on-screen keyboard with Turkish letters (Ç Ğ I İ Ö Ş Ü); Results grid updates as the user types; Optional voice input via RecognizerIntent if the mic key is usable
  REMOTE: D-pad on the keyboard, OK types; Down moves to results, OK plays; Back clears the query, then leaves search
- Program Rehberi (EPG grid) [screen/later]: Full time-based guide; only if a reliable Turkish EPG source exists.
  ELEMENTS: Channels vertical, time horizontal, 'now' line; Program cells with title and time; the current program highlighted; Category chips at top
  REMOTE: ▲▼ changes channel row, ◀▶ moves through time; OK on a current program plays the channel; Back returns to Player or Home
- Favori Sıralama (reorder favorites) [screen/later]: Let the family set the order of favorites, which also sets the ▲▼ zapping order in the Favoriler filter.
  ELEMENTS: Vertical list of favorite channels with a drag handle icon; Instruction: 'OK ile seç, ▲▼ ile taşı, OK ile bırak'
  REMOTE: OK picks up or drops an item; ▲▼ moves the picked item; Back saves and exits
- Güncelleme Mevcut (self-update for sideloaded APK) [dialog/later]: Tell the user a new APK or channel list is available, since there is no Play Store.
  ELEMENTS: 'Yeni sürüm hazır' + short change note; Buttons 'Şimdi güncelle' (focused) / 'Sonra'; Uses REQUEST_INSTALL_PACKAGES + the system installer confirmation
  REMOTE: ◀▶ moves between buttons, OK activates; Back = Sonra
- Çıkış İpucu (optional exit hint) [overlay/later]: Optional, default-OFF guard against accidental exits. Google advises against exit confirmation dialogs, so use a non-blocking toast instead of a modal.
  ELEMENTS: Toast at bottom: 'Çıkmak için Geri'ye tekrar basın' for 2 s; Setting toggle in Ayarlar
  REMOTE: A second Back within 2 s exits; any other key cancels