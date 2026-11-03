const fs = require('fs');
const path = require('path');
const os = require('os');
const AdmZip = require('adm-zip');
const { getJson, download, pool } = require('./http');
const instances = require('./instances');
const config = require('./config');

const API = 'https://api.curseforge.com/v1';
const GAME_MINECRAFT = 432;
const CLASS = { mod: 6, modpack: 4471, resourcepack: 12, shader: 6552 };
const FABRIC = 4;
const SORT = { relevance: 1, downloads: 6, updated: 3, newest: 11, follows: 2 };

function headers() {
  const key = config.load().curseforgeKey;
  if (!key) throw new Error('CurseForge needs an API key. Add one in Settings → CurseForge (free at console.curseforge.com).');
  return { 'x-api-key': key };
}

function normalize(m, type) {
  return {
    source: 'curseforge',
    id: m.id,
    slug: m.slug,
    title: m.name,
    description: m.summary,
    author: m.authors?.[0]?.name || 'Unknown',
    downloads: m.downloadCount,
    iconUrl: m.logo?.thumbnailUrl || null,
    type,
    url: m.links?.websiteUrl,
    categories: (m.categories || []).slice(0, 3).map(c => c.name),
  };
}

async function search({ query = '', type = 'mod', mcVersion = '', offset = 0, sort = 'relevance' }) {
  const params = new URLSearchParams({
    gameId: String(GAME_MINECRAFT), classId: String(CLASS[type]), searchFilter: query,
    sortField: String(SORT[sort] || 2), sortOrder: 'desc', pageSize: '24', index: String(offset),
  });
  if (mcVersion) params.set('gameVersion', mcVersion);
  if (type === 'mod' || type === 'modpack') params.set('modLoaderType', String(FABRIC));
  const res = await getJson(`${API}/mods/search?${params}`, headers());
  return { hits: res.data.map(m => normalize(m, type)), total: res.pagination?.totalCount || res.data.length };
}

async function installProject(instanceId, modId, type = 'mod', onProgress = () => {}, seen = new Set()) {
  if (seen.has(modId)) return [];
  seen.add(modId);
  if (instances.hasProject(instanceId, 'curseforge', modId)) return [];
  const inst = instances.get(instanceId);
  const mod = (await getJson(`${API}/mods/${modId}`, headers())).data;
  const contentType = mod.classId === CLASS.resourcepack ? 'resourcepack' : mod.classId === CLASS.shader ? 'shader' : 'mod';
  const params = new URLSearchParams({ gameVersion: inst.mcVersion, pageSize: '20' });
  if (contentType === 'mod') params.set('modLoaderType', String(FABRIC));
  const files = (await getJson(`${API}/mods/${modId}/files?${params}`, headers())).data;
  const file = files.find(f => f.releaseType === 1) || files[0];
  if (!file) throw new Error(`${mod.name} has no file for Minecraft ${inst.mcVersion}`);
  if (!file.downloadUrl) throw new Error(`${mod.name}'s author blocks third-party downloads. Get it from ${mod.links?.websiteUrl}`);
  onProgress({ stage: `Downloading ${mod.name}` });
  const folder = instances.CONTENT_DIRS[contentType];
  const sha1 = file.hashes?.find(h => h.algo === 1)?.value;
  await download(file.downloadUrl, path.join(instances.dirOf(instanceId), folder, file.fileName), sha1);
  instances.addContent(instanceId, { type: contentType, file: file.fileName, source: 'curseforge', projectId: mod.id, versionId: file.id, title: mod.name, iconUrl: mod.logo?.thumbnailUrl });
  const installed = [mod.name];
  for (const dep of file.dependencies || []) {
    if (dep.relationType !== 3) continue;
    try { installed.push(...await installProject(instanceId, dep.modId, 'mod', onProgress, seen)); } catch (e) { onProgress({ stage: `Skipped dependency: ${e.message}` }); }
  }
  return installed;
}

async function installModpack(modId, onProgress = () => {}) {
  const mod = (await getJson(`${API}/mods/${modId}`, headers())).data;
  const files = (await getJson(`${API}/mods/${modId}/files?modLoaderType=${FABRIC}&pageSize=20`, headers())).data;
  const packFile = files.find(f => f.releaseType === 1) || files[0];
  if (!packFile?.downloadUrl) throw new Error(`${mod.name} can't be downloaded by third-party launchers`);
  onProgress({ stage: `Downloading ${mod.name}` });
  const tmp = path.join(os.tmpdir(), `surge-${Date.now()}.zip`);
  await download(packFile.downloadUrl, tmp);
  try {
    const zip = new AdmZip(tmp);
    const manifest = JSON.parse(zip.getEntry('manifest.json').getData().toString('utf8'));
    const loader = manifest.minecraft.modLoaders.find(l => l.primary) || manifest.minecraft.modLoaders[0];
    if (!loader?.id.startsWith('fabric-')) throw new Error(`This pack uses ${loader?.id || 'an unknown loader'}. Surge supports Fabric modpacks only.`);
    const inst = instances.create({ name: manifest.name || mod.name, mcVersion: manifest.minecraft.version, loaderVersion: loader.id.slice('fabric-'.length), icon: mod.logo?.thumbnailUrl, description: mod.summary });
    const dir = instances.dirOf(inst.id);
    const blocked = [];
    await pool(manifest.files, 6, async f => {
      try {
        const info = (await getJson(`${API}/mods/${f.projectID}/files/${f.fileID}`, headers())).data;
        if (!info.downloadUrl) { blocked.push(info.displayName || info.fileName); return; }
        const folder = info.fileName.endsWith('.zip') ? 'resourcepacks' : 'mods';
        await download(info.downloadUrl, path.join(dir, folder, info.fileName));
        instances.addContent(inst.id, { type: folder === 'mods' ? 'mod' : 'resourcepack', file: info.fileName, source: 'curseforge', projectId: f.projectID, versionId: f.fileID, title: info.displayName });
      } catch { blocked.push(`project ${f.projectID}`); }
    }, (current, total) => onProgress({ stage: `Downloading ${mod.name}`, current, total }));
    const prefix = `${manifest.overrides || 'overrides'}/`;
    for (const e of zip.getEntries()) {
      if (e.isDirectory || !e.entryName.startsWith(prefix)) continue;
      const dest = path.resolve(dir, e.entryName.slice(prefix.length));
      if (!dest.startsWith(path.resolve(dir))) continue;
      fs.mkdirSync(path.dirname(dest), { recursive: true });
      fs.writeFileSync(dest, e.getData());
    }
    return { instance: instances.get(inst.id), blocked };
  } finally {
    fs.rmSync(tmp, { force: true });
  }
}

module.exports = { search, installProject, installModpack };
