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
for (const f of ['cape_runes.png']) {
  const src = path.join(__dirname, '..', '..', 'server', 'img', f);
  if (fs.existsSync(src)) { fs.copyFileSync(src, path.join(to, f)); n++; }
}
for (const f of ['cape_overlay_logo.png', 'cape_overlay_logo_center.png', 'cape_text_beta.png', 'cape_text_beta_tester.png', 'cape_text_early.png', 'cape_text_og.png', 'cape_text_surge.png']) {
  const src = path.join(from, f);
  if (fs.existsSync(src)) { fs.copyFileSync(src, path.join(to, f)); n++; }
}
console.log(`copied ${n} cosmetic textures`);
