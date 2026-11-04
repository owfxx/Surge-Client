const fs = require('fs');
const path = require('path');
const paths = require('./paths');
const { getJson } = require('./http');

const META = 'https://meta.fabricmc.net/v2';

async function gameVersions() {
  const list = await getJson(`${META}/versions/game`);
  return list.filter(v => v.stable && !/^26\.[1-3](\.|$)/.test(v.version)).map(v => v.version);
}

async function latestLoader(mcVersion) {
  const list = await getJson(`${META}/versions/loader/${encodeURIComponent(mcVersion)}`);
  if (!list.length) throw new Error(`Fabric doesn't support ${mcVersion} yet`);
  return (list.find(l => l.loader.stable) || list[0]).loader.version;
}

async function ensureProfile(mcVersion, loaderVersion) {
  const id = `fabric-loader-${loaderVersion}-${mcVersion}`;
  const dir = path.join(paths.versions, id);
  const file = path.join(dir, `${id}.json`);
  if (!fs.existsSync(file)) {
    const profile = await getJson(`${META}/versions/loader/${encodeURIComponent(mcVersion)}/${encodeURIComponent(loaderVersion)}/profile/json`);
    fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(file, JSON.stringify(profile, null, 2));
  }
  return id;
}

module.exports = { gameVersions, latestLoader, ensureProfile };
