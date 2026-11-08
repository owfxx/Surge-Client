import { adminSetup, adminApi, publicData, redeemCode, inbox, claimGifts, playerExtras, capeImage } from './admin.js';
import { ADMIN_PAGE } from './admin-page.js';
const FRESH_MS = 15 * 60 * 1000;
const ONLINE_MS = 3 * 60 * 1000;
const UUID_RE = /^[0-9a-f]{32}$/;
const clean = v => (typeof v === 'string' && /^[a-z0-9_:\-]{0,40}$/i.test(v) ? v : 'none');
const norm = u => String(u || '').replace(/-/g, '').toLowerCase();
const json = (data, status = 200) => new Response(JSON.stringify(data), {
  status, headers: { 'content-type': 'application/json', 'access-control-allow-origin': '*' },
});
const STATUSES = ['online', 'idle', 'busy', 'invisible'];

let setupDone = false;

async function setup(db) {
  if (setupDone) return;
  await db.batch([
    db.prepare('CREATE TABLE IF NOT EXISTS players (uuid TEXT PRIMARY KEY, name TEXT, data TEXT, seen INTEGER)'),
    db.prepare('CREATE TABLE IF NOT EXISTS users (uuid TEXT PRIMARY KEY, name TEXT, name_lc TEXT, token TEXT, status TEXT, server TEXT, seen INTEGER)'),
    db.prepare('CREATE INDEX IF NOT EXISTS users_name ON users(name_lc)'),
    db.prepare('CREATE INDEX IF NOT EXISTS users_token ON users(token)'),
    db.prepare('CREATE TABLE IF NOT EXISTS friends (a TEXT, b TEXT, state TEXT, requester TEXT, PRIMARY KEY (a, b))'),
    db.prepare('CREATE TABLE IF NOT EXISTS blocks (owner TEXT, target TEXT, PRIMARY KEY (owner, target))'),
    db.prepare('CREATE TABLE IF NOT EXISTS mutes (owner TEXT, target TEXT, PRIMARY KEY (owner, target))'),
    db.prepare('CREATE TABLE IF NOT EXISTS messages (id INTEGER PRIMARY KEY AUTOINCREMENT, sender TEXT, receiver TEXT, text TEXT, ts INTEGER)'),
    db.prepare('CREATE INDEX IF NOT EXISTS messages_pair ON messages(receiver, id)'),
  ]);
  for (const col of ['edited INTEGER DEFAULT 0', 'deleted INTEGER DEFAULT 0', 'updated INTEGER DEFAULT 0']) {
    try { await db.prepare(`ALTER TABLE messages ADD COLUMN ${col}`).run(); } catch { }
  }
  try { await db.prepare('ALTER TABLE users ADD COLUMN country TEXT').run(); } catch { }
  setupDone = true;
}

async function lookupName(name) {
  if (!/^[A-Za-z0-9_]{1,16}$/.test(name || '')) return null;
  try {
    const r = await fetch(`https://playerdb.co/api/player/minecraft/${encodeURIComponent(name)}`, { headers: { 'User-Agent': 'SurgeClient/1.0' } });
    if (!r.ok) return null;
    const d = await r.json();
    const p = d && d.data && d.data.player;
    if (!p || !p.raw_id || !p.username) return null;
    return { uuid: norm(p.raw_id), name: p.username };
  } catch { return null; }
}

const pair = (x, y) => (x < y ? [x, y] : [y, x]);

async function me(req, db) {
  const m = (req.headers.get('authorization') || '').match(/^Bearer ([a-f0-9]{48})$/);
  if (!m) return null;
  return db.prepare('SELECT uuid, name FROM users WHERE token = ?1').bind(m[1]).first();
}

async function userByName(db, name) {
  return db.prepare('SELECT uuid, name FROM users WHERE name_lc = ?1').bind(String(name || '').toLowerCase()).first();
}

async function blocked(db, x, y) {
  return !!(await db.prepare('SELECT 1 FROM blocks WHERE (owner = ?1 AND target = ?2) OR (owner = ?2 AND target = ?1)').bind(x, y).first());
}

const REPO = env => env.REPO || 'owfxx/Surge-Client';
let latestCache = null;

async function latest(env) {
  if (latestCache && Date.now() - latestCache.at < 10 * 60000) return latestCache;
  const r = await fetch(`https://api.github.com/repos/${REPO(env)}/releases/latest`, { headers: { 'User-Agent': 'surge-client-site', Accept: 'application/vnd.github+json' } });
  if (!r.ok) return latestCache;
  const rel = await r.json();
  const assets = rel.assets || [];
  const setup = assets.find(a => a.name === 'Surge-Client-Setup.exe') || assets.find(a => /^Surge-Client-Setup-[\d.]+\.exe$/.test(a.name));
  const exe = setup || assets.find(a => /Standalone\.exe$/.test(a.name));
  if (!exe) return latestCache;
  const jar = assets.find(a => /^surge-client-.+\.jar$/.test(a.name));
  const pkg = assets.find(a => /\.nsis\.7z$/.test(a.name));
  latestCache = { at: Date.now(), notes: String(rel.body || ''), version: String(rel.tag_name || '').replace(/^v/, ''), size: exe.size, url: exe.browser_download_url, setup: !!setup, pkg: pkg ? pkg.browser_download_url : null, jar: jar ? { name: jar.name, url: jar.browser_download_url } : null };
  return latestCache;
}

async function packageFile(env) {
  latestCache = null;
  const l = await latest(env).catch(() => null);
  if (!l || !l.pkg) return new Response('Download is not available right now. Try again in a minute.', { status: 502 });
  return Response.redirect(l.pkg, 302);
}

async function download(env, mod) {
  const l = await latest(env).catch(() => null);
  const base = `https://github.com/${REPO(env)}/releases/latest/download/`;
  const src = mod
    ? (l && l.jar ? l.jar : { url: base + 'Surge-Client-Mod.jar', name: 'surge-client-mc1.21.11.jar' })
    : { url: l ? l.url : base + 'Surge-Client-Setup.exe', name: l && !l.setup ? 'Surge Client.exe' : 'Surge Client Setup.exe' };
  const r = await fetch(src.url, { redirect: 'follow', headers: { 'User-Agent': 'surge-client-site' } });
  if (!r.ok) return new Response('Download is not available right now. Try again in a minute.', { status: 502 });
  const h = new Headers({ 'content-type': mod ? 'application/java-archive' : 'application/octet-stream', 'content-disposition': `attachment; filename="${src.name}"`, 'cache-control': 'no-store' });
  const len = r.headers.get('content-length');
  if (len) h.set('content-length', len);
  return new Response(r.body, { headers: h });
}

const DISCORD = 'https://discord.gg/7wGxqGN86R';
const SHOWCASE = [['koi', 'Koi', 24, 120], ['phoenix', 'Phoenix', 20, 90], ['storm', 'Storm', 24, 110], ['bloodmoon', 'Blood Moon', 20, 120], ['obsidian', 'Obsidian', 16, 110], ['lava', 'Lava', 20, 110], ['matrix', 'Matrix', 20, 80], ['frost', 'Frost', 16, 130]];

