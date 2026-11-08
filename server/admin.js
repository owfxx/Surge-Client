const ITEMS = [{"id": "wings:angel", "name": "Angel Wings", "price": 1200}, {"id": "wings:surge", "name": "Surge Wings", "price": 1500}, {"id": "wings:soul", "name": "Soul Wings", "price": 1100}, {"id": "wings:flame", "name": "Flame Wings", "price": 1100}, {"id": "wings:void", "name": "Void Wings", "price": 1300}, {"id": "wings:crystal", "name": "Crystal Wings", "price": 1400}, {"id": "wings:gold", "name": "Golden Wings", "price": 1600}, {"id": "halo:angel", "name": "Angel Halo", "price": 900}, {"id": "halo:surge", "name": "Surge Halo", "price": 1000}, {"id": "halo:fire", "name": "Fire Halo", "price": 750}, {"id": "halo:enchanted", "name": "Enchanted Halo", "price": 800}, {"id": "halo:void", "name": "Void Halo", "price": 850}, {"id": "halo:rose", "name": "Rose Halo", "price": 700}, {"id": "cape:surge", "name": "Surge Cape", "price": 1000}, {"id": "cape:void", "name": "Void Cape", "price": 800}, {"id": "cape:flame", "name": "Flame Cape", "price": 1200}, {"id": "cape:ocean", "name": "Ocean Cape", "price": 700}, {"id": "cape:aurora", "name": "Aurora Cape", "price": 900}, {"id": "cape:sakura", "name": "Sakura Cape", "price": 700}, {"id": "cape:midnight", "name": "Midnight Cape", "price": 750}, {"id": "cape:royal", "name": "Royal Cape", "price": 950}, {"id": "cape:emerald", "name": "Emerald Cape", "price": 700}, {"id": "cape:galaxy", "name": "Galaxy Cape", "price": 1100}, {"id": "cape:custom", "name": "Color Cape", "price": 300}, {"id": "cape:blinkcat", "name": "Blink Cat Cape", "price": 1500}, {"id": "cape:enchanted", "name": "Enchanted Cape", "price": 1800}, {"id": "cape:runes", "name": "Rune Cape", "price": 1600}, {"id": "cape:beta", "name": "Beta Tester Cape", "price": 0}, {"id": "cape:image", "name": "Picture Cape", "price": 0}, {"id": "emote:WAVE", "name": "Wave (emote)", "price": 600}, {"id": "emote:CLAP", "name": "Clap (emote)", "price": 600}, {"id": "emote:FACEPALM", "name": "Facepalm (emote)", "price": 650}, {"id": "emote:DAB", "name": "Dab (emote)", "price": 750}, {"id": "emote:SHRUG", "name": "Shrug (emote)", "price": 600}, {"id": "emote:SALUTE", "name": "Salute (emote)", "price": 600}, {"id": "emote:CHEER", "name": "Cheer (emote)", "price": 800}, {"id": "emote:LAUGH", "name": "Laugh (emote)", "price": 700}, {"id": "emote:BOW", "name": "Bow (emote)", "price": 650}, {"id": "emote:HEART", "name": "Heart (emote)", "price": 850}, {"id": "emote:FLOSS", "name": "Floss (emote)", "price": 1000}, {"id": "emote:TPOSE", "name": "T-Pose (emote)", "price": 600}, {"id": "emote:CRY", "name": "Cry (emote)", "price": 750}, {"id": "emote:SIT", "name": "Sit (emote)", "price": 600}, {"id": "emote:GRIDDY", "name": "Griddy (emote)", "price": 1200}, {"id": "emote:SHUFFLE", "name": "Shuffle (emote)", "price": 1100}, {"id": "emote:DISCO", "name": "Disco Fever (emote)", "price": 1000}, {"id": "idle:IDLE_LOOK", "name": "Look Around (idle)", "price": 400}, {"id": "idle:IDLE_STRETCH", "name": "Big Stretch (idle)", "price": 500}, {"id": "idle:IDLE_YAWN", "name": "Sleepy (idle)", "price": 500}, {"id": "idle:IDLE_TAP", "name": "Foot Tap (idle)", "price": 450}, {"id": "idle:IDLE_CROSS", "name": "Arms Crossed (idle)", "price": 450}, {"id": "idle:IDLE_VIBE", "name": "Vibing (idle)", "price": 600}];

