const fs = require('fs');
const path = require('path');
const { Client } = require('minecraft-launcher-core');
const paths = require('./paths');
const config = require('./config');
const instances = require('./instances');
const fabric = require('./fabric');
const java = require('./java');
const auth = require('./auth');
const modrinth = require('./modrinth');

const release = require('./release');
const social = require('./social');

const SURGE_ZGC = [
  '-XX:+UseZGC', '-XX:+ZGenerational', '-XX:+AlwaysPreTouch', '-XX:+DisableExplicitGC',
  '-XX:+PerfDisableSharedMem', '-XX:+UseStringDeduplication', '-Dsurge.client=true',
];

const SURGE_JVM = [
  '-XX:+UnlockExperimentalVMOptions', '-XX:+UseG1GC', '-XX:G1NewSizePercent=20',
  '-XX:G1ReservePercent=20', '-XX:MaxGCPauseMillis=50', '-XX:G1HeapRegionSize=32M',
  '-XX:+ParallelRefProcEnabled', '-XX:+DisableExplicitGC', '-XX:+PerfDisableSharedMem',
  '-XX:G1MixedGCCountTarget=4', '-XX:InitiatingHeapOccupancyPercent=15',
  '-XX:G1RSetUpdatingPauseTimePercent=5', '-XX:SurvivorRatio=32', '-XX:MaxTenuringThreshold=1',
  '-Dsurge.client=true',
];

let running = null;

function bundledModsDir(mcVersion) {
  const base = process.resourcesPath && fs.existsSync(path.join(process.resourcesPath, 'mods'))
    ? path.join(process.resourcesPath, 'mods')
    : path.join(__dirname, '..', '..', 'resources', 'mods');
  return path.join(base, mcVersion);
}

function surgeModAvailable(mcVersion) {
  const dir = bundledModsDir(mcVersion);
  return fs.existsSync(dir) && fs.readdirSync(dir).some(f => f.endsWith('.jar'));
}

function applyPendingModToggles(inst) {
  const file = path.join(instances.dirOf(inst.id), 'surge-mods.json');
  if (!fs.existsSync(file)) return;
  try {
    const wanted = JSON.parse(fs.readFileSync(file, 'utf8'));
    const modsDir = path.join(instances.dirOf(inst.id), 'mods');
    for (const [name, on] of Object.entries(wanted)) {
      if (name.includes('/') || name.includes('\\')) continue;
      const jar = path.join(modsDir, name);
      const off = `${jar}.disabled`;
      if (on && fs.existsSync(off)) fs.renameSync(off, jar);
      if (!on && fs.existsSync(jar)) fs.renameSync(jar, off);
    }
  } catch { }
  fs.rmSync(file, { force: true });
}

function bestSurgeJar(mcVersion) {
  const cands = [];
  const dir = bundledModsDir(mcVersion);
  if (fs.existsSync(dir)) for (const f of fs.readdirSync(dir)) if (/^surge-client-.+\.jar$/.test(f)) cands.push(path.join(dir, f));
  const upd = release.downloadedMod(mcVersion);
  if (upd) cands.push(upd);
  cands.sort((a, b) => (release.newer(release.jarVersion(path.basename(a)), release.jarVersion(path.basename(b))) ? -1 : 1));
  return cands[0] || null;
}

async function syncSurgeMod(inst, onProgress) {
  const modsDir = path.join(instances.dirOf(inst.id), 'mods');
  fs.mkdirSync(modsDir, { recursive: true });
  for (const f of fs.readdirSync(modsDir)) if (/^surge-client-.*\.jar(\.disabled)?$/.test(f)) fs.rmSync(path.join(modsDir, f), { force: true });
  if (inst.surgeMod === false) return null;
  try {
    const have = bestSurgeJar(inst.mcVersion);
    await Promise.race([
      release.updateMod(inst.mcVersion, have ? release.jarVersion(path.basename(have)) : '0', onProgress),
      new Promise(r => setTimeout(r, 8000)),
    ]);
  } catch { }
  const jar = bestSurgeJar(inst.mcVersion);
  if (!jar) return null;
  const hidden = path.join(paths.root, 'core', inst.mcVersion);
  fs.mkdirSync(hidden, { recursive: true });
  const dest = path.join(hidden, 'surge-core.jar');
  fs.copyFileSync(jar, dest);
  try { await modrinth.installProject(inst.id, 'fabric-api', 'mod', onProgress, new Set(), { optional: true, locked: true }); } catch { }
  return dest;
}

