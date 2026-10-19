// Probes every enabled direct-HLS source in channels.json sequentially (conc 2) and writes a verification file.
import { spawn } from "node:child_process";
import { readFileSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
const here = dirname(fileURLToPath(import.meta.url));
const [inPath, outPath] = process.argv.slice(2);
const doc = JSON.parse(readFileSync(inPath, "utf8"));
const jobs = [];
for (const c of doc.channels) for (const s of c.sources) if (s.type === "hls" && s.enabled) jobs.push({ id: c.id, s });
function probe(s) {
  return new Promise((resolve) => {
    const args = [join(here, "probe.mjs"), s.url, "--wait=14000"];
    if (s.headers && s.headers.Referer) args.push(`--referer=${s.headers.Referer}`, `--origin=${s.headers.Origin}`);
    const p = spawn(process.execPath, args, { stdio: ["ignore", "pipe", "pipe"] });
    let out = ""; p.stdout.on("data", (d) => (out += d));
    const t = setTimeout(() => { p.kill(); resolve({ verdict: "DOWN", reason: "timeout" }); }, 70000);
    p.on("close", () => { clearTimeout(t); try { resolve(JSON.parse(out.trim().split("\n").pop())); } catch { resolve({ verdict: "DOWN", reason: "unparseable" }); } });
  });
}
const results = []; let i = 0;
async function worker() { while (i < jobs.length) { const j = jobs[i++]; let r = await probe(j.s); if (r.verdict !== "LIVE") { await new Promise((x) => setTimeout(x, 3000)); r = await probe(j.s); r.retried = true; } results.push({ id: j.id, url: j.s.url, verdict: r.verdict, best: r.best, reason: r.reason, retried: !!r.retried, probedAt: r.probedAt }); console.log(j.id.padEnd(16), (r.verdict || "?").padEnd(6), (r.best || "").padEnd(26), r.retried ? "(retry)" : "", r.reason || ""); } }
await Promise.all([worker(), worker()]);
writeFileSync(outPath, JSON.stringify(results, null, 1));
const bad = results.filter((r) => r.verdict !== "LIVE");
console.log(`\n${results.length} source probed, ${results.length - bad.length} LIVE, ${bad.length} not:`, bad.map((b) => b.id + ":" + b.verdict).join(", ") || "none");