const json = (data, status = 200) => new Response(JSON.stringify(data), { status, headers: { 'content-type': 'application/json', 'cache-control': 'no-store' } });
const ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
const normalize = c => String(c || '').toUpperCase().replace(/[^A-Z0-9]/g, '');

async function sha256(s) {
  const d = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(s));
  return [...new Uint8Array(d)].map(b => b.toString(16).padStart(2, '0')).join('');
}

function randomCode() {
  const b = crypto.getRandomValues(new Uint8Array(12));
  let s = '';
  for (let i = 0; i < 12; i++) s += ALPHABET[b[i] % ALPHABET.length];
  return `${s.slice(0, 4)}-${s.slice(4, 8)}-${s.slice(8)}`;
}

function same(a, b) {
  const x = new TextEncoder().encode(a), y = new TextEncoder().encode(b);
  let diff = x.length ^ y.length;
  for (let i = 0; i < Math.max(x.length, y.length); i++) diff |= (x[i] || 0) ^ (y[i] || 0);
  return diff === 0;
}

export async function adminSetup(db) {
  await db.batch([
    db.prepare('CREATE TABLE IF NOT EXISTS admin_codes (hash TEXT PRIMARY KEY, code TEXT, coins INTEGER, items TEXT, created INTEGER)'),
    db.prepare('CREATE TABLE IF NOT EXISTS admin_sales (id INTEGER PRIMARY KEY AUTOINCREMENT, target TEXT, percent INTEGER, until INTEGER)'),
    db.prepare('CREATE TABLE IF NOT EXISTS admin_locks (item TEXT PRIMARY KEY)'),
    db.prepare('CREATE TABLE IF NOT EXISTS admin_settings (k TEXT PRIMARY KEY, v TEXT)'),
    db.prepare('CREATE TABLE IF NOT EXISTS admin_servers (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, ip TEXT, description TEXT, tag TEXT)'),
    db.prepare('CREATE TABLE IF NOT EXISTS admin_sessions (token TEXT PRIMARY KEY, expires INTEGER)'),
    db.prepare('CREATE TABLE IF NOT EXISTS admin_fails (ip TEXT PRIMARY KEY, count INTEGER, at INTEGER)'),
    db.prepare('CREATE TABLE IF NOT EXISTS code_uses (hash TEXT, uuid TEXT, at INTEGER, PRIMARY KEY (hash, uuid))'),
  ]);
  await db.batch([
    db.prepare('CREATE TABLE IF NOT EXISTS admin_gifts (id INTEGER PRIMARY KEY AUTOINCREMENT, uuid TEXT, name TEXT, coins INTEGER, items TEXT, note TEXT, created INTEGER, claimed INTEGER DEFAULT 0)'),
    db.prepare('CREATE INDEX IF NOT EXISTS admin_gifts_uuid ON admin_gifts(uuid, claimed)'),
    db.prepare('CREATE TABLE IF NOT EXISTS admin_badges (uuid TEXT PRIMARY KEY, name TEXT, badge TEXT)'),
    db.prepare('CREATE TABLE IF NOT EXISTS admin_capes (uuid TEXT PRIMARY KEY, name TEXT, png TEXT, v INTEGER)'),
    db.prepare('CREATE TABLE IF NOT EXISTS gift_claims (gift INTEGER, uuid TEXT, at INTEGER, PRIMARY KEY (gift, uuid))'),
  ]);
  try { await db.prepare('ALTER TABLE admin_codes ADD COLUMN max_uses INTEGER DEFAULT 0').run(); } catch { }
  try { await db.prepare("ALTER TABLE admin_sales ADD COLUMN uuid TEXT DEFAULT ''").run(); } catch { }
  try { await db.prepare("ALTER TABLE admin_sales ADD COLUMN name TEXT DEFAULT ''").run(); } catch { }
}

export const BADGES = ['owner', 'staff', 'booster', 'beta', 'media', 'vip', 'early', 'dev'];
export const OWNERS = ['baea491cd9724afc9858ca377d3cfbf9'];

