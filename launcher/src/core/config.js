const fs = require('fs');
const os = require('os');
const paths = require('./paths');

const totalGb = Math.floor(os.totalmem() / 1024 ** 3);

const DEFAULTS = {
  ramMb: Math.min(Math.max(2048, Math.floor(totalGb / 4) * 1024), 6144),
  javaPath: '',            // empty = let Surge download the right Java automatically
  jvmPreset: 'surge',      // 'surge' (tuned G1GC) | 'vanilla'
  extraJvmArgs: '',
  curseforgeKey: '',
  apiUrl: '',              // Surge online service (cosmetics/emotes/badges for everyone), see /server
  codesUrl: '',            // public codes.json (e.g. raw GitHub URL)
  closeOnLaunch: false,
  uiSounds: true,
  servers: [],             // [{ name, address }]
  discord: true,           // show what you're playing on Discord
  discordClientId: '',     // your Discord application ID (Rich Presence)
  discordShowServer: true, // include the server address
  selectedInstance: 'surge-default',
  width: 1280,
  height: 720,
};

function load() {
  try {
    return { ...DEFAULTS, ...JSON.parse(fs.readFileSync(paths.config, 'utf8')) };
  } catch {
    return { ...DEFAULTS };
  }
}

function save(patch) {
  const next = { ...load(), ...patch };
  fs.writeFileSync(paths.config, JSON.stringify(next, null, 2));
  return next;
}

module.exports = { load, save, totalGb };
