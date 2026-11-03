const fs = require('fs');
const path = require('path');
const instances = require('./instances');
const { postJson, download, hashFile } = require('./http');

const API = 'https://api.modrinth.com/v2';

async function check(instanceId) {
  const inst = instances.get(instanceId);
  const dir = path.join(instances.dirOf(instanceId), 'mods');
  if (!fs.existsSync(dir)) return [];
  const files = fs.readdirSync(dir).filter(f => f.endsWith('.jar') && !/^surge-client-/.test(f));
  const byHash = new Map(files.map(f => [hashFile(path.join(dir, f)), f]));
  if (!byHash.size) return [];
  const latest = await postJson(`${API}/version_files/update`, {
    hashes: [...byHash.keys()], algorithm: 'sha1', loaders: ['fabric'], game_versions: [inst.mcVersion],
  });
  const updates = [];
  for (const [hash, version] of Object.entries(latest)) {
    const primary = version.files.find(f => f.primary) || version.files[0];
    if (!primary || primary.hashes.sha1 === hash) continue;
    const file = byHash.get(hash);
    const meta = inst.content.find(c => c.file === file);
    updates.push({ file, title: meta?.title || file.replace(/\.jar$/, ''), projectId: version.project_id, version: version.version_number, newFile: primary.filename, url: primary.url, sha1: primary.hashes.sha1 });
  }
  return updates.sort((a, b) => a.title.localeCompare(b.title));
}

async function apply(instanceId, updates, onProgress = () => {}) {
  const dir = path.join(instances.dirOf(instanceId), 'mods');
  let done = 0;
  for (const u of updates) {
    onProgress({ stage: `Updating ${u.title}`, current: done, total: updates.length });
    await download(u.url, path.join(dir, u.newFile), u.sha1);
    if (u.newFile !== u.file) fs.rmSync(path.join(dir, u.file), { force: true });
    const inst = instances.get(instanceId);
    const old = inst.content.find(c => c.file === u.file);
    instances.update(instanceId, { content: inst.content.filter(c => c.file !== u.file) });
    instances.addContent(instanceId, { ...(old || {}), type: 'mod', file: u.newFile, source: 'modrinth', projectId: u.projectId, title: u.title, url: u.url });
    done++;
  }
  return done;
}

module.exports = { check, apply };
