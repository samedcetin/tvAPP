# channels
## SUMMARY
As of 18.10.2026 the national lineup is roughly 18 TRT channels (TRT 3 frequency carries TBMM TV) plus about 45 private national or satellite channels. The lineup changed a lot in 2025-2026. TMSF has taken over 10 TV channels in about 20 months:
- Flash Haber TV (03/2025): live news ended 05/2026, then sold to Safe Media on 31.08.2026 to become an entertainment channel, "Flash TV".
- Ekotürk (08/2025): sold on 05.11.2025.
- The Can Medya group (11.09.2025): Habertürk, Show TV, Bloomberg HT, HT Spor, Show Türk and Show Max. All are still on air under TMSF.
- TELE1 (24.10.2025): news programmes removed; the TMSF sale tender was halted in 06/2026. Its successor TELE2 Haber stopped in 10/2026.
- Halk TV (14.10.2026): TV broadcast halted. Journalists keep streaming on YouTube.
Ekol TV closed on 22.12.2025. FOX was renamed NOW on 12.02.2024 and is still owned by Disney. Sözcü TV is on air despite repeated RTÜK fines.

Streams, probed live from a Turkish IP on 18.10.2026:
- Plain HLS that ExoPlayer can play: all TRT channels (tv-<kanal>.medya.trt.com.tr/master.m3u8). TRT's own tabii web app uses these URLs. Also plain: TBMM TV, Kanal 7, 360, 24 TV, TGRT Haber, TVNET, Ekotürk and Diyanet TV.
- HLS on daioncdn that needs an `app` query parameter and hands out short-lived session tokens (sid, st, ex=10800 s, so about 3 hours) inside the child playlists: Kanal D, teve2, Show TV, Show Türk, Habertürk, Bloomberg HT, HT Spor, Star TV, NTV and TV8. These play if the app always re-requests the master URL.
- Tokenized or protected: NOW (expiring st and e tokens), DMAX and TLC (token parameter), Beyaz TV (401), CNN Türk and CNBC-e (403 on direct requests).
- Web player only: the Turkuvaz channels (ATV, A Haber, A Spor, A Para, Minika). Their streams load through JavaScript.
- Official YouTube live only, with no direct HLS: Sözcü TV, TV100, Haber Global, Bengütürk, Akit, A Para and, for now, Halk TV.

iptv-org streams/tr.m3u has 221 entries and a bot updates it every few days. It is unreliable as a primary source:
- It is missing CNN Türk, Show TV, teve2, Sözcü TV, TV100, A Para, A News, Akit, Ulusal and TV5.
- Many entries point to third-party restream hosts or bare IPs.
- Its metadata has errors.

Legal picture:
- Playing streams that broadcasters themselves publish for free, inside a private family app that is not distributed, is low risk.
- Re-hosting or re-transmitting streams, bypassing tokens or other technical measures (FSEK m.72), using pirate sports feeds (FSEK m.71, TCK 163/2), or distributing the app publicly as a channel aggregator (possible RTÜK platform authorisation under 6112 m.29/A) is not low risk.

Recommended architecture: a remote JSON channel list hosted on GitHub or Gist. Each channel gets an ordered source list (official HLS, then official YouTube live, then the official web page in a WebView), with optional headers. Health checks must run from a Turkish IP. No stream URLs are hard-coded in the APK, and no token scraping is done.

