// Writes the curated, verified channel list to <out>. Sources are ordered: the app tries them top to bottom.
// Evidence: batch1/batch2 probes (TR IP, 2026-10-19), hard-channel workflow (research + independent re-verification).
import { writeFileSync } from "node:fs";

const out = process.argv[2];
const R = (site) => ({ Referer: site, Origin: new URL(site).origin });
const trt = (slug, max = "2560x1440", note) => ({ type: "hls", url: `https://tv-${slug}.medya.trt.com.tr/master.m3u8`, origin: "official", tier: 1, max, note });
const yt = (handle, channel_id, note) => ({ type: "youtube", handle, channel_id, url: `https://www.youtube.com/channel/${channel_id}/live`, origin: "youtube-official", tier: 2, note });
const web = (url, note) => ({ type: "web", url, origin: "official-page", tier: 3, note });
// Turkuvaz (ATV, A Haber, A Spor, Minika): the official site player asks securevideotoken.tmgrup.com.tr for a
// tokenised URL of its own daioncdn stream; the service only answers requests carrying the site's Referer.
const turkuvaz = (site, stream, max) => ({
  type: "api-token",
  url: "https://securevideotoken.tmgrup.com.tr/webtv/secure?url=" + encodeURIComponent(stream),
  path: "Url",
  request_headers: { Referer: site },
  origin: "official-api", tier: 1, max,
  note: "Resmi site oynatıcısının yaptığı gibi Turkuvaz token servisinden geçici (~11 saat) resmi adres alınır; token üretilmez.",
});
const restream = (url, max, note) => ({ type: "hls", url, origin: "thirdparty-restream", tier: 3, max, enabled: false, note: "Üçüncü taraf yeniden yayın (tv.ensonhaber.com). Varsayılan olarak kapalı; kararınıza bağlı. " + (note || "") });

