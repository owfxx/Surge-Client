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
  const cr = await fetch('https://api.minecraftservices.com/player/certificates', {
    method: 'POST', headers: { Authorization: `Bearer ${authorization.access_token}`, 'Content-Type': 'application/json' }, signal: AbortSignal.timeout(10000),
  });
  if (!cr.ok) throw new Error(`Minecraft couldn't confirm your account (${cr.status})`);
  const cert = await cr.json();
  const strip = pem => String(pem || '').replace(/-----[A-Z ]+-----/g, '').replace(/\s+/g, '');
  const key = crypto.createPrivateKey({ key: Buffer.from(strip(cert.keyPair.privateKey), 'base64'), format: 'der', type: 'pkcs8' });
  const ts = Date.now();
  const sig = crypto.sign('sha256', Buffer.from(`surge-auth:${uuid}:${ts}`), key).toString('base64');
  const res = await fetch(`${api}/v1/auth/key`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, signal: AbortSignal.timeout(15000),
    body: JSON.stringify({ uuid, name: authorization.name, ts, sig, publicKey: strip(cert.keyPair.publicKey), expiresAt: new Date(cert.expiresAt).getTime(), keySig: cert.publicKeySignatureV2 }),
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

async function call(method, pth, body) {
  if (!cached) { const r = await social(); if (!cached) throw new Error((r && r.error) || 'Sign in with a Microsoft account first'); }
  const api = apiUrl();
  const res = await fetch(`${api}${pth}`, {
    method, headers: { Authorization: `Bearer ${cached.token}`, 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined, signal: AbortSignal.timeout(10000),
  });
  let data = {};
  try { data = await res.json(); } catch { }
  if (!res.ok) throw new Error(data.error || `The Surge service answered ${res.status}`);
  return data;
}

const messages = since => call('GET', `/v1/messages?since=${Math.max(0, Number(since) || 0)}`);
const sendMessage = (uuid, text) => call('POST', '/v1/messages', { uuid, text });

module.exports = { refreshService, apiUrl, signIn, social, token, forget, messages, sendMessage, call };
