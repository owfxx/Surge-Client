const fs = require('fs');
const path = require('path');
const from = path.join(__dirname, '..', '..', 'mod', 'src', 'main', 'resources', 'assets', 'surge', 'textures', 'cosmetic');
const to = path.join(__dirname, '..', 'src', 'renderer', 'assets', 'cosmetics');
fs.mkdirSync(to, { recursive: true });
let n = 0;
for (const f of fs.readdirSync(from)) {
  if (!/^(cape_[a-z]+|wings_[a-z]+)\.png$/.test(f) || f.startsWith('cape_text_') || f.startsWith('cape_overlay')) continue;
  fs.copyFileSync(path.join(from, f), path.join(to, f));
  n++;
}
console.log(`copied ${n} cosmetic textures`);
