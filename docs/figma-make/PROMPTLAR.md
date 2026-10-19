# Aile TV — Figma Make ile adım adım tasarım

Bu dosyadaki promptlarla Figma Make'te Aile TV'nin **kumandayla (klavyeyle) gezilebilen** etkileşimli prototipini kuracaksınız. Prototip hazır olunca bana göndereceksiniz; ben de aynı tasarımı Android TV uygulaması (APK) olarak kodlayacağım.

---

## Başlamadan önce

1. **Dosyayı açın:** Figma'da yeni bir **Figma Make** dosyası açın ve adını "Aile TV" koyun.
2. **Kuralları ekleyin:** Make'te **Code** sekmesini açın. `guidelines/Guidelines.md` dosyasının içeriğini bu klasördeki [Guidelines.md](Guidelines.md) dosyasının **tamamıyla** değiştirin.
   - Make bu dosyayı her promptta okur. Renkler, ölçüler ve odak kuralları buradan gelir.
   - Dosya yoksa ilk promptun başına şunu ekleyin: "Önce aşağıdaki metni `guidelines/Guidelines.md` olarak kaydet:", ardından Guidelines.md'nin içeriğini yapıştırın.
3. **Promptları sırayla, birer birer gönderin.**
   - Her prompttan sonra önizlemeye tıklayın ve klavyeyle deneyin: ok tuşları = ▲▼◀▶, **Enter** = OK, **Esc** = Geri.
   - Her promptun sonundaki kontrol listesine bakın. Sorun varsa bir sonrakine geçmeden düzeltin.
4. **Düzeltmeleri küçük tutun.**
   - Tek öğe için point-and-edit kullanın (önizlemede öğeyi seçip yazın).
   - Kısa bir düzeltme promptu da yazabilirsiniz, ör. "Ana ekranda çip sırası 8 px aşağı insin".
   - Bir şey bozulursa sürüm geçmişinden önceki sürüme dönün.
5. **Yardımcı tuşlar:**
   - **T** tuşu Tasarım Panelini açar: yazı boyutu, kart biçimi ve hata/internet simülasyonları.
   - **G** tuşu Ekran Galerisini açar: tüm ekranlar ve durumlar.

> Büyük bir isteği tek promptta yazmayın. Make her adımda tek bir ekran grubuna odaklandığında çok daha iyi sonuç veriyor.

---

## Prompt 1 — Altyapı ve bileşen vitrini

````text
Guidelines.md'deki kurallara birebir uyarak "Aile TV" adlı Google TV uygulamasının etkileşimli prototipinin ALTYAPISINI kur. Bu adımda uygulama ekranı yapma; sadece altyapıyı ve bir bileşen vitrinini kur.

1. TV sahnesi: 960×540 px sabit #tv-stage. Pencereye orantılı sığdır ve ortala; dışı #000. Pencere boyutu değişince yeniden ölçekle.
2. Tasarım token'ları: Guidelines §3 renkleri ve §4 yazı ölçeği CSS değişkenleri olsun. Roboto'yu Google Fonts'tan yükle. Yazı çarpanı --ts (1 / 1.25 / 1.5) bütün yazı boyutlarına ve yazıya bağlı yüksekliklere uygulansın.
3. Kumanda katmanı: Guidelines §6'daki tuşları tek yerden dinleyen bir mekanizma kur. Tuş adları: up, down, left, right, ok, back, chUp, chDown. Tarayıcının ok tuşu kaydırmasını ve Tab odağını engelle.
4. Sanal odak sistemi:
   - Ekranlar "odak bölgelerinden" oluşur (yatay sıra, dikey liste, ızgara). Her bölge kendi içindeki yön tuşu davranışını ve hangi yönde hangi bölgeye geçileceğini tanımlar.
   - Bir bölgeye geri dönülünce son odaklı öğe hatırlanır.
   - Odaklı öğe görünür alanda kalacak şekilde kapsayıcısı kayar (kaydırma çubuğu yok, 150 ms geçiş).
   - Fareyle tıklama = odakla + OK.
5. Uygulama durumu ve localStorage: Guidelines §11.
6. Hash yönlendirme: Guidelines §11'deki rotalar. Henüz yapılmamış ekranlar için "Bu ekran sonraki adımda yapılacak" yazan sade bir yer tutucu göster. Bilinmeyen rota → #ana.
7. Kanal verisi: aşağıdaki JSON'u src/data/channels.ts dosyasına koy. Ayrıca şu yardımcıları ekle:
   - kategori listesi
   - filtreye göre kanallar ("favoriler" = favori listesi, "tumu" = hepsi)
   - aktif filtre içinde sonraki ve önceki kanal (sarmalı)
   - tr-TR duyarlı arama (İ/i/ı/I ve ç ş ğ ö ü harflerini doğru eşleştirsin)
8. Tasarım Paneli (T tuşu): sahnenin sağ üst köşesinde, uygulamanın parçası olmayan küçük, sade, açık renkli bir geliştirici paneli. Fareyle kullanılır. Ayarlar:
   - Yazı boyutu: Normal 1 / Büyük 1.25 / Çok büyük 1.5
   - Kart biçimi: 16:9 / 1:1
   - Filtre uygulama: Odaklanınca / OK'a basınca
   - Yayın hatası simülasyonu: kapalı / açık
   - Program yayınlanmıyor simülasyonu: kapalı / açık
   - İnternet yok simülasyonu: kapalı / açık
   - Favorileri sıfırla
   - Kumanda rehberini tekrar göster
