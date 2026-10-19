# Aile TV Uygulaması — Proje Planı

> Hedef cihaz: **Xiaomi TV Box S (2. Nesil)**, model MDZ-28-AA
> Hazırlanma tarihi: 18.10.2026. Kanal ve yayın bilgileri bu tarihte Türkiye IP'sinden test edildi.
> Ham araştırma notları (İngilizce, kaynaklı): [`docs/research/`](research/)

---

## 0. Önce bilinmesi gereken 5 gerçek

1. **Kumandada rakam tuşu, CH+/CH− tuşu ve oynat/duraklat tuşu yok.** Uygulamanın güvenebileceği tuşlar yalnızca şunlar: **▲ ▼ ◀ ▶, OK ve Geri.**
   - Home, Asistan, Güç ve Ses tuşlarını sistem yönetir. Netflix, Prime Video, YouTube, Xiaomi TV+ ve Uygulamalar tuşları da sistemindir.
   - Bu yüzden kanal değiştirme **▲▼ ile** yapılacak.
2. **İşletim sistemi Android TV 11 (API 30, 32-bit)** olarak geldi. Bazı cihazlara 2025'te Android TV 14 güncellemesi geldi. Sizin kutunuzun hangisi olduğunu Ayarlar > Sistem > Hakkında'dan kontrol etmeliyiz. Uygulamayı ikisini de destekleyecek şekilde yapacağız.
3. **Kutu zayıf bir donanım.** 2 GB RAM'in yaklaşık 1 GB'ı boşta. Depolamanın yaklaşık 3,5 GB'ı boş ve grafik işlemcisi zayıf (Mali-G31).
   - Arayüz sade olmalı: bulanıklık efekti ve kanal kartlarında canlı video önizlemesi olmayacak.
   - Uygulama hafif tutulacak (≤15 MB).
4. **Ulusal kanalların hepsi aynı şekilde açılmıyor.** Durum üç grupta:
   - Çoğunun resmi, ücretsiz ve doğrudan oynatılabilir yayını var (bütün TRT kanalları, Kanal D, Show, TV8, Kanal 7, NTV…).
   - Bir kısmı sadece resmi YouTube canlı yayını üzerinden izlenebiliyor (A Haber, CNN Türk, Sözcü, TV100…).
   - Birkaçı yalnızca kendi web sayfasında veya resmi uygulamasında oynuyor (ATV, NOW, Beyaz TV, DMAX, TLC).
   - Uygulama bu üç tür kaynağı da desteklemeli (bkz. §3).
5. **Uygulama yalnızca aile içi kullanım için kalacak.** Ücretsiz resmi yayınları oynatmak düşük risklidir. Kaçınacaklarımız:
   - yayın şifresini/token'ını kırmak (FSEK m.72),
   - korsan spor yayını kullanmak,
   - APK'yı herkese dağıtmak (RTÜK platform izni gerekebilir).

---

## 1. EKRAN LİSTESİ