## RECOMMENDATIONS
- Keep every stream URL out of the APK. Host a versioned channels.json in a public GitHub repo, or a private Gist the app can read. Fetch it through raw.githubusercontent.com or jsDelivr, cache the last good copy on the device, and fall back to that cache when offline. Suggested schema: {version, updated_at, channels:[{id, name, logo, category, order, enabled, note, sources:[{type:'hls'|'youtube'|'webview', url | youtube_channel_id | video_id, headers:{Referer, User-Agent}, priority, geo:'TR'|null, last_ok}]}]}.
- Source priority per channel:
1. Official HLS from the broadcaster's own domain or CDN. Use TRT's medya.trt.com.tr, daioncdn URLs copied from the broadcaster's own page, turkmedya-live.ercdn.net and canli.tgrthaber.com.
2. Official YouTube live.
3. The broadcaster's official live page in a WebView.
Never use iptv-org restream hosts, bare IPs, or unknown domains such as ensonhaber, siteyaptim, smartplaytv, freeott, onrender, random raw GitHub files or mangeinde4.
- Player: Media3 ExoPlayer with HlsMediaSource. Set per-source headers through DefaultHttpDataSource.Factory().setDefaultRequestProperties(...) and setUserAgent(...). For daioncdn channels, always open the master URL fresh on each tune and retry. Never cache child playlist URLs, because they carry session tokens (sid, st, ex=10800). On an HTTP or behind-live-window error, retry the same source twice, then try the next source, then show a friendly 'Bu kanal şu an açılamıyor' card with a button to try the alternative.
- Do not scrape or regenerate tokens for NOW, DMAX, TLC, Beyaz TV, CNN Türk, CNBC-e or the Turkuvaz channels. Bypassing access-control tokens risks FSEK m.72. Use their official YouTube live (A Haber, A Spor, A Para, CNN Türk, CNBC-e) or a full-screen WebView of the official live page (ATV, NOW, Star, Beyaz TV, DMAX, TLC, Minika). Test WebView on the Mi Box: autoplay, D-pad focus, and the ad-blocker wall on atv.com.tr.
- YouTube fallback: resolve the current live videoId server-side so the API key never ships in the APK. A GitHub Action can call the YouTube Data API search.list with channelId and eventType=live every 30-60 minutes and write video_id into channels.json. On the TV, either deep-link into the preinstalled YouTube app (simplest and ToS-safe), or embed with the IFrame Player in a WebView, for example the PierfrancescoSoffritti android-youtube-player library. Do not extract YouTube's raw HLS. Store verified channel IDs, not handles: @atv, @tv5, @flashhaber and @Tele2Haber are not the broadcasters' own accounts.
- Health checks must run from a Turkish IP, because TRT 2, TRT Genç, TRT Spor Yıldız and some sports windows are geo-blocked. Options: run them on-device (a WorkManager job that does a HEAD/GET on each master.m3u8 plus one child playlist and reports an anonymous status), or on a home Raspberry Pi or TR VPS that commits status into the JSON. GitHub Actions runners (US/EU) give false failures. Mark channels as up, degraded or down, and hide or grey out down channels for the parents instead of showing errors.
- Add a remote 'note' and 'enabled' flag per channel so the list can be fixed without reinstalling. Example note: 'Halk TV: TV yayını 14.10.2026'dan beri durduruldu, YouTube yayını gösteriliyor'. Re-check Halk TV, Flash TV and TELE1 weekly. Exclude Ekol TV and TELE1. Treat TRT 4K as paused.
- Sports: expect slates, blackouts or geo-blocks on free web streams during licensed matches (UEFA on TRT/tabii, cup qualifiers on TV100 and TV8). Never add beIN, TOD or other pay channels, or pirate sports feeds. Süper Lig is pay-only through 2026-27.
- Keep the app private (family use only, sideloaded, not published as a public APK or on Play Store). Show each channel's own branding and ads, without stripping. Credit sources in an 'Hakkında' screen. If public distribution is ever considered, get legal advice on RTÜK platform authorisation (6112 m.29/A) and FSEK m.25/80 first.
- Use iptv-org only as a discovery aid: diff its tr.m3u weekly to spot URL changes. Never use it as the runtime source. Its tr list lacks key channels and has metadata errors.
- MVP channel set (direct, official, plain or daioncdn HLS, verified 18.10.2026): all TRT channels, TBMM TV, Kanal D, Show TV, Star TV, TV8, Kanal 7, teve2, 360, NTV, Habertürk, TGRT Haber, 24 TV, TVNET, Bloomberg HT, HT Spor, Show Türk and Ekotürk. Second tier, YouTube-only: Sözcü TV, TV100, Haber Global, A Haber, CNN Türk, A Spor, A Para, CNBC-e, Akit, Bengütürk, Ülke TV, Diyanet TV and Halk TV. Third tier, WebView: ATV, NOW, Beyaz TV, DMAX, TLC and Minika.