9. Ekran Galerisi (G tuşu): sahnenin üstünde açılan, tüm rotaların Türkçe adlarıyla listelendiği sade bir panel. Bir satıra tıklayınca o ekrana gider.
10. Bileşen vitrini (#vitrin, şimdilik başlangıç ekranı): Guidelines §7'deki her bileşeni normal / odaklı / seçili / pasif hâlleriyle göster. Ok tuşlarıyla aralarında gezilebilsin; odak görünümü canlı değişsin. Vitrinde şunlar olsun:
   - 5 kanal kartı: TRT 1, Kanal D, Show TV, A Haber, TRT Belgesel
   - 1 devam kartı
   - 9 çip: Favoriler, Tümü, Genel, Haber, Spor, Çocuk, Belgesel, Müzik, Diğer ("Tümü" seçili)
   - 3 buton
   - 4 liste satırı
   - 1 ayar satırı ve 1 switch
   - KeyHint örnekleri, toast ve yükleme halkası

Kanal verisi (source: hls = uygulama içinde, youtube = resmi YouTube yayını, web = resmi internet sayfası; defaultFavorite = ilk açılıştaki favoriler):

```json
{
  "categories": [
    { "id": "favoriler", "label": "Favoriler" },
    { "id": "tumu", "label": "Tümü" },
    { "id": "genel", "label": "Genel" },
    { "id": "haber", "label": "Haber" },
    { "id": "spor", "label": "Spor" },
    { "id": "cocuk", "label": "Çocuk" },
    { "id": "belgesel", "label": "Belgesel" },
    { "id": "muzik", "label": "Müzik" },
    { "id": "diger", "label": "Diğer" }
  ],
  "channels": [
    { "id": "trt1", "number": 1, "name": "TRT 1", "category": "genel", "source": "hls", "defaultFavorite": true },
    { "id": "atv", "number": 2, "name": "ATV", "category": "genel", "source": "web", "defaultFavorite": true },
    { "id": "kanald", "number": 3, "name": "Kanal D", "category": "genel", "source": "hls", "defaultFavorite": true },
    { "id": "showtv", "number": 4, "name": "Show TV", "category": "genel", "source": "hls", "defaultFavorite": true },
    { "id": "startv", "number": 5, "name": "Star TV", "category": "genel", "source": "hls", "defaultFavorite": true },
    { "id": "now", "number": 6, "name": "NOW", "category": "genel", "source": "web", "defaultFavorite": false },
    { "id": "tv8", "number": 7, "name": "TV8", "category": "genel", "source": "hls", "defaultFavorite": true },
    { "id": "kanal7", "number": 8, "name": "Kanal 7", "category": "genel", "source": "hls", "defaultFavorite": false },
    { "id": "teve2", "number": 9, "name": "teve2", "category": "genel", "source": "hls", "defaultFavorite": false },
    { "id": "beyaztv", "number": 10, "name": "Beyaz TV", "category": "genel", "source": "web", "defaultFavorite": false },
    { "id": "tv360", "number": 11, "name": "360", "category": "genel", "source": "hls", "defaultFavorite": false },
    { "id": "trt2", "number": 12, "name": "TRT 2", "category": "genel", "source": "hls", "defaultFavorite": false },
    { "id": "trthaber", "number": 13, "name": "TRT Haber", "category": "haber", "source": "hls", "defaultFavorite": true },
    { "id": "ntv", "number": 14, "name": "NTV", "category": "haber", "source": "hls", "defaultFavorite": false },
    { "id": "cnnturk", "number": 15, "name": "CNN Türk", "category": "haber", "source": "youtube", "defaultFavorite": false },
    { "id": "haberturk", "number": 16, "name": "Habertürk", "category": "haber", "source": "hls", "defaultFavorite": false },
    { "id": "ahaber", "number": 17, "name": "A Haber", "category": "haber", "source": "youtube", "defaultFavorite": false },
    { "id": "sozcutv", "number": 18, "name": "Sözcü TV", "category": "haber", "source": "youtube", "defaultFavorite": false },
    { "id": "tv100", "number": 19, "name": "TV100", "category": "haber", "source": "youtube", "defaultFavorite": false },
    { "id": "haberglobal", "number": 20, "name": "Haber Global", "category": "haber", "source": "youtube", "defaultFavorite": false },
    { "id": "tgrthaber", "number": 21, "name": "TGRT Haber", "category": "haber", "source": "hls", "defaultFavorite": false },
    { "id": "tv24", "number": 22, "name": "24 TV", "category": "haber", "source": "hls", "defaultFavorite": false },
    { "id": "tvnet", "number": 23, "name": "TVNET", "category": "haber", "source": "hls", "defaultFavorite": false },
    { "id": "halktv", "number": 24, "name": "Halk TV", "category": "haber", "source": "youtube", "defaultFavorite": false },
    { "id": "ulketv", "number": 25, "name": "Ülke TV", "category": "haber", "source": "youtube", "defaultFavorite": false },
    { "id": "akittv", "number": 26, "name": "Akit TV", "category": "haber", "source": "youtube", "defaultFavorite": false },
    { "id": "benguturk", "number": 27, "name": "Bengütürk", "category": "haber", "source": "youtube", "defaultFavorite": false },
    { "id": "bloomberght", "number": 28, "name": "Bloomberg HT", "category": "haber", "source": "hls", "defaultFavorite": false },
    { "id": "apara", "number": 29, "name": "A Para", "category": "haber", "source": "youtube", "defaultFavorite": false },
    { "id": "ekoturk", "number": 30, "name": "Ekotürk", "category": "haber", "source": "hls", "defaultFavorite": false },
    { "id": "cnbce", "number": 31, "name": "CNBC-e", "category": "haber", "source": "youtube", "defaultFavorite": false },
    { "id": "trtspor", "number": 32, "name": "TRT Spor", "category": "spor", "source": "hls", "defaultFavorite": false },
    { "id": "trtsporyildiz", "number": 33, "name": "TRT Spor Yıldız", "category": "spor", "source": "hls", "defaultFavorite": false },
    { "id": "aspor", "number": 34, "name": "A Spor", "category": "spor", "source": "youtube", "defaultFavorite": false },
    { "id": "htspor", "number": 35, "name": "HT Spor", "category": "spor", "source": "hls", "defaultFavorite": false },
    { "id": "trtcocuk", "number": 36, "name": "TRT Çocuk", "category": "cocuk", "source": "hls", "defaultFavorite": false },
    { "id": "trtdiyanetcocuk", "number": 37, "name": "TRT Diyanet Çocuk", "category": "cocuk", "source": "hls", "defaultFavorite": false },
    { "id": "minikacocuk", "number": 38, "name": "Minika Çocuk", "category": "cocuk", "source": "web", "defaultFavorite": false },
    { "id": "minikago", "number": 39, "name": "Minika GO", "category": "cocuk", "source": "web", "defaultFavorite": false },
    { "id": "trtbelgesel", "number": 40, "name": "TRT Belgesel", "category": "belgesel", "source": "hls", "defaultFavorite": false },
    { "id": "dmax", "number": 41, "name": "DMAX", "category": "belgesel", "source": "web", "defaultFavorite": false },
    { "id": "tlc", "number": 42, "name": "TLC", "category": "belgesel", "source": "web", "defaultFavorite": false },
    { "id": "tgrtbelgesel", "number": 43, "name": "TGRT Belgesel", "category": "belgesel", "source": "hls", "defaultFavorite": false },
    { "id": "trtmuzik", "number": 44, "name": "TRT Müzik", "category": "muzik", "source": "hls", "defaultFavorite": false },
    { "id": "dreamturk", "number": 45, "name": "Dream Türk", "category": "muzik", "source": "hls", "defaultFavorite": false },
    { "id": "trtgenc", "number": 46, "name": "TRT Genç", "category": "genel", "source": "hls", "defaultFavorite": false },
    { "id": "diyanettv", "number": 47, "name": "Diyanet TV", "category": "diger", "source": "youtube", "defaultFavorite": false },
    { "id": "tbmmtv", "number": 48, "name": "TBMM TV", "category": "diger", "source": "hls", "defaultFavorite": false },
    { "id": "trtturk", "number": 49, "name": "TRT Türk", "category": "diger", "source": "hls", "defaultFavorite": false },
    { "id": "trtavaz", "number": 50, "name": "TRT Avaz", "category": "diger", "source": "hls", "defaultFavorite": false },
    { "id": "trtkurdi", "number": 51, "name": "TRT Kurdî", "category": "diger", "source": "hls", "defaultFavorite": false },
    { "id": "trtarabi", "number": 52, "name": "TRT Arabi", "category": "diger", "source": "hls", "defaultFavorite": false },
    { "id": "trtworld", "number": 53, "name": "TRT World", "category": "diger", "source": "hls", "defaultFavorite": false },
    { "id": "trteba", "number": 54, "name": "TRT EBA", "category": "diger", "source": "hls", "defaultFavorite": false },
    { "id": "showturk", "number": 55, "name": "Show Türk", "category": "diger", "source": "hls", "defaultFavorite": false }
  ]
}
```
````

**Kontrol:**
- [ ] Sahne her pencere boyutunda 16:9 kalıyor ve ortada duruyor.
- [ ] Vitrinde ok tuşları odağı taşıyor. Odaklı buton ve çip beyazlaşıp büyüyor; kartta beyaz çerçeve var.
- [ ] Seçili "Tümü" çipi ✓ ile odaklı çipten ayırt edilebiliyor.
- [ ] T tuşunda yazı boyutu değişince vitrindeki her şey birlikte büyüyor.
- [ ] Sayfada kaydırma çubuğu yok; ok tuşları sayfayı kaydırmıyor.

---

## Prompt 2 — Açılış, kumanda rehberi, Google TV yer tutucusu

````text
Guidelines'a uyarak 3 ekran yap. Başlangıç rotasını #acilis yap (vitrin galeride kalsın).

1) AÇILIŞ (#acilis) — odak yok, tuşlar etkisiz.
- Ortada büyük LogoMark (72 px), altında 16 px boşlukla "Aile TV" (headline).
- 1 saniyeden uzun sürerse alt ortada (alttan 72 px) küçük yükleme halkası (24 px) ve yanında "Kanallar hazırlanıyor…" (body, --text-2) görünsün.
- 1,2 saniye sonra şu sırayla yönlendir:
  - rehber daha önce görülmediyse → #kumanda-rehberi
  - "Açılışta son kanalı başlat" açıksa → #oynatici/<sonKanal> (oynatıcıdan Geri → #ana)
  - değilse → #ana
- Açılış ekranı geri geçmişinde tutulmaz.

2) KUMANDA REHBERİ (#kumanda-rehberi) — ilk açılışta bir kez gösterilir; Ayarlar'dan tekrar açılabilir.
- Sol sütun (x 58–420): sade bir kumanda çizimi (SVG). 150×380 yuvarlatılmış dikdörtgen gövde.
  - Üstte yön halkası ve ortasında OK düğmesi.
  - Altında yan yana Geri ve Ana sayfa düğmeleri.
  - Daha aşağıda 4 adet isimsiz, soluk hap şeklinde kısayol düğmesi. Marka logosu çizme.
  - Yön halkası, OK ve Geri düğmeleri --accent renkli 2 px halkayla vurgulu olsun.
- Sağ sütun (x 460–902):
  - Başlık "Kumandada bu tuşlar yeter" (headline).
  - Altında "Aile TV'yi yalnızca yön tuşları, OK ve Geri ile kullanırsınız." (body, --text-2).
  - 4 satır: her satırda 40×40 tuş rozeti, yanında başlık (18 px 500) ve açıklama (body-m, --text-2). Satır aralığı 16.
    - [▲▼] "Kanal değiştir" — "Yayın izlerken önceki veya sonraki kanala geçer."
    - [OK] "Seç ve kanal listesi" — "Seçtiğinizi açar. Yayın sırasında kanal listesini gösterir."
    - [◀▶] "Menüde gezin" — "Seçenekler arasında sağa ve sola gider."
    - [Geri ikonu] "Geri" — "Bir önceki ekrana döner. Arka arkaya basınca ana ekrana çıkarsınız."
  - En altta buton "Anladım, başla" (açılış odağı, sol kenarı yazılarla hizalı).
- OK veya Geri → rehber görüldü kaydedilsin → açılıştaki yönlendirme kuralına göre devam.

3) GOOGLE TV YER TUTUCUSU (#google-tv) — uygulamadan çıkıldığında görülen ekran (Google TV'nin kendi ekranını taklit etme).
- --bg zemin, ortada "Google TV ana ekranı" (title-l, --text-2) ve altında "Aile TV'den çıktınız." (body, --text-2).
- Altında buton "Aile TV'yi aç" (odaklı) → #acilis.
````

**Kontrol:**
- [ ] İlk açılışta rehber geliyor; ikinci açılışta gelmiyor (T paneli > "Kumanda rehberini tekrar göster" ile tekrar geliyor).
- [ ] Rehberdeki kumanda çiziminde marka logosu yok; vurgulanan tuşlar açıkça seçiliyor.
- [ ] "Anladım, başla" açılışta odaklı.

---

## Prompt 3 — Ana ekran

````text
Guidelines'a uyarak ANA EKRAN'ı (#ana) yap. Ölçüler yazı boyutu 1 ve 16:9 kart içindir. Yazı büyüyünce yükseklikler çarpanla büyüsün, ızgara sütun sayısı aşağıdaki kurala göre değişsin.

YERLEŞİM
- Üst bar (top 28, yükseklik 40):
  - Solda LogoMark 32, 12 px boşluk, "Aile TV" (title-l).
  - Sağda, aralarında 12 px boşlukla: buton "Favorileri düzenle" (Star ikonu), buton "Ayarlar" (SlidersHorizontal ikonu), canlı saat (title-m, --text-2, solundan 8 px fazla boşluk).
- Devam kartı (top 88): ResumeCard. Son izlenen kanalı gösterir.
- Filtre çipleri (top 216): 9 kategori. Sığmazsa (büyük yazıda) sıra yatay kayar ve odaklı çip hep görünür kalır.
- Kanal ızgarası (top 280, genişlik 844, sütun ve satır aralığı 20):
  - 16:9 kartlarda yazı boyutu 1 / 1.25 / 1.5 için 5 / 4 / 3 sütun.
  - 1:1 kartlarda 6 / 5 / 4 sütun.
  - Alt kenarda bir sonraki satır yarım görünsün (devamı olduğu anlaşılsın).

KAYDIRMA
- Odak ızgaranın 2. satırına veya daha aşağıya inince:
  - üst bar ve devam kartı yukarı kayarak gizlenir,
  - çip sırası en üste (top 28) yapışır,
  - ızgara, odaklı satırın bir üstündeki satırdan başlayarak gösterilir.
- Odak ilk satıra dönünce sayfa eski hâline gelir. Geçişler 150 ms.

GEZİNME
- Üst barda ◀▶ butonlar arasında gezer.
- ▼ yönü: üst bar → devam kartı → seçili çip → ızgaranın ilk kartı.
- ▲ yönü: ızgaranın ilk satırı → seçili çip → devam kartı → "Favorileri düzenle".
- Izgarada sarma yok: satır sonunda ▶, satır başında ◀ ve son satırda ▼ hiçbir şey yapmaz.

FİLTRE
- Tasarım Panelindeki ayara göre çip ya odaklanınca (300 ms gecikmeyle) ya da OK'a basınca uygulanır.
- Filtre değişince ızgara başa döner.
- Aktif filtre kaydedilir ve oynatıcıdaki kanal değiştirme de bu filtre içinde çalışır.

OK
- Devam kartı → son kanal, oynatıcıda açılır.
- Kanal kartı → o kanal oynatıcıda açılır (son kanal güncellenir). Oynatıcı ekranı bir sonraki adımda yapılacak; şimdilik yer tutucu yeterli.
- "Ayarlar" → #ayarlar.
- "Favorileri düzenle" → favori modu (sonraki adım).

GERİ
- Izgaradayken → sayfa başa kayar, odak seçili çipe gider.
- Çip, devam kartı veya üst bardayken → #google-tv.

ODAK
- Açılış odağı devam kartıdır.
- Oynatıcıdan dönünce odak izlenen kanalın kartına gelir. Kanal o filtrede yoksa ilk karta gelir.
````

**Kontrol:**
- [ ] İlk görünüm: üst bar, devam kartı (odaklı, beyaz çerçeveli), çipler ("Tümü" ✓), 2 satır kart (ikincisi yarım).
- [ ] ▼ ile kartlara inip 2. satıra geçince üst kısım kayboluyor, çipler yukarı yapışıyor.
- [ ] Geri tuşu ızgaradan çiplere, çiplerden Google TV ekranına götürüyor.
- [ ] T panelinde "Çok büyük" yazı seçilince 3 sütun oluyor ve hiçbir yazı kesilmiyor.
- [ ] Favori kanallarda sarı yıldız rozeti var.

---

## Prompt 4 — Favori düzenleme modu, boş durum, bildirim (toast)

````text
Guidelines'a uyarak ana ekrana 3 parça ekle.

1) FAVORİ DÜZENLEME MODU (#ana?mod=favori) — "Favorileri düzenle" butonuyla açılır.
- Üst bar değişir:
  - Solda Star ikonu (22) ve "Favorileri düzenle" (title-l).
  - Ortada KeyHint'ler: [OK] "★ ekle veya çıkar" · [Geri] "Bitti".
  - Sağda buton "Bitti".
- Devam kartı gizlenir. Çipler ve ızgara kalır (çipler top 88, ızgara top 152).
- Her kartın sağ üstünde 28 px yuvarlak yıldız düğmesi var:
  - favori değilse içi boş yıldız, kenarı --key-outline,
  - favoriyse dolu sarı yıldız.
- Açılış odağı ızgaradaki ilk karttır.
- Bu modda OK kartın favori durumunu değiştirir (oynatıcı AÇMAZ) ve toast gösterir: "Favorilere eklendi" (Star) veya "Favorilerden çıkarıldı" (StarOff).
- Geri veya "Bitti" moddan çıkar; odak "Favorileri düzenle" butonuna döner.

2) BOŞ DURUM — "Favoriler" filtresi seçili ve hiç favori yoksa ızgara yerine gösterilir.
- Izgara alanında ortalı, 520 px genişlikte bir blok:
  - içi boş yıldız ikonu (40, --text-2)
  - "Henüz favori kanalınız yok." (title-l)
  - "Sık izlediğiniz kanalları favorilere ekleyin. Kanal değiştirirken yalnızca onlar arasında dolaşırsınız." (body, --text-2, en fazla 2 satır)
  - altında yan yana iki buton: "Tüm kanallara git" (odak buraya gelir; "Tümü" filtresini seçer) ve "Favorileri düzenle"
- ▲ ile çiplere çıkılır.

3) TOAST — Guidelines §7: alt ortada, 2 saniye. Uygulamanın her yerinde kullanılabilen tek bir bileşen olsun.
````