const MARK = `<svg viewBox="0 0 120 140" aria-hidden="true"><path fill="#fff" d="M30 80a22 22 0 0 1-2-44 30 30 0 0 1 57-8 24 24 0 0 1 7 52z"/><path fill="#fff" d="M64 84 44 112h14l-6 26 24-34H62l8-20z"/></svg>`;

const esc = s => String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

function notesHtml(body) {
  const lines = String(body || '').split(/\r?\n/).map(x => x.trim()).filter(x => /^[-*] /.test(x)).slice(0, 6);
  return lines.map(x => `<li>${esc(x.slice(2).replace(/\*\*/g, ''))}</li>`).join('');
}

function page(l, stats) {
  const ver = l ? `Version ${esc(l.version)}` : 'Latest version';
  const notes = l ? notesHtml(l.notes) : '';
  const live = stats && stats.online > 0 ? `<span class="live"><i></i>${stats.online} playing right now</span>` : '';
  const icon = encodeURIComponent(MARK.replace('<svg ', '<svg xmlns="http://www.w3.org/2000/svg" '));
  return `<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Surge Client</title><meta name="description" content="A fast Minecraft launcher and client for 1.21.11 with 40+ mods, friends, cosmetics and emotes.">
<meta property="og:title" content="Surge Client"><meta property="og:description" content="A fast Minecraft launcher and client for 1.21.11."><meta name="theme-color" content="#0e0e11">
<link rel="icon" href="data:image/svg+xml,${icon}">
<link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin><link href="https://fonts.googleapis.com/css2?family=Pixelify+Sans:wght@500;700&family=Rubik:wght@400;500;600&display=swap" rel="stylesheet">
<style>
:root{--bg:#0e0e11;--panel:#16161a;--panel2:#1c1c21;--line:#26262d;--text:#f4f4f5;--muted:#a1a1aa;--faint:#6b6b74;--gold:#f2c430;--gold2:#ffe07a}
*{box-sizing:border-box}html{scroll-behavior:smooth}html,body{margin:0;overflow-x:clip}
body{background:var(--bg);color:var(--text);font:16px/1.65 Rubik,system-ui,sans-serif;overflow-x:hidden;-webkit-font-smoothing:antialiased}
a{color:inherit}
.px{font-family:"Pixelify Sans",sans-serif}
.wrap{max-width:1080px;margin:0 auto;padding:0 20px}
nav{position:sticky;top:0;z-index:20;backdrop-filter:blur(12px);background:#0e0e11cc;border-bottom:1px solid #ffffff0a}
nav .wrap{display:flex;align-items:center;gap:22px;height:60px}
.brand{display:flex;align-items:center;gap:10px;text-decoration:none;font:700 20px "Pixelify Sans",sans-serif}
.brand svg{width:24px;height:28px}
nav .links{margin-left:auto;display:flex;gap:22px;align-items:center}
nav .links a{color:var(--muted);text-decoration:none;font-size:15px;transition:color .15s}
nav .links a:hover{color:var(--text)}
nav .links .pill{color:var(--text);border:1px solid var(--line);padding:6px 14px;border-radius:999px;background:var(--panel)}
nav .links .pill:hover{border-color:#5865f2;background:#5865f222}
@media (max-width:640px){nav .links a:not(.pill){display:none}}
.hero{position:relative;padding:84px 0 70px;text-align:center;isolation:isolate;overflow:hidden}
#rain{position:absolute;inset:0;width:100%;height:100%;z-index:-1;opacity:.35;pointer-events:none;-webkit-mask-image:linear-gradient(#000 60%,transparent);mask-image:linear-gradient(#000 60%,transparent)}
.hero::before{content:"";position:absolute;inset:-20% -10% auto;height:620px;z-index:-2;background:radial-gradient(560px 360px at 50% 30%,#3a3a44 0%,#1d1d23 45%,transparent 75%)}
.flash{position:absolute;inset:0;z-index:-1;background:radial-gradient(500px 300px at 50% 28%,#fff3,transparent 70%);opacity:0;pointer-events:none}
.storm{width:132px;height:154px;margin:0 auto;position:relative}
.storm svg{width:100%;height:100%;overflow:visible;filter:drop-shadow(0 14px 30px #000a)}
.storm .cloud{animation:drift 6s ease-in-out infinite}
.storm .bolt{transform-origin:60px 100px}
@keyframes drift{50%{transform:translateY(-6px)}}
h1{font:700 clamp(48px,9vw,96px)/1 "Pixelify Sans",sans-serif;margin:22px 0 14px;letter-spacing:.5px}
.lead{color:#d4d4d8;font-size:clamp(17px,2.2vw,20px);max-width:560px;margin:0 auto 34px}
.cta{display:flex;gap:12px;justify-content:center;flex-wrap:wrap}
.dl{display:inline-flex;align-items:center;gap:12px;background:var(--gold);color:#1a1405;text-decoration:none;font:700 20px "Pixelify Sans",sans-serif;padding:15px 28px;border-radius:4px;box-shadow:0 5px 0 #b38a12,0 14px 40px #f2c43033;transition:transform .12s,box-shadow .12s,background .15s}
.dl:hover{background:var(--gold2);transform:translateY(-2px);box-shadow:0 7px 0 #b38a12,0 20px 50px #f2c43055}
.dl:active{transform:translateY(4px);box-shadow:0 1px 0 #b38a12}
.dl svg{width:22px;height:22px}
.ghost{display:inline-flex;align-items:center;gap:10px;color:var(--text);text-decoration:none;font:700 18px "Pixelify Sans",sans-serif;padding:15px 24px;border-radius:4px;border:1px solid var(--line);background:var(--panel);transition:border-color .15s,background .15s}
.ghost:hover{border-color:#5865f2;background:#5865f21f}
.ghost svg{width:22px;height:22px}
.meta{color:var(--muted);font-size:14px;margin-top:18px;display:flex;gap:14px;justify-content:center;flex-wrap:wrap;align-items:center}
.live{display:inline-flex;align-items:center;gap:8px;color:#d4d4d8}
.live i{width:8px;height:8px;border-radius:50%;background:#4ade80;box-shadow:0 0 0 0 #4ade80aa;animation:ping 2s infinite}
@keyframes ping{70%{box-shadow:0 0 0 8px #4ade8000}100%{box-shadow:0 0 0 0 #4ade8000}}
.alt{color:var(--faint);font-size:14px;margin:10px auto 0}.alt a{color:var(--muted);text-underline-offset:3px}.alt a:hover{color:var(--text)}
section{padding:70px 0}
h2{font:700 clamp(28px,4vw,40px)/1.15 "Pixelify Sans",sans-serif;margin:0 0 10px}
.sub{color:var(--muted);margin:0 0 34px;max-width:560px}
.grid{display:grid;grid-template-columns:repeat(3,1fr);gap:12px}@media (max-width:900px){.grid{grid-template-columns:repeat(2,1fr)}}@media (max-width:560px){.grid{grid-template-columns:1fr}}
.f{background:var(--panel);border:1px solid var(--line);border-radius:6px;padding:22px;transition:border-color .2s,transform .2s,background .2s}
.f:hover{border-color:#3a3a44;background:var(--panel2);transform:translateY(-3px)}
.f .ic{width:34px;height:34px;border-radius:6px;display:grid;place-items:center;background:#f2c43014;color:var(--gold);margin-bottom:14px}
.f .ic svg{width:20px;height:20px}
.f h3{font:700 20px "Pixelify Sans",sans-serif;margin:0 0 6px}.f p{margin:0;color:var(--muted);font-size:15px}
.f.wide{grid-column:span 2}@media (max-width:560px){.f.wide{grid-column:auto}}
.f .tag{display:inline-block;font-size:12px;color:var(--gold);border:1px solid #f2c43044;border-radius:999px;padding:1px 9px;margin-left:8px;vertical-align:3px;font-family:Rubik,sans-serif;font-weight:500}
.split{display:grid;grid-template-columns:1.1fr 1fr;gap:40px;align-items:start}
@media (max-width:820px){.split{grid-template-columns:1fr}}
.steps{list-style:none;margin:0;padding:0;counter-reset:s}
.steps li{counter-increment:s;position:relative;padding:0 0 26px 56px}
.steps li::before{content:counter(s);position:absolute;left:0;top:-2px;width:38px;height:38px;border-radius:6px;display:grid;place-items:center;font:700 18px "Pixelify Sans",sans-serif;background:var(--panel);border:1px solid var(--line);color:var(--gold)}
.steps li::after{content:"";position:absolute;left:19px;top:40px;bottom:6px;width:1px;background:var(--line)}
.steps li:last-child::after{display:none}
.steps b{color:var(--text);font-weight:600}.steps p{margin:2px 0 0;color:var(--muted)}
.new{background:var(--panel);border:1px solid var(--line);border-radius:6px;padding:24px}
.new h3{font:700 22px "Pixelify Sans",sans-serif;margin:0 0 12px;display:flex;align-items:center;gap:10px}
.new h3 span{font:500 13px Rubik,sans-serif;color:#1a1405;background:var(--gold);border-radius:999px;padding:2px 10px}
.new ul{margin:0;padding-left:18px;color:var(--muted);font-size:15px}.new li{margin:7px 0}.new li::marker{color:var(--gold)}
.faq{max-width:760px}
details{border-bottom:1px solid var(--line)}
summary{cursor:pointer;list-style:none;padding:18px 36px 18px 0;font-weight:500;font-size:17px;position:relative}
summary::-webkit-details-marker{display:none}
summary::after{content:"";position:absolute;right:6px;top:50%;width:10px;height:10px;border-right:2px solid var(--muted);border-bottom:2px solid var(--muted);transform:translateY(-70%) rotate(45deg);transition:transform .2s}
details[open] summary::after{transform:translateY(-30%) rotate(225deg)}
details p{margin:0 0 18px;color:var(--muted)}
.capes{display:flex;gap:18px;overflow-x:auto;padding:6px 2px 18px;scroll-snap-type:x mandatory;scrollbar-width:thin}
.cape{flex:none;scroll-snap-align:start;text-align:center;width:132px}
.cape .c{width:120px;height:192px;margin:0 auto 10px;border-radius:6px;background-repeat:no-repeat;image-rendering:pixelated;box-shadow:0 18px 40px -16px #000,0 0 0 1px #ffffff14;transition:transform .25s cubic-bezier(.2,.8,.3,1.3)}
.cape:hover .c{transform:translateY(-6px) rotate(-2deg)}
.cape b{font:700 16px "Pixelify Sans",sans-serif}
.navdl{display:none!important;background:var(--gold)!important;color:#1a1405!important;border-color:var(--gold)!important;font-weight:600}
nav.scrolled .navdl{display:inline-block!important}
.band{margin:40px 0 0;border:1px solid var(--line);border-radius:8px;padding:38px;display:flex;gap:24px;align-items:center;justify-content:space-between;flex-wrap:wrap;background:linear-gradient(120deg,#1a1a2e,#16161a 60%)}
.band h2{margin:0 0 4px}.band p{margin:0;color:var(--muted)}
footer{color:var(--faint);font-size:13px;padding:40px 0 50px;text-align:center}
footer a{color:var(--muted);text-underline-offset:3px}
:focus-visible{outline:2px solid var(--gold);outline-offset:3px}
.in{opacity:0;transform:translateY(14px);animation:in .7s cubic-bezier(.2,.7,.2,1) forwards}
.rv{opacity:0;transform:translateY(18px);transition:opacity .6s cubic-bezier(.2,.7,.2,1),transform .6s cubic-bezier(.2,.7,.2,1)}.rv.on{opacity:1;transform:none}
.snd{position:fixed;right:16px;bottom:16px;z-index:30;width:40px;height:40px;border-radius:50%;border:1px solid var(--line);background:#16161ae6;color:var(--muted);display:grid;place-items:center;cursor:pointer;transition:color .15s,border-color .15s}.snd:hover{color:var(--text);border-color:#3a3a44}.snd svg{width:18px;height:18px}
.d1{animation-delay:.1s}.d2{animation-delay:.22s}.d3{animation-delay:.34s}.d4{animation-delay:.46s}
@keyframes in{to{opacity:1;transform:none}}
@media (prefers-reduced-motion:reduce){*{animation:none!important;transition:none!important}.in,.rv{opacity:1;transform:none}#rain{display:none}}
</style></head><body>
<nav><div class="wrap"><a class="brand" href="#top">${MARK}Surge</a><div class="links"><a href="#features">Features</a><a href="#start">Get started</a><a href="#faq">FAQ</a><a href="#capes">Cosmetics</a><a class="pill" href="${DISCORD}" rel="noopener">Discord</a><a class="pill navdl" href="/download">Download</a></div></div></nav>
<header class="hero" id="top"><canvas id="rain" aria-hidden="true"></canvas><div class="flash" id="flash"></div>
<div class="wrap">
<div class="storm in"><svg viewBox="0 0 120 140" aria-hidden="true"><path class="cloud" fill="#fff" d="M30 80a22 22 0 0 1-2-44 30 30 0 0 1 57-8 24 24 0 0 1 7 52z"/><path class="bolt" id="bolt" fill="#fff" d="M64 84 44 112h14l-6 26 24-34H62l8-20z"/></svg></div>
<h1 class="in d1">Surge Client</h1>
<p class="lead in d2">A fast Minecraft launcher and client for 1.21.11. Mods, friends and cosmetics, ready the moment you press Play.</p>
<div class="cta in d3"><a class="dl" href="/download"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><path d="M12 3v13m-6-6 6 6 6-6M4 21h16"/></svg>Download for Windows</a><a class="ghost" href="${DISCORD}" rel="noopener"><svg viewBox="0 0 24 24" fill="currentColor"><path d="M20 5.3A16.6 16.6 0 0 0 15.9 4l-.5 1a15.4 15.4 0 0 0-4.8 0L10.1 4A16.6 16.6 0 0 0 6 5.3 17.3 17.3 0 0 0 3 17.1 16.8 16.8 0 0 0 8.1 19.7l1.1-1.7a10.8 10.8 0 0 1-1.7-.8l.4-.3a11.9 11.9 0 0 0 10.2 0l.4.3c-.5.3-1.1.6-1.7.8l1.1 1.7A16.8 16.8 0 0 0 21 17.1 17.2 17.2 0 0 0 20 5.3ZM9.3 14.7c-1 0-1.8-.9-1.8-2s.8-2 1.8-2 1.8.9 1.8 2-.8 2-1.8 2Zm5.4 0c-1 0-1.8-.9-1.8-2s.8-2 1.8-2 1.8.9 1.8 2-.8 2-1.8 2Z"/></svg>Join Discord</a></div>
<div class="meta in d4"><span>${ver} for Windows 10 and 11</span>${live}</div>
<p class="alt in d4">Using your own launcher? <a href="/download/mod">Get the mod on its own</a> for Fabric 1.21.11 with <a href="https://modrinth.com/mod/fabric-api" rel="noopener">Fabric API</a>.</p>
</div></header>
<main class="wrap">
<section id="features"><h2>Everything in one place</h2><p class="sub">No mod folders, no setup. Install once and everything is there, and it keeps itself up to date.</p>
<div class="grid">
<div class="f wide rv"><div class="ic"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 12h4l3-8 4 16 3-8h4"/></svg></div><h3>More FPS out of the box</h3><p>Sodium, Lithium and friends are set up for you, with tuned Java settings, background FPS limits and smart culling.</p></div>
<div class="f rv"><div class="ic"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg></div><h3>40+ mods</h3><p>Keystrokes, armor, CPS, zoom, waypoints, hitboxes, motion blur and more.</p></div>
<div class="f rv"><div class="ic"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h6v6H4zM14 14h6v6h-6z"/><path d="M10 7h4a3 3 0 0 1 3 3v4"/></svg></div><h3>Your HUD, your way</h3><p>Drag anything on screen. Alt snaps to the grid, Ctrl lines it up with the rest.</p></div>
<div class="f rv"><div class="ic"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M2 18 8 9l4 5 3-4 7 8z"/><circle cx="17" cy="6" r="2"/></svg></div><h3>Distant Terrain<span class="tag">Beta</span></h3><p>See land up to 1024 chunks away, with detail all the way up to real block textures.</p></div>
<div class="f rv"><div class="ic"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="9" cy="8" r="3"/><circle cx="17" cy="9" r="2.5"/><path d="M3 20c0-3.3 2.7-6 6-6s6 2.7 6 6M15 14.5c3 0 6 1.8 6 5.5"/></svg></div><h3>Friends</h3><p>Chat, set your status and join the server your friends are playing on in one click.</p></div>
<div class="f rv"><div class="ic"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M8 3 4 6v15h16V6l-4-3"/><path d="M8 3c0 2 1.8 4 4 4s4-2 4-4"/></svg></div><h3>Wardrobe</h3><p>Animated capes, wings, halos and emotes that every Surge player can see.</p></div>
<div class="f rv"><div class="ic"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="6" width="13" height="12" rx="2"/><path d="m16 10 5-3v10l-5-3"/></svg></div><h3>Clips</h3><p>Press F8 to save the last few seconds of gameplay as a video, up to 120 FPS.</p></div>
<div class="f rv"><div class="ic"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg></div><h3>Custom Sounds</h3><p>Swap any game sound for a built in one or your own mp3, wav or ogg.</p></div>
</div></section>
<section id="capes"><h2>Look the part</h2><p class="sub">Animated capes, wings, halos and emotes. Every Surge player sees them, and you can try them all on in the launcher.</p>
<div class="capes rv">${SHOWCASE.map(([k, n, f, ms]) => `<div class="cape"><div class="c" style="background-image:url(/img/cape/${k}.png);background-size:768px auto;background-position:-12px -12px;animation:cape-${k} ${f * ms}ms steps(${f}) infinite"></div><b>${n}</b></div><style>@keyframes cape-${k}{to{background-position:-12px ${-12 - 384 * f}px}}</style>`).join('')}</div></section>
<section id="start"><div class="split">
<div><h2>Get started</h2><p class="sub">From download to playing in about a minute.</p>
<ol class="steps rv">
<li><b>Download and run the installer</b><p>Open Surge Client Setup.exe. If Windows warns you, choose More info, then Run anyway.</p></li>
<li><b>Sign in with Microsoft</b><p>Use the account you play Minecraft with. Surge never sees your password.</p></li>
<li><b>Press Play</b><p>In game, Right Shift opens the mods and B opens your emote wheel.</p></li>
</ol></div>
${notes ? `<div class="new rv"><h3>What's new <span>${esc(l.version)}</span></h3><ul>${notes}</ul></div>` : ''}
</div></section>
<section id="faq" class="faq"><h2>Questions</h2><p class="sub">The things people ask most.</p>
<details><summary>Is Surge Client free?</summary><p>Yes. The client, the launcher and updates are free. Cosmetics use Surge Coins, which you get from daily rewards, codes and events.</p></details>
<details><summary>Can I get banned for using it?</summary><p>Surge Client has no cheat features, so it is fine on normal servers. Some servers have their own rules about clients, so check them if you are unsure.</p></details>
<details><summary>Why does Windows warn me about the installer?</summary><p>New programs without a code signing certificate often get this warning. Choose More info, then Run anyway. Only download Surge Client from this website.</p></details>
<details><summary>Can other players see my cosmetics?</summary><p>Everyone else using Surge Client sees your capes, wings, halos and emotes.</p></details>
<details><summary>What do I need to run it?</summary><p>Windows 10 or 11 and a Minecraft Java account. Java is downloaded for you, and 4 GB of RAM or more is recommended.</p></details>
<details><summary>How do I get Surge Coins?</summary><p>Claim your daily reward, redeem codes from giveaways and events on our Discord, and watch for gifts from the team.</p></details>
<details><summary>Does it work on Mac or Linux?</summary><p>The launcher is Windows only for now. On other systems you can use the mod on its own with any Fabric 1.21.11 launcher.</p></details>
</section>
<div class="band rv"><div><h2>Join the community</h2><p>Updates, giveaways, codes and help from other players.</p></div><a class="dl" href="${DISCORD}" rel="noopener">Join our Discord</a></div>
</main>
<button class="snd" id="snd" aria-label="Turn sounds on" title="Sounds"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 9v6h4l5 4V5L8 9z"/><path id="sndw" d="M16 9a4 4 0 0 1 0 6M19 6a8 8 0 0 1 0 12" opacity=".25"/></svg></button>
<footer class="wrap">Surge Client by owfx. Not affiliated with Mojang or Microsoft. <a href="${DISCORD}" rel="noopener">Discord</a></footer>
<script>
(function(){
var nv=document.querySelector('nav');addEventListener('scroll',function(){nv.classList.toggle('scrolled',scrollY>420)},{passive:true});
var reduce=matchMedia('(prefers-reduced-motion: reduce)').matches;
var c=document.getElementById('rain'),x=c.getContext('2d'),drops=[],w,h,dpr=Math.min(2,devicePixelRatio||1);
function size(){w=c.clientWidth;h=c.clientHeight;c.width=w*dpr;c.height=h*dpr;x.setTransform(dpr,0,0,dpr,0,0);drops=[];var n=Math.round(w*h/9000);for(var i=0;i<n;i++)drops.push({x:Math.random()*w,y:Math.random()*h,l:8+Math.random()*14,v:5+Math.random()*6});}
function frame(){x.clearRect(0,0,w,h);x.strokeStyle='rgba(200,205,220,.35)';x.lineWidth=1;x.beginPath();for(var i=0;i<drops.length;i++){var d=drops[i];x.moveTo(d.x,d.y);x.lineTo(d.x-d.l*.25,d.y+d.l);d.y+=d.v;d.x-=d.v*.25;if(d.y>h){d.y=-20;d.x=Math.random()*(w+80);}}x.stroke();if(!document.hidden)requestAnimationFrame(frame);else setTimeout(function(){requestAnimationFrame(frame)},500);}
var f=document.getElementById('flash'),b=document.getElementById('bolt');
function strike(){if(on)setTimeout(thunder,250+Math.random()*500);f.animate([{opacity:0},{opacity:1,offset:.08},{opacity:.2,offset:.2},{opacity:.8,offset:.28},{opacity:0}],{duration:900,easing:'ease-out'});b.animate([{fill:'#fff'},{fill:'#ffe07a',offset:.1},{fill:'#fff',offset:.3},{fill:'#ffe07a',offset:.4},{fill:'#fff'}],{duration:900});setTimeout(strike,5000+Math.random()*6000);}
var ac=null,on=false;try{on=localStorage.getItem('surgeSnd')==='1'}catch(e){}
var sb=document.getElementById('snd'),sw=document.getElementById('sndw');
function paint(){sw.setAttribute('opacity',on?'1':'.25');sb.setAttribute('aria-label',on?'Turn sounds off':'Turn sounds on')}paint();
function ctx(){if(!ac){try{ac=new (window.AudioContext||window.webkitAudioContext)()}catch(e){}}if(ac&&ac.state==='suspended')ac.resume();return ac}
function tone(f,at,len,type,vol,to){var a=ctx();if(!a)return;var t=a.currentTime+at,o=a.createOscillator(),g=a.createGain();o.type=type;o.frequency.setValueAtTime(f,t);if(to)o.frequency.exponentialRampToValueAtTime(to,t+len);g.gain.setValueAtTime(.0001,t);g.gain.exponentialRampToValueAtTime(vol,t+.01);g.gain.exponentialRampToValueAtTime(.0001,t+len);o.connect(g).connect(a.destination);o.start(t);o.stop(t+len+.02)}
function thunder(){var a=ctx();if(!a)return;var len=2.4,b=a.createBuffer(1,a.sampleRate*len,a.sampleRate),d=b.getChannelData(0),l=0;for(var i=0;i<d.length;i++){l=(l+.02*(Math.random()*2-1))/1.02;d[i]=l*3.5*Math.pow(1-i/d.length,2)*(i<a.sampleRate*.05?i/(a.sampleRate*.05):1)}var s=a.createBufferSource(),f=a.createBiquadFilter(),g=a.createGain();s.buffer=b;f.type='lowpass';f.frequency.value=300;g.gain.value=.5;s.connect(f).connect(g).connect(a.destination);s.start()}
function blip(){if(on)tone(1500,0,.035,'sine',.03)}
function click(){if(on){tone(660,0,.08,'triangle',.06);tone(990,.05,.12,'triangle',.05)}}
sb.onclick=function(){on=!on;try{localStorage.setItem('surgeSnd',on?'1':'0')}catch(e){}paint();if(on){ctx();tone(523,0,.15,'triangle',.06);tone(784,.08,.2,'triangle',.06)}};
var lastEl=null;document.addEventListener('mouseover',function(e){var el=e.target.closest('a,button,summary');if(el&&el!==lastEl){lastEl=el;blip()}});
document.addEventListener('click',function(e){if(e.target.closest('a,summary')&&!e.target.closest('#snd'))click()});
var io='IntersectionObserver' in window?new IntersectionObserver(function(es){es.forEach(function(en){if(en.isIntersecting){var el=en.target,sib=[].indexOf.call(el.parentNode.children,el);el.style.transitionDelay=(el.classList.contains('f')?Math.min(sib,8)*60:0)+'ms';el.classList.add('on');io.unobserve(el)}})},{threshold:.15}):null;
document.querySelectorAll('.rv').forEach(function(el){if(io&&!reduce)io.observe(el);else el.classList.add('on')});
if(!reduce){size();addEventListener('resize',size);requestAnimationFrame(frame);setTimeout(strike,1400);}
})();
</script>
</body></html>`;
}

const b64bytes = s => Uint8Array.from(atob(String(s || '').replace(/\s+/g, '')), c => c.charCodeAt(0));

async function mojangKeyValid(env, payload, sig) {
  let keys = [];
  try { keys = JSON.parse(env.MOJANG_KEYS || '[]'); } catch { keys = []; }
  for (const k of keys) {
    try {
      const key = await crypto.subtle.importKey('spki', b64bytes(k), { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-1' }, false, ['verify']);
      if (await crypto.subtle.verify('RSASSA-PKCS1-v1_5', key, sig, payload)) return true;
    } catch { }
  }
  return false;
}

async function verifyProfileKey(env, body) {
  const uuid = norm(body.uuid);
  if (!/^[0-9a-f]{32}$/.test(uuid)) return 'bad uuid';
  const ts = Number(body.ts) || 0, exp = Number(body.expiresAt) || 0;
  if (Math.abs(Date.now() - ts) > 5 * 60 * 1000) return 'clock';
  if (exp < Date.now()) return 'expired key';
  const spki = b64bytes(body.publicKey), keySig = b64bytes(body.keySig), sig = b64bytes(body.sig);
  const payload = new Uint8Array(24 + spki.length);
  const dv = new DataView(payload.buffer);
  for (let i = 0; i < 16; i++) payload[i] = parseInt(uuid.substr(i * 2, 2), 16);
  dv.setBigInt64(16, BigInt(exp));
  payload.set(spki, 24);
  if (!(await mojangKeyValid(env, payload, keySig))) return 'not signed by Mojang';
  const player = await crypto.subtle.importKey('spki', spki, { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' }, false, ['verify']);
  const msg = new TextEncoder().encode(`surge-auth:${uuid}:${ts}`);
  if (!(await crypto.subtle.verify('RSASSA-PKCS1-v1_5', player, sig, msg))) return 'bad signature';
  return null;
}

async function nameFor(uuid, fallback) {
  try {
    const r = await fetch(`https://playerdb.co/api/player/minecraft/${uuid}`, { headers: { 'User-Agent': 'SurgeClient/1.0' } });
    if (r.ok) { const d = await r.json(); const n = d && d.data && d.data.player && d.data.player.username; if (/^[A-Za-z0-9_]{1,16}$/.test(n || '')) return n; }
  } catch { }
  return /^[A-Za-z0-9_]{1,16}$/.test(fallback || '') ? fallback : null;
}