## OPEN QUESTIONS
- Halk TV after the 14.10.2026 trustee appointment: will TV broadcasting resume under TMSF, will the YouTube channel remain reachable in Turkey, and does halktv-live.daioncdn.net carry anything?
- Flash TV under Safe Media: new name, logo, format, launch date and official stream URL.
- TELE1: is the frequency still on air with documentaries, or effectively dark? The tender was halted in 06/2026.
- The exact 2026 list of RTÜK 'ulusal' licence holders (terrestrial vs satellite). The RTÜK licence lists load dynamically and could not be extracted, so they need a manual check on rtuk.gov.tr.
- Turkuvaz channels (ATV, A Haber, A Spor, A Para, Minika) and NOW: does the official live page work acceptably in an Android TV WebView on the Mi Box S 2nd Gen (autoplay, full screen, D-pad, ad-blocker wall)? Do they offer official Android TV apps that could be launched by intent instead?
- Which daioncdn `app` values are stable over time? Show TV's page still embeds an old token from 2022, which suggests the `app` id is the real key. Monitor whether broadcasters rotate it.
- Unverified official sources: TBMM TV (meclistv-live.ercdn.net is not referenced on tbmm.gov.tr), Diyanet TV and TGRT Belgesel (mediatriple), TV8.5, TV5, A News, Kral Pop, Power Türk, Number1 and Ulusal Kanal's YouTube handle.
- Ownership details flagged low or medium: Kanal 7 and Ülke TV group, DMAX and TLC (Doğuş vs Warner Bros. Discovery vs the 'Skydance' that iptv-org lists), Ekotürk's buyer, Haber Global.
- Whether the 'live_stream?channel=' YouTube embed works reliably in 2026 for each channel, or whether Data API resolution or a YouTube app deep link is required.
- Legal: would even a privately shared APK (for example sent to relatives' boxes) count as 'platform' activity under the RTÜK internet regulation? This needs a Turkish media lawyer if distribution widens beyond one household.

## VERIFY CORRECTIONS
- CLAIM: TRT EBA TV (İlkokul / Ortaokul / Lise) is active on tv-e-okul00, tv-e-okul01 and tv-e-okul02 (confidence high).
  PROBLEM: Only one of the three is live. tv-e-okul00 and tv-e-okul02 return HTTP 200, but their child playlists are a finished VOD: they end with #EXT-X-ENDLIST and the segments are named master_720_backup_20220412T09xxxx (April 2022). TRT now lists a single channel, 'TRT EBA', and tabii's homepage HTML references only tv-e-okul01.
  CORRECTED: TRT EBA is one channel: https://tv-e-okul01.medya.trt.com.tr/master.m3u8, which is live (MEDIA-SEQUENCE ~2971636, no ENDLIST). Drop e-okul00 and e-okul02.
  SRC: Direct probe from a TR IP (Cloudflare trace loc=TR), 2026-10-18 ~20:00Z; https://www.trt.net.tr/kurumsal/televizyon ; https://www.tabii.com/ homepage HTML
- CLAIM: All TRT channels 'returned 200 from a TR IP', so they are verified playable (this logic is also used for the MVP set).
  PROBLEM: A 200 on master.m3u8 does not prove a stream is live. The EBA feeds above return 200 but serve a stale 2022 loop. The report's health-check design (HEAD/GET on master plus one child) would mark them as up.
  CORRECTED: Liveness check: fetch the child playlist and require no #EXT-X-ENDLIST, a MEDIA-SEQUENCE that increases between two polls, and, where present, a recent EXT-X-PROGRAM-DATE-TIME or a recent timestamp in the segment names.
  SRC: Direct probe 2026-10-18
- CLAIM: TBMM TV: meclistv-live.ercdn.net/meclistv/meclistv.m3u8 comes from iptv-org and is not on tbmm.gov.tr (confidence medium).
  PROBLEM: The URL is embedded on TBMM's own TV site, tv.tbmm.gov.tr, so it is official. Its segments were current (meclistv_720p-1792353960.ts, about 20:06Z on 18.10.2026).
  CORRECTED: TBMM TV official HLS is https://meclistv-live.ercdn.net/meclistv/meclistv.m3u8, referenced on https://tv.tbmm.gov.tr/. Raise to high confidence. TRT's page confirms that TRT 3 is the TBMM TV channel. No tv-trt3.medya.trt.com.tr host exists.
  SRC: curl of https://tv.tbmm.gov.tr/ 2026-10-18 ; https://www.trt.net.tr/kurumsal/televizyon
- CLAIM: The daioncdn channels (Kanal D, teve2, Show TV, Show Türk, Habertürk, Bloomberg HT, HT Spor, Star TV, NTV, TV8) all get child playlists with sid, st and ex=10800 tokens (about 3 hours).
  PROBLEM: Token behaviour differs by channel. Only Show TV and Bloomberg HT children carried ex=10800&st=…. Show Türk and HT Spor carried ex=0. Kanal D, Star TV, TV8, teve2, NTV and Habertürk carried only sid plus app, with no st or ex. The `app` value can also be an alias that the CDN rewrites to a UUID: kanald_web became da2109ea-…, ntv_web became c68bddbe-…, and ?app=web for Show Türk became 3a516c79-….
  CORRECTED: Treat every daioncdn child URL as session-scoped. Always re-request the master on each tune, which is the report's own recommendation and is still right. Do not assume a fixed 3-hour expiry.
  SRC: Direct probes 2026-10-18 of the master URLs listed in the report
- CLAIM: The iptv-org Turkuvaz entries on turknet.ercdn.net 'look like an ISP IPTV feed rather than the broadcaster's public web stream'.
  PROBLEM: The reasoning is unsupported. Kanal D's official daioncdn child playlist serves its segments from ackaxsqacw.turknet.ercdn.net/ozfkfbbjba/kanald/…, so turknet.ercdn.net is a CDN edge that official web players use. The iptv-org ATV and A Haber URLs (rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/atv/atv_1080p.m3u8) return 200 and follow the same path pattern.
  CORRECTED: turknet.ercdn.net is an ercdn edge also used behind official daioncdn players. The iptv-org Turkuvaz, CNBC-e and NOW URLs on it work, but they are not published on any official page. Keep excluding them because their origin is unverified, not because they are 'ISP IPTV'.
  SRC: Direct probe 2026-10-18 (Kanal D child playlist; iptv-org tr.m3u entries)
- CLAIM: tv8.com.tr references a 'tv8-geo' playlist, so TV8 sports or licensed shows may be geo-limited or blacked out.
  PROBLEM: The tv8-geo URL on the page (tv8-tb-live.ercdn.net/tv8-geo/playlist.m3u8?st=…&e=1639061641) carries an expiry from December 2021. It is a stale leftover, not evidence of current geo-blocking.
  CORRECTED: No evidence of current TV8 geo-limiting. Sports blackouts on free web streams are still possible in general but unproven for TV8. Primary source: https://tv8.daioncdn.net/tv8/tv8.m3u8?app=7ddc255a-ef47-4e81-ab14-c0e5f2949788&ce=3.
  SRC: curl of https://www.tv8.com.tr/canli-yayin 2026-10-18
- CLAIM: teve2 alternate source https://tv2.duhnet.tv/S2/HLS_LIVE/tv2np/playlist.m3u8.
  PROBLEM: The host timed out on two attempts (HTTP 000), so it looks dead.
  CORRECTED: Use demiroren.daioncdn.net/teve2/teve2.m3u8?ce=3&app=6aab838a-… (200, live) with YouTube @tv2resmi (live) as the fallback. Drop the duhnet alternate.
  SRC: Direct probe 2026-10-18
- CLAIM: Diyanet TV has plain HLS at eustr73.mediatriple.net/…/broadcast_5e3bf95a47e07.smil/playlist.m3u8.
  PROBLEM: The URL was intermittent (timeout first, 200 on retry) and was not found on the official site: diyanet.tv/canli-yayin redirects to the homepage and no stream URL is in the HTML. Its origin is unverified, but the report lists it as 'plain HLS' in the summary.
  CORRECTED: Make YouTube @diyanettv (verified live 18.10.2026) the primary source. Use the mediatriple URL only as an unverified fallback.
  SRC: Direct probe 2026-10-18; youtube.com/@diyanettv/live isLiveNow=true
- CLAIM: Halk TV: TV broadcast halted on 14.10.2026; halktv-live.daioncdn.net 'still answers 200 (content unknown)'; halktv.com.tr live page embeds YouTube.
  PROBLEM: This is incomplete. The trustee-run TV feed is still on air: halktv-live.daioncdn.net/halktv/halktv_1080p.m3u8 had segments about 20 s old at 20:07Z on 18.10.2026. Reports say live programming was replaced with pre-recorded shows. Separately, halktv.com.tr/canli-yayin now redirects (302) to youtube.com/@Halktvkanali/live, which was live with the 'Parantez' programme. These are two different feeds with different editorial control.
  CORRECTED: Model Halk TV as two sources with distinct labels: (a) the trustee-run TV feed on daioncdn HLS, with pre-recorded content; (b) the journalists' YouTube live (@Halktvkanali). Decide editorially which one 'Halk TV' means and say so in the notice text. The trustee is İbrahim Paşalı. The investigation covers bribery, bid-rigging and money-laundering allegations.
  SRC: Direct probes 2026-10-18; https://medyascope.tv/2026/10/14/halk-tvye-kayyum-atanmasina-giden-surec/ ; https://tr.euronews.com/2026/10/14/halk-tvye-kayyum-atandi-polis-kanali-basti-yayin-durduruldu
- CLAIM: Safe Media bought Flash Haber TV on 31.08.2026 'to become an entertainment channel, "Flash TV"'.
  PROBLEM: No new name has been announced. Medyascope calls the channel 'Flash TV' throughout its 2025-2026 TMSF coverage, while Karar (05/2026) used 'Flash Haber TV'. The trustee appointment date is 15.03.2025, after owner Erkan Kork's detention in an illegal-betting probe. The report omits that Halk TV owner Mahiroğlu announced buying Flash Haber in 11/2024, which may explain the naming confusion.
  CORRECTED: Flash TV / Flash Haber TV: trustee since 15.03.2025; live programming ended and archive-only output from about 17.05.2026; sold to Safe Media (chair Seyfi Şahin) on 31.08.2026 for a family entertainment relaunch. New name, launch date and price are undisclosed.
  SRC: https://medyascope.tv/2026/08/31/flash-tv-satildi-abdullah-catli-filmini-ceken-sirket-flash-tvyi-satin-aldi/ ; https://www.karar.com/guncel-haberler/tmsfye-devredilen-flash-haber-tvde-canli-yayin-donemi-sona-erdi-2048821 (17.05.2026) ; https://medyascope.tv/2026/10/14/tmsf-yonetimine-gecen-medya-kuruluslari/
- CLAIM: TELE2 Haber was founded on 14.11.2025 and decided on 02.10.2026 to stop 'over a 2M TL RTÜK licence fee'.
  PROBLEM: The dates are slightly off. TELE1 staff resigned on 31.10.2025, and TELE2 Haber started on 15.11.2025 on the internet and social media. The 2 M TL figure covers two RTÜK licences, and the outlet ended all broadcasts under the TELE2 Haber name, not only its plan to become a TV channel.
  CORRECTED: TELE2 Haber: launched 15.11.2025; ended broadcasts under that name after a decision announced 02.10.2026, citing the 2 M TL fee for two RTÜK licences. TELE1's own on-air status in 2026 remains unverified. The TMSF tender's bid deadline was 16.06.2026 at an estimated 28 M TL.
  SRC: https://www.evrensel.net/haber/6003176/tele2-haber-yayin-hayatini-sonlandiriyor ; https://medyascope.tv/2026/10/14/tmsf-yonetimine-gecen-medya-kuruluslari/
- CLAIM: Sports: Süper Lig and 1. Lig are paid-only through 2026-27 'with a new tender on 13.01.2027'; 'TV100 aired UEFA qualifiers free in summer 2026'.
  PROBLEM: The cited teknoblog source confirms the TFF–Digiturk/beIN deal (3 seasons from 2024-25 to 2026-27) but mentions neither a 13.01.2027 tender nor TV100 UEFA coverage. Both details are unverified.
  CORRECTED: The beIN/Digiturk rights (beIN Connect and TOD digital) run to the end of 2026-27. The tender date is unverified. Free-to-air sports that could trigger web or YouTube blackouts: TRT 1 and TRT Spor (selected UEFA CL/EL/ECL; TRT Spor also some 1. Lig and cup matches); ATV and A Spor (Ziraat Türkiye Kupası, Süper Kupa); HT Spor (Copa del Rey, Supercopa, selected Eredivisie and Saudi Pro League). tabii carries full UEFA coverage and has Android TV and Google TV apps.
  SRC: https://www.teknoblog.com/2026-2027-futbol-sezonu-hangi-lig-hangi-kanalda/
- CLAIM: FSEK m.72 (amended by Law 7346, 25.12.2021) criminalises 'circumventing effective technological measures such as access control or encryption', 6 months-2 years.
  PROBLEM: The scope and the date are imprecise. The official text says m.72 was amended by Law 7346 art.1 dated 21.12.2021 (25.12.2021 may be the Official Gazette date; unverified). It punishes making, importing, distributing, selling, renting or commercially holding products and tools aimed at circumventing such measures, and offering advertising, marketing, design or application services for them. It does not punish the bare act of circumvention by a viewer.
  CORRECTED: Under FSEK m.72 (as amended 21.12.2021 by Law 7346/1), a token-scraping APK could itself be a circumvention 'tool', and sharing it with relatives could count as distribution. The report's conclusion (do not scrape tokens) stands, but the legal basis should be stated this way.
  SRC: https://www.mevzuat.gov.tr/MevzuatMetin/1.3.5846.pdf (m.72, m.71)
- CLAIM: TCK 163/2: 'benefiting from encrypted or unencrypted broadcasts without the owner's consent, 6 months-2 years or a fine'.
  PROBLEM: The paraphrase drops the statutory scope. The text covers encrypted or unencrypted broadcasts made 'via telephone lines and frequencies or electromagnetic waves' without the consent of the owner or possessor ('sahibinin veya zilyedinin rızası olmadan').
  CORRECTED: TCK 163/2: 'Telefon hatları ile frekanslarından veya elektromanyetik dalgalarla yapılan şifreli veya şifresiz yayınlardan sahibinin veya zilyedinin rızası olmadan yararlanan kişi, altı aydan iki yıla kadar hapis veya adlî para cezası ile cezalandırılır.' The implied-consent argument for free official streams turns on 'sahibinin veya zilyedinin rızası'.
  SRC: https://www.mevzuat.gov.tr/MevzuatMetin/1.5.5237.pdf (m.163)
- CLAIM: FSEK: re-broadcasting a work through another broadcaster requires permission (m.25/3).
  PROBLEM: The paragraph citation looks wrong. The retransmission right ('başka yayın kuruluşları tarafından yeniden yayınlanması suretiyle umuma iletilmesi hakkı') is in the first paragraph of m.25 and belongs exclusively to the author. Broadcasters' own neighbouring rights are in m.80 (70-year term).
  CORRECTED: Cite FSEK m.25(1) for the author's exclusive retransmission right and m.80 for broadcasters' neighbouring rights. Low importance for the private-app conclusion.
  SRC: https://www.mevzuat.gov.tr/MevzuatMetin/1.3.5846.pdf
- CLAIM: YouTube fallback: a GitHub Action calls the YouTube Data API search.list (channelId, eventType=live) every 30-60 minutes per channel.
  PROBLEM: This does not fit the default quota. search.list now has its own bucket of 100 calls per day per project, separate from the 10,000-unit pool. About 15-25 YouTube-only channels polled every 30-60 min needs roughly 360-1,200 calls per day.
  CORRECTED: Use the free channel feed https://www.youtube.com/feeds/videos.xml?channel_id=<UC…> (verified 200) to get candidate video IDs, then one batched videos.list call (up to 50 IDs) and check snippet.liveBroadcastContent=='live'. Alternatively, deep-link the YouTube app to the channel's /live. Use search.list only for occasional fallback, or request a quota increase.
  SRC: https://developers.google.com/youtube/v3/docs/search/list ; https://developers.google.com/youtube/v3/getting-started
- CLAIM: Embed YouTube with the IFrame Player in a WebView and show a fallback overlay, a channel badge and other UI over the player.
  PROBLEM: YouTube's Required Minimum Functionality forbids this design. Embedded clients must identify themselves via the HTTP Referer header, so a WebView needs loadDataWithBaseURL with a baseUrl or a Referer header, set to the app ID in reverse-DNS form. It is forbidden to show 'overlays, frames, or other visual elements in front of any part' of the player. The player viewport must be at least 200x200.
  CORRECTED: For YouTube sources, set a Referer or baseUrl to the app ID, never draw app UI over the YouTube player (put info outside the player, or only before or after playback), and design the error overlay for ExoPlayer and WebView sources only. Deep-linking to the preinstalled YouTube app avoids these constraints.
  SRC: https://developers.google.com/youtube/terms/required-minimum-functionality
- CLAIM: CNBC-e owner: İlbak Holding (CNBC licence; relaunched 10.06.2024).
  PROBLEM: This is incomplete or possibly outdated. Turkish Wikipedia lists 'Versant, İlbak Holding (2026–present)' as owners. Versant is the NBCUniversal cable spin-off that now holds CNBC. The relaunch date of 10.06.2024 is confirmed.
  CORRECTED: CNBC-e: relaunched 10.06.2024; operated by İlbak Holding with Versant (2026–). Low-to-medium confidence; it only matters for the metadata or About screen.
  SRC: https://tr.wikipedia.org/wiki/CNBC-e
- CLAIM: In 01/2025 RTÜK ordered NOW to change its name after an objection from Istanbul Now TV.
  PROBLEM: The details are slightly off. The RTÜK order (30 days to change name and logo) is dated 31.01.2025. Coverage in February 2025 warned of a possible closure if the channel did not comply. The trademark objection is variously attributed to Huzur Radyo TV's 'Now' mark or to NAM Televizyon. The outcome of the court appeal is not confirmed. The report also omits that NOW's live feed has been inside Disney+ Türkiye since 15.01.2026.
  CORRECTED: NOW (formerly FOX, renamed 12.02.2024; owned by Disney): RTÜK name and logo order of 31.01.2025, appeal outcome unverified. Official live viewing is available to subscribers in Disney+ since 15.01.2026, which makes a Disney+ app intent a legitimate route instead of a WebView for subscribers.
  SRC: https://en.wikipedia.org/wiki/Now_(Turkish_TV_channel) ; https://www.evrensel.net/haber/543444/now-tv-15-gun-icinde-ismini-degistirmezse-kapanabilir ; search results citing Disney+ integration 15.01.2026 (single-source, medium)
- CLAIM: Habertürk and Show TV: TMSF called the sale claims untrue in 12/2025.
  PROBLEM: Minor. The exact date is 13.12.2025, and the statement concerned Habertürk TV.
  CORRECTED: TMSF statement of 13.12.2025: social-media claims that Habertürk TV was sold 'gerçeği yansıtmamaktadır'. Sales are only made through public TMSF announcements.
  SRC: https://www.bloomberght.com/tmsf-haberturk-tv-satildi-iddialari-asilsizdir-3763851

## VERIFY MISSING
- My web-search budget ran out partway through, so I could not re-check these claims and they keep the report's confidence or lower: the ownership of Kanal 7 and Ülke TV, 360 and 24 TV (Türk Medya / Ethem Sancak), TV100 (Gülseven), Haber Global, TV5 and Beyaz TV (Turkish Wikipedia still says Osman Gökçek); the TRT 2, TRT Genç and TRT Spor Yıldız geo-block (I could not test from outside Turkey); tabii free-tier and 99 TL pricing; the dates of Sözcü TV's 2025 blackout and its 2026 fines (only the October 2026 reply-text fine is confirmed); and RTÜK's list of 'ulusal' licence holders.
- Liveness algorithm for the health checker: parse the child playlist, reject #EXT-X-ENDLIST, require MEDIA-SEQUENCE to advance and PROGRAM-DATE-TIME to be recent. As the EBA example shows, a 200 status on the master playlist is not enough.
- daioncdn playlists contain #EXT-X-DISCONTINUITY (Kanal D's discontinuity sequence is about 50k, which suggests server-side ad insertion). Their segment URLs are absolute and point at other hosts (turknet.ercdn.net). Test the Media3 player on the Mi Box against ad-break discontinuities, and make sure any headers or Referer apply on those other hosts too.
- Official app routes that avoid WebView and token problems: tabii's Android TV / Google TV app (TRT channels, UEFA, free account) and Disney+ (NOW live since 15.01.2026, subscription). Whether ATV, Kanal D, Show, Star, TV8, Exxen and similar have Android TV apps, and their package names and intents, was not verified. The plan needs a 'launch external app' source type.
- Star TV: the startv.com.tr live page (Next.js) has no stream URL in its HTML. The URL is fetched by JavaScript. A guessed alias (dogus.daioncdn.net/startv/startv.m3u8?app=startv_web) currently returns 200 but is not taken from the official page. Treat it as fragile or unverified, or use a WebView.
- Pages that embed tokens server-side (confirmed 18.10.2026): NOW (st plus e, about 4 h, in the nowtv.com.tr HTML), DMAX and TLC (token=…; without it the master returns 200 with an empty body), Show TV (a stale 2022 st/e alongside a working app-only URL). CNN Türk's page references live.duhnet.tv/S2/HLS_LIVE/cnnturknp/playlist.m3u8 and CNBC-e's references cnbc-e-live.ercdn.net/cnbce/cnbce_low.m3u8; both return 403 even with Referer and Origin. Beyaz TV's page URL beyaztv.daioncdn.net/…app=fcd5c66b… returns 401 even with Referer. These support the report's WebView/YouTube routing.
- Channels missing from the list: A2 (Turkuvaz, present in iptv-org); Cem TV (returned 11.03.2026, closure announced 05.10.2026); the Europe feeds (ATV Avrupa, Euro D, Euro Star, Kanal 7 Avrupa, TGRT EU), which should be excluded or included deliberately. Completeness against RTÜK's licence lists is still unverified.
- The official live pages of Sözcü TV, TV100 and Haber Global embed a hard-coded YouTube video ID (e.g. ztmY_cCtUl0, 4WSvLRk83-c, EqoCJ8BPxtE) that changes when a stream restarts, so they cannot be used as stable sources. Resolve the live ID dynamically (RSS plus videos.list).
- EPG ('şimdi / sonra' now-and-next data) and channel-logo sourcing are not researched at all, although a family-friendly channel grid normally needs both.
- The report's screen list covers only three edge-case screens. The plan still needs the core screens (home or channel grid with category filter chips, player with an info banner, channel number entry, favourites, settings and About) and the remote key mapping (DPAD, OK, BACK, CH+/CH-, number keys if the Mi Box remote lacks them). Those fall under other research topics and were not verified here.
- Ekotürk buyer (Negmar Denizcilik) rests on a single MedyaRadar report. The buyer's identity was reportedly withheld pending Competition Authority approval, and there is no official TMSF confirmation. Ekotürk's stream (cdn.ekoturk.net/hls/master.m3u8) was confirmed live.
- AYM ruling details for the plan: Law 5894 Additional Article 1 (TFF power to order access blocks without a court decision, added 21.12.2021) was annulled on 17.06.2025, published in the Official Gazette on 14.10.2025, effective 14.07.2026.

## VERIFIED KEY FACTS
- Probes ran from a Turkish IP (Cloudflare trace loc=TR) on 2026-10-18, about 20:00Z.
- TRT lists 18 TV channels: TRT 1, TRT 2, TRT Belgesel, TRT Çocuk, TRT Müzik, TRT Haber, TRT Spor, TRT Spor Yıldız, TRT Avaz, TRT Türk, TRT Kurdî, TRT World, TRT Arabi, TRT EBA, TRT Genç, TRT Diyanet Çocuk, TRT 4K (UHD) and TRT 3 (TBMM TV, on air since 10.12.1994). Source: trt.net.tr/kurumsal/televizyon.
- These TRT masters return 200 with live child playlists: tv-<slug>.medya.trt.com.tr/master.m3u8 for trt1, trt2, trthaber, trtspor1, trtspor2 (Spor Yıldız, 720p25), trtbelgesel, trtcocuk, trtdiyanetcocuk, trtmuzik, trtgenc, trtturk, trtavaz, trtkurdi, trtarabi, trtworld and e-okul01. TRT 1 has variants from 360p to 1440p at 50 fps (H.264/AAC TS). The trt.daioncdn.net trt-1, trtspor and trtspor-yildiz masters (?app=clean) also return 200. tabii.com's homepage HTML embeds these URLs.
- trtizle.com and trtizle.com/canli redirect (301) to https://www.tabii.com/.
- The private-channel HLS URLs in the report return 200 with current child playlists: Kanal D (daioncdn and the kdlive.duhnet.tv alternate), Show TV, Star TV (alias app), TV8, Kanal 7 (no app parameter), teve2 (daioncdn), 360, Show Türk, NTV, Habertürk, TGRT Haber, 24 TV, TVNET (URL is on tvnet.com.tr), HT Spor, Bloomberg HT, Ekotürk, TBMM TV, Halk TV daioncdn, Dream Türk and TGRT Belgesel (mediatriple).
- The TMSF timeline is confirmed by Medyascope (14.10.2026): Flash TV trustee 15.03.2025; Ekotürk 11.08.2025 (Papara illegal-betting probe); Can Medya group (Habertürk, Bloomberg HT, Show TV, Show Max, Show Türk, HT Spor) 11.09.2025; TELE1 24.10.2025 (espionage probe); Halk TV 14.10.2026. On 23.09.2026, 33 Can Holding companies were returned to the group, with the media companies excluded. The Can Medya board term runs to 12.10.2028. İbrahim Paşalı is the trustee across these outlets.
- Halk TV trustee appointment on 14.10.2026 is confirmed. Owner Cafer Mahiroğlu faces bribery, bid-rigging and money-laundering allegations; 17 detention orders were issued and 11 people detained. No permanent closure has been announced.
- Flash TV was sold to Safe Media (chair Seyfi Şahin) on 31.08.2026 for a family entertainment format. The earlier sale to Eşref Keleş (Öz Er-Ka) at the 84 M TL reserve price was reportedly cancelled in April 2026. Live programming ended around 17.05.2026.
- Ekol TV closed on 22.12.2025, citing changes in its capital structure; about 300 journalists were affected.
- FOX was renamed NOW on 12.02.2024. The owner is Disney, and no sale was found.
- TMSF said on 13.12.2025 that the Habertürk TV sale claims were untrue.
- YouTube live (isLiveNow=true, official owner names) was confirmed on 18.10.2026 for A Haber, A Spor, A Para, Sözcü, TV100, Haber Global, CNN Türk, NTV, Habertürk, TRT Haber, TRT 1, TGRT Haber, TVNET, Bloomberg HT, CNBC-e, teve2, Kanal D, HT Spor (7/24), Ülke TV, Akit TV, Diyanet TV, Ulusal Kanal and Show TV (showing Show MAX). Star TV, NOW, TV8, Kanal 7 and Beyaz TV were not live. @atv resolves to an unrelated 'hkatv' channel.
- The official Sözcü TV, TV100 and Haber Global live pages embed YouTube.
- The iptv-org streams/tr.m3u has exactly 221 entries. It lacks CNN Türk, Show TV, teve2, Sözcü, TV100, A Para, A News, Akit, Ulusal, TV5, DMAX, TLC, TV8.5 and Show Max.
- Law 5651 m.4/2 (no liability for linked third-party content unless the presentation shows adoption) is confirmed verbatim.
- Law 6112 m.29/A (added 21.3.2018 by Law 7103) is confirmed. Platform operators transmitting radio, TV and on-demand services over the internet need a 'yayın iletim yetkisi' (broadcast transmission authorisation). Under paragraph 4, individual communication and platforms not dedicated to transmitting such services are not platform operators.
- FSEK m.71 is confirmed: 1-5 years prison or a judicial fine for communicating protected works to the public without written permission.
- The TFF–Digiturk (beIN) Süper Lig and 1. Lig deal covers 2024-25 to 2026-27. UEFA club matches are free on TRT 1, TRT Spor and tabii.
- The AYM annulment of TFF's power to block access without a court order took effect on 14.07.2026.
- YouTube's rules are confirmed. Embedded players must send an HTTP Referer, so an Android WebView uses loadDataWithBaseURL or a Referer header set to the app ID. No overlays may cover the player, and its minimum size is 200x200. search.list is limited to 100 calls per day by default, inside a 10,000-unit daily allocation.