**Kontrol:**
- [ ] Düzenleme modunda OK kanalı açmıyor, yalnızca yıldızı değiştiriyor ve toast çıkıyor.
- [ ] Tüm favoriler çıkarılıp "Favoriler" çipine gelince boş durum görünüyor.
- [ ] Geri moddan çıkıyor, odak "Favorileri düzenle"de.

---

## Prompt 5 — Oynatıcı, bilgi bandı, kanal değiştirme

````text
Guidelines'a uyarak OYNATICI (#oynatici/<kanalId>) ve BİLGİ BANDI'nı yap.

OYNATICI
- Tam ekran VideoPlaceholder. Ekranda kalıcı arayüz yok; katmanlar tuşa basınca görünür.
- Kanal açılırken önce ~1 saniye "açılıyor" durumu (şimdilik ortada yükleme halkası), sonra yayın.
- Kanal değiştirme:
  - ▲ = sonraki kanal, ▼ = önceki kanal. Ayarlarda "▲ önceki kanal" seçiliyse yönler ters çalışır.
  - PageUp/PageDown = CH+/CH−.
  - Kanal değiştirme aktif filtre içinde yapılır ve sona gelince başa döner.
- Hızlı ardışık basışlarda bant hemen yeni kanalı gösterir; kanal ancak 400 ms tuşa basılmayınca açılır.
- OK veya ◀ → Kanal Listesi Paneli (sonraki adım; şimdilik yer tutucu).
- ▶ → bilgi bandını göster.
- Geri → bant açıksa kapatır; değilse #ana'ya döner (odak izlenen kanalın kartında).
- Son kanal her açılışta kaydedilir.

