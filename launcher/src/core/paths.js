const path = require('path');
const os = require('os');
const fs = require('fs');

const base = process.env.APPDATA
  || (process.platform === 'darwin'
    ? path.join(os.homedir(), 'Library', 'Application Support')
    : path.join(os.homedir(), '.local', 'share'));

const ROOT = path.join(base, '.surgeclient');

const paths = {
  root: ROOT,
  instances: path.join(ROOT, 'instances'),
  runtimes: path.join(ROOT, 'runtimes'),
  versions: path.join(ROOT, 'versions'),
  cache: path.join(ROOT, 'cache'),
  config: path.join(ROOT, 'launcher.json'),
  accounts: path.join(ROOT, 'accounts.json'),
};

for (const p of [paths.root, paths.instances, paths.runtimes, paths.versions, paths.cache]) {
  fs.mkdirSync(p, { recursive: true });
}

module.exports = paths;
