const fs = require('fs');
const path = require('path');
const os = require('os');
const AdmZip = require('adm-zip');
const { getJson, download, pool } = require('./http');
const instances = require('./instances');

const API = 'https://api.modrinth.com/v2';

function normalize(hit) {
  return {
    source: 'modrinth',
    id: hit.project_id,
    slug: hit.slug,
    title: hit.title,
    description: hit.description,
    author: hit.author,
    downloads: hit.downloads,
    follows: hit.follows,
    iconUrl: hit.icon_url || null,
    type: hit.project_type,
    url: `https://modrinth.com/${hit.project_type}/${hit.slug}`,
    categories: hit.display_categories || hit.categories || [],
  };
}

async function search({ query = '', type = 'mod', mcVersion = '', offset = 0, sort = 'relevance' }) {
  const facets = [[`project_type:${type}`]];
  if (mcVersion) facets.push([`versions:${mcVersion}`]);
  if (type === 'mod' || type === 'modpack') facets.push(['categories:fabric']);
  const params = new URLSearchParams({ query, facets: JSON.stringify(facets), index: sort, offset: String(offset), limit: '24' });
  const res = await getJson(`${API}/search?${params}`);
  return { hits: res.hits.map(normalize), total: res.total_hits };
}

function loadersFor(type) {
  if (type === 'resourcepack') return ['minecraft'];
  if (type === 'shader') return ['iris', 'optifine'];
  return ['fabric'];
}

async function bestVersion(projectId, mcVersion, type) {
  const params = new URLSearchParams({ loaders: JSON.stringify(loadersFor(type)), game_versions: JSON.stringify([mcVersion]) });
  const versions = await getJson(`${API}/project/${encodeURIComponent(projectId)}/version?${params}`);
  return versions.find(v => v.version_type === 'release') || versions[0] || null;
}

async function installProject(instanceId, projectId, type = 'mod', onProgress = () => {}, seen = new Set(), opts = {}) {
  if (seen.has(projectId)) return [];
  seen.add(projectId);
  const inst = instances.get(instanceId);
  if (instances.hasProject(instanceId, 'modrinth', projectId)) return [];
  const project = await getJson(`${API}/project/${encodeURIComponent(projectId)}`);
  const contentType = project.project_type === 'resourcepack' ? 'resourcepack' : project.project_type === 'shader' ? 'shader' : 'mod';
  const version = await bestVersion(project.id, inst.mcVersion, contentType);
  if (!version) {
    if (opts.optional) return [];
    throw new Error(`${project.title} has no ${contentType === 'mod' ? 'Fabric ' : ''}version for Minecraft ${inst.mcVersion}`);
  }
  const file = version.files.find(f => f.primary) || version.files[0];
  onProgress({ stage: `Downloading ${project.title}` });
  const folder = instances.CONTENT_DIRS[contentType];
  await download(file.url, path.join(instances.dirOf(instanceId), folder, file.filename), file.hashes.sha1);
  instances.addContent(instanceId, {
    type: contentType, file: file.filename, source: 'modrinth', projectId: project.id, versionId: version.id,
    title: project.title, iconUrl: project.icon_url, url: file.url, locked: !!opts.locked,
  });
  const installed = [project.title];
  for (const dep of version.dependencies || []) {
    if (dep.dependency_type !== 'required') continue;
    const depId = dep.project_id || (dep.version_id && (await getJson(`${API}/version/${dep.version_id}`)).project_id);
    if (depId) installed.push(...await installProject(instanceId, depId, 'mod', onProgress, seen, { optional: true }));
  }
  return installed;
}