async function prices(db) {
  const row = await db.prepare("SELECT v FROM admin_settings WHERE k = 'prices'").first();
  try { return row ? JSON.parse(row.v) : {}; } catch { return {}; }
}

async function findPlayer(db, raw) {
  const q = String(raw || '').trim();
  const hex = q.replace(/-/g, '').toLowerCase();
  if (/^[0-9a-f]{32}$/.test(hex)) {
    const u = await db.prepare('SELECT uuid, name FROM users WHERE uuid = ?1').bind(hex).first();
    return u || { uuid: hex, name: hex.slice(0, 8) };
  }
  if (!/^[A-Za-z0-9_]{1,16}$/.test(q)) return null;
  const u = await db.prepare('SELECT uuid, name FROM users WHERE name_lc = ?1').bind(q.toLowerCase()).first();
  if (u) return u;
  try {
    const r = await fetch(`https://playerdb.co/api/player/minecraft/${encodeURIComponent(q)}`, { headers: { 'User-Agent': 'SurgeClient/1.0' } });
    if (!r.ok) return null;
    const d = await r.json();
    const p = d && d.data && d.data.player;
    if (!p || !p.raw_id) return null;
    return { uuid: String(p.raw_id).toLowerCase(), name: p.username };
  } catch { return null; }
}

export async function inbox(db, uuid) {
  await adminSetup(db);
  const now = Date.now();
  const gifts = (await db.prepare("SELECT id, coins, items, note FROM admin_gifts WHERE (uuid = ?1 AND claimed = 0) OR (uuid = '*' AND (claimed = 0 OR claimed > ?2) AND id NOT IN (SELECT gift FROM gift_claims WHERE uuid = ?1)) ORDER BY id LIMIT 20").bind(uuid, now).all()).results.map(g => ({ ...g, items: JSON.parse(g.items || '[]') }));
  const sales = (await db.prepare("SELECT target, percent, until FROM admin_sales WHERE uuid = ?1 AND (until = 0 OR until > ?2)").bind(uuid, now).all()).results;
  const b = await db.prepare('SELECT badge FROM admin_badges WHERE uuid = ?1').bind(uuid).first();
  const c = await db.prepare('SELECT v FROM admin_capes WHERE uuid = ?1').bind(uuid).first();
  return { gifts, sales, badge: b ? b.badge : (OWNERS.includes(uuid) ? 'owner' : ''), capev: c ? c.v : 0 };
}

export async function claimGifts(db, uuid, ids) {
  await adminSetup(db);
  const list = (Array.isArray(ids) ? ids : []).map(Number).filter(n => Number.isInteger(n) && n > 0).slice(0, 20);
  const claimed = [];
  for (const id of list) {
    let r = await db.prepare('UPDATE admin_gifts SET claimed = ?3 WHERE id = ?1 AND uuid = ?2 AND claimed = 0').bind(id, uuid, Date.now()).run();
    if (!r.meta.changes) {
      const g = await db.prepare("SELECT claimed FROM admin_gifts WHERE id = ?1 AND uuid = '*'").bind(id).first();
      if (g && (!g.claimed || g.claimed > Date.now())) r = await db.prepare('INSERT OR IGNORE INTO gift_claims (gift, uuid, at) VALUES (?1, ?2, ?3)').bind(id, uuid, Date.now()).run();
    }
    if (r.meta.changes) {
      const g = await db.prepare('SELECT id, coins, items, note FROM admin_gifts WHERE id = ?1').bind(id).first();
      if (g) claimed.push({ ...g, items: JSON.parse(g.items || '[]') });
    }
  }
  return { claimed };
}

export async function playerExtras(db, ids) {
  await adminSetup(db);
  const out = {};
  if (!ids.length) return out;
  const marks = ids.map((_, i) => `?${i + 1}`).join(',');
  for (const r of (await db.prepare(`SELECT uuid, badge FROM admin_badges WHERE uuid IN (${marks})`).bind(...ids).all()).results) (out[r.uuid] = out[r.uuid] || {}).badge = r.badge;
  for (const r of (await db.prepare(`SELECT uuid, v FROM admin_capes WHERE uuid IN (${marks})`).bind(...ids).all()).results) (out[r.uuid] = out[r.uuid] || {}).capev = r.v;
  for (const id of ids) if (OWNERS.includes(id) && !(out[id] && out[id].badge)) (out[id] = out[id] || {}).badge = 'owner';
  return out;
}

