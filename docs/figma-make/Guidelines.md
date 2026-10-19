# Aile TV — Tasarım Kuralları

Bu dosya her promptta geçerlidir. Kurallar kesindir: "mümkünse" değil, "her zaman".

## 1. Ne yapıyoruz

- Google TV (Android TV) için bir canlı TV uygulamasının **etkileşimli web prototipi**. Hedef cihaz Xiaomi TV Box S (2. Nesil), arayüz 1080p.
- Kullanıcılar teknolojiye uzak, yaşlı ebeveynler de dahil bir aile. Her şey sade, büyük ve tahmin edilebilir olmalı.
- Prototip daha sonra Kotlin + Jetpack Compose for TV (Material 3 for TV) ile birebir yeniden yazılacak. Bu yüzden web'e özgü kalıplar kullanılmaz (bkz. §9).

## 2. Ölçü birimi — önce bunu oku

- Tüm arayüz **960×540 px sabit bir sahnede** (`#tv-stage`) çizilir. Bu sahnede 1 px, Android'deki 1 dp'ye eşittir.
- Sahne tarayıcı penceresine `transform: scale()` ile orantılı olarak sığdırılır ve ortalanır. Sahnenin dışı `#000` olur.
- Bu dosyadaki tüm sayılar 960×540 sahnesindeki px değerleridir. Tailwind kullanıyorsan keyfi değer yaz (`w-[844px]`, `text-[22px]`); Tailwind'in varsayılan ölçeğine yuvarlama.
- Güvenli alan: soldan ve sağdan 58 px, üstten ve alttan 28 px. Metin ve odaklanabilir öğeler bu alanın dışına çıkmaz. Yalnızca video, arka plan ve kaydırılınca yarısı görünen içerik taşabilir.
- İçerik genişliği 844 px'tir: 12 sütun × 52 px, sütunlar arası 20 px.
- Sayfa kaydırması yoktur; sahne `overflow: hidden` olur. Uzun liste ve ızgaralar odağı takip ederek kendi içlerinde kayar. Kaydırma çubuğu hiç görünmez.

## 3. Renkler (yalnızca koyu tema)

| Token | Değer | Kullanım |
|-------|-------|----------|
| `--bg` | `#0F1013` | Uygulama zemini |
| `--surface-1` | `#1C1D21` | Devam kartı, arama kutusu, program hücresi |
| `--surface-tile` | `#24262B` | Kanal logosu alanı |
| `--surface-2` | `#2A2C31` | Buton, çip ve rozetin normal hâli |
| `--surface-panel` | `#17181C` | Yan panel, önizleme kutusu |
| `--surface-dialog` | `#202226` | Diyalog, hata kartı |
| `--outline` | `#3A3C42` | İnce çizgi, kapalı switch |
| `--key-outline` | `#6C6F77` | Tuş rozeti kenarı |
| `--text` | `#EDEEF0` | Ana yazı |
| `--text-2` | `#B8BBC2` | İkincil yazı |
| `--text-disabled` | `#7D8088` | Pasif yazı |
| `--focus-bg` | `#F1F3F4` | Odaklı buton, çip ve satır zemini |
| `--focus-text` | `#15161A` | Odaklı öğenin yazı ve ikonu |
| `--selected-bg` | `#33405A` | Seçili çip veya seçenek |
| `--selected-text` | `#D3E3FD` | Seçili öğenin yazısı |
| `--accent` | `#8AB4F8` | Vurgu, yükleme halkası, açık switch, "şimdi" çizgisi |
| `--star` | `#FDD663` | Favori yıldızı |
| `--danger` | `#F28B82` | Hata ikonu, "CANLI" noktası |
| `--scrim` | `rgba(8,9,11,0.72)` | Diyalog arkası |
| `--banner-bg` | `rgba(20,21,25,0.94)` | Bilgi bandı |
| `--video-bg` | `#16181C` | Video yer tutucu |
| `--video-text` | `#4A4D55` | Video yer tutucu yazısı |

Gradyan, bulanıklık (`backdrop-filter`) ve geniş parlak renk alanları kullanılmaz.

## 4. Yazı

- Yazı tipi: **Roboto**, Google Fonts'tan yüklenir (400, 500, 700).

