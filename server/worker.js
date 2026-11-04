import { adminSetup, adminApi, publicData, redeemCode } from './admin.js';
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

async function setup(db) {
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
  const exe = (rel.assets || []).find(a => /Standalone\.exe$/.test(a.name));
  if (!exe) return latestCache;
  const jar = (rel.assets || []).find(a => /^surge-client-.+\.jar$/.test(a.name));
  latestCache = { at: Date.now(), version: String(rel.tag_name || '').replace(/^v/, ''), size: exe.size, url: exe.browser_download_url, jar: jar ? { name: jar.name, url: jar.browser_download_url } : null };
  return latestCache;
}

async function download(env, mod) {
  const l = await latest(env).catch(() => null);
  const base = `https://github.com/${REPO(env)}/releases/latest/download/`;
  const src = mod
    ? (l && l.jar ? l.jar : { url: base + 'Surge-Client-Mod.jar', name: 'surge-client-mc1.21.11.jar' })
    : { url: l ? l.url : base + 'Surge-Client.exe', name: 'Surge Client.exe' };
  const r = await fetch(src.url, { redirect: 'follow', headers: { 'User-Agent': 'surge-client-site' } });
  if (!r.ok) return new Response('Download is not available right now. Try again in a minute.', { status: 502 });
  const h = new Headers({ 'content-type': mod ? 'application/java-archive' : 'application/octet-stream', 'content-disposition': `attachment; filename="${src.name}"`, 'cache-control': 'no-store' });
  const len = r.headers.get('content-length');
  if (len) h.set('content-length', len);
  return new Response(r.body, { headers: h });
}

const MARK = `<svg viewBox="0 0 120 140" aria-hidden="true"><path fill="#fff" d="M30 80a22 22 0 0 1-2-44 30 30 0 0 1 57-8 24 24 0 0 1 7 52z"/><path fill="#fff" d="M64 84 44 112h14l-6 26 24-34H62l8-20z"/></svg>`;

