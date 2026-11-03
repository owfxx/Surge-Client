// Signs the player in to the Surge online service with their Minecraft account,
// the same way servers check accounts: Mojang "join" with a random id, then the
// service asks Mojang "hasJoined". The token goes to the game (-Dsurge.token).
const crypto = require('crypto');
const config = require('./config');

let cached = null; // { uuid, token, name, at }

function apiUrl() {
  return (config.load().apiUrl || '').trim().replace(/\/$/, '');
}

async function signIn(authorization) {
  const api = apiUrl();
  if (!api || !authorization || !authorization.access_token) return null;
  const uuid = String(authorization.uuid || '').replace(/-/g, '');
  if (cached && cached.uuid === uuid && Date.now() - cached.at < 6 * 3600 * 1000) return cached;
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
  return cached;
}

/** Friends and requests for the launcher's home screen (signs in first if needed). */
async function social() {
  const api = apiUrl();
  if (!api) return null;
  if (!cached) {
    try { await signIn(await require('./auth').getLaunchAuth()); } catch { return null; }
    if (!cached) return null;
  }
  const res = await fetch(`${api}/v1/social`, { headers: { Authorization: `Bearer ${cached.token}` } });
  return res.ok ? res.json() : null;
}

function token() { return cached ? cached.token : ''; }

module.exports = { signIn, social, token };