BİLGİ BANDI (InfoBanner, #oynatici/<id>?katman=bant)
- Her kanal değişiminde ve ▶ tuşunda görünür. Ayardaki süre dolunca (varsayılan 5 sn; 3 / 5 / 10) kaybolur. Bant açıkken ▲▼ kanal değiştirmeye devam eder.
- Üst satır:
  - Solda 112×63 logo alanı (kanal adı logo yer tutucu).
  - Ortada, aynı satırda numara (title-m, --text-2) ve 12 px boşlukla kanal adı (display).
  - Onların altında rozetler: kategori adı ve "CANLI". Rozetler: yükseklik 28, padding 0 12, radius 999, zemin --surface-2, label yazı; "CANLI" rozetinin solunda 8 px --danger renkli nokta.
  - En sağda saat (title-l) ve kanal favoriyse 24 px dolu yıldız.
- Alt satır KeyHint'ler: [▲▼] "Kanal değiştir" · [OK] "Kanal listesi" · [▶] "Bilgi" · [Geri] "Ana ekran".
- Program bilgisi YOK (ilk sürümde program rehberi yok). Program adı uydurma.
````

**Kontrol:**
- [ ] Ana ekranda bir karta OK → oynatıcı açılıyor, bant 5 sn görünüp kayboluyor.
- [ ] ▲▼ kanal değiştiriyor; "Haber" filtresindeyken yalnızca haber kanalları arasında dönüyor.
- [ ] ▲'ya hızlıca 4 kez basınca bant numaraları hızla değişiyor, sonra tek seferde açılıyor.
- [ ] Geri ana ekrana dönüyor ve odak izlenen kanalın kartında.

---

## Prompt 6 — Kanal listesi paneli ve kanal seçenekleri

````text
Guidelines'a uyarak oynatıcıya 2 katman ekle.

1) KANAL LİSTESİ PANELİ (#oynatici/<id>?katman=liste) — OK veya ◀ ile açılır. Video arkada oynamaya devam eder.
- SidePanel (solda, 400 px). Panelin sağında kalan video %45 karartılır.
- Panelin üstünde "Kanallar" (title-l).
- Altında tam genişlikte, 44 px yüksekliğinde bir kategori seçici: ChevronLeft + "Tümü · 55 kanal" + ChevronRight. Normal zemini --surface-2; odaklanınca beyaz olur.
- Liste: ListRow'lar, aralarında 6 px.
  - Yaklaşık 7 satır görünür; odaklı satır görünür alanın ortasına yakın tutulur.
  - Çalan kanalın satırında Play ikonu ve "Şu an" yazısı; favorilerde yıldız.
- Panelin en altında KeyHint'ler: [OK] "İzle" · [▶] "Seçenekler" · [Geri] "Kapat".
- Gezinme:
  - Açılış odağı çalan kanalın satırıdır.
  - ▲▼ listede gezer (kanal değiştirmez).
  - Listenin en üstündeyken ▲ veya ◀ → kategori seçici. Seçicide ◀▶ kategori değiştirir (liste ve kanal sayısı güncellenir); ▼ → listenin ilk satırı.
  - Listede ▶ → o satır için Kanal Seçenekleri.
  - OK → o kanala geç ve paneli kapat.
  - Geri → paneli kapat.
  - 30 saniye tuşa basılmazsa panel kendiliğinden kapanır.
- Panelde seçilen kategori, oynatıcının aktif filtresi olur.

2) KANAL SEÇENEKLERİ (#oynatici/<id>?katman=secenekler) — Dialog (440 px), arkasında --scrim.
- Üst kısımda 96×54 logo alanı, yanında "3 · Kanal D" (title-l) ve altında kategori adı (body-m, --text-2).
- Diyalog butonları (dikey): 
  - "Favorilere ekle" (Star) veya "Favorilerden çıkar" (StarOff)
  - "Yayını yeniden yükle" (RefreshCw)
  - "Kapat" (X)
- Açılış odağı ilk butondur.
- OK işlemi yapar, diyaloğu kapatır ve toast gösterir ("Favorilere eklendi" / "Favorilerden çıkarıldı" / "Yayın yeniden yükleniyor").
- Geri hiçbir şeyi değiştirmeden kapatır.
````

**Kontrol:**
- [ ] Panel açılınca odak çalan kanalda; ▲▼ kanalı değiştirmiyor, OK değiştiriyor.
- [ ] Kategori seçicide ◀▶ ile "Spor"a geçince liste ve sayı değişiyor.
- [ ] ▶ ile seçenekler açılıyor; Geri önce diyaloğu, sonra paneli kapatıyor.

---

## Prompt 7 — Yayın durumları ve "İnternet yok"

````text
Guidelines'a uyarak oynatıcının durum katmanlarını ve internet yok ekranını yap. Tüm durumlarda ▲▼ kanal değiştirmeye devam eder.

1) YAYIN AÇILIYOR (katman=aciliyor) — --video-bg zemin üstünde ortada:
- 160×90 logo alanı
- altında "1 · TRT 1" (title-l)
- altında 24 px yükleme halkası ve "Yayın açılıyor…" (body, --text-2)
- OK → kanal listesi, Geri → ana ekran.
- Normalde 1 sn sonra yayın başlar. Tasarım Panelinde "Yayın hatası" açıksa 2 sn sonra Yayın Açılamadı durumuna geçer.