function page(l) {
  const ver = l ? `Version ${l.version} · ${Math.round(l.size / 1048576)} MB` : 'Windows 10 and 11';
  return `<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Surge Client</title><meta name="description" content="A fast Minecraft launcher and client for 1.21.11.">
<link rel="icon" href="data:image/svg+xml,${encodeURIComponent(MARK.replace('<svg ', '<svg xmlns="http://www.w3.org/2000/svg" '))}">
<link rel="preconnect" href="https://fonts.googleapis.com"><link href="https://fonts.googleapis.com/css2?family=Pixelify+Sans:wght@500;700&family=Inter:wght@400;600&display=swap" rel="stylesheet">
<style>
:root{--bg:#121214;--deep:#27272a;--violet:#a1a1aa;--lilac:#e4e4e7;--text:#fafafa;--muted:#a1a1aa}
*{box-sizing:border-box}html,body{margin:0}body{background:var(--bg);color:var(--text);font:16px/1.6 Inter,system-ui,sans-serif;min-height:100vh;overflow-x:hidden}
.sky{position:fixed;inset:0;z-index:-1;background:radial-gradient(1200px 700px at 50% -10%,#52525b 0%,#2a2a2e 35%,var(--bg) 75%)}
.sky::after{content:"";position:absolute;inset:0;background-image:radial-gradient(1px 1px at 20% 30%,#fff8,transparent),radial-gradient(1px 1px at 70% 20%,#fff6,transparent),radial-gradient(1.5px 1.5px at 85% 60%,#fff5,transparent),radial-gradient(1px 1px at 35% 75%,#fff4,transparent),radial-gradient(1px 1px at 55% 45%,#fff5,transparent);}
main{max-width:980px;margin:0 auto;padding:72px 20px 40px;text-align:center}
.mark{width:96px;height:112px;margin:0 auto;filter:drop-shadow(0 8px 24px #0008);animation:float 5s ease-in-out infinite}
@keyframes float{50%{transform:translateY(-8px)}}
h1{font:700 clamp(44px,8vw,84px)/1 "Pixelify Sans",sans-serif;margin:18px 0 10px;letter-spacing:.5px;text-shadow:0 4px 0 #0005}
.lead{color:var(--lilac);font-size:clamp(17px,2.2vw,20px);max-width:560px;margin:0 auto 34px}
.dl{display:inline-flex;align-items:center;gap:12px;background:#fff;color:#18181b;text-decoration:none;font:700 20px "Pixelify Sans",sans-serif;padding:16px 30px;box-shadow:0 6px 0 #a1a1aa,0 18px 40px #0007;transition:transform .12s,box-shadow .12s}
.dl:hover{transform:translateY(-2px);box-shadow:0 8px 0 #a1a1aa,0 22px 46px #0008}.dl:active{transform:translateY(4px);box-shadow:0 2px 0 #a1a1aa}
.dl:focus-visible{outline:3px solid var(--lilac);outline-offset:4px}
.dl svg{width:22px;height:22px}
.meta{color:var(--muted);font-size:14px;margin-top:14px}
.alt{color:var(--muted);font-size:14px;margin:18px auto 0;max-width:520px}.alt a{color:#fff;text-underline-offset:3px}
.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:14px;margin:80px 0 0;text-align:left}
.f{background:#ffffff0d;border:2px solid #ffffff14;padding:18px 18px 16px}
.f h3{font:700 19px "Pixelify Sans",sans-serif;margin:0 0 6px;color:#fff}.f p{margin:0;color:var(--muted);font-size:14.5px}
.steps{margin:64px auto 0;max-width:620px;text-align:left;color:var(--muted)}.steps h2{font:700 24px "Pixelify Sans",sans-serif;color:#fff;margin:0 0 10px;text-align:center}
.steps ol{margin:0;padding-left:22px}.steps li{margin:6px 0}.steps b{color:#fff}
footer{color:#8a8a93;font-size:13px;text-align:center;padding:40px 20px}
@media (prefers-reduced-motion:reduce){.mark{animation:none}.dl{transition:none}}
</style></head><body><div class="sky"></div>
<main>
<div class="mark">${MARK}</div>
<h1>Surge Client</h1>
<p class="lead">A fast Minecraft launcher and client for 1.21.11. Mods, friends and cosmetics, ready the moment you press Play.</p>
<a class="dl" href="/download"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><path d="M12 3v13m-6-6 6 6 6-6M4 21h16"/></svg>Download for Windows</a>
<div class="meta">${ver} · No install needed</div>
<p class="alt">Using your own launcher? <a href="/download/mod">Get the mod on its own</a> for Fabric 1.21.11. It needs <a href="https://modrinth.com/mod/fabric-api" rel="noopener">Fabric API</a> and updates itself.</p>
<section class="grid">
<div class="f"><h3>40+ mods</h3><p>Keystrokes, armor, CPS, zoom, waypoints, motion blur and more, each with its own settings.</p></div>
<div class="f"><h3>Your HUD, your way</h3><p>Drag anything on screen. Alt snaps to the grid, Ctrl lines it up with the rest.</p></div>
<div class="f"><h3>More FPS</h3><p>Sodium, Lithium and friends are added for you, with smooth Java settings.</p></div>
<div class="f"><h3>Friends</h3><p>Chat, set your status and join the server your friends are playing on.</p></div>
<div class="f"><h3>Wardrobe</h3><p>Wings, halos, capes and animated emotes that other Surge players see.</p></div>
<div class="f"><h3>Always up to date</h3><p>The launcher and the mod update themselves. Nothing to download twice.</p></div>
</section>
<section class="steps"><h2>Getting started</h2><ol>
<li>Press <b>Download for Windows</b> and open <b>Surge Client.exe</b>. If Windows warns you, choose <b>More info</b>, then <b>Run anyway</b>.</li>
<li>Sign in with your Microsoft account.</li>
<li>Press <b>Play</b>. In game, <b>Right Shift</b> opens the mods and <b>B</b> opens emotes.</li>
</ol></section>
</main>
<footer>Surge Client by owfx · Not affiliated with Mojang or Microsoft</footer>
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
      return new Response(page(await latest(env).catch(() => null)), { headers: { 'content-type': 'text/html; charset=utf-8', 'cache-control': 'public, max-age=300' } });
    }
    if (req.method === 'GET' && p === '/download') return download(env, false);
    if (req.method === 'GET' && p === '/download/mod') return download(env, true);
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
        emote: clean(body.emote), emoteAt: Number(body.emoteAt) || 0, flag: /^[A-Z]{2}$/.test(body.flag || '') ? body.flag : 'none',
      };
      const now = Date.now();
      await db.prepare('INSERT INTO players (uuid, name, data, seen) VALUES (?1, ?2, ?3, ?4) ON CONFLICT(uuid) DO UPDATE SET name = ?2, data = ?3, seen = ?4')
        .bind(uuid, String(body.name || (u && u.name) || '').slice(0, 16), JSON.stringify(data), now).run();
      if (u) {
        const status = STATUSES.includes(body.status) ? body.status : 'online';
        const server = typeof body.server === 'string' ? body.server.slice(0, 80) : '';
        await db.prepare('UPDATE users SET status = ?2, server = ?3, seen = ?4 WHERE uuid = ?1').bind(uuid, status, server, now).run();
      }
      return json({ ok: true, now });
    }

    if (p === '/v1/players' && req.method === 'GET') {
      const ids = (url.searchParams.get('ids') || '').split(',').map(norm).filter(s => UUID_RE.test(s)).slice(0, 200);
      if (!ids.length) return json({ players: {}, now: Date.now() });
      const q = `SELECT uuid, data, seen FROM players WHERE seen > ?1 AND uuid IN (${ids.map((_, i) => `?${i + 2}`).join(',')})`;
      const { results } = await db.prepare(q).bind(Date.now() - FRESH_MS, ...ids).all();
      const players = {};
      for (const r of results) players[r.uuid] = { ...JSON.parse(r.data), seen: r.seen };
      return json({ players, now: Date.now() });
    }

    if (!p.startsWith('/v1/')) return json({ name: 'Surge Client service', ok: true });
    const u = await me(req, db);
    if (!u) return json({ error: 'Sign in through the Surge launcher' }, 401);
    const id = u.uuid;
    const target = norm(body.uuid);

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
      const other = body.name ? await userByName(db, body.name) : await db.prepare('SELECT uuid, name FROM users WHERE uuid = ?1').bind(target).first();
      if (!other) return json({ error: 'No Surge player with that name' }, 404);
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
      const r = await db.prepare('INSERT INTO messages (sender, receiver, text, ts) VALUES (?1, ?2, ?3, ?4)').bind(id, target, text, ts).run();
      return json({ ok: true, id: r.meta.last_row_id, ts });
    }

    if (p === '/v1/messages' && req.method === 'GET') {
      const since = Math.max(0, Number(url.searchParams.get('since')) || 0);
      const { results } = await db.prepare(`SELECT id, sender, receiver, text, ts FROM messages
        WHERE id > ?2 AND (sender = ?1 OR receiver = ?1) AND ts > ?3 ORDER BY id LIMIT 300`).bind(id, since, Date.now() - 30 * 86400000).all();
      return json({ messages: results });
    }

    return json({ error: 'not found' }, 404);
  },
};