| Rol | Boyut / satır yüksekliği | Kalınlık | Kullanım |
|-----|--------------------------|----------|----------|
| display | 30 / 36 | 700 | Bilgi bandındaki kanal adı |
| headline | 28 / 34 | 500 | Ekran başlığı |
| title-l | 22 / 28 | 500 | Uygulama adı, diyalog başlığı, devam kartındaki kanal adı |
| title-m | 18 / 24 | 500 | Saat, bilgi bandındaki numara |
| label-l | 16 / 22 | 500 | Buton, çip, kart etiketi |
| body | 16 / 22 | 400 | Açıklama |
| body-m | 15 / 20 | 400 | İkincil açıklama, ayar değeri |
| label | 14 / 18 | 500 | Tuş ipucu, rozet |

- **14 px'in altında yazı yok.**
- Yazı boyutu çarpanı `--ts` (1 · 1.25 · 1.5) tüm yazı boyutlarına uygulanır, örneğin `calc(16px * var(--ts))`. Yazıya bağlı yükseklikler de (buton, çip, satır) aynı çarpanla büyür.
- `text-transform: uppercase` kullanılmaz, çünkü Türkçe İ/ı harflerini bozar. Gerekirse `toLocaleUpperCase('tr-TR')` kullanılır.
- Saat 24 saat biçimindedir (20:45), tarih gün.ay.yıl biçimindedir (18.10.2026).

## 5. Odak sistemi (en önemli kural)

- Uygulama **sanal odak** kullanır: her an tam olarak bir öğe odaklıdır.
  - Tarayıcının Tab odağı ve ok tuşlarıyla sayfa kaydırması kapatılır.
- Her ekranın açılış odağı ekran tarifinde yazar.
  - Bir ekrana veya katmana geri dönülünce son odak geri yüklenir.
- Odak görünümünde renk tek başına yetmez: büyüme ile çerçeve veya zemin değişimi birlikte kullanılır.
  - **Buton ve çip:** zemin `--focus-bg`, yazı ve ikon `--focus-text`, `scale(1.05)`, gölge `0 6px 18px rgba(0,0,0,.45)`.
  - **Liste ve ayar satırı:** aynı renkler, `scale(1.02)`.
  - **Kanal kartı:** `scale(1.1)`. Çerçeve: `box-shadow: 0 0 0 3px #FFFFFF, 0 10px 24px rgba(0,0,0,.55)`.
    - Logo alanı ve altındaki etiket birlikte büyür.
    - Odaklı kart diğerlerinin üstünde kalır (z-index).
  - **Devam kartı:** kanal kartıyla aynı çerçeve, `scale(1.02)`.
  - **Seçili öğe odaklı öğe demek değildir:**
    - Seçili çip veya seçenek: `--selected-bg` zemin, `--selected-text` yazı ve ✓ ikonu.
    - Seçili öğe odaklanınca beyazlaşır ama ✓ ikonu kalır.
  - **Pasif öğe:** `opacity: .38`, odak almaz.
- Geçişler 150 ms ease-out ile yapılır. Yükleme halkası dışında başka animasyon yoktur.
- Odaklı öğe her zaman görünür alanda kalır. Liste veya ızgara, odaklı öğeyi görünür tutacak şekilde kayar.

## 6. Kumanda = klavye

| Kumanda | Klavye | Not |
|---------|--------|-----|
| ▲ ▼ ◀ ▶ | Ok tuşları | Gezinme |
| OK | Enter (ve Space) | Seçme |
| Geri | Escape, Backspace | Geri dönme |
| CH+ / CH− | PageUp / PageDown | Gerçek kumandada yok; yalnızca oynatıcıda kanal değiştirir |

- Gerçek kumandada rakam, menü, oynat/duraklat ve renkli tuşlar **yoktur**. Bu tuşlara bağlı özellik tasarlanmaz.
- **Geri tuşu:**
  - Önce en üstteki katmanı kapatır (diyalog, panel, bant). Katman yoksa bir önceki ekrana döner.
  - Asla aç/kapa düğmesi gibi davranmaz.
  - Ana ekranda Geri `#google-tv` yer tutucu ekranına çıkar. Çıkış onayı sorulmaz.
- Ekrana "Geri" butonu konmaz; geri işlemi kumandadan yapılır. Diyaloglardaki "Kapat" ve "Vazgeç" butonları kullanılabilir.
- Uzun basma, çift basma ve gizli kısayol yoktur. Tek istisna: Hakkında ekranında "Sürüm" satırına 5 kez OK'a basılınca Tanı ekranı açılır.
- **Fare:** bir öğeye tıklamak, o öğeyi odaklayıp OK'a basmakla aynıdır. Bu yalnızca önizleme kolaylığıdır; tasarım fareye göre yapılmaz.
- **Prototip yardımcıları** (tasarımın parçası değil):
  - `T` tuşu Tasarım Paneli'ni açıp kapatır.
  - `G` tuşu Ekran Galerisi'ni açıp kapatır.