const hide = m => String(m)
  .replace(/(--accessToken\s+)\S+/g, '$1[hidden]')
  .replace(/(--xuid\s+)\S+/g, '$1[hidden]')
  .replace(/(-Dsurge\.token=)\S+/g, '$1[hidden]')
  .replace(/eyJ[\w-]{10,}\.[\w-]{10,}\.[\w-]{10,}/g, '[hidden]');

async function launch(instanceId, { onProgress, onLog: rawLog, onExit, server, world }) {
  const onLog = m => rawLog(hide(m));
  if (running) throw new Error('Minecraft is already running');
  const cfg = config.load();
  const inst = instances.get(instanceId);
  if (!inst) throw new Error('Modpack not found');

  onProgress({ stage: 'Signing in…' });
  const authorization = await auth.getLaunchAuth();

  onProgress({ stage: 'Preparing Fabric…' });
  const customId = await fabric.ensureProfile(inst.mcVersion, inst.loaderVersion);

  const javaPath = cfg.javaPath || await java.ensureJava(inst.mcVersion, onProgress);

  onProgress({ stage: 'Syncing Surge mod…' });
  applyPendingModToggles(inst);
  if (inst.loader === 'fabric' && inst.perfChecked !== 3 && inst.autoPerf !== false) {
    onProgress({ stage: 'Adding performance mods…' });
    try { await modrinth.installPerformanceStack(inst.id, onProgress); } catch { }
    instances.update(inst.id, { perfChecked: 3 });
  }
  if (inst.loader === 'fabric' && !inst.voiceChecked) {
    onProgress({ stage: 'Adding Simple Voice Chat…' });
    try {
      await modrinth.installProject(inst.id, 'simple-voice-chat', 'mod', onProgress, new Set(), { optional: true });
      instances.update(inst.id, { voiceChecked: true });
    } catch { }
  }
  const surgeJar = await syncSurgeMod(inst, onProgress);
  await social.refreshService();
  let surgeToken = '';
  try {
    const sess = await Promise.race([social.signIn(authorization), new Promise(r => setTimeout(() => r(null), 8000))]);
    if (sess) surgeToken = sess.token;
  } catch (e) { onLog(`[Surge] Online features off: ${e.message}`); }

  const ram = inst.ramMb || cfg.ramMb;
  const useZgc = ram >= 6144;
  const jvm = (cfg.jvmPreset === 'surge' ? (useZgc ? SURGE_ZGC : SURGE_JVM) : [])
    .concat([`-Dsurge.home=${paths.root}`, `-Dsurge.codesUrl=${cfg.codesUrl || release.codesUrl()}`, `-Dsurge.apiUrl=${social.apiUrl()}`, `-Dsurge.token=${surgeToken}`, `-Dsurge.repo=${release.repo()}`])
    .concat(surgeJar ? [`-Dfabric.addMods=${surgeJar}`] : [])
    .concat(cfg.extraJvmArgs.split(/\s+/).filter(Boolean));

  const client = new Client();
  running = client;
  client.on('progress', e => onProgress({ stage: `Downloading ${e.type}`, current: e.task, total: e.total }));
  client.on('debug', m => onLog(m));
  client.on('data', m => onLog(m));
  let started = false;
  client.on('data', () => { if (!started) { started = true; onProgress({ stage: 'running' }); } });
  client.on('close', code => { running = null; onExit(code); });

  instances.update(inst.id, { lastPlayed: Date.now() });
  config.save({ selectedInstance: inst.id });

  const proc = await client.launch({
    authorization,
    root: paths.root,
    version: { number: inst.mcVersion, type: 'release', custom: customId },
    memory: { max: ram, min: cfg.jvmPreset === 'surge' && useZgc ? ram : Math.min(1024, ram) },
    javaPath,
    customArgs: jvm,
    window: { width: cfg.width, height: cfg.height },
    overrides: { gameDirectory: instances.dirOf(inst.id), maxSockets: 16, detached: false },
    ...(server ? { quickPlay: { type: 'multiplayer', identifier: server } } : world ? { quickPlay: { type: 'singleplayer', identifier: world } } : {}),
  });
  if (!proc) { running = null; throw new Error('Minecraft failed to start — check the log.'); }
  return true;
}

function isRunning() { return !!running; }

module.exports = { launch, isRunning, surgeModAvailable };
