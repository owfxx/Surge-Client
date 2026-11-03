// Downloads the exact Java runtime Mojang ships for a Minecraft version
// (e.g. Java 21 for 1.21.x), so players never have to install Java themselves.
const fs = require('fs');
const path = require('path');
const paths = require('./paths');
const { getJson, download, pool } = require('./http');

const RUNTIME_INDEX = 'https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json';
const VERSION_MANIFEST = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json';

function platformKey() {
  const arm = process.arch === 'arm64';
  if (process.platform === 'win32') return arm ? 'windows-arm64' : (process.arch === 'ia32' ? 'windows-x86' : 'windows-x64');
  if (process.platform === 'darwin') return arm ? 'mac-os-arm64' : 'mac-os';
  return process.arch === 'ia32' ? 'linux-i386' : 'linux';
}

async function getVersionJson(mcVersion) {
  const cached = path.join(paths.cache, `${mcVersion}.json`);
  if (fs.existsSync(cached)) return JSON.parse(fs.readFileSync(cached, 'utf8'));
  const manifest = await getJson(VERSION_MANIFEST);
  const entry = manifest.versions.find(v => v.id === mcVersion);
  if (!entry) throw new Error(`Unknown Minecraft version ${mcVersion}`);
  const json = await getJson(entry.url);
  fs.writeFileSync(cached, JSON.stringify(json));
  return json;
}

function javaBinary(dir) {
  if (process.platform === 'win32') return path.join(dir, 'bin', 'javaw.exe');
  if (process.platform === 'darwin') return path.join(dir, 'jre.bundle', 'Contents', 'Home', 'bin', 'java');
  return path.join(dir, 'bin', 'java');
}

async function ensureJava(mcVersion, onProgress = () => {}) {
  const version = await getVersionJson(mcVersion);
  const component = version.javaVersion?.component || 'jre-legacy';
  const dir = path.join(paths.runtimes, component);
  const bin = javaBinary(dir);
  const marker = path.join(dir, '.surge-complete');
  if (fs.existsSync(marker) && fs.existsSync(bin)) return bin;

  onProgress({ stage: `Downloading Java (${component})`, current: 0, total: 1 });
  const index = await getJson(RUNTIME_INDEX);
  const entry = index[platformKey()]?.[component]?.[0];
  if (!entry) throw new Error(`Mojang has no ${component} runtime for ${platformKey()}. Set a Java path in Settings.`);
  const manifest = await getJson(entry.manifest.url);

  const files = [];
  for (const [rel, info] of Object.entries(manifest.files)) {
    const target = path.join(dir, rel);
    if (info.type === 'directory') fs.mkdirSync(target, { recursive: true });
    else if (info.type === 'file') files.push({ target, raw: info.downloads.raw, executable: info.executable });
  }
  await pool(files, 16, async f => {
    await download(f.raw.url, f.target, f.raw.sha1);
    if (f.executable && process.platform !== 'win32') fs.chmodSync(f.target, 0o755);
  }, (current, total) => onProgress({ stage: `Downloading Java (${component})`, current, total }));

  for (const [rel, info] of Object.entries(manifest.files)) {
    if (info.type === 'link' && process.platform !== 'win32') {
      const target = path.join(dir, rel);
      try { fs.symlinkSync(info.target, target); } catch { /* already exists */ }
    }
  }
  fs.writeFileSync(marker, new Date().toISOString());
  return bin;
}

module.exports = { ensureJava, getVersionJson };