## 7. Bileşenler (tek kaynak — her ekranda bunlar kullanılır)

- **Button**
  - Normal buton: yükseklik 40, yatay padding 18, radius 999, label-l. İkon 18 px, yazıyla arası 8 px. Normal zemin `--surface-2`.
  - Diyalog butonu: yükseklik 48, tam genişlik, içerik sola yaslı, ikon 20 px.
- **Chip** (filtre)
  - Yükseklik 40, padding 0 14, radius 999, label-l. Çipler arası 10 px.
  - Seçili çipte 16 px ✓ ikonu bulunur.
  - "Favoriler" çipinde 16 px dolu yıldız vardır; normalde `--star`, odaklıyken `--focus-text` renktedir.
- **ChannelCard**
  - Genişlik = sütun genişliği.
  - **Logo alanı:** 16:9 (seçenek: 1:1), radius 12, zemin `--surface-tile`.
    - İçinde kanal adı, 700 kalınlıkta "logo yer tutucu" olarak yazılır.
    - Yazı boyutu: 5 harfe kadar 22, 8 harfe kadar 19, daha uzunsa 16.
  - **Etiket:** logo alanının 10 px altında. Önce numara (`--text-2`, label-l, en az 18 px genişlik), 8 px boşluk, sonra kanal adı (label-l). Etiket tek satırdır; taşarsa "…" ile kesilir.
  - **Favori rozeti:** kanal favoriyse sağ üstte 22 px yuvarlak rozet (zemin `rgba(15,16,19,.78)`), içinde 14 px dolu yıldız.
- **ResumeCard** ("Kaldığın yerden")
  - 844×104, radius 16, zemin `--surface-1`, padding 16 24 16 16.
  - Solda 128×72 logo alanı.
  - Ortada "Kaldığın yerden" (label, `--text-2`) ve altında "1 · TRT 1" (title-l).
  - Sağda KeyHint: OK + "İzlemeye devam et".
- **ListRow**
  - Yükseklik 52, radius 12, padding 0 14, öğeler arası 12.
  - Numara (label-l, `--text-2`, genişlik 28), ad (17 px, 500).
  - Sağda favori yıldızı (16, `--star`). Çalan kanalda ayrıca Play ikonu (16, `--accent`) ve "Şu an" (label) bulunur.
- **SettingsRow**
  - Yükseklik 60, radius 14, padding 0 20.
  - Etiket 17 px 500, altında değer (body-m, `--text-2`).
  - Sağda ChevronRight (20) ya da Switch.
- **Switch**
  - İz 44×24, radius 12.
  - Açık: iz `--accent`, 18 px top `--bg` renginde ve sağda.
  - Kapalı: iz `--outline`, top `--text-2` renginde ve solda.
- **Dialog**
  - Genişlik 440 (seçim penceresi 480), radius 24, zemin `--surface-dialog`, padding 28, gölge `0 24px 60px rgba(0,0,0,.6)`.
  - Başlık title-l, açıklama body (`--text-2`).
  - Butonlar dikey sıralanır, aralarında 10 px.
  - Arkasında `--scrim` bulunur.
- **SidePanel**
  - Solda, tam yükseklikte, genişlik 400, zemin `--surface-panel`, padding 28 24 28 58.
  - Panelin sağında kalan video %45 karartılır.
- **InfoBanner**
  - Alt kenarda; soldan ve sağdan 58, alttan 28 px.
  - Zemin `--banner-bg`, radius 20, padding 18 22.
- **Toast**
  - Alt ortada, alttan 40 px.
  - Yükseklik 44, padding 0 20, radius 999, zemin `--focus-bg`, yazı `--focus-text` label-l.
  - Solda 18 px ikon; 2 saniye görünür.
- **KeyHint**
  - Tuş rozeti: en az 26 px genişlik, 24 px yükseklik, padding 0 6, radius 6, 1.5 px `--key-outline` kenar, label (`--text`).
  - Rozetten 8 px sonra açıklama (label, `--text-2`). İki ipucu arası 20 px.
  - Rozet içeriği: ok ikonları (14), "OK" yazısı ya da Geri için CornerDownLeft ikonu (14).
- **Spinner**
  - 40 px halka (küçüğü 24), 4 px kalınlık.
  - Renk `--outline`, üst kenar `--accent`. 0,9 saniyede bir tur döner.