const CH = [
  { id: "trt1", n: 1, name: "TRT 1", cat: "genel", fav: true, s: [trt("trt1"), { type: "hls", url: "https://trt.daioncdn.net/trt-1/master.m3u8?app=clean", origin: "official-alt", tier: 1, max: "2560x1440" }] },
  { id: "atv", n: 2, name: "ATV", cat: "genel", fav: true, s: [
    turkuvaz("https://www.atv.com.tr/", "https://trkvz.daioncdn.net/atv/atv.m3u8?ce=3&app=d1ce2d40-5256-4550-b02e-e73c185a314e", "1920x1080"),
    { type: "hls", url: "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/atv/atv.m3u8", origin: "broadcaster-cdn-edge", tier: 2, max: "1920x1080", note: "Turkuvaz'ın kendi CDN'i (Erstream) üzerindeki yayınlanmamış kenar adresi; resmi oynatıcı segmentleri buradan çeker. Token yok." },
    web("https://www.atv.com.tr/canli-yayin", "Resmi sayfa; oynatıcı securevideotoken.tmgrup.com.tr'den token alır.") ] },
  { id: "kanald", n: 3, name: "Kanal D", cat: "genel", fav: true, s: [
    { type: "hls", url: "https://demiroren.daioncdn.net/kanald/kanald.m3u8?app=kanald_web&ce=3", headers: R("https://www.kanald.com.tr/"), origin: "official", tier: 1, max: "1920x1080", note: "Her açılışta ana adres yeniden istenir (alt listeler oturuma bağlı)." },
    { type: "hls", url: "https://kdlive.duhnet.tv/S2/HLS_LIVE/kanalddainp/playlist.m3u8", origin: "official-alt", tier: 1, max: "1280x720" },
    yt("@kanald", "UCFoe1tg8MuHjRzmqXtV816A") ] },
  { id: "showtv", n: 4, name: "Show TV", cat: "genel", fav: true, s: [
    { type: "hls", url: "https://ciner.daioncdn.net/showtv/showtv.m3u8?ce=3&app=4bc856ef-4c68-4a94-bc87-37dfaaa66558", headers: R("https://www.showtv.com.tr/"), origin: "official", tier: 1, max: "1920x1080" } ] },
  { id: "startv", n: 5, name: "Star TV", cat: "genel", fav: true, s: [
    { type: "hls", url: "https://dygvideo.dygdigital.com/live/hls/startv4puhu", origin: "official-redirect", tier: 1, max: "1920x1080", live_offset_ms: 8000, note: "Doğuş'un kendi yönlendiricisi: 302 ile imzalı mncdn adresine gider (ExoPlayer takip eder). Canlı pencere 18 sn: liveTargetOffset ~8 sn ayarlanmalı; 403'te adres yeniden açılır." },
    { type: "hls", url: "https://dogus.daioncdn.net/startv/startv.m3u8?app=startv_web&ce=3", origin: "official", tier: 1, max: "1280x720", note: "Reklam aralarında sunucu taraflı reklam levhası gösterir." } ] },
  { id: "now", n: 6, name: "NOW", cat: "genel", fav: false, s: [
    { type: "page-token", url: "https://www.nowtv.com.tr/canli-yayin", pattern: "https://nowtv\\.daioncdn\\.net/nowtv/nowtv\\.m3u8\\?[^\"']*", origin: "official-page", tier: 1, max: "1280x720", note: "KARAR GEREKİR: resmi sayfa her yüklendiğinde içinde 4 saatlik st/e token'lı adres gelir; uygulama sayfayı tarayıcı gibi okuyup bu adresi oynatır (token üretmez). Token süresi dolmadan sayfa yeniden okunur." },
    { type: "hls", url: "https://uycyyuuzyh.turknet.ercdn.net/nphindgytw/nowtv/nowtv.m3u8", origin: "broadcaster-cdn-edge", tier: 2, max: "1280x720", note: "Token'sız kenar; alt liste bazen 4-26 sn gecikiyor (kararsız)." },
    web("https://www.nowtv.com.tr/canli-yayin", "Disney+ abonelerine ayrıca Disney+ uygulaması.") ] },
  { id: "tv8", n: 7, name: "TV8", cat: "genel", fav: true, s: [
    { type: "hls", url: "https://tv8.daioncdn.net/tv8/tv8.m3u8?app=7ddc255a-ef47-4e81-ab14-c0e5f2949788&ce=3", headers: R("https://www.tv8.com.tr/"), origin: "official", tier: 1, max: "1920x1080" } ] },
  { id: "kanal7", n: 8, name: "Kanal 7", cat: "genel", fav: false, s: [
    { type: "hls", url: "https://kanal7-live.daioncdn.net/kanal7/kanal7.m3u8", origin: "official", tier: 1, max: "1920x1080" } ] },
  { id: "teve2", n: 9, name: "teve2", cat: "genel", fav: false, s: [
    { type: "hls", url: "https://demiroren.daioncdn.net/teve2/teve2.m3u8?ce=3&app=6aab838a-437e-4a1b-bbd0-e30f79cdbbbd", headers: R("https://www.teve2.com.tr/"), origin: "official", tier: 1, max: "1920x1080" },
    yt("@tv2resmi", "UCFJBVnqU3i74W3HeIcGFmYQ") ] },
  { id: "beyaztv", n: 10, name: "Beyaz TV", cat: "genel", fav: false, s: [
    { type: "hls", url: "https://beyaztv-live.daioncdn.net/beyaztv/beyaztv.m3u8", origin: "official-cdn", tier: 1, max: "1920x1080", note: "Resmi oynatıcının arkasındaki Erstream kaynağı (aynı daioncdn.net alanı). Token/başlık gerekmiyor; ileride kapatılabilir, sağlık kontrolü şart." },
    { type: "hls", url: "https://mlatlxfaaa.turknet.ercdn.net/zqhpjaumpm/beyaztv/beyaztv.m3u8", origin: "broadcaster-cdn-edge", tier: 2, max: "1920x1080" },
    web("https://www.beyaztv.com.tr/canli-yayin") ] },
  { id: "tv360", n: 11, name: "360", cat: "genel", fav: false, s: [ { type: "hls", url: "https://turkmedya-live.ercdn.net/tv360/tv360.m3u8", origin: "official", tier: 1, max: "1920x1080" } ] },
  { id: "trt2", n: 12, name: "TRT 2", cat: "genel", fav: false, s: [trt("trt2", "2560x1440", "Yalnızca Türkiye IP'sinden açılır.")] },
  { id: "trthaber", n: 13, name: "TRT Haber", cat: "haber", fav: true, s: [trt("trthaber"), yt("@trthaber", "UC1E6Hs0vMq3LM1OQbIg9fSg")] },
  { id: "ntv", n: 14, name: "NTV", cat: "haber", fav: false, s: [
    { type: "hls", url: "https://dygvideo.dygdigital.com/live/hls/ntv4puhu", origin: "official-redirect", tier: 1, max: "1920x1080", note: "En yüksek bit hızı (5,6 Mb/s, tepe 8 Mb/s). Zayıf Wi-Fi'da 720p kademesine iner." },
    { type: "hls", url: "https://dogus.daioncdn.net/ntv/ntv.m3u8?app=ntv_web&ce=3", origin: "official", tier: 1, max: "1920x1080" },
    yt("@ntv", "UC9TDTjbOjFB9jADmPhSAPsw") ] },
  { id: "cnnturk", n: 15, name: "CNN Türk", cat: "haber", fav: false, s: [
    { type: "api-token", url: "https://www.cnnturk.com/api/cnnvideo/media?id=62d6814670380e2cdc7c124c&isMobile=false", path: "Media.Link.ServiceUrl + Media.Link.SecurePath", origin: "official-api", tier: 1, max: "1280x720", note: "KARAR GEREKİR: resmi sitenin kendi API'si 3 saatlik token'lı adres döndürür (web oynatıcı da aynısını yapar). Her açılışta ve 403'te yeniden istenir. CNN Türk'ün kendi CDN'inde 1080p yok." },
    yt("@cnnturk", "UCV6zcRug6Hqp1UX_FdyUeBg") ] },
  { id: "haberturk", n: 16, name: "Habertürk", cat: "haber", fav: false, s: [
    { type: "hls", url: "https://ciner.daioncdn.net/haberturktv/haberturktv.m3u8?app=c98ab0b0-50cc-495b-bb37-778e91f5ff5b&ce=3", headers: R("https://www.haberturk.com/"), origin: "official", tier: 1, max: "1920x1080" },
    yt("@HaberturkTV", "UCn6dNfiRE_Xunu7iMyvD7AA") ] },
  { id: "ahaber", n: 17, name: "A Haber", cat: "haber", fav: false, s: [
    turkuvaz("https://www.ahaber.com.tr/", "https://trkvz.daioncdn.net/ahaber/ahaber.m3u8?app=web", "1920x1080"),
    { type: "hls", url: "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/ahaber/ahaber.m3u8", origin: "broadcaster-cdn-edge", tier: 2, max: "1920x1080" },
    yt("@ahaber", "UCKQhfw-lzz0uKnE1fY1PsAA") ] },
  { id: "sozcutv", n: 18, name: "Sözcü TV", cat: "haber", fav: false, s: [ yt("@Sozcutelevizyonu", "UCOulx_rep5O4i9y6AyDqVvw", "Kanalın tek resmi canlı kaynağı YouTube.") ] },
  { id: "tv100", n: 19, name: "TV100", cat: "haber", fav: false, s: [ yt("@tv100", "UCndsdUW_oPLqpQJY9J8oIRg", "Tek resmi canlı kaynak YouTube."), restream("https://tv.ensonhaber.com/tv100/tv100.m3u8", "1920x1080") ] },
  { id: "haberglobal", n: 20, name: "Haber Global", cat: "haber", fav: false, s: [ yt("@haberglobal", "UCtc-a9ZUIg0_5HpsPxEO7Qg", "Tek resmi canlı kaynak YouTube."), restream("https://tv.ensonhaber.com/haberglobal/haberglobal.m3u8", "1280x720") ] },
  { id: "tgrthaber", n: 21, name: "TGRT Haber", cat: "haber", fav: false, s: [
    { type: "hls", url: "https://canli.tgrthaber.com/tgrt.m3u8", origin: "official", tier: 1, max: "1280x720", note: "Tek kademeli yayın (ffprobe ile 1280x720 ölçüldü)." },
    yt("@tgrthaber", "UCzgrZ-CndOoylh2_e72nSBQ") ] },
  { id: "tv24", n: 22, name: "24 TV", cat: "haber", fav: false, s: [ { type: "hls", url: "https://turkmedya-live.ercdn.net/tv24/tv24.m3u8", origin: "official", tier: 1, max: "1920x1080" } ] },
  { id: "tvnet", n: 23, name: "TVNET", cat: "haber", fav: false, s: [
    { type: "hls", url: "https://tvnet-live.lg.mncdn.com/tvnet-live/tvnet/playlist.m3u8", origin: "official", tier: 1, max: "1920x1080", live_offset_ms: 15000, note: "10 sn'lik segmentler, 30 sn pencere." },
    yt("@tvnet", "UC8rh34IlJTN0lDZlTwzWzjg") ] },
  { id: "ulketv", n: 25, name: "Ülke TV", cat: "haber", fav: false, s: [
    { type: "hls", url: "https://livetv.radyotvonline.net/kanal7live/ulketv/playlist.m3u8", origin: "broadcaster-provider", tier: 2, max: "1280x720", live_offset_ms: 10000, note: "Kanal 7 grubunun yayın sağlayıcısı (radyotvonline). Görüntü Ülke TV olarak doğrulandı. 28 sn pencere: liveTargetOffset ~10 sn." },
    yt("@ulketv", "UCi65FGbYYj-OzJm2luB_fNQ") ] },
  { id: "akittv", n: 26, name: "Akit TV", cat: "haber", fav: false, s: [
    { type: "json-youtube", url: "https://www.akittv.com.tr/live.json", origin: "official-json", tier: 2, note: "Yayıncının kendi JSON'u güncel YouTube adresini verir." },
    yt("@akittv", "UCbaLyHJp6S9Lsj6oT9aJsQw") ] },
  { id: "benguturk", n: 27, name: "Bengütürk", cat: "haber", fav: false, s: [ yt("@tvbenguturk", "UChNgvcVZ_ggDdZ0zCcuuzFw", "Tek resmi canlı kaynak YouTube."), restream("https://tv.ensonhaber.com/benguturk/benguturk.m3u8", "1280x720") ] },
  { id: "bloomberght", n: 28, name: "Bloomberg HT", cat: "haber", fav: false, s: [
    { type: "hls", url: "https://ciner.daioncdn.net/bloomberght/bloomberght.m3u8?app=web", headers: R("https://www.bloomberght.com/"), origin: "official", tier: 1, max: "1280x720", note: "Kanalın kendi yayını 720p." },
    yt("@bloomberght", "UCApLxl6oYQafxvykuoC2uxQ"), restream("https://tv.ensonhaber.com/bloomberght/bloomberght.m3u8", "1920x1080") ] },
  { id: "apara", n: 29, name: "A Para", cat: "haber", fav: false, s: [
    { type: "hls", url: "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/apara/apara.m3u8", origin: "broadcaster-cdn-edge", tier: 2, max: "1920x1080" },
    yt("@apara", "UCzrGg6iyRDuUSnO0cQ5oLcA") ] },
  { id: "ekoturk", n: 30, name: "Ekotürk", cat: "haber", fav: false, s: [
    { type: "hls", url: "https://cdn.ekoturk.net/hls/master.m3u8", origin: "official", tier: 1, max: "1280x720", live_offset_ms: 3000, note: "6 sn'lik canlı pencere (2 sn segment): liveTargetOffset ~3 sn, BehindLiveWindow'da yeniden hazırlama." } ] },
  { id: "cnbce", n: 31, name: "CNBC-e", cat: "haber", fav: false, s: [
    { type: "hls", url: "https://hnpsechtsc.turknet.ercdn.net/xpnvudnlsv/cnbc-e/cnbc-e.m3u8", origin: "broadcaster-cdn-edge", tier: 2, max: "1920x1080" },
    yt("@cnbce", "UCaO-M1dXacMwtyg0Pvovk4w") ] },
  { id: "trtspor", n: 32, name: "TRT Spor", cat: "spor", fav: false, s: [trt("trtspor1", "2560x1440", "Lisanslı maçlarda internet yayını kesilebilir.")] },
  { id: "trtsporyildiz", n: 33, name: "TRT Spor Yıldız", cat: "spor", fav: false, s: [trt("trtspor2", "2560x1440", "Yalnızca Türkiye IP'sinden."), { type: "hls", url: "https://trt.daioncdn.net/trtspor-yildiz/master.m3u8?app=clean", origin: "official-alt", tier: 1, max: "2560x1440" }] },
  { id: "aspor", n: 34, name: "A Spor", cat: "spor", fav: false, s: [
    turkuvaz("https://www.aspor.com.tr/", "https://trkvz.daioncdn.net/aspor/aspor.m3u8?ce=3&app=45f847c4-04e8-419a-a561-2ebf87084765", "1920x1080"),
    { type: "hls", url: "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/aspor/aspor.m3u8", origin: "broadcaster-cdn-edge", tier: 2, max: "1920x1080" },
    yt("@aspor", "UCJElRTCNEmLemgirqvsW63Q") ] },
  { id: "htspor", n: 35, name: "HT Spor", cat: "spor", fav: false, s: [
    { type: "hls", url: "https://ciner.daioncdn.net/ht-spor/ht-spor.m3u8?app=web", headers: R("https://www.htspor.com/"), origin: "official", tier: 1, max: "1920x1080" },
    yt("@htspor", "UCK3mI2lsk3LSo8PBUc8JTSw") ] },
  { id: "trtcocuk", n: 36, name: "TRT Çocuk", cat: "cocuk", fav: false, s: [trt("trtcocuk")] },
  { id: "trtdiyanetcocuk", n: 37, name: "TRT Diyanet Çocuk", cat: "cocuk", fav: false, s: [trt("trtdiyanetcocuk")] },
  { id: "minikacocuk", n: 38, name: "Minika Çocuk", cat: "cocuk", fav: false, s: [
    turkuvaz("https://www.minikacocuk.com.tr/", "https://trkvz.daioncdn.net/minikago_cocuk/minikago_cocuk.m3u8?app=web&ce=3", "1280x720"),
    { type: "hls", url: "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/minikago_cocuk/minikago_cocuk.m3u8", origin: "broadcaster-cdn-edge", tier: 2, max: "1280x720" },
    web("https://www.minikacocuk.com.tr/webtv/canli-yayin") ] },
  { id: "minikago", n: 39, name: "Minika GO", cat: "cocuk", fav: false, s: [
    turkuvaz("https://www.minikago.com.tr/", "https://trkvz.daioncdn.net/minikago/minikago.m3u8?app=web&ce=3", "1280x720"),
    { type: "hls", url: "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/minikago/minikago.m3u8", origin: "broadcaster-cdn-edge", tier: 2, max: "1280x720" },
    web("https://www.minikago.com.tr/webtv/canli-yayin") ] },
  { id: "trtbelgesel", n: 40, name: "TRT Belgesel", cat: "belgesel", fav: false, s: [trt("trtbelgesel")] },
  { id: "dmax", n: 41, name: "DMAX", cat: "belgesel", fav: false, s: [
    { type: "hls", url: "https://dogus.daioncdn.net/dmax/dmax.m3u8?ce=3&app=5a02c599-d17e-4982-9b04-090934d51af7", headers: R("https://www.dmax.com.tr/"), origin: "official", tier: 1, max: "1280x720", note: "Sayfadaki token parametresi sunucuda zorunlu tutulmuyor; token'sız adres doğrulandı." },
    web("https://www.dmax.com.tr/canli-izle") ] },
  { id: "tlc", n: 42, name: "TLC", cat: "belgesel", fav: false, s: [
    { type: "hls", url: "https://dogus.daioncdn.net/tlc/tlc.m3u8?ce=3&app=1aca0aa1-06e3-41ff-a8e5-38d7df475469", headers: R("https://www.tlctv.com.tr/"), origin: "official", tier: 1, max: "1280x720", note: "DMAX ile aynı durum." },
    web("https://www.tlctv.com.tr/canli-izle") ] },
  { id: "tgrtbelgesel", n: 43, name: "TGRT Belgesel", cat: "belgesel", fav: false, s: [ yt("@tgrtbelgeseltv", "UCkQV7uH1Otv47CZz6tD3jGQ", "Eski mediatriple adresleri öldü; tek resmi kaynak YouTube.") ] },
  { id: "trtmuzik", n: 44, name: "TRT Müzik", cat: "muzik", fav: false, s: [trt("trtmuzik")] },
  { id: "dreamturk", n: 45, name: "Dream Türk", cat: "muzik", fav: false, s: [ { type: "hls", url: "https://live.duhnet.tv/S2/HLS_LIVE/dreamturknp/playlist.m3u8", origin: "official", tier: 1, max: "1280x720" } ] },
  { id: "trtgenc", n: 46, name: "TRT Genç", cat: "genel", fav: false, s: [trt("trtgenc", "2560x1440", "Yalnızca Türkiye IP'sinden.")] },
  { id: "diyanettv", n: 47, name: "Diyanet TV", cat: "diger", fav: false, s: [
    { type: "hls", url: "https://eustr73.mediatriple.net/videoonlylive/mtikoimxnztxlive/broadcast_5e3bf95a47e07.smil/playlist.m3u8", origin: "official-cdn", tier: 1, max: "1920x1080", note: "Kenar değişirse https://titan.mediatriple.net/?br=broadcast_5e3bf95a47e07 güncel adresi verir." },
    yt("@diyanettv", "UC3d1AdmvAP6Jq16-WpD086g") ] },
  { id: "tbmmtv", n: 48, name: "TBMM TV", cat: "diger", fav: false, s: [ { type: "hls", url: "https://meclistv-live.ercdn.net/meclistv/meclistv.m3u8", origin: "official", tier: 1, max: "1280x720", note: "Genel Kurul saatleri dışında yayın içeriği değişebilir." } ] },
  { id: "trtturk", n: 49, name: "TRT Türk", cat: "diger", fav: false, s: [trt("trtturk")] },
  { id: "trtavaz", n: 50, name: "TRT Avaz", cat: "diger", fav: false, s: [trt("trtavaz")] },
  { id: "trtkurdi", n: 51, name: "TRT Kurdî", cat: "diger", fav: false, s: [trt("trtkurdi")] },
  { id: "trtarabi", n: 52, name: "TRT Arabi", cat: "diger", fav: false, s: [trt("trtarabi")] },
  { id: "trtworld", n: 53, name: "TRT World", cat: "diger", fav: false, s: [trt("trtworld")] },
  { id: "trteba", n: 54, name: "TRT EBA", cat: "diger", fav: false, s: [trt("e-okul01", "1280x720")] },
  { id: "showturk", n: 55, name: "Show Türk", cat: "diger", fav: false, s: [ { type: "hls", url: "https://ciner.daioncdn.net/showturk/showturk.m3u8?app=web", headers: R("https://www.showturk.com.tr/"), origin: "official", tier: 1, max: "1920x1080" } ] },
];