2) YAYIN AÇILAMADI (katman=hata) — ortada 520 px genişlikte kart (zemin --surface-dialog, radius 24, padding 28):
- AlertCircle (40, --danger)
- "Star TV şu anda açılamıyor." (title-l; kanal adı değişken)
- "Biraz sonra tekrar deneyin veya başka bir kanal seçin." (body, --text-2)
- Yan yana butonlar: "Tekrar dene" (RefreshCw, açılış odağı), "Sonraki kanal" (ChevronUp), "Ana ekran" (Home). ◀▶ butonlar arasında gezer.

3) PROGRAM YAYINLANMIYOR (katman=program-yok; Tasarım Panelindeki simülasyonla tetiklenir) — aynı kart düzeni:
- Info ikonu (40, --accent)
- "Bu program internetten yayınlanmıyor." (title-l)
- "Yayıncı bu programı internette göstermiyor. Program bitince yayın kendiliğinden devam eder." (body, --text-2)
- Butonlar: "Başka kanal seç" (List → kanal listesi paneli) ve "Ana ekran" (Home).

4) HARİCİ KAYNAK (katman=harici) — kaynağı youtube veya web olan kanallarda yayından önce 1,5 saniye gösterilir. Tam ekran --bg (video yok), ortada:
- 160×90 logo alanı ve "17 · A Haber" (title-l)
- youtube için MonitorPlay ikonu (20) ve "Resmi YouTube yayınından açılıyor…" (body)
- web için Globe ikonu ve "Kanalın resmi internet sayfasından açılıyor…"
- 24 px yükleme halkası
- Sonra yayın başlar. Video yer tutucu metni: youtube için "[Resmi YouTube yayını · A Haber]", web için "[Resmi web yayını · ATV]".
- YouTube kanallarında bant, panel veya diyalog açılacaksa video yer tutucusu önce gizlenir (siyah zemin), katman onun üstüne açılır. Guidelines §11'deki YouTube kuralı.