- **VideoPlaceholder**
  - Tam ekran `--video-bg`.
  - Ortada `[Canlı yayın görüntüsü · KANAL ADI]` (18 px 400, `--video-text`).
  - Gerçek video oynatılmaz.
- **LogoMark** (uygulama işareti)
  - Yuvarlatılmış kare: 32 px (radius 9), büyüğü 72 px (radius 20).
  - Zemin `--accent`, içinde `--bg` renginde Tv ikonu.

## 8. İkonlar

lucide-react, stroke 2. Kullanılan ikonlar:
- Tv, Star, StarOff, SlidersHorizontal, Check, ChevronLeft, ChevronRight, ChevronUp, ChevronDown
- CornerDownLeft (Geri), Play, Info, List, RefreshCw, Home, AlertCircle, WifiOff, X
- MonitorPlay (YouTube kaynağı), Globe (web kaynağı)
- Moon, Clock, Lock, Delete, Search, Download, GripVertical

Emoji kullanılmaz.

## 9. Yapılmayacaklar

- Hover efekti, kaydırma çubuğu, sağ tık, sürükle-bırak, açılır menü (dropdown/select), tooltip, hamburger menü, alt sekme çubuğu, modal içinde modal.
- Kartlarda canlı video önizlemesi, otomatik oynayan içerik, kendiliğinden geçen karusel.
- Gerçek kanal logosu ya da marka rengi. Logo yerine kanal adı yazılır.
- Uydurma program adı, puan veya istatistik. Bilinmeyen veri `[Program adı]` gibi köşeli parantezli bir yer tutucuyla gösterilir.
- Teknik kelimeler: EPG, stream, buffer, IPTV, cache, URL, hata kodu. Kullanıcıyla sade Türkçe konuşulur.
- Çıkış onayı penceresi.
- shadcn/ui gibi hazır bileşenlerin fare ve Tab odaklı davranışı. Bileşenler bu dosyadaki ölçülerle sıfırdan yazılır.

## 10. Metin kuralları

- Tüm arayüz Türkçedir. Cümleler kısadır ve ricayla kurulur ("Tekrar deneyin"). Buton metinleri 1–3 kelimedir.
- Ekran tariflerinde tırnak içinde verilen metinler birebir kullanılır.

## 11. Uygulama yapısı

**Rotalar** (Ekran Galerisi ve ekran görüntüleri için her ekran ve durum hash ile açılabilir):

| Rota | Ekran |
|------|-------|
| `#vitrin` | Bileşen vitrini (geliştirme) |
| `#acilis` | Açılış |
| `#kumanda-rehberi` | Kumanda rehberi |
| `#google-tv` | Google TV ana ekranı yer tutucusu |
| `#ana` | Ana ekran |
| `#ana?mod=favori` | Favori düzenleme modu |
| `#ana?katman=guncelleme` · `#ana?katman=cikis-ipucu` | Ana ekran katmanları (sonraki sürüm) |
| `#oynatici/<kanalId>` | Oynatıcı |
| `#oynatici/<kanalId>?katman=…` | Oynatıcı katmanları: `bant`, `liste`, `secenekler`, `aciliyor`, `hata`, `program-yok`, `harici`, `uyku`, `hala-izliyor` |
| `#ayarlar` | Ayarlar |
| `#ayarlar?pencere=…` | Seçim pencereleri: `yazi-boyutu`, `kanal-yonu`, `bant-suresi` |
| `#hakkinda` | Hakkında |
| `#tani` | Tanı ekranı |
| `#internet-yok` | İnternet yok |
| `#pin` · `#arama` · `#program-rehberi` · `#favori-siralama` | Sonraki sürüm ekranları |

**Kalıcı durum** (localStorage, `aileTv.` önekiyle):
- favoriler
- son kanal
- ayarlar: yazıBoyutu, açılıştaSonKanal, kanalYönü, bantSüresi, çıkışİpucu
- aktif filtre
- rehberGörüldü

**Kanal verisi** `src/data/channels.ts` dosyasında tutulur.
- Kanal sırası numara sırasıdır.
- Kanal değiştirme aktif filtre içinde yapılır; sona gelince başa döner.

**Kaynak türleri:**
- `hls`: uygulama içinde oynar.
- `youtube`: resmi YouTube yayını.
- `web`: resmi internet sayfası.

Prototipte hiçbir gerçek yayın açılmaz.

**YouTube kaynaklı kanallar:** YouTube kuralları gereği video alanının üzerine bant, panel veya diyalog çizilmez. Böyle bir katman açılacaksa önce video yer tutucusu gizlenir (siyah zemin), katman sonra açılır.
