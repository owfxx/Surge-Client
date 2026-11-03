const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const AdmZip = require('adm-zip');
const paths = require('./paths');

const CONTENT_DIRS = { mod: 'mods', resourcepack: 'resourcepacks', shader: 'shaderpacks' };

function dirOf(id) { return path.join(paths.instances, id); }
function metaPath(id) { return path.join(dirOf(id), 'instance.json'); }

function slugify(name) {
  return name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '').slice(0, 40) || 'modpack';
}

function list() {
  if (!fs.existsSync(paths.instances)) return [];
  return fs.readdirSync(paths.instances)
    .filter(id => fs.existsSync(metaPath(id)))
    .map(get)
    .filter(Boolean)
    .sort((a, b) => (b.lastPlayed || b.created || 0) - (a.lastPlayed || a.created || 0));
}

function get(id) {
  try { return JSON.parse(fs.readFileSync(metaPath(id), 'utf8')); } catch { return null; }
}

function write(inst) {
  fs.mkdirSync(dirOf(inst.id), { recursive: true });
  fs.writeFileSync(metaPath(inst.id), JSON.stringify(inst, null, 2));
  return inst;
}

function create({ id, name, mcVersion, loaderVersion, icon = null, description = '', surgeOptimized = false }) {
  let finalId = id || slugify(name);
  if (!id) { let n = 2; while (fs.existsSync(dirOf(finalId))) finalId = `${slugify(name)}-${n++}`; }
  for (const d of Object.values(CONTENT_DIRS)) fs.mkdirSync(path.join(dirOf(finalId), d), { recursive: true });
  return write({
    id: finalId, name, mcVersion, loader: 'fabric', loaderVersion, icon, description,
    surgeOptimized, ramMb: null, created: Date.now(), lastPlayed: null, content: [],
  });
}

function update(id, patch) {
  const inst = get(id);
  if (!inst) throw new Error('Modpack not found');
  return write({ ...inst, ...patch, id });
}

function remove(id) {
  fs.rmSync(dirOf(id), { recursive: true, force: true });
}

function addContent(id, record) {
  const inst = get(id);
  inst.content = inst.content.filter(c => !(c.type === record.type && (c.file === record.file || (record.projectId && c.projectId === record.projectId && c.source === record.source))));
  inst.content.push(record);
  write(inst);
}

function hasProject(id, source, projectId) {
  const inst = get(id);
  return !!inst?.content.find(c => {
    if (c.source !== source || String(c.projectId) !== String(projectId)) return false;
    const base = path.join(dirOf(id), CONTENT_DIRS[c.type] || 'mods', c.file);
    return fs.existsSync(base) || fs.existsSync(`${base}.disabled`);
  });
}

function listContent(id) {
  const inst = get(id);
  const out = [];
  for (const [type, folder] of Object.entries(CONTENT_DIRS)) {
    const dir = path.join(dirOf(id), folder);
    if (!fs.existsSync(dir)) continue;
    for (const f of fs.readdirSync(dir)) {
      const full = path.join(dir, f);
      if (fs.statSync(full).isDirectory() && type !== 'resourcepack' && type !== 'shader') continue;
      const enabled = !f.endsWith('.disabled');
      const file = enabled ? f : f.slice(0, -'.disabled'.length);
      const meta = inst.content.find(c => c.type === type && c.file === file) || {};
      out.push({ type, file, enabled, title: meta.title || file.replace(/\.(jar|zip)$/, ''), iconUrl: meta.iconUrl || null, source: meta.source || 'local', projectId: meta.projectId || null, locked: !!meta.locked });
    }
  }
  return out.sort((a, b) => a.title.localeCompare(b.title));
}

function toggleContent(id, type, file) {
  const dir = path.join(dirOf(id), CONTENT_DIRS[type]);
  const on = path.join(dir, file);
  const off = `${on}.disabled`;
  if (fs.existsSync(on)) fs.renameSync(on, off);
  else if (fs.existsSync(off)) fs.renameSync(off, on);
}

function removeContent(id, type, file) {
  const dir = path.join(dirOf(id), CONTENT_DIRS[type]);
  for (const p of [path.join(dir, file), path.join(dir, `${file}.disabled`)]) fs.rmSync(p, { recursive: true, force: true });
  const inst = get(id);
  inst.content = inst.content.filter(c => !(c.type === type && c.file === file));
  write(inst);
}

function sha(file, algo) { return crypto.createHash(algo).update(fs.readFileSync(file)).digest('hex'); }

function exportMrpack(id, dest) {
  const inst = get(id);
  const zip = new AdmZip();
  const files = [];
  for (const item of listContent(id)) {
    if (!item.enabled) continue;
    const folder = CONTENT_DIRS[item.type];
    const full = path.join(dirOf(id), folder, item.file);
    const meta = inst.content.find(c => c.type === item.type && c.file === item.file);
    if (meta?.source === 'modrinth' && meta.url) {
      files.push({
        path: `${folder}/${item.file}`,
        hashes: { sha1: sha(full, 'sha1'), sha512: sha(full, 'sha512') },
        env: { client: 'required', server: item.type === 'mod' ? 'optional' : 'unsupported' },
        downloads: [meta.url],
        fileSize: fs.statSync(full).size,
      });
    } else if (fs.statSync(full).isFile()) {
      zip.addLocalFile(full, `overrides/${folder}`);
    }
  }
  const cfg = path.join(dirOf(id), 'config');
  if (fs.existsSync(cfg)) zip.addLocalFolder(cfg, 'overrides/config');
  const index = {
    formatVersion: 1, game: 'minecraft', versionId: '1.0.0', name: inst.name,
    summary: inst.description || 'Made with Surge Client',
    files,
    dependencies: { minecraft: inst.mcVersion, 'fabric-loader': inst.loaderVersion },
  };
  zip.addFile('modrinth.index.json', Buffer.from(JSON.stringify(index, null, 2)));
  zip.writeZip(dest);
  return dest;
}

function worlds(id) {
  const dir = path.join(dirOf(id), 'saves');
  if (!fs.existsSync(dir)) return [];
  return fs.readdirSync(dir).filter(f => fs.existsSync(path.join(dir, f, 'level.dat'))).map(f => {
    const icon = path.join(dir, f, 'icon.png');
    return { folder: f, path: path.join(dir, f), lastPlayed: fs.statSync(path.join(dir, f, 'level.dat')).mtimeMs, icon: fs.existsSync(icon) ? icon : null };
  }).sort((a, b) => b.lastPlayed - a.lastPlayed);
}

function screenshots(id) {
  const dir = path.join(dirOf(id), 'screenshots');
  if (!fs.existsSync(dir)) return [];
  return fs.readdirSync(dir).filter(f => /\.png$/i.test(f)).map(f => ({ name: f, path: path.join(dir, f), time: fs.statSync(path.join(dir, f)).mtimeMs }))
    .sort((a, b) => b.time - a.time);
}

module.exports = { worlds, screenshots, CONTENT_DIRS, dirOf, list, get, create, update, remove, addContent, hasProject, listContent, toggleContent, removeContent, exportMrpack, slugify };
