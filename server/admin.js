const ITEMS = [{"id": "wings:angel", "name": "Angel Wings"}, {"id": "wings:surge", "name": "Surge Wings"}, {"id": "wings:soul", "name": "Soul Wings"}, {"id": "wings:flame", "name": "Flame Wings"}, {"id": "wings:void", "name": "Void Wings"}, {"id": "wings:crystal", "name": "Crystal Wings"}, {"id": "wings:gold", "name": "Golden Wings"}, {"id": "halo:angel", "name": "Angel Halo"}, {"id": "halo:surge", "name": "Surge Halo"}, {"id": "halo:fire", "name": "Fire Halo"}, {"id": "halo:enchanted", "name": "Enchanted Halo"}, {"id": "halo:void", "name": "Void Halo"}, {"id": "halo:rose", "name": "Rose Halo"}, {"id": "cape:surge", "name": "Surge Cape"}, {"id": "cape:void", "name": "Void Cape"}, {"id": "cape:flame", "name": "Flame Cape"}, {"id": "cape:ocean", "name": "Ocean Cape"}, {"id": "cape:aurora", "name": "Aurora Cape"}, {"id": "cape:sakura", "name": "Sakura Cape"}, {"id": "cape:midnight", "name": "Midnight Cape"}, {"id": "cape:royal", "name": "Royal Cape"}, {"id": "cape:emerald", "name": "Emerald Cape"}, {"id": "cape:galaxy", "name": "Galaxy Cape"}, {"id": "cape:classic_red", "name": "Red Cape"}, {"id": "cape:classic_orange", "name": "Orange Cape"}, {"id": "cape:classic_yellow", "name": "Yellow Cape"}, {"id": "cape:classic_lime", "name": "Lime Cape"}, {"id": "cape:classic_green", "name": "Green Cape"}, {"id": "cape:classic_cyan", "name": "Cyan Cape"}, {"id": "cape:classic_blue", "name": "Blue Cape"}, {"id": "cape:classic_purple", "name": "Purple Cape"}, {"id": "cape:classic_pink", "name": "Pink Cape"}, {"id": "cape:classic_white", "name": "White Cape"}, {"id": "cape:classic_gray", "name": "Gray Cape"}, {"id": "cape:classic_black", "name": "Black Cape"}, {"id": "cape:beta", "name": "Beta Tester Cape"}, {"id": "emote:WAVE", "name": "Wave (emote)"}, {"id": "emote:CLAP", "name": "Clap (emote)"}, {"id": "emote:FLUSTERED", "name": "Flustered (emote)"}, {"id": "emote:FACEPALM", "name": "Facepalm (emote)"}, {"id": "emote:DAB", "name": "Dab (emote)"}, {"id": "emote:SHRUG", "name": "Shrug (emote)"}, {"id": "emote:SALUTE", "name": "Salute (emote)"}, {"id": "emote:CHEER", "name": "Cheer (emote)"}, {"id": "emote:THINKING", "name": "Thinking (emote)"}, {"id": "emote:LAUGH", "name": "Laugh (emote)"}, {"id": "emote:BOW", "name": "Bow (emote)"}, {"id": "emote:HEART", "name": "Heart (emote)"}, {"id": "emote:BOXING", "name": "Boxing (emote)"}, {"id": "emote:FLOSS", "name": "Floss (emote)"}, {"id": "emote:GROOVE", "name": "Groove (emote)"}, {"id": "emote:ZOMBIE", "name": "Zombie (emote)"}, {"id": "emote:TPOSE", "name": "T-Pose (emote)"}, {"id": "emote:CRY", "name": "Cry (emote)"}, {"id": "emote:SIT", "name": "Sit (emote)"}, {"id": "emote:POINT", "name": "Point (emote)"}, {"id": "emote:PEACE", "name": "Peace (emote)"}, {"id": "emote:STRETCH", "name": "Stretch (emote)"}, {"id": "emote:TWIRL", "name": "Twirl (emote)"}, {"id": "emote:JUMP", "name": "Jump for Joy (emote)"}, {"id": "emote:MEDITATE", "name": "Meditate (emote)"}, {"id": "emote:HEADBANG", "name": "Headbang (emote)"}, {"id": "emote:YAWN", "name": "Yawn (emote)"}, {"id": "emote:GUITAR", "name": "Air Guitar (emote)"}, {"id": "emote:ROBOT", "name": "Robot (emote)"}, {"id": "emote:CHICKEN", "name": "Chicken Dance (emote)"}, {"id": "emote:WAVE_BOTH", "name": "Big Wave (emote)"}, {"id": "emote:VICTORY", "name": "Victory (emote)"}, {"id": "emote:SHY", "name": "Shy (emote)"}, {"id": "emote:KICK", "name": "Kick (emote)"}, {"id": "emote:SALSA", "name": "Salsa (emote)"}, {"id": "emote:DRUMS", "name": "Drums (emote)"}, {"id": "emote:SWIM", "name": "Swim (emote)"}, {"id": "emote:GRIDDY", "name": "Griddy (emote)"}, {"id": "emote:MOONWALK", "name": "Moonwalk (emote)"}, {"id": "emote:SHUFFLE", "name": "Shuffle (emote)"}, {"id": "emote:RUNNING_MAN", "name": "Running Man (emote)"}, {"id": "emote:DISCO", "name": "Disco Fever (emote)"}, {"id": "emote:TWIST", "name": "The Twist (emote)"}, {"id": "emote:CHARLESTON", "name": "Charleston (emote)"}, {"id": "emote:PONY", "name": "Pony (emote)"}, {"id": "emote:TOPROCK", "name": "Toprock (emote)"}, {"id": "emote:WOAH", "name": "The Woah (emote)"}];

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
  try { await db.prepare('ALTER TABLE admin_codes ADD COLUMN max_uses INTEGER DEFAULT 0').run(); } catch { }
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
  const sales = (await db.prepare('SELECT target, percent, until FROM admin_sales WHERE until = 0 OR until > ?1').bind(now).all()).results;
  const servers = (await db.prepare('SELECT name, ip, description, tag FROM admin_servers ORDER BY id').all()).results;
  const out = { codes, sales, servers };
  const locked = await lockedList(db);
  if (locked) out.locked = locked;
  return out;
}