const doc = {
  version: 1,
  updated_at: "2026-10-28",
  verified_from: "Türkiye (ev interneti), 19.10.2026; Turkuvaz kanalları 28.10.2026",
  policy: {
    decided_at: "2026-10-19",
    token_reading: true,
    youtube_playback: "embedded-webview",
    thirdparty_restreams: false,
    removed_channels: ["halktv"],
  },
  notes: [
    "Kaynaklar sırayla denenir; ilk açılan oynatılır. enabled:false olanlar denenmez (üçüncü taraf kopyalar kararla kapalı).",
    "type=hls: ExoPlayer HlsMediaSource ile doğrudan. headers varsa DefaultHttpDataSource'a eklenir. 302 yönlendirmeleri takip edilir.",
    "type=page-token / api-token: resmi sayfa veya API tarayıcı gibi istenir, içindeki token'lı adres oynatılır; token üretilmez. (Karar: evet)",
    "type=youtube: resmi YouTube canlı yayını, uygulama içinde gömülü (WebView + IFrame Player) oynatılır. (Karar: gömülü) Kural: video alanının üstüne bant/panel çizilmez; katman açılacaksa video önce gizlenir.",
    "type=web: resmi canlı yayın sayfası WebView'da açılır (son çare).",
    "max=2560x1440 olan TRT yayınları uygulamada 1080p ile sınırlandırılacak.",
    "Halk TV kararla listeden çıkarıldı; numaralar 1-54 olarak yeniden verildi.",
  ],
  categories: [
    { id: "favoriler", label: "Favoriler" }, { id: "tumu", label: "Tümü" }, { id: "genel", label: "Genel" }, { id: "haber", label: "Haber" },
    { id: "spor", label: "Spor" }, { id: "cocuk", label: "Çocuk" }, { id: "belgesel", label: "Belgesel" }, { id: "muzik", label: "Müzik" }, { id: "diger", label: "Diğer" },
  ],
  channels: CH.map((c, i) => ({ id: c.id, number: i + 1, name: c.name, category: c.cat, default_favorite: c.fav, enabled: true,
    sources: c.s.map((s) => ({ enabled: true, ...s })) })),
};
writeFileSync(out, JSON.stringify(doc, null, 2));
const stat = { direct: 0, resolver: 0, youtubeOnly: 0 };
for (const c of doc.channels) {
  const en = c.sources.filter((s) => s.enabled);
  if (en.some((s) => s.type === "hls")) stat.direct++; else if (en.some((s) => s.type === "page-token" || s.type === "api-token")) stat.resolver++; else stat.youtubeOnly++;
  console.log(String(c.number).padStart(2), c.name.padEnd(18), en.map((s) => `${s.type}${s.max ? " " + s.max : ""}${s.tier ? " t" + s.tier : ""}`).join(" | "));
}
console.log(stat, "total", doc.channels.length);
