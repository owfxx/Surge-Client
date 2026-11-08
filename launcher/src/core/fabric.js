const fs = require('fs');
const path = require('path');
const paths = require('./paths');
const { getJson } = require('./http');

const META = 'https://meta.fabricmc.net/v2';
const LEGACY_META = 'https://meta.legacyfabric.net/v2';
const LEGACY_VERSIONS = ['1.8.9'];

function isLegacy(mcVersion) {
  const [a, b] = String(mcVersion || '').split('.').map(Number);
  return a === 1 && b < 14;
}

const metaFor = v => (isLegacy(v) ? LEGACY_META : META);

async function gameVersions() {
  const list = await getJson(`${META}/versions/game`);
  return list.filter(v => v.stable).map(v => v.version).concat(LEGACY_VERSIONS);
}

async function latestLoader(mcVersion) {
  const list = await getJson(`${metaFor(mcVersion)}/versions/loader/${encodeURIComponent(mcVersion)}`);
  if (!list.length) throw new Error(`Fabric doesn't support ${mcVersion} yet`);
  return (list.find(l => l.loader.stable) || list[0]).loader.version;
}

async function ensureProfile(mcVersion, loaderVersion) {
  const id = `fabric-loader-${loaderVersion}-${mcVersion}`;
  const dir = path.join(paths.versions, id);
  const file = path.join(dir, `${id}.json`);
  if (!fs.existsSync(file)) {
    const profile = await getJson(`${metaFor(mcVersion)}/versions/loader/${encodeURIComponent(mcVersion)}/${encodeURIComponent(loaderVersion)}/profile/json`);
    fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(file, JSON.stringify(profile, null, 2));
  }
  return id;
}

module.exports = { gameVersions, latestLoader, ensureProfile, isLegacy, LEGACY_VERSIONS };
