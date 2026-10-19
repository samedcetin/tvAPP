// Source: https://github.com/iptv-org/iptv/blob/master/streams/tr.m3u
// Included streams passed HTTPS manifest, media-segment, and browser CORS validation.
export const streamUrls: Readonly<Record<string, string>> = {
  "trt1": "https://tv-trt1.medya.trt.com.tr/master.m3u8",
  "tv360": "https://turkmedya-live.ercdn.net/tv360/tv360.m3u8",
  "trthaber": "https://tv-trthaber.medya.trt.com.tr/master.m3u8",
  "haberturk": "https://tv.ensonhaber.com/haberturk/haberturk.m3u8",
  "haberglobal": "https://tv.ensonhaber.com/haberglobal/haberglobal.m3u8",
  "tv24": "https://turkmedya-live.ercdn.net/tv24/tv24.m3u8",
  "benguturk": "https://tv.ensonhaber.com/benguturk/benguturk.m3u8",
  "ekoturk": "https://cdn.ekoturk.net/hls/master.m3u8",
  "trtcocuk": "https://tv-trtcocuk.medya.trt.com.tr/master.m3u8",
  "trtdiyanetcocuk": "https://tv-trtdiyanetcocuk.medya.trt.com.tr/master.m3u8",
  "trtmuzik": "https://tv-trtmuzik.medya.trt.com.tr/master.m3u8",
  "trtgenc": "https://tv-trtgenc.medya.trt.com.tr/master.m3u8",
  "diyanettv": "https://eustr73.mediatriple.net/videoonlylive/mtikoimxnztxlive/broadcast_5e3bf95a47e07.smil/playlist.m3u8",
  "tbmmtv": "https://meclistv-live.ercdn.net/meclistv/meclistv.m3u8",
  "trtturk": "https://tv-trtturk.medya.trt.com.tr/master.m3u8",
  "trtavaz": "https://tv-trtavaz.medya.trt.com.tr/master.m3u8",
  "trtkurdi": "https://tv-trtkurdi.medya.trt.com.tr/master.m3u8",
  "trtarabi": "https://tv-trtarabi.medya.trt.com.tr/master.m3u8",
  "trteba": "https://tv-e-okul01.medya.trt.com.tr/master.m3u8"
} as const;

export function getStreamUrl(channelId: string) {
  return streamUrls[channelId];
}

export function hasVerifiedStream(channelId: string) {
  return Boolean(streamUrls[channelId]);
}
