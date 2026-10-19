// HLS live-stream probe. Usage:
//   node probe.mjs <url> [--referer=URL] [--ua=STRING] [--origin=URL] [--wait=6000]
// Prints one JSON object describing: HTTP status, master variants (resolution/bandwidth),
// the best variant's media playlist liveness (two polls), and whether a media segment downloads.
import { setTimeout as sleep } from "node:timers/promises";

const args = process.argv.slice(2);
const url = args.find((a) => !a.startsWith("--"));
const opt = Object.fromEntries(args.filter((a) => a.startsWith("--")).map((a) => {
  const [k, ...v] = a.slice(2).split("=");
  return [k, v.join("=") || "true"];
}));
const WAIT = Number(opt.wait || 6000);
const UA_DEFAULT = "Mozilla/5.0 (Linux; Android 11; MiTV-AFKR0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

function headers() {
  const h = { "User-Agent": opt.ua || UA_DEFAULT, Accept: "*/*" };
  if (opt.referer) h.Referer = opt.referer;
  if (opt.origin) h.Origin = opt.origin;
  return h;
}

async function get(u, { range, timeout = 12000 } = {}) {
  const ac = new AbortController();
  const t = setTimeout(() => ac.abort(), timeout);
  const started = Date.now();
  try {
    const h = headers();
    if (range) h.Range = range;
    const res = await fetch(u, { headers: h, redirect: "follow", signal: ac.signal });
    const buf = Buffer.from(await res.arrayBuffer());
    return { ok: true, status: res.status, finalUrl: res.url, contentType: res.headers.get("content-type") || "", cors: res.headers.get("access-control-allow-origin") || "", bytes: buf.length, buf, ms: Date.now() - started };
  } catch (e) {
    return { ok: false, status: 0, error: String(e && e.cause ? e.cause.code || e.cause.message || e.cause : e.name || e), ms: Date.now() - started };
  } finally {
    clearTimeout(t);
  }
}

function parseAttrs(line) {
  const out = {};
  const re = /([A-Z0-9-]+)=("([^"]*)"|[^,]*)/g;
  let m;
  while ((m = re.exec(line))) out[m[1]] = m[3] !== undefined ? m[3] : m[2];
  return out;
}

function parseMaster(text, base) {
  const lines = text.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
  const variants = [];
  for (let i = 0; i < lines.length; i++) {
    if (lines[i].startsWith("#EXT-X-STREAM-INF")) {
      const a = parseAttrs(lines[i]);
      const uri = lines[i + 1] && !lines[i + 1].startsWith("#") ? lines[i + 1] : null;
      if (!uri) continue;
      let abs = uri;
      try { abs = new URL(uri, base).toString(); } catch {}
      variants.push({ bandwidth: Number(a.BANDWIDTH || a["AVERAGE-BANDWIDTH"] || 0), resolution: a.RESOLUTION || "", frameRate: a["FRAME-RATE"] || "", codecs: a.CODECS || "", uri: abs });
    }
  }
  return variants.sort((x, y) => y.bandwidth - x.bandwidth);
}

function parseMedia(text, base) {
  const lines = text.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
  const seqLine = lines.find((l) => l.startsWith("#EXT-X-MEDIA-SEQUENCE"));
  const tdLine = lines.find((l) => l.startsWith("#EXT-X-TARGETDURATION"));
  const pdt = lines.filter((l) => l.startsWith("#EXT-X-PROGRAM-DATE-TIME")).pop();
  const segs = [];
  for (let i = 0; i < lines.length; i++) {
    if (lines[i].startsWith("#EXTINF")) {
      const uri = lines[i + 1] && !lines[i + 1].startsWith("#") ? lines[i + 1] : null;
      if (uri) { let abs = uri; try { abs = new URL(uri, base).toString(); } catch {} segs.push(abs); }
    }
  }
  return {
    mediaSequence: seqLine ? Number(seqLine.split(":")[1]) : null,
    targetDuration: tdLine ? Number(tdLine.split(":")[1]) : null,
    endList: lines.includes("#EXT-X-ENDLIST"),
    lastPdt: pdt ? pdt.split(/:(.+)/)[1] : null,
    segmentCount: segs.length,
    lastSegment: segs[segs.length - 1] || null,
    discontinuity: lines.some((l) => l.startsWith("#EXT-X-DISCONTINUITY")),
  };
}

