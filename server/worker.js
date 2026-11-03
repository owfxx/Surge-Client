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

export default {
  async fetch(req, env) {
    const db = env.DB;
    const url = new URL(req.url);
    const p = url.pathname;
    await setup(db);
    let body = {};
    if (req.method === 'POST' || req.method === 'PUT') { try { body = await req.json(); } catch { body = {}; } }

    if (p === '/v1/auth' && req.method === 'POST') {
      const name = String(body.name || ''), serverId = String(body.serverId || '');
      if (!/^[A-Za-z0-9_]{1,16}$/.test(name) || !/^[A-Za-z0-9_-]{8,64}$/.test(serverId)) return json({ error: 'bad request' }, 400);
      const r = await fetch(`https://sessionserver.mojang.com/session/minecraft/hasJoined?username=${encodeURIComponent(name)}&serverId=${encodeURIComponent(serverId)}`);
      if (r.status !== 200) return json({ error: 'Mojang could not confirm this account' }, 401);
      const prof = await r.json();
      const uuid = norm(prof.id);
      const token = [...crypto.getRandomValues(new Uint8Array(24))].map(b => b.toString(16).padStart(2, '0')).join('');
      await db.prepare(`INSERT INTO users (uuid, name, name_lc, token, status, server, seen) VALUES (?1, ?2, ?3, ?4, 'online', '', ?5)
        ON CONFLICT(uuid) DO UPDATE SET name = ?2, name_lc = ?3, token = ?4, seen = ?5`).bind(uuid, prof.name, prof.name.toLowerCase(), token, Date.now()).run();
      return json({ token, uuid, name: prof.name });
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