async function issueToken(db, uuid, name) {
  const fresh = [...crypto.getRandomValues(new Uint8Array(24))].map(b => b.toString(16).padStart(2, '0')).join('');
  await db.prepare(`INSERT INTO users (uuid, name, name_lc, token, status, server, seen) VALUES (?1, ?2, ?3, ?4, 'online', '', ?5)
    ON CONFLICT(uuid) DO UPDATE SET name = ?2, name_lc = ?3, token = COALESCE(NULLIF(users.token, ''), ?4), seen = ?5`).bind(uuid, name, name.toLowerCase(), fresh, Date.now()).run();
  const row = await db.prepare('SELECT token FROM users WHERE uuid = ?1').bind(uuid).first();
  return row ? row.token : fresh;
}

export default {
  async fetch(req, env) {
    const url = new URL(req.url);
    const p = url.pathname;
    if (req.method === 'GET' && (p === '/' || p === '/index.html')) {
      let stats = null;
      try {
        await setup(env.DB);
        const r = await env.DB.prepare("SELECT COUNT(*) AS n FROM users WHERE seen > ?1 AND (status IS NULL OR status != 'invisible')").bind(Date.now() - ONLINE_MS).first();
        stats = { online: r ? r.n : 0 };
      } catch { }
      return new Response(page(await latest(env).catch(() => null), stats), { headers: { 'content-type': 'text/html; charset=utf-8', 'cache-control': 'public, max-age=60' } });
    }
    if (req.method === 'GET' && p === '/download') return download(env, false);
    if (req.method === 'GET' && p === '/download/mod') return download(env, true);
    if (req.method === 'GET' && p === '/download/package') return packageFile(env);
    if (req.method === 'GET' && p === '/discord') return Response.redirect(DISCORD, 302);
    if (req.method === 'GET' && p.startsWith('/img/cape/')) {
      const key = p.slice('/img/cape/'.length).replace(/\.png$/, '');
      if (!SHOWCASE.some(c => c[0] === key)) return new Response('not found', { status: 404 });
      const r = await fetch(`https://raw.githubusercontent.com/${REPO(env)}/main/mod/src/main/resources/assets/surge/textures/cosmetic/cape_${key}.png`, { cf: { cacheTtl: 86400, cacheEverything: true } });
      if (!r.ok) return new Response('not found', { status: 404 });
      return new Response(r.body, { headers: { 'content-type': 'image/png', 'cache-control': 'public, max-age=86400' } });
    }
    if (req.method === 'GET' && (p === '/admin' || p === '/admin/')) {
      return new Response(ADMIN_PAGE, { headers: { 'content-type': 'text/html; charset=utf-8', 'cache-control': 'no-store', 'x-frame-options': 'DENY', 'referrer-policy': 'no-referrer' } });
    }
    const db = env.DB;
    await setup(db);
    if (p.startsWith('/admin/api/')) { await adminSetup(db); return adminApi(req, env, db, p.slice('/admin/api/'.length)); }
    if (p === '/v1/public' && req.method === 'GET') {
      await adminSetup(db);
      return new Response(JSON.stringify(await publicData(db)), { headers: { 'content-type': 'application/json', 'access-control-allow-origin': '*', 'cache-control': 'public, max-age=30' } });
    }
    let body = {};
    if (req.method === 'POST' || req.method === 'PUT') { try { body = await req.json(); } catch { body = {}; } }

    if (p === '/v1/redeem' && req.method === 'POST') {
      const u = await me(req, db);
      if (!u) return json({ error: 'sign in' }, 401);
      const r = await redeemCode(db, u.uuid, body.code);
      return json(r.body, r.status);
    }

    if (p === '/v1/health' && req.method === 'GET') {
      let n = 0;
      try { n = JSON.parse(env.MOJANG_KEYS || '[]').length; } catch { }
      return json({ ok: true, accountKeys: n });
    }

    if (p === '/v1/auth/key' && req.method === 'POST') {
      const why = await verifyProfileKey(env, body);
      if (why) return json({ error: why === 'clock' ? 'Your computer clock is off, fix the time and try again' : `Couldn't confirm your Minecraft account (${why})` }, 401);
      const uuid = norm(body.uuid);
      const name = await nameFor(uuid, String(body.name || ''));
      if (!name) return json({ error: 'bad name' }, 400);
      return json({ token: await issueToken(db, uuid, name), uuid, name });
    }

    if (p === '/v1/auth' && req.method === 'POST') {
      const name = String(body.name || ''), serverId = String(body.serverId || '');
      if (!/^[A-Za-z0-9_]{1,16}$/.test(name) || !/^[A-Za-z0-9_-]{8,64}$/.test(serverId)) return json({ error: 'bad request' }, 400);
      let r = null, prof = null;
      for (let i = 0; i < 6 && !prof; i++) {
        if (i) await new Promise(res => setTimeout(res, 300 + i * 300));
        r = await fetch(`https://sessionserver.mojang.com/session/minecraft/hasJoined?username=${encodeURIComponent(name)}&serverId=${encodeURIComponent(serverId)}`, { headers: { 'User-Agent': 'SurgeClient/1.0' } });
        if (r.status === 200) { try { prof = await r.json(); } catch { prof = null; } }
        else if (r.status !== 204 && r.status !== 404) break;
      }
      if (!prof || !prof.id) return json({ error: `Mojang could not confirm this account (${r ? r.status : 0})` }, 401);
      const uuid = norm(prof.id);
      const fresh = [...crypto.getRandomValues(new Uint8Array(24))].map(b => b.toString(16).padStart(2, '0')).join('');
      await db.prepare(`INSERT INTO users (uuid, name, name_lc, token, status, server, seen) VALUES (?1, ?2, ?3, ?4, 'online', '', ?5)
        ON CONFLICT(uuid) DO UPDATE SET name = ?2, name_lc = ?3, token = COALESCE(NULLIF(users.token, ''), ?4), seen = ?5`).bind(uuid, prof.name, prof.name.toLowerCase(), fresh, Date.now()).run();
      const row = await db.prepare('SELECT token FROM users WHERE uuid = ?1').bind(uuid).first();
      return json({ token: row ? row.token : fresh, uuid, name: prof.name });
    }

    if (p === '/v1/me' && req.method === 'PUT') {
      const u = await me(req, db);
      const uuid = u ? u.uuid : norm(body.uuid);
      if (!UUID_RE.test(uuid)) return json({ error: 'bad uuid' }, 400);
      const data = {
        wings: clean(body.wings), halo: clean(body.halo), cape: clean(body.cape),
        emote: clean(body.emote), emoteAt: Number(body.emoteAt) || 0, flag: body.flag === 'none' ? 'none' : (() => { const c = String((req.cf && req.cf.country) || '').toUpperCase(); return /^[A-Z]{2}$/.test(c) && c !== 'XX' && c !== 'T1' ? c : 'none'; })(),
      };
      const now = Date.now();
      const prev = await db.prepare('SELECT data FROM players WHERE uuid = ?1').bind(uuid).first().catch(() => null);
      let prevData = {};
      try { prevData = prev ? JSON.parse(prev.data) : {}; } catch { }
      data.emoteSrv = prevData.emoteAt === data.emoteAt && prevData.emote === data.emote && prevData.emoteSrv ? prevData.emoteSrv : now;
      await db.prepare('INSERT INTO players (uuid, name, data, seen) VALUES (?1, ?2, ?3, ?4) ON CONFLICT(uuid) DO UPDATE SET name = ?2, data = ?3, seen = ?4')
        .bind(uuid, String(body.name || (u && u.name) || '').slice(0, 16), JSON.stringify(data), now).run();
      if (u) {
        const status = STATUSES.includes(body.status) ? body.status : 'online';
        const server = typeof body.server === 'string' ? body.server.slice(0, 80) : '';
        await db.prepare('UPDATE users SET status = ?2, server = ?3, seen = ?4 WHERE uuid = ?1').bind(uuid, status, server, now).run();
      }
      const cc = String((req.cf && req.cf.country) || '').toUpperCase();
      return json({ ok: true, now, flag: /^[A-Z]{2}$/.test(cc) ? cc : 'none' });
    }

    if (p === '/v1/players' && req.method === 'GET') {
      const ids = (url.searchParams.get('ids') || '').split(',').map(norm).filter(s => UUID_RE.test(s)).slice(0, 200);
      if (!ids.length) return json({ players: {}, now: Date.now() });
      const q = `SELECT uuid, data, seen FROM players WHERE seen > ?1 AND uuid IN (${ids.map((_, i) => `?${i + 2}`).join(',')})`;
      const { results } = await db.prepare(q).bind(Date.now() - FRESH_MS, ...ids).all();
      const players = {};
      for (const r of results) players[r.uuid] = { ...JSON.parse(r.data), seen: r.seen };
      const found = Object.keys(players);
      if (found.length) {
        const extra = await playerExtras(db, found).catch(() => ({}));
        for (const k of found) if (extra[k]) Object.assign(players[k], extra[k]);
      }
      return new Response(JSON.stringify({ players, now: Date.now() }), { headers: { 'content-type': 'application/json', 'cache-control': 'no-store', 'access-control-allow-origin': '*' } });
    }

    const profMatch = p.match(/^\/v1\/profiles\/([A-Z0-9]{4,12})$/);
    if (profMatch && req.method === 'GET') {
      await db.prepare('CREATE TABLE IF NOT EXISTS profile_shares (code TEXT PRIMARY KEY, owner TEXT, name TEXT, data TEXT, created INTEGER, expires INTEGER)').run();
      const row = await db.prepare('SELECT name, data, expires FROM profile_shares WHERE code = ?1').bind(profMatch[1]).first();
      if (!row) return json({ error: 'not found' }, 404);
      if (row.expires && row.expires < Date.now()) return json({ error: 'expired' }, 410);
      return json({ name: row.name, data: row.data, expires: row.expires });
    }

    const capeMatch = p.match(/^\/v1\/cape\/([0-9a-f]{32})$/);
    if (capeMatch && req.method === 'GET') {
      const png = await capeImage(db, capeMatch[1]);
      if (!png) return new Response('not found', { status: 404 });
      return new Response(png, { headers: { 'content-type': 'image/png', 'cache-control': 'public, max-age=300', 'access-control-allow-origin': '*' } });
    }

    if (!p.startsWith('/v1/')) return json({ name: 'Surge Client service', ok: true });
    const u = await me(req, db);
    if (!u) return json({ error: 'Sign in through the Surge launcher' }, 401);
    const id = u.uuid;
    const target = norm(body.uuid);

    if (p === '/v1/profiles/share' && req.method === 'POST') {
      await db.prepare('CREATE TABLE IF NOT EXISTS profile_shares (code TEXT PRIMARY KEY, owner TEXT, name TEXT, data TEXT, created INTEGER, expires INTEGER)').run();
      const data = String(body.data || '');
      if (data.length < 2 || data.length > 200000) return json({ error: 'That setup is too big to share' }, 400);
      try { JSON.parse(data); } catch { return json({ error: 'That setup is damaged' }, 400); }
      const hours = Math.max(0, Math.min(24 * 365, Number(body.hours) || 0));
      await db.prepare('DELETE FROM profile_shares WHERE expires > 0 AND expires < ?1').bind(Date.now()).run();
      const A = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
      let code = '';
      for (let tries = 0; tries < 5; tries++) {
        code = [...crypto.getRandomValues(new Uint8Array(6))].map(b => A[b % A.length]).join('');
        const r = await db.prepare('INSERT OR IGNORE INTO profile_shares (code, owner, name, data, created, expires) VALUES (?1, ?2, ?3, ?4, ?5, ?6)').bind(code, id, String(body.name || 'Shared setup').slice(0, 32), data, Date.now(), hours ? Date.now() + hours * 3600000 : 0).run();
        if (r.meta.changes) return json({ code, expires: hours ? Date.now() + hours * 3600000 : 0 });
      }
      return json({ error: 'Try again' }, 500);
    }

    if (p === '/v1/inbox' && req.method === 'GET') return json(await inbox(db, id));
    if (p === '/v1/inbox/claim' && req.method === 'POST') return json(await claimGifts(db, id, body.ids));

    if (p === '/v1/social' && req.method === 'GET') {
      const now = Date.now();
      const { results: rows } = await db.prepare(`SELECT f.a, f.b, f.state, f.requester, u.uuid, u.name, u.status, u.server, u.seen
        FROM friends f JOIN users u ON u.uuid = (CASE WHEN f.a = ?1 THEN f.b ELSE f.a END) WHERE f.a = ?1 OR f.b = ?1`).bind(id).all();
      const friends = [], incoming = [], outgoing = [];
      for (const r of rows) {
        const entry = { uuid: r.uuid, name: r.name };
        if (r.state === 'friends') {
          const online = now - (r.seen || 0) < ONLINE_MS && r.status !== 'invisible';
          friends.push({ ...entry, status: online ? r.status : 'offline', server: online ? r.server || '' : '' });
        } else if (r.requester === id) outgoing.push(entry);
        else incoming.push(entry);
      }
      const { results: b } = await db.prepare('SELECT u.uuid, u.name FROM blocks x JOIN users u ON u.uuid = x.target WHERE x.owner = ?1').bind(id).all();
      const { results: m } = await db.prepare('SELECT target FROM mutes WHERE owner = ?1').bind(id).all();
      return json({ me: { uuid: id, name: u.name }, friends, incoming, outgoing, blocked: b, muted: m.map(x => x.target) });
    }

    if (p === '/v1/friends/add' && req.method === 'POST') {
      let other = body.name ? await userByName(db, body.name) : await db.prepare('SELECT uuid, name FROM users WHERE uuid = ?1').bind(target).first();
      if (!other && body.name) {
        const found = await lookupName(String(body.name).trim());
        if (!found) return json({ error: 'No Minecraft player with that name' }, 404);
        await db.prepare("INSERT OR IGNORE INTO users (uuid, name, name_lc, token, status, server, seen) VALUES (?1, ?2, ?3, NULL, 'online', '', 0)").bind(found.uuid, found.name, found.name.toLowerCase()).run();
        other = found;
      }
      if (!other) return json({ error: 'No Minecraft player with that name' }, 404);
      if (other.uuid === id) return json({ error: "You can't add yourself" }, 400);
      if (await blocked(db, id, other.uuid)) return json({ error: "You can't add this player" }, 403);
      const [a, b] = pair(id, other.uuid);
      const ex = await db.prepare('SELECT state, requester FROM friends WHERE a = ?1 AND b = ?2').bind(a, b).first();
      if (ex && ex.state === 'friends') return json({ error: 'Already friends' }, 400);
      if (ex && ex.requester !== id) {
        await db.prepare("UPDATE friends SET state = 'friends' WHERE a = ?1 AND b = ?2").bind(a, b).run();
        return json({ ok: true, friends: true });
      }
      await db.prepare("INSERT OR IGNORE INTO friends (a, b, state, requester) VALUES (?1, ?2, 'pending', ?3)").bind(a, b, id).run();
      return json({ ok: true, name: other.name });
    }

    if (['/v1/friends/accept', '/v1/friends/decline', '/v1/friends/cancel', '/v1/friends/remove'].includes(p) && req.method === 'POST') {
      if (!UUID_RE.test(target)) return json({ error: 'bad uuid' }, 400);
      const [a, b] = pair(id, target);
      if (p.endsWith('accept')) await db.prepare("UPDATE friends SET state = 'friends' WHERE a = ?1 AND b = ?2 AND state = 'pending' AND requester = ?3").bind(a, b, target).run();
      else await db.prepare('DELETE FROM friends WHERE a = ?1 AND b = ?2').bind(a, b).run();
      return json({ ok: true });
    }

    if (['/v1/block', '/v1/unblock', '/v1/mute', '/v1/unmute'].includes(p) && req.method === 'POST') {
      if (!UUID_RE.test(target) || target === id) return json({ error: 'bad uuid' }, 400);
      const table = p.includes('block') ? 'blocks' : 'mutes';
      if (p === '/v1/block' || p === '/v1/mute') {
        await db.prepare(`INSERT OR IGNORE INTO ${table} (owner, target) VALUES (?1, ?2)`).bind(id, target).run();
        if (p === '/v1/block') { const [a, b] = pair(id, target); await db.prepare('DELETE FROM friends WHERE a = ?1 AND b = ?2').bind(a, b).run(); }
      } else {
        await db.prepare(`DELETE FROM ${table} WHERE owner = ?1 AND target = ?2`).bind(id, target).run();
      }
      return json({ ok: true });
    }

    if (p === '/v1/messages' && req.method === 'POST') {
      const text = String(body.text || '').trim().slice(0, 500);
      if (!UUID_RE.test(target) || !text) return json({ error: 'bad request' }, 400);
      const [a, b] = pair(id, target);
      const f = await db.prepare("SELECT 1 FROM friends WHERE a = ?1 AND b = ?2 AND state = 'friends'").bind(a, b).first();
      if (!f) return json({ error: 'You can only message friends' }, 403);
      if (await blocked(db, id, target)) return json({ error: "You can't message this player" }, 403);
      const ts = Date.now();
      const r = await db.prepare('INSERT INTO messages (sender, receiver, text, ts, updated) VALUES (?1, ?2, ?3, ?4, ?4)').bind(id, target, text, ts).run();
      return json({ ok: true, id: r.meta.last_row_id, ts });
    }

    if ((p === '/v1/messages/edit' || p === '/v1/messages/delete') && req.method === 'POST') {
      const mid = Math.floor(Number(body.id) || 0);
      if (mid <= 0) return json({ error: 'bad request' }, 400);
      const now = Date.now();
      if (p.endsWith('edit')) {
        const text = String(body.text || '').trim().slice(0, 500);
        if (!text) return json({ error: 'bad request' }, 400);
        const r = await db.prepare('UPDATE messages SET text = ?3, edited = 1, updated = ?4 WHERE id = ?1 AND sender = ?2 AND deleted = 0').bind(mid, id, text, now).run();
        if (!r.meta.changes) return json({ error: "You can only edit your own messages" }, 403);
      } else {
        const r = await db.prepare("UPDATE messages SET text = '', deleted = 1, updated = ?3 WHERE id = ?1 AND sender = ?2").bind(mid, id, now).run();
        if (!r.meta.changes) return json({ error: "You can only delete your own messages" }, 403);
      }
      return json({ ok: true, now });
    }

    if (p === '/v1/messages' && req.method === 'GET') {
      const since = Math.max(0, Number(url.searchParams.get('since')) || 0);
      const changed = Math.max(0, Number(url.searchParams.get('changed')) || 0);
      const now = Date.now();
      const { results } = await db.prepare(`SELECT id, sender, receiver, text, ts, edited FROM messages
        WHERE id > ?2 AND (sender = ?1 OR receiver = ?1) AND ts > ?3 AND deleted = 0 ORDER BY id LIMIT 300`).bind(id, since, now - 30 * 86400000).all();
      let changes = [];
      if (changed > 0) {
        const c = await db.prepare(`SELECT id, text, edited, deleted FROM messages WHERE (sender = ?1 OR receiver = ?1) AND updated > ?2 AND id <= ?3 AND (edited = 1 OR deleted = 1) LIMIT 300`).bind(id, changed, since).all();
        changes = c.results;
      }
      return json({ messages: results, changes, now, me: id });
    }

    return json({ error: 'not found' }, 404);
  },
};