export async function capeImage(db, uuid) {
  await adminSetup(db);
  const c = await db.prepare('SELECT png FROM admin_capes WHERE uuid = ?1').bind(uuid).first();
  if (!c) return null;
  return Uint8Array.from(atob(c.png), ch => ch.charCodeAt(0));
}

export async function redeemCode(db, uuid, raw) {
  await adminSetup(db);
  const n = normalize(raw);
  if (n.length < 4) return { status: 404, body: { error: 'not found' } };
  const hash = await sha256(n);
  const c = await db.prepare('SELECT hash, coins, items, max_uses FROM admin_codes WHERE hash = ?1').bind(hash).first();
  if (!c) return { status: 404, body: { error: 'not found' } };
  const mine = await db.prepare('SELECT at FROM code_uses WHERE hash = ?1 AND uuid = ?2').bind(hash, uuid).first();
  if (mine) return { status: 409, body: { error: 'already redeemed' } };
  if (c.max_uses > 0) {
    const used = await db.prepare('SELECT COUNT(*) AS n FROM code_uses WHERE hash = ?1').bind(hash).first();
    if ((used ? used.n : 0) >= c.max_uses) return { status: 410, body: { error: 'used up' } };
  }
  await db.prepare('INSERT OR IGNORE INTO code_uses (hash, uuid, at) VALUES (?1, ?2, ?3)').bind(hash, uuid, Date.now()).run();
  return { status: 200, body: { hash, coins: c.coins || 0, items: JSON.parse(c.items || '[]') } };
}

async function lockedList(db) {
  const conf = await db.prepare("SELECT v FROM admin_settings WHERE k = 'locks'").first();
  if (!conf) return null;
  const { results } = await db.prepare('SELECT item FROM admin_locks').all();
  return results.map(r => r.item);
}

export async function publicData(db) {
  const now = Date.now();
  const codes = (await db.prepare('SELECT hash, coins, items FROM admin_codes WHERE max_uses IS NULL OR max_uses = 0').all()).results.map(c => ({ hash: c.hash, coins: c.coins, items: JSON.parse(c.items || '[]') }));
  const sales = (await db.prepare("SELECT target, percent, until FROM admin_sales WHERE (uuid IS NULL OR uuid = '') AND (until = 0 OR until > ?1)").bind(now).all()).results;
  const servers = (await db.prepare('SELECT name, ip, description, tag FROM admin_servers ORDER BY id').all()).results;
  const out = { codes, sales, servers, prices: await prices(db) };
  const ann = await announcement(db);
  if (ann) out.announce = ann;
  const locked = await lockedList(db);
  if (locked) out.locked = locked;
  return out;
}

async function announcement(db) {
  const row = await db.prepare("SELECT v FROM admin_settings WHERE k = 'announce'").first();
  if (!row) return null;
  try {
    const a = JSON.parse(row.v);
    if (!a || !a.text || (a.until && a.until < Date.now())) return null;
    return a;
  } catch { return null; }
}

async function overview(db, onlineMs) {
  const now = Date.now();
  const one = async (sql, ...b) => { const r = await db.prepare(sql).bind(...b).first(); return r ? r.n : 0; };
  return {
    players: await one('SELECT COUNT(*) AS n FROM users'),
    online: await one("SELECT COUNT(*) AS n FROM users WHERE seen > ?1", now - onlineMs),
    day: await one('SELECT COUNT(*) AS n FROM users WHERE seen > ?1', now - 86400000),
    week: await one('SELECT COUNT(*) AS n FROM users WHERE seen > ?1', now - 7 * 86400000),
    redeemed: await one('SELECT COUNT(*) AS n FROM code_uses WHERE at > ?1', now - 86400000),
    waiting: await one('SELECT COUNT(*) AS n FROM admin_gifts WHERE uuid != ?1 AND claimed = 0', '*'),
    list: (await db.prepare("SELECT name, status, server FROM users WHERE seen > ?1 ORDER BY name_lc LIMIT 100").bind(now - onlineMs).all()).results,
  };
}

