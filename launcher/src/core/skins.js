const fs = require('fs');
const path = require('path');
const paths = require('./paths');
const auth = require('./auth');
const { UA } = require('./http');

const API = 'https://api.minecraftservices.com/minecraft/profile';
const LIB = path.join(paths.root, 'skins');
fs.mkdirSync(LIB, { recursive: true });

async function token() {
  const a = await auth.getLaunchAuth();
  return a.access_token;
}

async function call(method, url, body, headers = {}) {
  const res = await fetch(url, { method, body, headers: { 'User-Agent': UA, Authorization: `Bearer ${await token()}`, ...headers } });
  if (res.status === 401) throw new Error('Sign in again to change skins');
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`Minecraft rejected the change (${res.status})${text.includes('errorMessage') ? `: ${JSON.parse(text).errorMessage}` : ''}`);
  }
  return res.status === 204 ? null : res.json();
}

async function profile() {
  const p = await call('GET', API);
  return {
    name: p.name,
    skin: (p.skins || []).find(s => s.state === 'ACTIVE') || null,
    capes: (p.capes || []).map(c => ({ id: c.id, name: c.alias, url: c.url, active: c.state === 'ACTIVE' })),
  };
}

function validate(file) {
  const buf = fs.readFileSync(file);
  if (buf.subarray(1, 4).toString() !== 'PNG') throw new Error('Skins must be PNG images');
  const w = buf.readUInt32BE(16), h = buf.readUInt32BE(20);
  if (w !== 64 || (h !== 64 && h !== 32)) throw new Error('Skins must be 64×64 (or 64×32) pixels');
  return buf;
}

async function upload(name, variant) {
  const file = path.join(LIB, path.basename(name));
  const buf = validate(file);
  const form = new FormData();
  form.append('variant', variant === 'slim' ? 'slim' : 'classic');
  form.append('file', new Blob([buf], { type: 'image/png' }), 'skin.png');
  await call('POST', `${API}/skins`, form);
  const now = new Date();
  try { fs.utimesSync(file, now, now); } catch { }
  return profile();
}

async function setCape(id) {
  if (id) await call('PUT', `${API}/capes/active`, JSON.stringify({ capeId: id }), { 'Content-Type': 'application/json' });
  else await call('DELETE', `${API}/capes/active`);
  return profile();
}

function library() {
  return fs.readdirSync(LIB).filter(f => f.toLowerCase().endsWith('.png'))
    .map(f => ({ f, t: fs.statSync(path.join(LIB, f)).mtimeMs })).sort((a, b) => b.t - a.t).map(x => x.f)
    .map(f => ({ name: f, data: `data:image/png;base64,${fs.readFileSync(path.join(LIB, f)).toString('base64')}` }));
}

function importFiles(files) {
  for (const f of files) {
    validate(f);
    let dest = path.join(LIB, path.basename(f));
    for (let n = 2; fs.existsSync(dest); n++) dest = path.join(LIB, `${path.basename(f, '.png')}-${n}.png`);
    fs.copyFileSync(f, dest);
  }
  return library();
}

function remove(name) {
  fs.rmSync(path.join(LIB, path.basename(name)), { force: true });
  return library();
}

async function saveCurrent() {
  const p = await profile();
  if (!p.skin) throw new Error('This account has no custom skin');
  const res = await fetch(p.skin.url);
  fs.writeFileSync(path.join(LIB, `${p.name}.png`), Buffer.from(await res.arrayBuffer()));
  return library();
}

async function current() {
  const p = await profile();
  if (!p.skin) return { name: null, variant: 'classic', library: library() };
  const res = await fetch(p.skin.url);
  const buf = Buffer.from(await res.arrayBuffer());
  let name = null;
  for (const f of fs.readdirSync(LIB).filter(x => x.toLowerCase().endsWith('.png'))) {
    try { if (fs.readFileSync(path.join(LIB, f)).equals(buf)) { name = f; break; } } catch { }
  }
  if (!name) {
    name = `${p.name}.png`;
    for (let n = 2; fs.existsSync(path.join(LIB, name)); n++) name = `${p.name}-${n}.png`;
    fs.writeFileSync(path.join(LIB, name), buf);
  } else {
    const now = new Date();
    try { fs.utimesSync(path.join(LIB, name), now, now); } catch { }
  }
  return { name, variant: p.skin.variant === 'SLIM' ? 'slim' : 'classic', library: library() };
}

module.exports = { profile, upload, setCape, library, importFiles, remove, saveCurrent, current, LIB };