async function main() {
  if (!url) { console.log(JSON.stringify({ error: "no url" })); process.exit(2); }
  const out = { url, referer: opt.referer || null, probedAt: new Date().toISOString() };
  const m = await get(url);
  out.master = { status: m.status, finalUrl: m.finalUrl, contentType: m.contentType, cors: m.cors, bytes: m.bytes, ms: m.ms, error: m.error };
  if (!m.ok || m.status >= 400 || !m.buf) { out.verdict = "DOWN"; out.reason = m.error || `HTTP ${m.status}`; return console.log(JSON.stringify(out)); }
  const text = m.buf.toString("utf8");
  if (!text.includes("#EXTM3U")) { out.verdict = "DOWN"; out.reason = "not an m3u8 (" + text.slice(0, 60).replace(/\s+/g, " ") + ")"; return console.log(JSON.stringify(out)); }
  let mediaUrl = m.finalUrl || url;
  let variants = parseMaster(text, mediaUrl);
  out.variants = variants.map((v) => ({ resolution: v.resolution, bandwidth: v.bandwidth, frameRate: v.frameRate, codecs: v.codecs }));
  let mediaText = text;
  if (variants.length) {
    mediaUrl = variants[0].uri;
    const c = await get(mediaUrl);
    out.media = { status: c.status, bytes: c.bytes, ms: c.ms, error: c.error, url: mediaUrl };
    if (!c.ok || c.status >= 400) { out.verdict = "DOWN"; out.reason = "variant playlist " + (c.error || `HTTP ${c.status}`); return console.log(JSON.stringify(out)); }
    mediaText = c.buf.toString("utf8");
    // Some CDNs return a second-level master (e.g. tokenised redirect). Follow one more level.
    const inner = parseMaster(mediaText, c.finalUrl || mediaUrl);
    if (inner.length) { mediaUrl = inner[0].uri; const c2 = await get(mediaUrl); if (c2.ok && c2.status < 400) mediaText = c2.buf.toString("utf8"); }
  } else {
    out.media = { url: mediaUrl, note: "url is a media playlist, no variants" };
  }
  const p1 = parseMedia(mediaText, mediaUrl);
  out.poll1 = p1;
  if (p1.endList) { out.verdict = "VOD"; out.reason = "playlist has ENDLIST (recorded, not live)"; return console.log(JSON.stringify(out)); }
  if (!p1.segmentCount) { out.verdict = "DOWN"; out.reason = "media playlist has no segments"; return console.log(JSON.stringify(out)); }
  await sleep(WAIT);
  const c3 = await get(mediaUrl);
  const p2 = c3.ok && c3.status < 400 ? parseMedia(c3.buf.toString("utf8"), mediaUrl) : null;
  out.poll2 = p2;
  const advancing = p2 && ((p2.mediaSequence != null && p1.mediaSequence != null && p2.mediaSequence > p1.mediaSequence) || (p2.lastSegment && p2.lastSegment !== p1.lastSegment));
  // Fetch the newest segment (short-window playlists such as Ekotürk's 2 s segments expire the poll-1 segment within seconds).
  const segUrl = (p2 && p2.lastSegment) || p1.lastSegment;
  let seg = await get(segUrl, { range: "bytes=0-2047", timeout: 15000 });
  if (!(seg.ok && (seg.status === 200 || seg.status === 206))) { await sleep(1500); seg = await get(segUrl, { range: "bytes=0-2047", timeout: 15000 }); }
  const head = seg.buf ? seg.buf.subarray(0, 8) : Buffer.alloc(0);
  const isTs = head[0] === 0x47;
  const isMp4 = head.length >= 8 && ["ftyp", "styp", "moof", "sidx"].includes(head.subarray(4, 8).toString("latin1"));
  out.segment = { status: seg.status, contentType: seg.contentType, bytes: seg.bytes, ms: seg.ms, error: seg.error, looksLikeMedia: isTs || isMp4 };
  const segOk = seg.ok && (seg.status === 200 || seg.status === 206) && (isTs || isMp4 || seg.bytes > 1000);
  if (advancing && segOk) out.verdict = "LIVE";
  else if (segOk && !advancing) { out.verdict = "STALE"; out.reason = "segments download but playlist did not advance in " + WAIT + " ms"; }
  else { out.verdict = "DOWN"; out.reason = "segment " + (seg.error || `HTTP ${seg.status}`); }
  const best = out.variants && out.variants[0];
  out.best = best ? `${best.resolution || "?"} @ ${Math.round(best.bandwidth / 1000)} kbps` : "single variant";
  console.log(JSON.stringify(out));
}
main().catch((e) => { console.log(JSON.stringify({ url, verdict: "DOWN", reason: "probe crashed: " + e.message })); });
