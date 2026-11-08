const fs = require('fs');
const os = require('os');
const paths = require('./paths');

const totalGb = Math.floor(os.totalmem() / 1024 ** 3);

const DEFAULTS = {
  ramMb: Math.min(Math.max(2048, Math.floor(totalGb / 4) * 1024), 6144),
  javaPath: '',
  jvmPreset: 'surge',
  extraJvmArgs: '',
  curseforgeKey: '',
  apiUrl: '',
  autoApiUrl: '',
  codesUrl: '',
  closeOnLaunch: false,
  minimizeOnLaunch: true,
  uiSounds: true,
  servers: [],
  discord: true,
  discordClientId: '',
  discordShowServer: true,
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
