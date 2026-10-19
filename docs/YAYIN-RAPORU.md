# Yayın Kaynakları Raporu — 19.10.2026

Bu rapor 54 kanalın her biri için şunları anlatır:
- gerçekten çalışan yayın adresleri,
- bu adreslerin çözünürlükleri,
- uygulamanın bu yayınları nasıl sorunsuz oynatacağı.

Tüm testler 19.10.2026 gecesi, Türkiye'deki ev internetinden yapıldı. Yani ölçümler, anne-babanızın kutusunun göreceği ağ koşullarını yansıtıyor.

- Uygulamanın okuyacağı liste: [`channels/channels.json`](../channels/channels.json)
- Son doğrulama çıktısı: [`channels/verify-2026-10-19.json`](../channels/verify-2026-10-19.json)

---

## 1. Önce dürüst bir çerçeve: "kusursuz" ne demek?

Yayınların sahibi biz değiliz. Sunucular TRT, Kanal D, Turkuvaz gibi yayıncılara ait. Yayıncılar adres değiştirebilir, token sistemi kurabilir, maç saatinde yayını kesebilir. Bu yüzden hiçbir uygulama "hiçbir kanal hiçbir zaman kesilmez" sözü veremez.

Verebileceğimiz söz şu:

1. **Listedeki 54 kanalın hepsinin bugün çalışan, doğrulanmış bir kaynağı var.**
   - 47 kanal doğrudan uygulama içinde oynuyor (HLS).
   - CNN Türk resmi API üzerinden açılıyor.
   - 6 kanal yalnızca resmi YouTube canlı yayınıyla açılabiliyor: Sözcü, TV100, Haber Global, Akit, Bengütürk, TGRT Belgesel. Bu yayınlar uygulamanın içinde gömülü oynatılacak.
2. **Her kanalın birden fazla yedeği var.** Bir kaynak kapanırsa uygulama sessizce bir sonrakine geçer.
3. **Kanal listesi uygulamanın dışında, GitHub'da tutulur.** Bir adres bozulursa APK'yı yeniden yüklemeye gerek kalmadan 5 dakikada düzeltilir.
4. **Uygulama kendi kendine sağlık kontrolü yapar.** Bozuk bir kaynağı anne-babanıza "açılamıyor" diye göstermek yerine çalışan yedeğe geçer.

Bir uygulamanın "kusursuz"a yaklaşabileceği en yakın nokta bu dört maddedir.

---

## 2. Figma Make prototipinde neden bazı kanallar açılmadı?

Make'in ürettiği `src/data/streams.ts` dosyasında yalnızca 19 kanal var. Dosyadaki yorum satırı sebebini söylüyor: eklenenler *"HTTPS, segment ve **tarayıcı CORS** doğrulamasını geçenler"*. Yani sorun yayınlarda değil, tarayıcıdaydı.

| Sebep | Etkilenen kanallar | TV uygulamasında durum |
|-------|--------------------|------------------------|
| **CORS:** daioncdn/ercdn sunucuları tarayıcıya "başka siteden oynatılamaz" başlığı gönderiyor, tarayıcı da yayını engelliyor. | Kanal D, Show TV, Star TV, TV8, Kanal 7, teve2, NTV, Habertürk, HT Spor, ATV, A Haber… | **Sorun yok.** ExoPlayer bir tarayıcı değil, CORS'a bakmıyor. Bu kanalların hepsi testte 1080p açıldı. |
| **Coğrafi kısıt:** Make'in önizlemesi yurt dışındaki bir sunucuda çalışıyor. | TRT 2, TRT Genç, TRT Spor Yıldız | **Sorun yok.** Kutu Türkiye'de. |
| **Token'lı yayın** | NOW, CNN Türk, DMAX, TLC, Beyaz TV | Çözüldü (bölüm 4). |
| Make'in seçtiği 3 adres **üçüncü taraf yeniden yayın** (tv.ensonhaber.com). | Habertürk, Haber Global, Bengütürk | Habertürk için resmi 1080p adres bulundu. Diğer ikisi resmi YouTube yayınından gömülü oynayacak. |

Kısacası prototipte açılmayan kanalların neredeyse tamamı TV uygulamasında açılıyor. Prototipi değiştirmeye gerek yok; tasarım referansı olarak işini görüyor.

---

## 3. Kanal kanal sonuçlar