async function installMrpackFile(file, { iconUrl = null, onProgress = () => {} } = {}) {
  const zip = new AdmZip(file);
  const indexEntry = zip.getEntry('modrinth.index.json');
  if (!indexEntry) throw new Error('Not a valid .mrpack (modrinth.index.json missing)');
  const index = JSON.parse(indexEntry.getData().toString('utf8'));
  const deps = index.dependencies || {};
  if (!deps['fabric-loader']) {
    const loader = Object.keys(deps).find(k => k !== 'minecraft') || 'vanilla';
    throw new Error(`This pack uses ${loader}. Surge currently supports Fabric modpacks only.`);
  }
  const inst = instances.create({ name: index.name || 'Imported pack', mcVersion: deps.minecraft, loaderVersion: deps['fabric-loader'], icon: iconUrl, description: index.summary || '' });
  const dir = instances.dirOf(inst.id);
  const files = (index.files || []).filter(f => f.env?.client !== 'unsupported');
  await pool(files, 8, async f => {
    const dest = path.resolve(dir, f.path);
    if (!dest.startsWith(path.resolve(dir))) return;
    await download(f.downloads[0], dest, f.hashes?.sha1);
    const [folder, name] = f.path.split('/');
    const type = Object.entries(instances.CONTENT_DIRS).find(([, v]) => v === folder)?.[0];
    if (type && name) instances.addContent(inst.id, { type, file: name, source: 'modrinth', url: f.downloads[0], title: name.replace(/\.(jar|zip)$/, '') });
  }, (current, total) => onProgress({ stage: `Downloading ${index.name}`, current, total }));
  for (const prefix of ['overrides/', 'client-overrides/']) {
    for (const e of zip.getEntries()) {
      if (e.isDirectory || !e.entryName.startsWith(prefix)) continue;
      const dest = path.resolve(dir, e.entryName.slice(prefix.length));
      if (!dest.startsWith(path.resolve(dir))) continue;
      fs.mkdirSync(path.dirname(dest), { recursive: true });
      fs.writeFileSync(dest, e.getData());
    }
  }
  return instances.get(inst.id);
}

async function installModpack(projectId, onProgress = () => {}, mcVersion = '') {
  const project = await getJson(`${API}/project/${encodeURIComponent(projectId)}`);
  const params = new URLSearchParams({ loaders: JSON.stringify(['fabric']) });
  if (mcVersion) params.set('game_versions', JSON.stringify([mcVersion]));
  const versions = await getJson(`${API}/project/${project.id}/version?${params}`);
  const version = versions.find(v => v.version_type === 'release') || versions[0];
  if (!version) throw new Error(`${project.title} has no Fabric version${mcVersion ? ` for ${mcVersion}` : ''}`);
  const file = version.files.find(f => f.primary) || version.files[0];
  onProgress({ stage: `Downloading ${project.title}` });
  const tmp = path.join(os.tmpdir(), `surge-${Date.now()}.mrpack`);
  await download(file.url, tmp, file.hashes.sha1);
  try {
    return await installMrpackFile(tmp, { iconUrl: project.icon_url, onProgress });
  } finally {
    fs.rmSync(tmp, { force: true });
  }
}

const PERFORMANCE_MODS = [
  ['fabric-api'],
  ['sodium'],
  ['lithium'],
  ['ferrite-core'],
  ['entityculling'],
  ['immediatelyfast'],
  ['moreculling'],
  ['badoptimizations'],
  ['dynamic-fps'],
  ['particle-core'],
  ['better-block-entities'],
  ['scalablelux'],
  ['krypton'],
  ['modernfix', 'modernfix-mvus'],
  ['memoryleakfix'],
];

async function installPerformanceStack(instanceId, onProgress = () => {}) {
  const seen = new Set();
  const installed = [];
  for (const alternatives of PERFORMANCE_MODS) {
    for (const slug of alternatives) {
      try {
        const got = await installProject(instanceId, slug, 'mod', onProgress, seen, { optional: true, locked: true });
        if (got.length) { installed.push(...got); break; }
      } catch (e) {
        onProgress({ stage: `Skipped ${slug}: ${e.message}` });
      }
    }
  }
  return installed;
}

module.exports = { search, installProject, installModpack, installMrpackFile, installPerformanceStack, PERFORMANCE_MODS };