async function state(db) {
  const now = Date.now();
  const codes = (await db.prepare('SELECT c.hash, c.code, c.coins, c.items, c.created, c.max_uses, (SELECT COUNT(*) FROM code_uses u WHERE u.hash = c.hash) AS used FROM admin_codes c ORDER BY c.created DESC').all()).results.map(c => ({ ...c, items: JSON.parse(c.items || '[]') }));
  const sales = (await db.prepare('SELECT id, target, percent, until, uuid, name FROM admin_sales WHERE until = 0 OR until > ?1 ORDER BY id').bind(now).all()).results;
  const servers = (await db.prepare('SELECT id, name, ip, description, tag FROM admin_servers ORDER BY id').all()).results;
  const locked = (await lockedList(db)) || ['cape:beta'];
  const gifts = (await db.prepare("SELECT g.id, g.uuid, g.name, g.coins, g.items, g.note, g.created, g.claimed, (SELECT COUNT(*) FROM gift_claims c WHERE c.gift = g.id) AS got FROM admin_gifts g ORDER BY g.id DESC LIMIT 60").all()).results.map(g => ({ ...g, items: JSON.parse(g.items || '[]') }));
  const badges = (await db.prepare('SELECT uuid, name, badge FROM admin_badges ORDER BY name').all()).results;
  const capes = (await db.prepare('SELECT uuid, name, v FROM admin_capes ORDER BY name').all()).results;
  return { codes, sales, servers, locked, items: ITEMS, gifts, badges, capes, prices: await prices(db), badgeKinds: BADGES, announce: await announcement(db), overview: await overview(db, 3 * 60 * 1000) };
}


async function authed(req, db) {
  const u = (req.headers.get('authorization') || '').match(/^Bearer ([a-f0-9]{48})$/);
  if (u) {
    const row = await db.prepare('SELECT uuid FROM users WHERE token = ?1').bind(u[1]).first();
    return !!row && OWNERS.includes(String(row.uuid).replace(/-/g, '').toLowerCase());
  }
  const m = (req.headers.get('authorization') || '').match(/^Bearer ([a-f0-9]{64})$/);
  if (!m) return false;
  const s = await db.prepare('SELECT expires FROM admin_sessions WHERE token = ?1').bind(m[1]).first();
  return !!s && s.expires > Date.now();
}