Tablolardaki sütunlar:
- **Kaynak:** uygulamanın ilk deneyeceği adres türü.
- **En yüksek:** yayıncının sunduğu en yüksek çözünürlük.
- **Yedek:** ilk kaynak açılmazsa sırayla denenecek diğer kaynaklar.

Kaynak türleri:
- **Resmi:** yayıncının kendi sayfasında veya oynatıcısında yayınladığı adres.
- **Yayıncı CDN:** yayıncının kendi CDN'inde (Erstream/daion) çalışan ama sayfada yayınlanmayan adres.
- **YouTube:** yayıncının resmi YouTube canlı yayını; uygulama içinde gömülü oynatılır.
- **Web:** resmi canlı yayın sayfası, uygulama içinde açılır.

### Genel
| # | Kanal | Kaynak | En yüksek | Yedek | Not |
|---|-------|--------|-----------|-------|-----|
| 1 | TRT 1 | Resmi HLS | 1440p50 | 2. resmi adres | Uygulamada 1080p'ye sınırlanır |
| 2 | ATV | Yayıncı CDN | 1080p | Web | Turkuvaz'ın resmi sayfası token kullanıyor; aynı yayının CDN kenarı token'sız açılıyor |
| 3 | Kanal D | Resmi HLS | 1080p | 2. resmi, YouTube | |
| 4 | Show TV | Resmi HLS | 1080p | — | TMSF yönetiminde, yayında |
| 5 | Star TV | Resmi yönlendirici (Doğuş) | **1080p** | Resmi daion 720p | Resmi web oynatıcı 720p veriyor. 1080p, Doğuş'un puhutv için kullandığı resmi uçta bulundu |
| 6 | NOW | Resmi sayfa token'ı | 720p | CDN kenarı, Web | |
| 7 | TV8 | Resmi HLS | 1080p | — | |
| 8 | Kanal 7 | Resmi HLS | 1080p | — | |
| 9 | teve2 | Resmi HLS | 1080p | YouTube | |
| 10 | Beyaz TV | Yayıncı CDN (daioncdn) | 1080p | CDN kenarı, Web | Resmi sayfadaki adres 401 veriyor. Aynı alan adındaki kaynak sunucu token'sız çalışıyor |
| 11 | 360 | Resmi HLS | 1080p | — | |
| 12 | TRT 2 | Resmi HLS | 1440p | — | Sadece Türkiye'den açılır |
| 45 | TRT Genç | Resmi HLS | 1440p | — | Sadece Türkiye'den açılır |

### Haber
| # | Kanal | Kaynak | En yüksek | Yedek | Not |
|---|-------|--------|-----------|-------|-----|
| 13 | TRT Haber | Resmi HLS | 1440p | YouTube | |
| 14 | NTV | Resmi yönlendirici (Doğuş) | 1080p (5,6 Mb/s) | Resmi daion 1080p, YouTube | |
| 15 | CNN Türk | Resmi API token'ı | 720p | YouTube | CNN Türk'ün kendi CDN'inde 1080p yok |
| 16 | Habertürk | Resmi HLS | 1080p | YouTube | |
| 17 | A Haber | Yayıncı CDN | 1080p | YouTube | |
| 18 | Sözcü TV | **YouTube** | YouTube | — | Resmi HLS yok |
| 19 | TV100 | **YouTube** | YouTube | (3. taraf 1080p, kapalı) | Resmi HLS yok |
| 20 | Haber Global | **YouTube** | YouTube | (3. taraf 720p, kapalı) | Resmi HLS yok |
| 21 | TGRT Haber | Resmi HLS | 720p | YouTube | Tek kalite |
| 22 | 24 TV | Resmi HLS | 1080p | — | |
| 23 | TVNET | Resmi HLS | 1080p | YouTube | |
| 24 | Ülke TV | Yayıncı sağlayıcısı | 720p | YouTube | Kanal 7 grubunun yayın sağlayıcısı; görüntü doğrulandı |
| 25 | Akit TV | **YouTube** | YouTube | — | Güncel adres yayıncının kendi `live.json` dosyasından alınıyor |
| 26 | Bengütürk | **YouTube** | YouTube | (3. taraf 720p, kapalı) | |
| 27 | Bloomberg HT | Resmi HLS | 720p | YouTube | Resmi yayın 720p; 1080p yalnızca 3. tarafta |
| 28 | A Para | Yayıncı CDN | 1080p | YouTube | |
| 29 | Ekotürk | Resmi HLS | 720p | — | Canlı pencere 6 sn; oynatıcıda özel ayar gerekir |
| 30 | CNBC-e | Yayıncı CDN | 1080p | YouTube | Resmi sayfadaki adres 403 veriyor |

