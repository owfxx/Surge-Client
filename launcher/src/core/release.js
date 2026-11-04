const fs = require('fs');
const path = require('path');
const os = require('os');
const { spawn } = require('child_process');
const paths = require('./paths');
const config = require('./config');

const API = 'https://api.github.com';
const UA = 'SurgeClient';
const UPDATES = path.join(paths.root, 'mod-updates');

function bundled() {
  for (const p of [
    process.resourcesPath && path.join(process.resourcesPath, 'update-source.json'),
    path.join(__dirname, '..', '..', 'resources', 'update-source.json'),
  ]) {
    try { if (p && fs.existsSync(p)) return JSON.parse(fs.readFileSync(p, 'utf8')); } catch { }
  }
  return {};
}

function bundledSource() { return bundled().repo || ''; }

function bundledApi() { return bundled().api || ''; }

function repo() {
  return (config.load().updateRepo || bundledSource() || '').trim().replace(/^https?:\/\/github\.com\//, '').replace(/\/$/, '');
}

function codesUrl() {
  const r = repo();
  return r ? `https://github.com/${r}/releases/download/data/codes.json` : '';
}

function headers(token) {
  const h = { 'User-Agent': UA, Accept: 'application/vnd.github+json', 'X-GitHub-Api-Version': '2022-11-28' };
  if (token) h.Authorization = `Bearer ${token}`;
  return h;
}

async function gh(method, url, token, body, extra = {}) {
  const res = await fetch(url.startsWith('http') ? url : API + url, {
    method, headers: { ...headers(token), ...extra }, body,
  });
  if (res.status === 204) return null;
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }
  if (!res.ok) {
    const e = new Error(res.status === 401 ? 'GitHub token is invalid or expired'
      : res.status === 404 ? 'Repo or release not found (check the repo name and the token\'s access)'
      : `GitHub said ${res.status}: ${data && data.message ? data.message : text.slice(0, 120)}`);
    e.status = res.status;
    throw e;
  }
  return data;
}

const num = v => String(v || '0').split(/[+]|-mc/)[0].replace(/^v/, '').split('.').map(n => parseInt(n, 10) || 0);
function newer(a, b) {
  const x = num(a), y = num(b);
  for (let i = 0; i < Math.max(x.length, y.length); i++) {
    if ((x[i] || 0) !== (y[i] || 0)) return (x[i] || 0) > (y[i] || 0);
  }
  return false;
}
const jarVersion = name => (name.match(/^surge-client-(.+?)\.jar$/) || [])[1] || '0';

async function releases() {
  const r = repo();
  if (!r) return [];
  return gh('GET', `/repos/${r}/releases?per_page=30`);
}

function updateDir(mcVersion) { return path.join(UPDATES, mcVersion); }

function downloadedMod(mcVersion) {
  const dir = updateDir(mcVersion);
  if (!fs.existsSync(dir)) return null;
  const jars = fs.readdirSync(dir).filter(f => f.endsWith('.jar'));
  jars.sort((a, b) => (newer(jarVersion(a), jarVersion(b)) ? -1 : 1));
  return jars[0] ? path.join(dir, jars[0]) : null;
}

async function updateMod(mcVersion, haveVersion, onProgress = () => {}) {
  const list = await releases();
  let best = null;
  for (const rel of list) {
    if (rel.draft) continue;
    for (const a of rel.assets || []) {
      if (!/^surge-client-.+\.jar$/.test(a.name) || !(a.name.endsWith(`-mc${mcVersion}.jar`) || a.name.endsWith(`+mc${mcVersion}.jar`))) continue;
      if (!best || newer(jarVersion(a.name), jarVersion(best.name))) best = a;
    }
  }
  if (!best) return null;
  const have = downloadedMod(mcVersion);
  const current = [haveVersion, have ? jarVersion(path.basename(have)) : '0'].reduce((m, v) => (newer(v, m) ? v : m), '0');
  if (!newer(jarVersion(best.name), current)) return null;
  onProgress({ stage: `Updating Surge mod to ${jarVersion(best.name).split('+')[0]}…` });
  const res = await fetch(best.browser_download_url, { headers: { 'User-Agent': UA } });
  if (!res.ok) throw new Error(`Download failed (${res.status})`);
  const buf = Buffer.from(await res.arrayBuffer());
  const dir = updateDir(mcVersion);
  fs.mkdirSync(dir, { recursive: true });
  for (const f of fs.readdirSync(dir)) fs.rmSync(path.join(dir, f), { force: true });
  fs.writeFileSync(path.join(dir, best.name), buf);
  return path.join(dir, best.name);
}

async function checkLauncher(currentVersion) {
  const list = await releases();
  for (const rel of list) {
    if (rel.draft) continue;
    const a = (rel.assets || []).find(x => /^Surge-Client-[\d.]+-Standalone\.exe$/.test(x.name));
    if (!a) continue;
    const v = a.name.match(/Surge-Client-([\d.]+)-Standalone/)[1];
    if (newer(v, currentVersion)) return { version: v, url: a.browser_download_url, notes: rel.body || '' };
    return null;
  }
  return null;
}