export async function adminApi(req, env, db, path) {
  const ip = req.headers.get('cf-connecting-ip') || 'unknown';
  let body = {};
  if (req.method === 'POST') { try { body = await req.json(); } catch { body = {}; } }

  if (path === 'status' && req.method === 'GET') return json({ configured: !!env.ADMIN_PASSWORD });

  if (path === 'login' && req.method === 'POST') {
    if (!env.ADMIN_PASSWORD) return json({ error: 'The admin password is not set up yet' }, 503);
    const f = await db.prepare('SELECT count, at FROM admin_fails WHERE ip = ?1').bind(ip).first();
    if (f && f.count >= 8 && Date.now() - f.at < 15 * 60000) return json({ error: 'Too many tries. Wait 15 minutes.' }, 429);
    if (!same(String(body.password || ''), env.ADMIN_PASSWORD)) {
      const count = f && Date.now() - f.at < 15 * 60000 ? f.count + 1 : 1;
      await db.prepare('INSERT OR REPLACE INTO admin_fails (ip, count, at) VALUES (?1, ?2, ?3)').bind(ip, count, Date.now()).run();
      return json({ error: 'Wrong password' }, 401);
    }
    await db.prepare('DELETE FROM admin_fails WHERE ip = ?1').bind(ip).run();
    await db.prepare('DELETE FROM admin_sessions WHERE expires < ?1').bind(Date.now()).run();
    const token = [...crypto.getRandomValues(new Uint8Array(32))].map(b => b.toString(16).padStart(2, '0')).join('');
    await db.prepare('INSERT INTO admin_sessions (token, expires) VALUES (?1, ?2)').bind(token, Date.now() + 12 * 3600000).run();
    return json({ token });
  }

  if (!(await authed(req, db))) return json({ error: 'Please sign in again' }, 401);

  if (path === 'logout' && req.method === 'POST') {
    const t = (req.headers.get('authorization') || '').slice(7);
    await db.prepare('DELETE FROM admin_sessions WHERE token = ?1').bind(t).run();
    return json({ ok: true });
  }
  if (path === 'state' && req.method === 'GET') return json(await state(db));

  if (path === 'codes' && req.method === 'POST') {
    const coins = Math.max(0, Math.min(1000000, Math.floor(Number(body.coins) || 0)));
    const valid = new Set(ITEMS.map(i => i.id));
    const items = (Array.isArray(body.items) ? body.items : []).filter(i => valid.has(i)).slice(0, 20);
    if (!coins && !items.length) return json({ error: 'A code needs coins or an item' }, 400);
    const uses = Math.max(0, Math.min(100000, Math.floor(Number(body.uses) || 0)));
    const custom = String(body.code || '').trim();
    const list = custom ? [custom] : Array.from({ length: Math.max(1, Math.min(100, Math.floor(Number(body.count) || 1))) }, randomCode);
    const made = [];
    for (const code of list) {
      const n = normalize(code);
      if (n.length < 4) return json({ error: 'Codes need at least 4 letters or numbers' }, 400);
      const hash = await sha256(n);
      const r = await db.prepare('INSERT OR IGNORE INTO admin_codes (hash, code, coins, items, created, max_uses) VALUES (?1, ?2, ?3, ?4, ?5, ?6)').bind(hash, code, coins, JSON.stringify(items), Date.now(), uses).run();
      if (r.meta.changes) made.push(code);
    }
    if (!made.length) return json({ error: 'That code already exists' }, 400);
    return json({ made });
  }
  if (path === 'codes/delete' && req.method === 'POST') {
    await db.prepare('DELETE FROM admin_codes WHERE hash = ?1').bind(String(body.hash || '')).run();
    await db.prepare('DELETE FROM code_uses WHERE hash = ?1').bind(String(body.hash || '')).run();
    return json({ ok: true });
  }
  if (path === 'sales' && req.method === 'POST') {
    const percent = Math.max(5, Math.min(100, Math.floor(Number(body.percent) || 20)));
    const hours = Math.max(0, Math.min(24 * 90, Number(body.hours) || 0));
    const target = String(body.target || 'all').slice(0, 60);
    let who = { uuid: '', name: '' };
    if (String(body.player || '').trim()) {
      who = await findPlayer(db, body.player);
      if (!who) return json({ error: "Couldn't find that player" }, 404);
    }
    await db.prepare('INSERT INTO admin_sales (target, percent, until, uuid, name) VALUES (?1, ?2, ?3, ?4, ?5)').bind(target, percent, hours ? Date.now() + hours * 3600000 : 0, who.uuid, who.name).run();
    return json({ ok: true });
  }
  if (path === 'gifts' && req.method === 'POST') {
    const who = body.everyone ? { uuid: '*', name: 'Everyone' } : await findPlayer(db, body.player);
    if (!who) return json({ error: "Couldn't find that player" }, 404);
    const coins = Math.max(0, Math.min(1000000, Math.floor(Number(body.coins) || 0)));
    const valid = new Set(ITEMS.map(i => i.id));
    const items = (Array.isArray(body.items) ? body.items : []).filter(i => valid.has(i)).slice(0, 20);
    if (!coins && !items.length) return json({ error: 'A gift needs coins or an item' }, 400);
    const note = String(body.note || '').slice(0, 140);
    const hours = Math.max(0, Math.min(24 * 90, Number(body.hours) || 0));
    await db.prepare('INSERT INTO admin_gifts (uuid, name, coins, items, note, created, claimed) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)').bind(who.uuid, who.name, coins, JSON.stringify(items), note, Date.now(), who.uuid === '*' && hours ? Date.now() + hours * 3600000 : 0).run();
    return json({ ok: true, name: who.name });
  }
  if (path === 'gifts/delete' && req.method === 'POST') {
    const id = Number(body.id) || 0;
    await db.prepare("DELETE FROM admin_gifts WHERE id = ?1 AND (claimed = 0 OR uuid = '*')").bind(id).run();
    await db.prepare('DELETE FROM gift_claims WHERE gift = ?1').bind(id).run();
    return json({ ok: true });
  }
  if (path === 'badges' && req.method === 'POST') {
    const who = await findPlayer(db, body.player);
    if (!who) return json({ error: "Couldn't find that player" }, 404);
    const badge = String(body.badge || '');
    if (!badge) await db.prepare('DELETE FROM admin_badges WHERE uuid = ?1').bind(who.uuid).run();
    else if (!BADGES.includes(badge)) return json({ error: 'Unknown icon' }, 400);
    else await db.prepare('INSERT OR REPLACE INTO admin_badges (uuid, name, badge) VALUES (?1, ?2, ?3)').bind(who.uuid, who.name, badge).run();
    return json({ ok: true, name: who.name });
  }
  if (path === 'capes' && req.method === 'POST') {
    const who = await findPlayer(db, body.player);
    if (!who) return json({ error: "Couldn't find that player" }, 404);
    const png = String(body.png || '').replace(/^data:image\/png;base64,/, '').replace(/\s+/g, '');
    if (!/^[A-Za-z0-9+/=]+$/.test(png) || png.length < 100) return json({ error: 'Pick a PNG image' }, 400);
    if (png.length > 900000) return json({ error: 'That image is too big. Keep it under 600 KB.' }, 400);
    const bytes = Uint8Array.from(atob(png.slice(0, 16)), c => c.charCodeAt(0));
    if (bytes[0] !== 0x89 || bytes[1] !== 0x50) return json({ error: 'Only PNG images work' }, 400);
    await db.prepare('INSERT OR REPLACE INTO admin_capes (uuid, name, png, v) VALUES (?1, ?2, ?3, ?4)').bind(who.uuid, who.name, png, Date.now()).run();
    if (body.gift !== false) await db.prepare('INSERT INTO admin_gifts (uuid, name, coins, items, note, created) VALUES (?1, ?2, 0, ?3, ?4, ?5)').bind(who.uuid, who.name, JSON.stringify(['cape:image']), String(body.note || 'Your own picture cape is ready.').slice(0, 140), Date.now()).run();
    return json({ ok: true, name: who.name });
  }
  if (path === 'capes/delete' && req.method === 'POST') {
    await db.prepare('DELETE FROM admin_capes WHERE uuid = ?1').bind(String(body.uuid || '')).run();
    return json({ ok: true });
  }
  if (path === 'prices' && req.method === 'POST') {
    const item = String(body.item || '');
    if (!ITEMS.some(i => i.id === item)) return json({ error: 'Unknown item' }, 400);
    const p = await prices(db);
    if (body.price === null || body.price === '' || body.price === undefined) delete p[item];
    else p[item] = Math.max(0, Math.min(1000000, Math.floor(Number(body.price) || 0)));
    await db.prepare("INSERT OR REPLACE INTO admin_settings (k, v) VALUES ('prices', ?1)").bind(JSON.stringify(p)).run();
    return json({ ok: true, prices: p });
  }
  if (path === 'prices/scale' && req.method === 'POST') {
    const pct = Math.max(1, Math.min(1000, Math.floor(Number(body.percent) || 100)));
    const base = body.base && typeof body.base === 'object' ? body.base : {};
    const p = {};
    for (const it of ITEMS) {
      const b = Number(base[it.id]);
      if (Number.isFinite(b) && b > 0 && pct !== 100) p[it.id] = Math.round(b * pct / 100);
    }
    await db.prepare("INSERT OR REPLACE INTO admin_settings (k, v) VALUES ('prices', ?1)").bind(JSON.stringify(p)).run();
    return json({ ok: true, prices: p });
  }
  if (path === 'sales/delete' && req.method === 'POST') {
    await db.prepare('DELETE FROM admin_sales WHERE id = ?1').bind(Number(body.id) || 0).run();
    return json({ ok: true });
  }
  if (path === 'locks/toggle' && req.method === 'POST') {
    const item = String(body.item || '');
    if (!ITEMS.some(i => i.id === item)) return json({ error: 'Unknown item' }, 400);
    const conf = await db.prepare("SELECT v FROM admin_settings WHERE k = 'locks'").first();
    if (!conf) {
      await db.prepare("INSERT INTO admin_settings (k, v) VALUES ('locks', '1')").run();
      await db.prepare("INSERT OR IGNORE INTO admin_locks (item) VALUES ('cape:beta')").run();
    }
    const has = await db.prepare('SELECT 1 FROM admin_locks WHERE item = ?1').bind(item).first();
    if (has) await db.prepare('DELETE FROM admin_locks WHERE item = ?1').bind(item).run();
    else await db.prepare('INSERT INTO admin_locks (item) VALUES (?1)').bind(item).run();
    return json({ locked: await lockedList(db) });
  }
  if (path === 'announce' && req.method === 'POST') {
    const text = String(body.text || '').trim().slice(0, 200);
    if (!text) { await db.prepare("DELETE FROM admin_settings WHERE k = 'announce'").run(); return json({ announce: null }); }
    const hours = Math.max(0, Math.min(24 * 60, Number(body.hours) || 0));
    const a = { id: Date.now(), text, until: hours ? Date.now() + hours * 3600000 : 0 };
    await db.prepare("INSERT OR REPLACE INTO admin_settings (k, v) VALUES ('announce', ?1)").bind(JSON.stringify(a)).run();
    return json({ announce: a });
  }
  if (path === 'player' && req.method === 'POST') {
    const who = await findPlayer(db, body.player);
    if (!who) return json({ error: "Couldn't find that player" }, 404);
    const u = await db.prepare('SELECT name, status, server, seen, country FROM users WHERE uuid = ?1').bind(who.uuid).first();
    const b = await db.prepare('SELECT badge FROM admin_badges WHERE uuid = ?1').bind(who.uuid).first();
    const c = await db.prepare('SELECT v FROM admin_capes WHERE uuid = ?1').bind(who.uuid).first();
    const codesUsed = (await db.prepare('SELECT c.code, u.at FROM code_uses u LEFT JOIN admin_codes c ON c.hash = u.hash WHERE u.uuid = ?1 ORDER BY u.at DESC LIMIT 20').bind(who.uuid).all()).results;
    const gifts = (await db.prepare('SELECT id, coins, items, note, created, claimed FROM admin_gifts WHERE uuid = ?1 ORDER BY id DESC LIMIT 20').bind(who.uuid).all()).results.map(g => ({ ...g, items: JSON.parse(g.items || '[]') }));
    const sales = (await db.prepare('SELECT id, target, percent, until FROM admin_sales WHERE uuid = ?1 AND (until = 0 OR until > ?2)').bind(who.uuid, Date.now()).all()).results;
    const friends = await db.prepare("SELECT COUNT(*) AS n FROM friends WHERE (a = ?1 OR b = ?1) AND state = 'friends'").bind(who.uuid).first().catch(() => null);
    return json({ uuid: who.uuid, name: (u && u.name) || who.name, known: !!u, status: u ? u.status : null, server: u ? u.server : null, seen: u ? u.seen : 0, country: u ? u.country : null, badge: b ? b.badge : (OWNERS.includes(who.uuid) ? 'owner' : ''), cape: c ? c.v : 0, codes: codesUsed, gifts, sales, friends: friends ? friends.n : 0 });
  }
  if (path === 'servers' && req.method === 'POST') {
    const ipAddr = String(body.ip || '').trim();
    if (!/^[a-zA-Z0-9.-]+(:\d{1,5})?$/.test(ipAddr)) return json({ error: 'Enter a server address like play.example.net' }, 400);
    await db.prepare('INSERT INTO admin_servers (name, ip, description, tag) VALUES (?1, ?2, ?3, ?4)')
      .bind(String(body.name || ipAddr).slice(0, 32), ipAddr, String(body.description || '').slice(0, 80), String(body.tag || '').slice(0, 16)).run();
    return json({ ok: true });
  }
  if (path === 'servers/delete' && req.method === 'POST') {
    await db.prepare('DELETE FROM admin_servers WHERE id = ?1').bind(Number(body.id) || 0).run();
    return json({ ok: true });
  }
  return json({ error: 'not found' }, 404);
}
