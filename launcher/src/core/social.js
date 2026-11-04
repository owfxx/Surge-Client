const crypto = require('crypto');
const config = require('./config');

let cached = null;

function loadCached(uuid) {
  const s = config.load().surgeSession;
  if (s && s.token && (!uuid || s.uuid === uuid)) return s;
  return null;
}

async function refreshService() {
  try {
    const repo = require('./release').repo();
    if (!repo) return;
    const r = await fetch(`https://github.com/${repo}/releases/download/data/service.json`, { signal: AbortSignal.timeout(6000) });
    if (!r.ok) return;
    const d = await r.json();
    if (d && typeof d.api === 'string' && /^https:\/\//.test(d.api) && d.api !== config.load().autoApiUrl) config.save({ autoApiUrl: d.api });
  } catch { }
}

function apiUrl() {
  const cfg = config.load();
  let u = (cfg.apiUrl || cfg.autoApiUrl || require('./release').bundledApi() || '').trim().replace(/\/+$/, '');
  if (u && !/^https?:\/\//i.test(u)) u = `https://${u}`;
  return u;
}

async function signIn(authorization) {
  const api = apiUrl();
  if (!api || !authorization || !authorization.access_token) return null;
  const uuid = String(authorization.uuid || '').replace(/-/g, '').toLowerCase();
  if (cached && cached.uuid === uuid) return cached;
  const saved = loadCached(uuid);
  if (saved) { cached = saved; return cached; }
  const serverId = crypto.randomBytes(16).toString('hex');
  const join = await fetch('https://sessionserver.mojang.com/session/minecraft/join', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ accessToken: authorization.access_token, selectedProfile: uuid, serverId }),
  });
  if (join.status !== 204 && !join.ok) throw new Error(`Mojang sign-in failed (${join.status})`);
  const res = await fetch(`${api}/v1/auth`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name: authorization.name, serverId }),
  });
  const data = await res.json();
  if (!res.ok || !data.token) throw new Error(data.error || `Surge sign-in failed (${res.status})`);
  cached = { uuid: data.uuid, token: data.token, name: data.name, at: Date.now() };
  config.save({ surgeSession: cached });
  return cached;
}

function forget() {
  cached = null;
  config.save({ surgeSession: null });
}

async function social(retry = true) {
  if (!config.load().apiUrl && !config.load().autoApiUrl) await refreshService();
  const api = apiUrl();
  if (!api) return { off: true };
  const active = require('./auth').activeUuid();
  if (!active) return { signedOut: true };
  if (cached && cached.uuid !== active) cached = null;
  if (!cached) cached = loadCached(active);
  if (!cached) {
    let auth;
    try { auth = await require('./auth').getLaunchAuth(); } catch (e) {
      return /sign in/i.test(e.message) ? { signedOut: true } : { error: `Couldn't refresh your Microsoft sign-in. ${e.message}` };
    }
    try { await signIn(auth); } catch (e) { return { error: e.message }; }
    if (!cached) return { error: 'Could not sign in to Surge' };
  }
  let res;
  try { res = await fetch(`${api}/v1/social`, { headers: { Authorization: `Bearer ${cached.token}` }, signal: AbortSignal.timeout(8000) }); }
  catch { return { error: 'The Surge service is not reachable right now' }; }
  if (res.status === 401 && retry) { forget(); return social(false); }
  if (!res.ok) return { error: `The Surge service answered ${res.status}` };
  return res.json();
}

function token() { return cached ? cached.token : ''; }

module.exports = { refreshService, apiUrl, signIn, social, token, forget };
