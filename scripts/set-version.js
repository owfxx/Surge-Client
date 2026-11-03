const fs = require('fs');
const path = require('path');
const ver = (process.argv[2] || '').replace(/^v/, '');
if (!/^\d+\.\d+\.\d+$/.test(ver)) { console.error('Usage: node scripts/set-version.js 1.0.1'); process.exit(1); }
const root = path.join(__dirname, '..');
const pkgFile = path.join(root, 'launcher', 'package.json');
const pkg = JSON.parse(fs.readFileSync(pkgFile, 'utf8'));
pkg.version = ver;
fs.writeFileSync(pkgFile, JSON.stringify(pkg, null, 2) + '\n');
const gp = path.join(root, 'mod', 'gradle.properties');
fs.writeFileSync(gp, fs.readFileSync(gp, 'utf8').replace(/^version=.*$/m, `version=${ver}+mc1.21.11`));
console.log(`Version set to ${ver}`);
