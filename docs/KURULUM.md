# Aile TV — Kutuya Kurulum Rehberi

Hedef cihaz: Xiaomi TV Box S (2. Nesil). Bu rehber önce sizin için (geliştirici kurulumu), sonra anne-babanızın kutusu için yazıldı.

---

## 0. Önce bilinmesi gerekenler

- **APK dosyası:** `app/build/outputs/apk/release/app-release.apk`
  - Bu dosyayı ben derliyorum.
  - Her sürümde aynı imza anahtarıyla imzalanır. Bu sayede yeni sürüm eskisinin üstüne kurulur ve favoriler, ayarlar silinmez.
- **İmza anahtarı:** `keystore/ailetv-release.jks` ve `keystore.properties`
  - **Bu iki dosyayı iki ayrı yerde yedekleyin** (ör. USB bellek + bulut).
  - Kaybolursa uygulamaya bir daha güncelleme kurulamaz; uygulamayı silip yeniden kurmanız gerekir.
  - Bu dosyalar git'e eklenmez.

---

## 1. Kutuda geliştirici seçeneklerini açın (bir kez)

1. Kumandayla **Ayarlar → Sistem → Hakkında** bölümüne gidin.
2. **"Android TV OS derlemesi"** satırına gelip **OK'a 7 kez** basın. Ekranda "Artık bir geliştiricisiniz" yazar.
3. Aynı ekranda **Android TV OS sürümünü** not edin (11 veya 14). Bana yazarsanız iyi olur.

---

## 2. Kurulum yolu A — Downloader ile (kolay, kablosuz)

1. Kutuda Play Store'dan **Downloader by AFTVnews** uygulamasını kurun.
2. Ben APK'yı bir indirme adresine koyacağım (ör. GitHub Releases). Downloader'da bu adresi yazın.
3. Kutu "Bilinmeyen uygulamaları yükle" izni isterse **Downloader için izin verin**.
4. **"Play Protect tarafından engellendi"** uyarısı çıkarsa:
   - **Ayrıntılar → Yine de yükle** seçin.
   - Bu uyarı, uygulamanın Play Store'dan gelmediği için çıkar.
5. Kurulum bitince Downloader'dan APK dosyasını silin; kutuda yer azdır.

## 2. Kurulum yolu B — ADB ile (bilgisayardan, hızlı güncelleme için)

1. Kutuda **Ayarlar → Sistem → Geliştirici seçenekleri** bölümünü açın:
   - **Android TV 11:** "USB hata ayıklama"yı açın.
   - **Android TV 14:** "Kablosuz hata ayıklama"yı açın.
2. Kutunun IP adresini öğrenin: **Ayarlar → Ağ ve İnternet → bağlı ağ**.
3. Bilgisayarda sırayla şunları çalıştırın:

```bash
source /c/Users/varca/.tvbox-toolchain/env.sh
```

```bash
adb connect KUTU_IP:5555
```

TV'de "USB hata ayıklamaya izin verilsin mi?" sorusu çıkar. **"Her zaman izin ver"** işaretleyip onaylayın. Ardından:

```bash
adb install -r /c/Users/varca/TvBoxApp/app/build/outputs/apk/release/app-release.apk
```

- Android TV 14'te önce eşleştirme gerekir: "Kablosuz hata ayıklama" ekranındaki eşleştirme kodu ve portla `adb pair KUTU_IP:PORT` çalıştırın, sonra `adb connect` ile devam edin.
- Kurulumdan sonra hata ayıklamayı kapatın.

---

## 3. Kurulumdan sonra (anne-babanızın kutusunda mutlaka yapın)

1. **Uygulamayı en öne alın:**
   - Google TV ana ekranında "Uygulamalarınız" satırında Aile TV'nin üzerine gelin.
   - OK'a **basılı tutun → Taşı** seçin ve uygulamayı en başa alın.
   - Böylece Home tuşuna ve ardından OK'a basınca hemen Aile TV açılır.
2. **5 GHz Wi-Fi'a bağlayın.** 2,4 GHz ağda yüksek çözünürlüklü yayınlar takılabilir.
3. **DNS'i Google DNS'e çevirin (önerilir):**
   - Testlerde ev modeminin DNS'i yük altında ara sıra adres çözemedi.
   - Uygulama bunu kendi kendine telafi ediyor; yine de kutuda ya da modemde DNS'i `8.8.8.8` / `8.8.4.4` yapmak bağlantıyı daha da sağlamlaştırır.
4. **Ekran kapanma süresini uzatın:**
   - **Ayarlar → Sistem → Güç ve enerji** (bazı sürümlerde "Enerji tasarrufu") bölümünde **"Ekranı kapat"** süresini en uzuna alın.
   - Android TV, kumandaya uzun süre basılmazsa video oynarken bile ekranı kapatır. Uygulama bunu engelleyemez.
5. **İsteğe bağlı — "Yalnızca uygulamalar" modu:**
   - Yeri: **Ayarlar → Hesaplar ve Oturum Açma → profil → Yalnızca uygulamalar modu**.
   - Ana ekrandaki reklam ve öneri kalabalığını kaldırır.
   - Bedeli: Google Asistan ve arama çalışmaz.

---

## 4. Güncelleme

- Yeni bir APK'yı aynı yolla (Downloader veya ADB) kurmanız yeterli. Eskisinin üstüne kurulur ve ayarlar korunur.
- **Kanal adresleri** değiştiğinde çoğu zaman APK güncellemesi gerekmez:
  - Kanal listesi GitHub'a konunca uygulama 6 saatte bir yeni listeyi kendisi çeker.
  - Bunun için bir GitHub deposu açılması gerekiyor (bkz. bölüm 5).

## 5. Uzaktan kanal listesi (GitHub) — henüz bağlanmadı

1. GitHub'da bir depo açın (gizli veya açık) ve içine `channels/channels.json` dosyasını koyun.
2. Dosyanın **raw** adresini (ör. `https://raw.githubusercontent.com/KULLANICI/DEPO/main/channels/channels.json`) bana verin.
3. Uygulamayı bu adresle bir kez yeniden derleyeceğim.

Bundan sonra bir kanal bozulduğunda yalnızca bu dosya düzeltilir ve kutular 6 saat içinde yeni listeyi kendileri alır.
