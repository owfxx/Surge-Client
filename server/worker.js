// Surge Client online service (Cloudflare Worker + D1, free tier).
// Lets every Surge player see each other's cosmetics, emotes and the Surge badge.
//
//   PUT  /v1/me          { uuid, name, wings, halo, cape, emote, emoteAt }  -> heartbeat + what you wear
//   GET  /v1/players?ids=uuid1,uuid2,...                             -> { uuid: {wings, halo, cape, emote, emoteAt, seen} }
//
// Only players seen in the last 15 minutes are returned, so the badge only shows on people using Surge.

const FRESH_MS = 15 * 60 * 1000;
const UUID = /^[0-9a-f]{8}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{12}$/i;
const clean = v => (typeof v === 'string' && /^[a-z0-9_:\-]{0,40}$/i.test(v) ? v : 'none');
const json = (data, status = 200) => new Response(JSON.stringify(data), {
  status, headers: { 'content-type': 'application/json', 'access-control-allow-origin': '*' },
});

export default {
  async fetch(req, env) {
    const url = new URL(req.url);
    await env.DB.prepare('CREATE TABLE IF NOT EXISTS players (uuid TEXT PRIMARY KEY, name TEXT, data TEXT, seen INTEGER)').run();

    if (url.pathname === '/v1/me' && req.method === 'PUT') {
      let body;
      try { body = await req.json(); } catch { return json({ error: 'bad json' }, 400); }
      if (!UUID.test(body.uuid || '')) return json({ error: 'bad uuid' }, 400);
      const uuid = body.uuid.replace(/-/g, '').toLowerCase();
      const data = {
        wings: clean(body.wings), halo: clean(body.halo), cape: clean(body.cape),
        emote: clean(body.emote), emoteAt: Number(body.emoteAt) || 0,
      };
      await env.DB.prepare('INSERT INTO players (uuid, name, data, seen) VALUES (?1, ?2, ?3, ?4) ON CONFLICT(uuid) DO UPDATE SET name = ?2, data = ?3, seen = ?4')
        .bind(uuid, String(body.name || '').slice(0, 16), JSON.stringify(data), Date.now()).run();
      return json({ ok: true, now: Date.now() });
    }

    if (url.pathname === '/v1/players' && req.method === 'GET') {
      const ids = (url.searchParams.get('ids') || '').split(',').map(s => s.replace(/-/g, '').toLowerCase()).filter(s => /^[0-9a-f]{32}$/.test(s)).slice(0, 200);
      if (!ids.length) return json({ players: {}, now: Date.now() });
      const q = `SELECT uuid, data, seen FROM players WHERE seen > ?1 AND uuid IN (${ids.map((_, i) => `?${i + 2}`).join(',')})`;
      const { results } = await env.DB.prepare(q).bind(Date.now() - FRESH_MS, ...ids).all();
      const players = {};
      for (const r of results) players[r.uuid] = { ...JSON.parse(r.data), seen: r.seen };
      return json({ players, now: Date.now() });
    }

    return json({ name: 'Surge Client service', ok: true });
  },
};