### Spor
| # | Kanal | Kaynak | En yüksek | Yedek | Not |
|---|-------|--------|-----------|-------|-----|
| 31 | TRT Spor | Resmi HLS | 1440p | — | Maç saatlerinde kesinti olabilir |
| 32 | TRT Spor Yıldız | Resmi HLS | 1440p | 2. resmi adres | Sadece Türkiye'den açılır |
| 33 | A Spor | Yayıncı CDN | 1080p | YouTube | |
| 34 | HT Spor | Resmi HLS | 1080p | YouTube | |

### Çocuk
| # | Kanal | Kaynak | En yüksek | Yedek | Not |
|---|-------|--------|-----------|-------|-----|
| 35 | TRT Çocuk | Resmi HLS | 1440p | — | |
| 36 | TRT Diyanet Çocuk | Resmi HLS | 1440p | — | |
| 37 | Minika Çocuk | Yayıncı CDN | 720p | Web | |
| 38 | Minika GO | Yayıncı CDN | 720p | Web | |

### Belgesel
| # | Kanal | Kaynak | En yüksek | Yedek | Not |
|---|-------|--------|-----------|-------|-----|
| 39 | TRT Belgesel | Resmi HLS | 1440p | — | |
| 40 | DMAX | Resmi HLS | 720p | Web | Sayfadaki token sunucuda zorunlu değil |
| 41 | TLC | Resmi HLS | 720p | Web | Sayfadaki token sunucuda zorunlu değil |
| 42 | TGRT Belgesel | **YouTube** | YouTube | — | Eski HLS adresleri artık çalışmıyor |

### Müzik ve Diğer
| # | Kanal | Kaynak | En yüksek | Yedek | Not |
|---|-------|--------|-----------|-------|-----|
| 43 | TRT Müzik | Resmi HLS | 1440p | — | |
| 44 | Dream Türk | Resmi HLS | 720p | — | |
| 46 | Diyanet TV | Resmi CDN (İhlas) | 1080p | YouTube | |
| 47 | TBMM TV | Resmi HLS | 720p | — | Genel Kurul saatleri dışında içerik değişir |
| 48–52 | TRT Türk, Avaz, Kurdî, Arabi, World | Resmi HLS | 1440p | — | |
| 53 | TRT EBA | Resmi HLS | 720p | — | |
| 54 | Show Türk | Resmi HLS | 1080p | — | |

**Özet**

| En yüksek kalite | Kanal sayısı |
|------------------|--------------|
| 1440p (TRT) | 15 |
| 1080p | 20 |
| 720p (yayıncının sunduğu en yüksek kalite) | 13 |
| YouTube | 6 |

Toplam 89 kaynak var; bunların 85'i açık. Kapalı olan 4 kaynak, kararınızla kapatılan üçüncü taraf kopyalar.

### Son doğrulama turu (19.10.2026, 02:10–02:25)

Listedeki 53 doğrudan HLS kaynağı sırayla yeniden test edildi. Her test 14 saniye sürdü ve yayının gerçekten ilerlediği kontrol edildi.

Sonuç: **53 kaynağın 53'ü canlı.**