async function state(db) {
  const now = Date.now();
  const codes = (await db.prepare('SELECT c.hash, c.code, c.coins, c.items, c.created, c.max_uses, (SELECT COUNT(*) FROM code_uses u WHERE u.hash = c.hash) AS used FROM admin_codes c ORDER BY c.created DESC').all()).results.map(c => ({ ...c, items: JSON.parse(c.items || '[]') }));
  const sales = (await db.prepare('SELECT id, target, percent, until FROM admin_sales WHERE until = 0 OR until > ?1 ORDER BY id').bind(now).all()).results;
  const servers = (await db.prepare('SELECT id, name, ip, description, tag FROM admin_servers ORDER BY id').all()).results;
  const locked = (await lockedList(db)) || ['cape:beta'];
  return { codes, sales, servers, locked, items: ITEMS };
}

async function authed(req, db) {
  const m = (req.headers.get('authorization') || '').match(/^Bearer ([a-f0-9]{64})$/);
  if (!m) return false;
  const s = await db.prepare('SELECT expires FROM admin_sessions WHERE token = ?1').bind(m[1]).first();
  return !!s && s.expires > Date.now();
}

export async function adminApi(req, env, db, path) {
  const ip = req.headers.get('cf-connecting-ip') || 'unknown';
  let body = {};
  if (req.method === 'POST') { try { body = await req.json(); } catch { body = {}; } }

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
    const percent = Math.max(5, Math.min(90, Math.floor(Number(body.percent) || 20)));
    const hours = Math.max(0, Math.min(24 * 90, Number(body.hours) || 0));
    const target = String(body.target || 'all').slice(0, 60);
    await db.prepare('INSERT INTO admin_sales (target, percent, until) VALUES (?1, ?2, ?3)').bind(target, percent, hours ? Date.now() + hours * 3600000 : 0).run();
    return json({ ok: true });
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
