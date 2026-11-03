// Turns a crash into a short explanation and a fix.
const fs = require('fs');
const path = require('path');

function newestCrashReport(gameDir, sinceMs) {
  const dir = path.join(gameDir, 'crash-reports');
  if (!fs.existsSync(dir)) return null;
  const files = fs.readdirSync(dir).filter(f => f.endsWith('.txt'))
    .map(f => ({ f: path.join(dir, f), t: fs.statSync(path.join(dir, f)).mtimeMs }))
    .filter(x => x.t >= sinceMs).sort((a, b) => b.t - a.t);
  return files[0]?.f || null;
}

function analyze(gameDir, logLines, startedAt) {
  const report = newestCrashReport(gameDir, startedAt - 5000);
  const text = `${logLines.join('\n')}\n${report ? fs.readFileSync(report, 'utf8') : ''}`;
  const find = re => text.match(re);
  let title = 'Minecraft closed unexpectedly';
  let fix = 'Check the log for details, or try disabling recently added mods.';
  let details = '';

  let m;
  if ((m = find(/Incompatible mods? found!|Mod resolution failed/))) {
    title = 'Some mods don\'t work together';
    details = text.slice(m.index).split('\n').filter(l => /^\s*-\s/.test(l)).slice(0, 8).map(l => l.trim()).join('\n');
    fix = 'Install the missing mods or remove the ones listed below, then try again.';
  } else if (find(/java\.lang\.OutOfMemoryError/)) {
    title = 'Minecraft ran out of memory';
    fix = 'Give it more memory in Settings → Memory (4–6 GB works for most modpacks).';
  } else if (find(/Could not reserve enough space|Invalid maximum heap size|Could not create the Java Virtual Machine/)) {
    title = 'Too much memory is set';
    fix = 'Lower the memory in Settings. Your PC needs some left over for Windows.';
  } else if (find(/GLFW error 65542|driver does not appear to support OpenGL|Pixel format not accelerated|WGL_ARB_create_context/)) {
    title = 'Your graphics driver can\'t run Minecraft';
    fix = 'Update your graphics drivers from NVIDIA, AMD or Intel, then try again.';
  } else if (find(/UnsupportedClassVersionError|requires.*Java \d+/i)) {
    title = 'Wrong Java version';
    fix = 'In Settings → Java, choose "Use automatic" so Surge picks the right Java.';
  } else if ((m = find(/(?:Mixin apply (?:for mod|failed)|from mod) ['"]?([a-z0-9_-]+)/i))) {
    title = `The mod "${m[1]}" failed to load`;
    fix = 'Update or disable that mod in the modpack, then try again.';
  } else if ((m = find(/Caused by: ([^\n]+)/))) {
    details = m[1].trim();
  }
  if (!details && report) {
    const d = text.match(/Description: ([^\n]+)/);
    if (d) details = d[1].trim();
  }
  return { title, fix, details, report };
}

module.exports = { analyze };