5) İNTERNET YOK (#internet-yok) — Tasarım Panelinde "İnternet yok" açıkken hangi ekranda olunursa olsun bu ekran gelir; kapatılınca kaldığı yere döner.
- Ortada WifiOff (56, --text-2), altında "İnternet bağlantısı yok" (headline).
- 3 satırlık kontrol listesi (her satırda solda 18 px Check ikonu, body):
  - "Modem açık mı?"
  - "Televizyon kutusu Wi-Fi'a bağlı mı?"
  - "Modemi kapatıp 10 saniye sonra tekrar açmayı deneyin."
- Butonlar: "Tekrar dene" (RefreshCw, açılış odağı) ve "Ağ ayarlarını aç" (SlidersHorizontal; prototipte toast: "Televizyonun ağ ayarları açılır").
- Geri → #google-tv.
````

**Kontrol:**
- [ ] A Haber (youtube) açılınca önce harici kaynak ekranı, sonra yer tutucu geliyor; OK'a basınca önce video siyaha dönüyor, sonra panel açılıyor.
- [ ] "Yayın hatası" açıkken kanallar 2 sn sonra hata kartına düşüyor; ▲▼ hâlâ çalışıyor.
- [ ] "İnternet yok" açılınca her ekrandan bu ekrana geçiliyor, kapatınca geri dönülüyor.

---

## Prompt 8 — Ayarlar, seçim pencereleri, Hakkında, Tanı ekranı

````text
Guidelines'a uyarak 4 parça yap.

1) AYARLAR (#ayarlar) — ana ekrandaki "Ayarlar" butonundan açılır.
- Sol sütun (x 58, genişlik 300): "Ayarlar" (headline) ve altında "Değişiklikler hemen uygulanır." (body, --text-2).
- Sağ sütun (x 398, genişlik 504, top 28): SettingsRow listesi, satır aralığı 6. Yazı büyüyünce liste odağı takip ederek kayar.
  1. "Yazı boyutu" — değer "Normal" / "Büyük" / "Çok büyük" → seçim penceresi
  2. "Açılışta son kanalı başlat" — Switch (varsayılan açık); değer "Açık" / "Kapalı"
  3. "Kanal değiştirme yönü" — "▲ sonraki kanal" / "▲ önceki kanal" → seçim penceresi
  4. "Bilgi bandı süresi" — "3 saniye" / "5 saniye" / "10 saniye" → seçim penceresi
  5. "Kanal listesini güncelle" — değer "Son güncelleme: 18.10.2026" → OK'ta 1 sn yükleme, sonra toast "Kanal listesi güncel"
  6. "Kumanda rehberini göster" → #kumanda-rehberi (rehberden sonra ayarlara dön)
  7. "Hakkında" — değer "Sürüm 1.0.0" → #hakkinda
- Açılış odağı 1. satırdır. Geri → #ana (odak "Ayarlar" butonunda).

2) SEÇİM PENCERESİ (#ayarlar?pencere=yazi-boyutu | kanal-yonu | bant-suresi) — Dialog 480 px.
- Başlık ayarın adıdır.
- Seçenekler 52 px satırlar: solda radyo dairesi (seçiliyse dolu ve ✓), yanında seçenek adı.
- Yazı boyutu penceresinde seçeneklerin altında önizleme kutusu (zemin --surface-panel, radius 16, padding 16):
  - "1 · TRT 1" (title-l) ve "Kanal değiştir" (body)
  - odaklı seçeneğin boyutunda canlı gösterilir.
- Açılış odağı mevcut değerdir. ▲▼ gezer, OK seçip kapatır (uygulama hemen güncellenir), Geri değiştirmeden kapatır.

3) HAKKINDA (#hakkinda)
- Ortada 560 px genişlikte sütun: LogoMark 56 ve "Aile TV" (headline).
- Altında bilgi satırları; solda etiket (--text-2), sağda değer:
  - "Sürüm" — "1.0.0"
  - "Kanal listesi" — "18.10.2026 · 55 kanal"
  - "Yayınlar" — "Kanalların kendi resmi yayınları"
- En altta "Bu uygulama aile içi kullanım içindir. Kanal yayınları ve logoları ilgili yayıncılara aittir." (body-m, --text-2).
- Odaklanabilen tek öğe "Sürüm" satırıdır. Bu satıra 5 kez OK → #tani (her basışta bir şey gösterme).
- Geri → #ayarlar.

4) TANI EKRANI (#tani) — gizli, geliştirici için.
- Başlık "Tanı ekranı" (headline), yanında rozet "Geliştirici".
- Sol yarı "Son basılan tuşlar": son 8 tuşu tablo olarak göster. Sütunlar: saat, tuş, Android karşılığı.
  - ArrowUp → KEYCODE_DPAD_UP (19), ArrowDown → KEYCODE_DPAD_DOWN (20), ArrowLeft → KEYCODE_DPAD_LEFT (21), ArrowRight → KEYCODE_DPAD_RIGHT (22)
  - Enter → KEYCODE_DPAD_CENTER (23), Escape/Backspace → KEYCODE_BACK (4)
  - PageUp → KEYCODE_CHANNEL_UP (166), PageDown → KEYCODE_CHANNEL_DOWN (167)
  - Tablo canlı güncellensin.
- Sağ yarı "Cihaz": etiket–değer satırları, değerler yer tutucu:
  - "Android TV sürümü" — "[11 / 14]"
  - "İşlemci mimarisi" — "[armeabi-v7a]"
  - "Bellek" — "[— MB]"
  - "Ağ" — "[Wi-Fi 5 GHz]"
  - "Ekran" — "1920×1080 · [50 / 60 Hz]"
  - "Kanal listesi" — "18.10.2026"
- Geri → #hakkinda. Bu ekranda tuşlar sadece kaydedilir; Geri dışında hiçbir şey yapmaz.
````

**Kontrol:**
- [ ] Yazı boyutu "Çok büyük" seçilince tüm uygulama hemen büyüyor, ayarlar listesi kayarak sığıyor.
- [ ] Seçim penceresinde ▲▼ ile gezerken önizleme yazısı büyüyüp küçülüyor.
- [ ] Hakkında'da "Sürüm"e 5 kez Enter → Tanı ekranı; tuş tablosu doluyor.

---

## Prompt 9 — Sonraki sürüm ekranları (1/2)

````text
Guidelines'a uyarak 3 "sonraki sürüm" ekranı yap. Hepsi Ekran Galerisinden açılabilsin.

1) UYKU ZAMANLAYICI (#oynatici/<id>?katman=uyku) — oynatıcı üstünde seçim penceresi.
- Kanal Seçenekleri diyaloğuna "Uyku zamanlayıcı" (Moon) butonu ekle; ayarlar listesine de "Uyku zamanlayıcı" satırı ekle.
- Başlık "Uyku zamanlayıcı", açıklama "Seçtiğiniz süre dolunca yayın durur."
- Seçenekler: "Kapalı", "30 dakika", "60 dakika", "90 dakika", "120 dakika".
- Etkinken bilgi bandında kategori rozetinin yanında "Uyku: 45 dk" rozeti (Moon ikonlu) görünür.

2) HÂLÂ İZLİYOR MUSUNUZ? (#oynatici/<id>?katman=hala-izliyor) — Dialog.
- Clock ikonu (32, --accent).
- "Hâlâ izliyor musunuz?" (title-l).
- "Bir süredir kumandaya basılmadı. 60 saniye içinde yanıt vermezseniz yayın duracak." (body, --text-2).
- Geri sayım "0:58" (display) canlı azalır.
- Butonlar: "Evet, izlemeye devam" (açılış odağı) ve "Yayını kapat".
- Geri = devam et. Süre biterse #ana ve toast "Yayın durduruldu".
- Prototipte süre 60 saniye değil 15 saniye olsun.

3) EBEVEYN KİLİDİ (#pin) — Ayarlar açılmadan önce sorulan PIN penceresi. Dialog 440 px.
- Lock ikonu, "PIN girin" (title-l), "Ayarları açmak için 4 haneli PIN'i girin." (body, --text-2).
- 4 nokta (16 px daire; girilen hane dolu --text, boş olan kenarlı).
- Altında 3×4 tuş takımı: 1 2 3 / 4 5 6 / 7 8 9 / "Sil" (Delete ikonu) 0 "Vazgeç".
  - Tuşlar 64×52, radius 14, aralık 10, rakamlar title-l. Odaklı tuş beyaz.
- Kumandada rakam yok: D-pad ile tuşta gezilir, OK haneyi girer.
- Geri son haneyi siler; hane yoksa pencereyi kapatır.
- Prototipte PIN "1234". Yanlışsa noktalar sarsılmadan temizlenir ve "PIN yanlış, tekrar deneyin." (body, --danger) yazar.
````

---

## Prompt 10 — Sonraki sürüm ekranları (2/2)

````text
Guidelines'a uyarak 5 "sonraki sürüm" ekranı daha yap. Hepsi Ekran Galerisinden açılabilsin.

1) KANAL ARAMA (#arama)
- Başlık "Kanal ara" (headline).
- Altında arama kutusu: 52 px yükseklik, radius 999, zemin --surface-1, solda Search ikonu, yazılan metin ve imleç.
- Sol taraf (x 58, genişlik 470): ekran klavyesi. Tuşlar 40×44, radius 10, aralık 6.
  - 1. satır: A B C Ç D E F G Ğ H
  - 2. satır: I İ J K L M N O Ö P
  - 3. satır: R S Ş T U Ü V Y Z
  - Son satır: geniş tuşlar "Boşluk", "Sil" (Delete), "Temizle".
- Sağ taraf (x 548, genişlik 354): sonuçlar ListRow olarak; yazdıkça güncellenir. tr-TR duyarlı arama yapılır ("trt" yazınca TRT kanalları gelir).
- Sonuç yoksa: "Bu adla bir kanal bulunamadı."
- Klavyeden ▶ ile sonuçlara geçilir; sonuçta OK kanalı açar.
- Geri önce yazılanı siler, boşsa ekrandan çıkar.
- Ana ekran üst barına "Ara" (Search) butonu ekle: "Favorileri düzenle"nin soluna.

2) PROGRAM REHBERİ (#program-rehberi)
- Başlık "Program rehberi" ve altında kategori çipleri.
- Saat satırı: 20:00 · 20:30 · 21:00 · 21:30 · 22:00. 20:45'te dikey "şimdi" çizgisi (2 px, --accent).
- Solda 200 px kanal sütunu: 96×54 logo alanı ve "numara + ad".
- Sağda program hücreleri: yükseklik 54, radius 10, zemin --surface-1. Şu an süren program --surface-2 zeminli. Odaklı hücre beyaz.
  - Hücre içinde "[Program adı]" (label-l) ve "20:00–21:30" (label, --text-2).
  - Program adı uydurma; hepsi yer tutucu.
- Gezinme: ▲▼ kanal satırı, ◀▶ zaman. Şu anki programda OK → o kanal oynatıcıda açılır.
- Ekranın altında not: "Program bilgileri yayıncılardan alınır." (label, --text-2).

3) FAVORİ SIRALAMA (#favori-siralama)
- Sol sütun:
  - "Favori sıralama" (headline)
  - "OK ile seçin, ▲▼ ile taşıyın, OK ile bırakın." (body)
  - "Kanal değiştirirken bu sırayla dolaşırsınız." (body-m, --text-2)
- Sağ sütun: favori kanallar ListRow olarak, solda GripVertical ikonu.
- Taşıma hâlindeki satır:
  - zemin --selected-bg, 2 px --accent kenar ve "Taşınıyor" rozeti
  - ▲▼ satırı listede yukarı aşağı taşır
  - OK bırakır, Geri taşımayı iptal eder.
- Ayarlar listesine "Favori sıralama" satırı ekle.

4) GÜNCELLEME MEVCUT (#ana?katman=guncelleme) — ana ekran üstünde Dialog.
- Download ikonu (32, --accent).
- "Yeni sürüm hazır" (title-l).
- "Sürüm 1.1.0" (body) ve "[Değişiklik notu]" (body, --text-2).
- "Güncelleme sırasında televizyon bir onay penceresi gösterecek, orada 'Yükle'ye basın." (body-m, --text-2).
- Butonlar: "Şimdi güncelle" (açılış odağı; prototipte 2 sn yükleme halkası, sonra toast "Güncelleme hazırlandı") ve "Sonra". Geri = Sonra.

5) ÇIKIŞ İPUCU (#ana?katman=cikis-ipucu)
- Ayarlara "Çıkış ipucu" Switch satırı ekle (varsayılan kapalı).
- Açıkken ana ekranda Geri ile çıkılacağı an, önce toast gösterilir: CornerDownLeft ikonu + "Çıkmak için Geri'ye tekrar basın".
- 2 saniye içinde tekrar Geri → #google-tv. Başka bir tuş ipucunu iptal eder.
````

---

## Prompt 11 — Son kontrol ve cila

````text
Tüm prototipi Guidelines.md'ye göre baştan sona denetle ve bulduğun sorunları düzelt. Sonunda yaptığın düzeltmeleri madde madde yaz.

1. Her ekranda açılış odağı var; hiçbir ekranda odak kaybolmuyor veya sıkışmıyor (her öğeden en az bir yöne çıkılabiliyor).
2. Geri tuşu her katmanda önce en üstteki katmanı kapatıyor; arka arkaya Geri sonunda #google-tv'ye çıkıyor; hiçbir yerde aç/kapa gibi davranmıyor.
3. 14 px altında yazı yok; yazı boyutu 1.25 ve 1.5'te hiçbir metin kesilmiyor ve güvenli alan dışına çıkmıyor.
4. Odaklı ve seçili hâller her bileşende aynı görünüyor (Guidelines §5); yalnızca renkle ayrılan durum yok.
5. Renk ve ölçüler token'lardan geliyor; sabit yazılmış farklı renk veya ölçü yok.
6. Kartlar 1:1 seçeneğinde de düzgün; ızgara sütun kuralı doğru.
7. YouTube kuralı: YouTube kaynaklı kanalda video yer tutucusunun üstüne hiçbir katman çizilmiyor.
8. Türkçe metinlerde yazım hatası yok, büyük harfe çevirme yok, uydurma program adı yok.
9. Ekran Galerisi tüm rotaları içeriyor ve her biri doğrudan açılıyor (ekran görüntüsü alabilmem için).
10. Başlangıç rotası #acilis; #vitrin yalnızca galeride.
````

---

## Tasarımlar bitince benimle nasıl paylaşacaksınız?

1. **Yayın bağlantısı:** Make'te sağ üstteki **Publish** ile prototipi yayınlayın ve bağlantıyı bana gönderin. Kendi tarayıcımda açıp klavyeyle ekran ekran gezerim.
2. **Kod (en önemlisi):** Make'te **Code** sekmesinde **Download code** düğmesine basın. İnen zip'i `C:\Users\varca\TvBoxApp\design\figma-make\` klasörüne açın. Ölçüleri, renkleri ve davranışları buradan birebir okuyup Android koduna aktaracağım.
3. **Ekran görüntüleri (isteğe bağlı):** Galerideki her ekranın görüntüsünü `C:\Users\varca\TvBoxApp\docs\tasarim\` klasörüne koyun. Dosya adlarında rota adlarını kullanın: `ana.png`, `oynatici-bant.png`…
   - Make'teki **Copy design** ile ekranları Figma Design dosyasına da aktarabilirsiniz. Make yalnızca o an önizlemede görünen durumu kopyalar, bu yüzden her durumu ayrı ayrı kopyalamanız gerekir.
4. **Kararlarınızı yazın:**
   - Kart biçimi: 16:9 mu, 1:1 mi?
   - Filtre ne zaman uygulansın: odaklanınca mı, OK'a basınca mı?
   - Varsayılan yazı boyutu
   - Uygulamanın adı ("Aile TV" kalsın mı?)
   - Halk TV hangi kaynaktan açılsın: TV yayını mı, YouTube mu?
   - Anne-babanızın denemesinde nerede takıldıkları

> Make'in ürettiği şey bir **web prototipi**dir, televizyona doğrudan yüklenmez. APK'yı bu prototipe bakarak ben Kotlin + Compose for TV ile yazacağım. Make'in kodu, ekranların ve davranışların birebir referansı olacak.