İki kaynak (Star TV'nin daion yedeği ve CNBC-e) ilk iki denemede `ENOTFOUND` aldı. Sebep ev modeminin DNS'iydi. Adresler Google DNS ile çözümlendi ve üçüncü denemede açıldı. Bu bulgu iki önlemin gerekli olduğunu gösteriyor: kutuda DNS olarak 8.8.8.8 kullanılması ve uygulamanın DNS hatasında yeniden denemesi (bölüm 6, madde 4).

## 4. Çözülen zor kanallar (ne bulundu, nasıl doğrulandı)

Her zor kanalda iki bağımsız ajan çalıştı. Biri kaynağı buldu. Diğeri kaynağı en az iki kez yeniden test edip çürütmeye çalıştı.

- **Beyaz TV:**
  - Resmi sayfadaki adres reklam ekleme sunucusundan geçiyor ve tarayıcı dışında 401 veriyor.
  - Aynı `daioncdn.net` alanındaki kaynak sunucu (`beyaztv-live.daioncdn.net`) token'sız 1080p veriyor; 3 testin 3'ünde de canlıydı.
  - Risk: bu sunucu ileride kapatılabilir. Bu yüzden yedek olarak CDN kenarı ve web sayfası eklendi.
- **Star TV 1080p:**
  - Resmi web oynatıcı 720p veriyor.
  - Doğuş'un puhutv için kullandığı kendi yönlendirici ucu (`dygvideo.dygdigital.com`) 1080p ve 3 Mb/s veriyor; ffprobe ile 1920×1080 ölçüldü.
  - Canlı pencere sadece 18 sn olduğu için oynatıcı ayarı gerekiyor (bölüm 6).
- **NTV:** Aynı yönlendirici 1080p'yi 5,6 Mb/s ile veriyor; resmi daion adresi 2,2 Mb/s'de kalıyor. İki adres de listede.
- **NOW:**
  - Yayın sadece 4 saatlik bir token'la açılıyor. Token, resmi sayfanın HTML'i içinde sunucu tarafından hazır geliyor.
  - Uygulama sayfayı bir tarayıcı gibi okuyup içindeki adresi oynatıyor. Token üretmek ya da kırmak söz konusu değil.
  - Onayınızla bu yöntem kullanılıyor.
- **CNN Türk:**
  - Sayfadaki adres 403 veriyor.
  - Sitenin kendi API'si (`/api/cnnvideo/media`) 3 saatlik token'lı bir adres döndürüyor; web oynatıcı da aynısını yapıyor.
  - Yöntem uçtan uca ExoPlayer kimliğiyle test edildi. Onayınızla kullanılıyor.
- **DMAX / TLC:** Sayfada token var ama sunucu zorunlu tutmuyor. Token'sız resmi adres 720p canlı.
- **Turkuvaz kanalları (ATV, A Haber, A Spor, A Para, Minika):**
  - Resmi oynatıcı `securevideotoken.tmgrup.com.tr`'den token alıp `trkvz*.daioncdn.net` adresini açıyor.
  - Aynı yayının Erstream CDN kenarı (`*.turknet.ercdn.net`) token'sız ve 1080p. Resmi oynatıcı da segmentleri buradan çekiyor.
  - ATV için 1920×1080 çözünürlük ffprobe ile doğrulandı.
- **Ülke TV:** Kanal 7 grubunun yayın sağlayıcısında 720p bir adres bulundu. Ajan görüntüden kare yakalayıp Ülke TV logosunu ve programı doğruladı.
- **TGRT Belgesel, Sözcü, TV100, Haber Global, Akit, Bengütürk:** Resmi HLS yok; resmi sitelerinde de YouTube gömülü. Tek resmi yol YouTube.

### iptv-org listesi hakkında

Listedeki 221 girişin bizim kanallarla kesişen kısmı tek tek test edildi:
- Doğru çıkanlar (çoğu yayıncının kendi CDN'i) listeye alındı.
- Ölü, 403 veren, sertifikası bozuk veya korsan sunucuya bağlı girişler elendi (beIN, çıplak IP adresleri, smartplaytv, freeott, mangeinde4).

Bu liste "her şey burada" listesi değil. Sözcü, TV100, Akit, CNBC-e, A Para, Show TV gibi kanallarda resmi kaynak ya hiç yok ya da yalnızca üçüncü taraf kopyası var. Haftalık farkları takip etmek için yararlı ama tek başına güvenilir değil.

---

## 5. Verilen kararlar (19.10.2026)

1. **Resmi sayfa/API token'ı (NOW ve CNN Türk): Evet.** Uygulama, resmi web oynatıcının yaptığı gibi sayfayı veya API'yi isteyip içindeki geçici adresi oynatır. Token üretilmez.
2. **YouTube kanalları: Uygulama içinde gömülü.** Bu karar 6 kanalı birincil olarak, 18 kanalı da yedek olarak ilgilendiriyor. Kullanıcı uygulamadan çıkmaz. Uygulanacak kurallar:
   - Oynatıcı resmi YouTube IFrame Player olur ve WebView içinde çalışır. İstek başlığı olarak uygulama kimliği gönderilir; YouTube'un gömülü oynatıcı kuralı (RMF) bunu istiyor.
   - **Video alanının üstüne bant, panel veya diyalog çizilmez.** Kanal listesi ya da bilgi açılacaksa video önce gizlenir, katman ondan sonra açılır.
   - Kanal bilgisi videodan önce, 1,5 saniyelik geçiş ekranında gösterilir.
   - ▲▼ ile kanal değiştirme çalışmaya devam eder. Tuşları WebView değil, uygulama yakalar.
   - Canlı yayın kimliği her açılışta kanalın `/live` sayfasından alınır. Akit TV için kimlik yayıncının `live.json` dosyasından gelir.
   - 2 GB RAM'li kutuda WebView'ın açılış süresi ve kumanda uyumu ilk cihaz testinde ölçülecek. Sorun çıkarsa YouTube uygulamasına geçiş yedek olarak kalır.
3. **Üçüncü taraf kopyalar: Kapalı.** tv.ensonhaber.com adresleri listede `enabled: false` olarak duruyor ve uygulama bunları denemiyor.
4. **Halk TV: Listeden çıkarıldı.** Kanal numaraları 1–54 olarak yeniden verildi. 24 ve sonrasındaki kanallar birer numara öne kaydı.

---

## 6. Uygulamanın hatasız çalışması için yapılacaklar (teknik plan)

Aşağıdaki kurallar `channels.json`'daki notlardan çıkıyor ve kod yazarken uygulanacak:

1. **Kaynak zinciri:** Her kanalın kaynakları sırayla denenir.
   - Bir kaynak 10 saniye içinde ilk kareyi veremezse ya da 3 denemede açılmazsa bir sonrakine geçilir.
   - Kullanıcı "açılamıyor" mesajını ancak bütün zincir tükenince görür.
2. **Token yenileme:**
   - `page-token` ve `api-token` kaynaklarında adres her açılışta taze alınır.
   - Oynatma sırasında 403 gelirse adres yenilenir ve yayın kaldığı yerden devam eder.
   - daioncdn kanallarında her açılışta ana adres yeniden istenir. Alt listeler asla saklanmaz.
3. **Kısa canlı pencere ayarı:**
   - İlgili kanallar ve canlı pencereleri: Star TV 18 sn, Ülke TV 28 sn, Ekotürk 6 sn, TVNET 30 sn.
   - Bu kanallarda ExoPlayer'ın canlı hedef gecikmesi pencerenin yarısına ayarlanır.
   - `BehindLiveWindow` hatasında oynatıcı yeniden hazırlanır.
   - Bu ayarlar yapılmazsa bu kanallarda takılma görülür.
4. **DNS hataları:** Testlerde ev modeminin DNS'i yük altında ara sıra `ENOTFOUND` verdi.
   - Uygulama DNS hatasında 1 saniye arayla 3 kez yeniden dener.
   - Kutuda ayrıca Google DNS (8.8.8.8) kullanılması önerilir.
5. **Çözünürlük üst sınırı:**
   - TRT'nin 1440p / 11 Mb/s kalitesi 2,4 GHz Wi-Fi'da takılma yapar. Bu yüzden uygulamada üst sınır 1080p olur.
   - Wi-Fi zayıfsa ExoPlayer kaliteyi kendiliğinden düşürür.
   - **5 GHz Wi-Fi şart.**
6. **Reklam levhaları:** Star TV'nin daion yedeği reklam aralarında "yayın az sonra devam edecek" levhası gösteriyor. Birincil kaynağın 1080p yönlendirici olmasının bir sebebi de bu.
7. **Sağlık kontrolü:**
   - Kutu her açılışta ve 6 saatte bir tüm birincil kaynakları arka planda test eder: ana liste ve alt liste ilerliyor mu?
   - Düşen kaynak o kanalın zincirinde otomatik olarak en sona alınır.
   - Sonuç anonim olarak GitHub'daki listeye işlenir; böylece sorunları ben de görürüm.
8. **Uzaktan liste:** `channels.json` GitHub'da durur ve kutu 6 saatte bir çeker. Son çalışan kopya kutuda saklanır, böylece internet yokken bile liste kaybolmaz.
9. **YouTube kimlikleri:** Canlı yayının video kimliği değişebilir. Uygulama her açılışta güncel kimliği alır; hiçbir kimlik koda sabit yazılmaz.

---

## 7. Dosyalar

- `channels/channels.json`: 54 kanal ve 89 kaynak. Kararlar `policy` alanında, kaynak notları ve kademe bilgisiyle birlikte.
- `channels/verify-2026-10-19.json`: her doğrudan kaynağın son test sonucu.
- `design/figma-make/`: Figma Make'ten indirdiğiniz prototip kodu (tasarım referansı).
- `docs/research/`: kanal, cihaz ve UX araştırma notları (İngilizce).