Tasarım tuvali: **960×540 dp** (Figma'da 960×540 çerçeve; 1920×1080 ekranda ×2).
Güvenli alan: kenarlardan **58 dp**, üst ve alttan **28 dp**.
Gezinme en fazla **2 seviye** derin: Ana Ekran → Oynatıcı / Ayarlar. Gerisi bu ekranların üzerine açılan katmanlar.

### A) MVP — İlk sürümde mutlaka olacaklar

| # | Ekran | Tür | Ne işe yarar | Kumanda |
|---|-------|-----|--------------|---------|
| 1 | **Açılış (Splash)** | Ekran | Logo, koyu arka plan. Kanal listesi yüklenirken görünür. 1 sn'den uzun sürerse "Kanallar hazırlanıyor…" yazar. Geri tuşu ile bu ekrana dönülmez. | Tuşlar etkisiz |
| 2 | **Kumanda Rehberi (ilk açılış)** | Katman | Tek sayfalık anlatım: kumanda çizimi ve 4 büyük satır: "▲▼ Kanal değiştir · OK Kanal listesi · ▶ Bilgi · Geri Ana ekran". Tek bir buton var: **"Anladım, başla"**. Ayarlar'dan tekrar açılabilir. | OK ve Geri = başla |
| 3 | **Ana Ekran – Kanallar** | Ekran | En üstte saat ve "Favorileri düzenle" / "Ayarlar" butonları. Altında **"Kaldığın yerden"** kartı (son izlenen kanal, açılışta odak burada). Sonra **filtre çipleri**: ★ Favoriler · Tümü · Genel · Haber · Spor · Çocuk · Belgesel · Müzik · Ekonomi · Diğer. En altta **kanal logosu ızgarası**: 5 sütun; büyük yazıda 4, çok büyük yazıda 3. Kanal adı ve numarası her zaman logonun altında yazar. | Yön tuşları gezinir. Çipe gelince filtre uygulanır. OK = kanalı aç. Oynatıcıdan dönünce odak izlenen kanalın kartına geri gelir. |
| 4 | **Favori Düzenleme modu** | Katman (Ana Ekran üstünde) | Üstte bilgi bandı: "OK ile ★ ekle/çıkar · Bitince Geri". Her kartta boş veya dolu yıldız. Gizli bir uzun basma hareketine gerek kalmaz. | OK = yıldızı aç/kapat, Geri = bitir |
| 5 | **Canlı Yayın Oynatıcı** | Ekran | Tam ekran video. Kalıcı bir arayüz öğesi yok; katmanlar ancak bir tuşa basınca açılır. Oynatma sırasında ekran açık kalır. | **▲▼ = önceki/sonraki kanal** (seçili filtre içinde döner). OK veya ◀ = kanal listesi. ▶ = bilgi bandı. Geri = Ana ekran. |
| 6 | **Kanal Bilgi Bandı** | Katman | Altta: logo, numara, büyük kanal adı (28–32 sp), saat, ★. EPG eklenirse "Şimdi / Sonra" da görünür. Tuş ipucu satırı var. Her kanal değişiminde çıkar, 5 sn sonra kaybolur. | ▲▼ ile kanal değiştirmeye devam edilir. Geri = gizle. |
| 7 | **Kanal Listesi Paneli** | Katman (oynatıcı üstünde) | Solda ekranın ⅓'ü kadar panel; video arkada oynamaya devam eder. Üstte kategori çipleri. Satırlarda numara, logo ve ad. ▶ işareti çalan kanalı, ★ favorileri gösterir. Açılınca odak çalan kanalda olur. | ▲▼ = listede gezin (kanal değişmez). OK = kanalı aç. ▶ = seçenekler. Geri = kapat. |
| 8 | **Kanal Seçenekleri** | Diyalog | Kanal logosu ve adı. Büyük butonlar: "★ Favorilere ekle / çıkar", "Yayını yeniden yükle", "Kapat". | ▲▼, OK, Geri |
| 9 | **Yayın Açılıyor** | Durum katmanı | Logo, ad, dönen yükleme simgesi ve "Yayın açılıyor…" yazısı. Arka planda önce sessizce yedek kaynak denenir. 10 sn içinde açılmazsa #10'a geçer. | ▲▼ ile kanal değiştirmek hâlâ çalışır |
| 10 | **Yayın Açılamadı** | Durum katmanı | "TRT 1 şu anda açılamıyor." Butonlar: **Tekrar dene** (odaklı) · Sonraki kanal · Ana ekran | ◀▶ ve OK. ▲▼ ile kanal değişir. |
| 11 | **İnternet Yok** | Ekran | "İnternet bağlantısı yok", altında "Modem açık mı? Wi-Fi bağlı mı?" kontrol listesi. Butonlar: Tekrar dene · Ağ ayarlarını aç. Bağlantı gelince kendiliğinden kapanır. | ◀▶ ve OK |
| 12 | **Boş Durum** | Ana Ekran içi | "Henüz favori kanalınız yok." Butonlar: "Tüm kanallara git" · "Favorileri düzenle" | OK |
| 13 | **Ayarlar** | Ekran | Tek düz liste: **Yazı boyutu** (Normal / Büyük / Çok büyük; Android TV 11'de sistem yazı boyutu ayarı olmadığı için şart) · Açılışta son kanalı başlat (varsayılan açık) · Kanal değiştirme yönü · Bilgi bandı süresi (3/5/10 sn) · Kanal listesini güncelle · Kumanda rehberini göster · Hakkında | ▲▼, OK, Geri |
| 14 | **Seçim Diyaloğu** | Diyalog | Ayarlarda tek seçim yapılan pencere (radyo butonlu liste). Yazı boyutu seçilirken örnek metin canlı olarak büyür/küçülür. | ▲▼, OK, Geri = vazgeç |
| 15 | **Hakkında** | Ekran/diyalog | Uygulama sürümü, kanal listesinin tarihi, yayın kaynaklarına teşekkür notu | Geri |
| 16 | **Harici Kaynak geçişi** | Durum katmanı | Kanal YouTube'dan veya resmi web sayfasından açılacaksa önce kısa bir bilgi gösterilir: "A Haber, resmi YouTube yayınından açılıyor…". YouTube kuralları gereği bu tür oynatıcının **üstüne hiçbir şey çizilemez**. | Geri = Ana ekran |
| 17 | **Tanı ekranı (gizli, sadece sizin için)** | Ekran | Basılan tuşların kodu, Android sürümü, RAM ve ağ bilgisi. Kumandanın gerçekte hangi tuşları gönderdiğini görmek için. Figma'da tasarlamaya gerek yok. | — |

### B) Sonraki sürümler

| # | Ekran | Ne işe yarar |
|---|-------|--------------|
| 18 | **Uyku Zamanlayıcı** | Kapalı / 30 / 60 / 90 / 120 dk seçenekleri. Kalan süre bilgi bandında görünür. |
| 19 | **"Hâlâ izliyor musunuz?"** | Saatlerce hiçbir tuşa basılmazsa 60 sn geri sayımlı soru. Gece boyu boşa yayın akmasını önler. |
| 20 | **Ebeveyn Kilidi (PIN)** | Ekranda 3×4 rakam tuş takımı (kumandada rakam yok). Ayarları veya belirli kanalları kilitler. |
| 21 | **Kanal Arama** | Türkçe harfli ekran klavyesi (Ç Ğ I İ Ö Ş Ü). İleride sesle arama eklenebilir. |
| 22 | **Program Rehberi (EPG)** | Kanallar × saat tablosu. Türkiye için hazır kaynak yok; veriyi kendimiz toplamamız gerekir. |
| 23 | **Favori Sıralama** | Favorilerin sırasını belirler; bu sıra aynı zamanda ▲▼ ile gezinme sırasıdır. |
| 24 | **Güncelleme Mevcut** | "Yeni sürüm hazır – Şimdi güncelle / Sonra". Play Store olmadığı için uygulama kendini güncelleyecek. |
| 25 | **Çıkış İpucu** | Varsayılan olarak kapalı. "Çıkmak için Geri'ye tekrar basın" uyarısı. Google çıkış onay penceresi önermiyor. |

### C) Her ekran için çizilmesi gereken durumlar

- Her buton, kart, çip ve satır için 4 hâl: **normal · odaklı · seçili · pasif**
  - Odaklı: 1.05–1.1 büyüme + 3 dp beyaz/amber çerçeve + daha açık yüzey. Sadece renk değişimi yeterli değil.
  - Seçili çip: dolu arka plan + ✓ işareti. Böylece odaklıdan ayırt edilir.
- 3 yazı boyutu varyantı: **Normal 1.0× · Büyük 1.25× · Çok büyük 1.5×**
- Yükleniyor, hata ve boş hâlleri
- Kanal kartı biçimi: Google kanal logoları için **1:1** kart öneriyor; Google TV'nin canlı TV sekmesi **16:9** kullanıyor. Tasarımda birini seçip nedenini not edin.

---

## 2. TASARIM KURALLARI (Figma'ya başlamadan)

| Konu | Kural |
|------|-------|
| Araç | Figma + Google **TV Design Kit** (figma.com/@tv), 960×540 çerçeve |
| Izgara | 12 sütun × 52 dp, 20 dp boşluk, kenarlar 58 dp |
| Tema | Sadece koyu tema. Yüzey ~#121318; saf siyah yalnızca video kenarlarında. Kontrast: metin ≥7:1, ikincil metin ≥4.5:1 |
| Yazı tipi | Roboto / Inter / Noto Sans. **Hiçbir yazı 14 sp altında olmayacak**; bu kitle için 16 sp önerilir. Kanal adı 16–18 sp, bilgi bandı 28–32 sp. tv-material'ın varsayılan küçük yazı rolleri (11–12 sp) mutlaka büyütülmeli. |
| Odak | Ekranda her zaman tek bir odaklı öğe olur. Her ekranın açılışta hangi öğeye odaklanacağı belli olmalı. Odağın takılıp kalacağı bir yer olmamalı. |
| Dil | Sade Türkçe. "EPG", "stream", "buffer", "IPTV" gibi terimler kullanılmayacak. "Kanallar, Favoriler, Tekrar dene, Şimdi, Sonra" gibi kelimeler kullanılacak. |
| Geri tuşu | Önce en üstteki katmanı kapatır. Arka arkaya basınca sonunda Google TV ana ekranına çıkılır. Geri tuşu hiçbir zaman aç/kapat düğmesi gibi çalışmaz. |
| Animasyon | ~150 ms, sade. Bulanıklık ve şeffaf katman yığını kullanılmayacak (zayıf grafik işlemcisi). |
| Teslim | Figma Dev Mode. İsterseniz **Figma MCP** üzerinden tasarımlarınızı doğrudan ben okuyup koda dökebilirim (ücretli Dev/Full koltuk gerekir). |

---

## 3. KANAL KAYNAKLARI (18.10.2026 testi)

Kanal listesi **APK'nın içine gömülmeyecek**. GitHub'da bir `channels.json` dosyasında duracak. Bir yayın adresi bozulursa uygulamayı yeniden yüklemeden sadece bu dosyayı düzeltiriz. Her kanal için sıralı yedek kaynaklar tutulur: önce doğrudan yayın, sonra YouTube, sonra web sayfası.

**1. grup — Doğrudan yayın (uygulama içinde oynar, en iyi deneyim)**
- **TRT:** TRT 1, TRT 2, TRT Haber, TRT Spor, TRT Spor Yıldız, TRT Belgesel, TRT Çocuk, TRT Diyanet Çocuk, TRT Müzik, TRT Genç, TRT Türk, TRT Avaz, TRT Kurdî, TRT Arabi, TRT World, TRT EBA, TBMM TV
- **Özel kanallar:** Kanal D, Show TV, Star TV (adres kırılgan), TV8, Kanal 7, teve2, 360, NTV, Habertürk, TGRT Haber, 24 TV, TVNET, Bloomberg HT, HT Spor, Ekotürk, Show Türk, Dream Türk
- **Dikkat:** daioncdn altyapısındaki kanallar (Kanal D, Show, Star, TV8, NTV, Habertürk…) oturum bazlı adres veriyor. Bu yüzden her açılışta ana adres yeniden istenmeli, alt adresler saklanmamalı.

**2. grup — Resmi YouTube canlı yayını**
- A Haber, A Spor, A Para, CNN Türk, CNBC-e, Sözcü TV, TV100, Haber Global, Akit TV, Bengütürk, Ülke TV, Diyanet TV, Halk TV
- Canlı yayının video kimliği yayın her yeniden başladığında değişiyor. Doğru kimliği ücretsiz RSS akışı ve YouTube API ile buluruz.
- En kolay ve kurallara en uygun yol: kanalı TV'deki YouTube uygulamasında açtırmak.

**3. grup — Yalnızca web sayfası veya resmi uygulama**
- ATV, NOW (canlı yayını Disney+ abonelerine açık), Beyaz TV, DMAX, TLC, Minika
- Bu kanalları resmi canlı yayın sayfasını uygulama içinde tam ekran açarak oynatırız. Kutuda test edilmeli: otomatik oynatma, kumanda ile odak, reklam engelleyici uyarısı.
- **ATV için temiz bir doğrudan kaynak yok.** Bu, listedeki en zayıf nokta.

**Listeye alınmayanlar:**
- Ekol TV (22.12.2025'te kapandı)
- TELE1 (kayyum, haber yayını yok)
- Flash TV (satıldı, yeni yayın biçimi bekleniyor)
- TRT 4K (durduruldu)
- beIN, TOD ve diğer paralı kanallar
- Korsan spor yayınları

**Değişken durumlar:**
- **Halk TV** (kayyum 14.10.2026): TV'de yalnızca hazır kayıtlar dönüyor, gazetecilerin canlı yayını YouTube'da devam ediyor. Uygulamada hangisinin "Halk TV" olarak görüneceğine sizin karar vermeniz gerekiyor.
- Show TV, Habertürk, Bloomberg HT ve HT Spor TMSF yönetiminde ama yayındalar.
- **Spor maçları:** Ücretsiz web yayınlarında lisanslı maçlarda yayın kesilebilir veya "bu program internetten yayınlanmıyor" ekranı çıkabilir. Bunun için ayrı bir bilgi mesajı tasarlanacak.

---

## 4. UYGULAMA ADIMLARI

### Adım 1 — Cihazı tanıma (½ gün)
1. Ayarlar > Sistem > Hakkında: modelin **MiTV-AFKR0** olduğunu ve **Android TV OS sürümünü** (11 mi, 14 mü?) not edin.
2. Aynı ekranda **"Android TV OS derlemesi"** satırına 7 kez OK'a basın: Geliştirici seçenekleri açılır.
3. Kutuyu **5 GHz Wi-Fi**'a bağlayın. 2,4 GHz canlı yayında takılmaya yol açar. Kutunun Ethernet girişi yok; ileride USB-Ethernet adaptörü düşünülebilir.
4. Tanı ekranı ile kumandanın gerçek tuş kodlarını kaydedin. Bu, uygulamanın ilk sürümünde gelecek.

### Adım 2 — Kanal listesi ve kaynaklar (1–2 gün)
1. GitHub'da (özel veya açık) bir depo açılır ve `channels.json` şeması yazılır. Her kanal için şu alanlar bulunur:
   - id, numara, ad, logo, kategori
   - kaynaklar: tür hls / youtube / webview / harici uygulama; adres; gerekirse Referer veya User-Agent başlığı; öncelik
   - etkin mi, not
2. §3'teki kanallar listeye girer ve hepsine sabit kanal numarası verilir. Yaşlı kullanıcılar kanalları numarasıyla hatırlar.
3. Kanal logoları toplanır ve küçük WebP dosyaları olarak hazırlanır.
4. Sizin vereceğiniz kararlar: Halk TV hangi kaynaktan, kanal numaralandırması, hangi kanallar varsayılan favori olacak.

### Adım 3 — Tasarım (sizin tarafınız, ~1 hafta)
1. TV Design Kit'i çoğaltın, renk/yazı/boşluk değerlerini Figma değişkenleri olarak tanımlayın.
2. §1-A'daki ekranları tüm durumlarıyla çizin (§1-C).
3. Ekranlar arası akışı Figma prototipiyle bağlayın.
4. **Anne-babanızla 10 dakikalık bir deneme yapın.** Görevler:
   - uygulamayı açmak
   - kanal değiştirmek
   - bir haber kanalı bulmak
   - favori eklemek
   - kanal listesine dönmek
   - uygulamadan çıkmak

   Nerede takıldıklarını not edin. Özellikle bakılacak soru: filtre çipe **odaklanınca mı** uygulansın, **OK'a basınca mı**?

### Adım 4 — Proje kurulumu (½ gün, ben yaparım)
| Bileşen | Sürüm |
|---------|-------|
| Android Studio | Rabbit 1 (2026.2.1) |
| Kotlin / AGP / Gradle | 2.4.21 / 9.4.0 / 9.6+ |
| Compose BOM | 2026.09.00 |
| Compose for TV | `androidx.tv:tv-material` **1.1.0** (Leanback kullanılmayacak, mobil material3 ile karıştırılmayacak) |
| Video oynatıcı | Media3 / ExoPlayer **1.11.1** (hls + ui-compose + datasource-okhttp) |
| Diğer | OkHttp 5.5.0, Coil 3.6.3, DataStore 1.2.1, kotlinx-serialization 1.11.0, core-splashscreen |
| SDK | minSdk 23 · targetSdk 36 · compileSdk 37 |

Manifest'te bulunacaklar:
- **LEANBACK_LAUNCHER** (bu olmazsa uygulama TV ana ekranında hiç görünmez)
- içinde Türkçe uygulama adı yazan **320×180 banner**
- dokunmatik ekran ve leanback özellikleri `required=false`
- yatay ekran yönü
- yalnızca http kullanan yayın adresleri için izin listesi (cleartext)

### Adım 5 — Geliştirme sırası (2–3 hafta)
1. **Veri katmanı:**
   - `channels.json` uzaktan çekilir ve cihazda saklanır; APK içinde bir yedek kopya bulunur.
   - Favoriler, son kanal ve ayarlar DataStore'da tutulur.
2. **Ana Ekran:** filtre çipleri, kanal ızgarası, "Kaldığın yerden" kartı, odağın geri gelmesi.
3. **Oynatıcı:**
   - Tek bir ExoPlayer örneği kullanılır. ▲▼ ile kanal değişince yeni oynatıcı açılmaz, aynısı yeni kanala geçer.
   - Kanala özel HTTP başlıkları gönderilir.
   - Hata olursa önce 3 deneme yapılır, sonra yedek kaynağa geçilir, en son anlaşılır bir hata mesajı gösterilir.
   - Canlı yayının gerisinde kalınırsa yayının en güncel noktasına atlanır.
   - Oynatıcı kapanınca bellek boşaltılır.
4. **Katmanlar:** bilgi bandı, kanal listesi paneli, kanal seçenekleri.
5. **Durum ekranları:** yükleniyor, hata, internet yok, boş liste.
6. **Ayarlar** ve yazı boyutu çarpanı.
7. **YouTube ve web sayfası kaynakları:**
   - YouTube uygulamasını açtırma veya kurallara uygun gömülü oynatıcı
   - uygulama içinde web sayfası görünümü
8. **Görüntü akıcılığı:** Türk yayınları saniyede 25/50 kare, TV ise genelde 60 Hz çalışır; bu fark görüntüde takılma yapar. Oynatıcı açılırken ekran bir kez **50 Hz**'e alınır.
9. **Kumanda Rehberi**, açılış ekranı ve Tanı ekranı.

### Adım 6 — Test (1 hafta)
1. Android Studio'da **API 30 Google TV emülatörü** kullanılır. Bilgisayarda kumanda yerine ok tuşları ve Esc kullanılır.
2. Gerçek kutuda ADB ile Wi-Fi üzerinden test yapılır:
   - açılış süresi
   - kanal değiştirme hızı
   - bellek kullanımı
   - **4 saatlik kesintisiz izleme testi**
3. **Kutunun kendi kendine uykuya geçme durumu:** Android TV 11, kumandaya uzun süre basılmazsa video oynarken bile ekranı kapatır ve uygulama bunu engelleyemez. Bu yüzden kutunun Ayarlar > Sistem > Güç ve Enerji bölümündeki "ekranı kapat" süresi uzatılmalı.
4. Anne-babayla ikinci bir deneme yapılır, bu sefer gerçek uygulamayla.

### Adım 7 — APK oluşturma
1. **İmza anahtarı (keystore) bir kez oluşturulur ve 2 ayrı yerde yedeklenir.** Kaybolursa uygulamaya bir daha güncelleme yüklenemez.
2. Kod küçültme (R8) açılır, ardından `./gradlew assembleRelease` çalıştırılır. Çıktı tek dosyalık evrensel bir APK olur.
3. Her sürümde versionCode artırılır: 1.2.3 → 10203.

### Adım 8 — TV kutusuna yükleme
**Aile için kolay yol (Downloader):**
1. Play Store'dan **Downloader (AFTVnews)** uygulaması kurulur.
2. APK, kısa ve sabit bir adrese konur (ör. GitHub Releases).
3. Downloader'a bu adres yazılır, ardından "Bilinmeyen uygulamaları yükle" izni verilir.
4. Play Protect uyarısı çıkarsa **Ayrıntılar > Yine de yükle** seçilir.
5. Kurulumdan sonra APK dosyası Downloader'dan silinir; kutuda yer azdır.

**Geliştirici yolu (ADB):** USB hata ayıklama açılır, ardından:

```bash
adb connect <kutu-ip>:5555
```

```bash
adb install -r aile-tv.apk
```

Android TV 14'te "Kablosuz hata ayıklama" ile eşleştirme yapılır. Bağlantı kurulamazsa yedek yol USB-A↔USB-A kablodur.

**Kurulumdan sonra:**
1. Uygulama Google TV'de **"Uygulamalarınız" satırının en başına** taşınır. Böylece Home tuşu + tek OK ile uygulamaya dönülür.
2. İsteğe bağlı: Google TV **"Yalnızca uygulamalar" modu** reklam ve öneri kalabalığını kaldırır. Bunun bedeli: Google Asistan ve arama çalışmaz.
3. Hata ayıklama ayarı kapatılır.

### Adım 9 — Bakım ve güncellemeler
1. **Yayın sağlık kontrolü** Türkiye IP'sinden yapılmalı. GitHub sunucuları yurt dışında olduğu için yanlış "kapalı" sonucu verir. Bu yüzden kontrol kutunun kendisinde veya evdeki bir cihazda çalışır.
   - Kontrol sırasında alt yayın listesi okunur ve yayının gerçekten ilerlediğine bakılır. Sadece "200 OK" yanıtı yeterli sayılmaz; çünkü bazı adresler 2022'den kalma kaydı döndürüyor.
2. Haftada bir `channels.json` gözden geçirilir. Özellikle Halk TV, Flash TV ve TMSF'deki kanallar takip edilir.
3. **Kendi kendini güncelleme (2. sürüm):**
   - Uygulama `version.json` dosyasını kontrol eder, yeni sürüm varsa APK'yı indirir ve SHA-256 ile doğrular, sonra sistem kurulum ekranını açar.
   - Android 11'de ailenin bir kez **"Yükle"** butonuna basması gerekir.
4. **2027 notu:** Google 2027'de geliştirici doğrulamasını dünya geneline yayacak. Önceden ücretsiz bir "sınırlı dağıtım" hesabına (20 cihaza kadar) paket adını kaydetmek iyi olur. ADB ile yükleme her durumda serbest kalacak.

---

## 5. Sizden beklenen kararlar ve bilgiler
1. Kutunuzun Android TV sürümü: 11 mi, 14 mü?
2. Kutuda bir Google hesabı açık mı (Play Store çalışıyor mu)? Çalışmıyorsa Downloader yolu kullanılamaz; USB bellek veya ADB gerekir.
3. Anne-babanızın en çok izlediği 5–8 kanal. Bunlar varsayılan favoriler ve ilk kanal numaraları olacak.
4. Halk TV: TV yayını mı, YouTube yayını mı?
5. Tasarımı Figma'da mı yapacaksınız, ücretli Dev koltuğunuz var mı? (Figma MCP ile tasarımı doğrudan koda aktarabilmek için.)
6. Kart biçimi 1:1 mi, 16:9 mu? Filtre odaklanınca mı, OK ile mi uygulansın?