async function applyLauncher(url, onProgress = () => {}) {
  onProgress({ stage: 'Downloading update…' });
  const res = await fetch(url, { headers: { 'User-Agent': UA } });
  if (!res.ok) throw new Error(`Download failed (${res.status})`);
  const buf = Buffer.from(await res.arrayBuffer());
  const target = process.env.PORTABLE_EXECUTABLE_FILE;
  const tmp = path.join(os.tmpdir(), `surge-update-${Date.now()}.exe`);
  fs.writeFileSync(tmp, buf);
  if (!target || process.platform !== 'win32') return { file: tmp, restart: false };
  const script = path.join(os.tmpdir(), `surge-update-${Date.now()}.cmd`);
  fs.writeFileSync(script, [
    '@echo off',
    'timeout /t 2 /nobreak >nul',
    ':retry',
    `move /y "${tmp}" "${target}" >nul 2>&1 || (timeout /t 1 /nobreak >nul & goto retry)`,
    `start "" "${target}"`,
    'del "%~f0"',
  ].join('\r\n'));
  spawn('cmd.exe', ['/c', script], { detached: true, stdio: 'ignore', windowsHide: true }).unref();
  return { file: target, restart: true };
}

async function releaseByTag(r, tag, token) {
  try { return await gh('GET', `/repos/${r}/releases/tags/${encodeURIComponent(tag)}`, token); } catch (e) { if (e.status === 404) return null; throw e; }
}

async function uploadAsset(r, rel, token, name, buf, type) {
  for (const a of rel.assets || []) if (a.name === name) await gh('DELETE', `/repos/${r}/releases/assets/${a.id}`, token);
  const up = rel.upload_url.replace(/\{.*\}$/, '');
  return gh('POST', `${up}?name=${encodeURIComponent(name)}`, token, buf, { 'Content-Type': type });
}

async function publishFile({ token, file, notes = '' }) {
  const r = repo();
  if (!r) throw new Error('Set the update repo first (owner/name)');
  if (!token) throw new Error('Add a GitHub token first');
  const name = path.basename(file);
  let tag, title, type;
  if (/^surge-client-.+\.jar$/.test(name)) {
    const v = jarVersion(name).split('+')[0];
    tag = `mod-v${v}`; title = `Surge mod ${v}`; type = 'application/java-archive';
  } else if (/^Surge-Client-[\d.]+-Standalone\.exe$/.test(name)) {
    const v = name.match(/Surge-Client-([\d.]+)-Standalone/)[1];
    tag = `v${v}`; title = `Surge Client ${v}`; type = 'application/octet-stream';
  } else {
    throw new Error('Pick a file named like surge-client-0.9.0+mc1.21.11.jar or Surge-Client-0.9.0-Standalone.exe');
  }
  let rel = await releaseByTag(r, tag, token);
  if (!rel) rel = await gh('POST', `/repos/${r}/releases`, token, JSON.stringify({ tag_name: tag, name: title, body: notes || title, make_latest: 'false' }), { 'Content-Type': 'application/json' });
  await uploadAsset(r, rel, token, name, fs.readFileSync(file), type);
  return { tag, url: rel.html_url };
}

async function publishCodes({ token, json }) {
  const r = repo();
  if (!r) throw new Error('Set the update repo first (owner/name)');
  if (!token) throw new Error('Add a GitHub token first');
  let rel = await releaseByTag(r, 'data', token);
  if (!rel) rel = await gh('POST', `/repos/${r}/releases`, token, JSON.stringify({ tag_name: 'data', name: 'Codes and sales', body: 'Used by Surge Client for codes and sales. Don\'t delete.', make_latest: 'false' }), { 'Content-Type': 'application/json' });
  await uploadAsset(r, rel, token, 'codes.json', Buffer.from(JSON.stringify(json)), 'application/json');
  return codesUrl();
}

async function publishDiscover({ token, servers }) {
  const r = repo();
  if (!r) throw new Error('Set the update repo first (owner/name)');
  if (!token) throw new Error('Add a GitHub token first');
  let rel = await releaseByTag(r, 'data', token);
  if (!rel) rel = await gh('POST', `/repos/${r}/releases`, token, JSON.stringify({ tag_name: 'data', name: 'Codes and sales', body: 'Used by Surge Client for codes, sales and Discover. Don\'t delete.', make_latest: 'false' }), { 'Content-Type': 'application/json' });
  await uploadAsset(r, rel, token, 'discover.json', Buffer.from(JSON.stringify({ servers })), 'application/json');
  return `https://github.com/${r}/releases/download/data/discover.json`;
}

module.exports = { bundledApi, publishDiscover, repo, codesUrl, releases, updateMod, downloadedMod, checkLauncher, applyLauncher, publishFile, publishCodes, newer, jarVersion };
