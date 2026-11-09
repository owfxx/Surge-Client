const { app, BrowserWindow, ipcMain, shell, dialog, clipboard, nativeImage } = require('electron');
app.commandLine.appendSwitch('disable-features', 'HardwareMediaKeyHandling,MediaSessionService,SpareRendererForSitePerProcess');
app.commandLine.appendSwitch('js-flags', '--max-old-space-size=384');
const path = require('path');
const fs = require('fs');

const paths = require('./core/paths');
const config = require('./core/config');
const auth = require('./core/auth');
const instances = require('./core/instances');
const fabric = require('./core/fabric');
const modrinth = require('./core/modrinth');
const curseforge = require('./core/curseforge');
const launcher = require('./core/launch');
const admin = require('./core/admin');
const release = require('./core/release');
const servers = require('./core/servers');
const skins = require('./core/skins');
const wardrobe = require('./core/wardrobe');
const updates = require('./core/updates');
const crash_ = require('./core/crash');
const discord = require('./core/discord');

const DEFAULT_MC = '1.21.11';
let win;

app.commandLine.appendSwitch('autoplay-policy', 'no-user-gesture-required');
if (!app.requestSingleInstanceLock()) app.quit();
const alive = () => win && !win.isDestroyed();
app.on('second-instance', () => {
  if (!alive()) { if (app.isReady()) createWindow(); return; }
  if (win.isMinimized()) win.restore();
  if (!win.isVisible()) win.show();
  win.focus();
});
process.on('uncaughtException', e => { try { console.error(e); } catch { } });

function createWindow() {
  win = new BrowserWindow({
    width: 1180, height: 740, minWidth: 980, minHeight: 620,
    frame: false, backgroundColor: '#0d0816', show: false,
    icon: path.join(__dirname, 'renderer', 'assets', 'icon.png'),
    title: 'Surge Client',
    webPreferences: { preload: path.join(__dirname, 'preload.js'), contextIsolation: true, nodeIntegration: false, sandbox: true, backgroundThrottling: true, spellcheck: false },
  });
  win.loadFile(path.join(__dirname, 'renderer', 'index.html'));
  win.once('ready-to-show', () => win.show());
  win.webContents.setWindowOpenHandler(({ url }) => { if (/^https?:\/\//.test(url)) shell.openExternal(url); return { action: 'deny' }; });
}

const send = (ch, data) => { if (win && !win.isDestroyed()) win.webContents.send(ch, data); };
const task = label => p => send('task:progress', { label, ...p });

function handle(channel, fn) {
  ipcMain.handle(channel, async (_e, ...args) => {
    try { return { ok: true, data: await fn(...args) }; } catch (err) {
      console.error(channel, err);
      return { ok: false, error: err?.message || String(err) };
    }
  });
}

ipcMain.on('win:min', () => { if (alive()) win.minimize(); });
ipcMain.on('win:max', () => { if (alive()) (win.isMaximized() ? win.unmaximize() : win.maximize()); });
ipcMain.on('win:close', () => { if (alive()) win.close(); });

handle('config:get', () => ({ ...config.load(), totalGb: config.totalGb, dataDir: paths.root }));
handle('config:set', patch => {
  const next = config.save(patch);
  if ('discord' in patch || 'discordClientId' in patch) applyDiscordSettings();
  return next;
});
handle('shell:open', url => { if (/^https?:\/\//.test(url)) return shell.openExternal(url); });
handle('shell:dataDir', () => shell.openPath(paths.root));
handle('dialog:java', async () => {
  const r = await dialog.showOpenDialog(win, { title: 'Select java / javaw executable', properties: ['openFile'], filters: process.platform === 'win32' ? [{ name: 'Java', extensions: ['exe'] }] : [] });
  return r.canceled ? null : r.filePaths[0];
});

handle('auth:list', () => auth.publicAccounts());
handle('auth:login', () => auth.login());
handle('auth:logout', uuid => auth.logout(uuid));
handle('auth:active', uuid => auth.setActive(uuid));

handle('fabric:versions', () => fabric.gameVersions());
handle('instances:list', () => instances.list().map(i => ({ ...i, surgeModAvailable: launcher.surgeModAvailable(i.mcVersion) })));
handle('instances:content', id => instances.listContent(id));
handle('instances:toggle', (id, type, file) => instances.toggleContent(id, type, file));
handle('instances:removeContent', (id, type, file) => instances.removeContent(id, type, file));
handle('instances:update', (id, patch) => instances.update(id, patch));
handle('instances:remove', id => instances.remove(id));
handle('instances:folder', id => shell.openPath(instances.dirOf(id)));

handle('app:changelog', () => { try { return require('fs').readFileSync(path.join(__dirname, 'renderer', 'assets', 'changelog.md'), 'utf8'); } catch { return ''; } });
handle('surge:versions', () => launcher.surgeVersions());

handle('instances:create', async ({ name, mcVersion, optimized }) => {
  const loaderVersion = await fabric.latestLoader(mcVersion);
  const inst = instances.create({ name, mcVersion, loaderVersion, surgeOptimized: !!optimized });
  if (optimized && !fabric.isLegacy(mcVersion)) {
    const progress = task(name);
    modrinth.installPerformanceStack(inst.id, progress)
      .then(list => progress({ done: true, message: `${name} ready` }))
      .catch(e => progress({ done: true, error: e.message }));
  }
  return inst;
});

handle('instances:optimize', async id => {
  const inst = instances.get(id);
  const progress = task(`Optimizing ${inst.name}`);
  modrinth.installPerformanceStack(id, progress)
    .then(list => progress({ done: true, message: list.length ? `Added ${list.length} performance mods to ${inst.name}` : `${inst.name} already has every performance mod` }))
    .catch(e => progress({ done: true, error: e.message }));
  return true;
});

handle('instances:ensureDefault', async () => {
  if (instances.list().length) return null;
  const mc = DEFAULT_MC;
  const loaderVersion = await fabric.latestLoader(mc);
  const inst = instances.create({ id: 'surge-default', name: 'Surge', mcVersion: mc, loaderVersion, surgeOptimized: true, description: '' });
  const progress = task('Surge');
  modrinth.installPerformanceStack(inst.id, progress)
    .then(list => progress({ done: true, message: 'Ready' }))
    .catch(e => progress({ done: true, error: e.message }));
  return inst;
});

handle('instances:export', async id => {
  const inst = instances.get(id);
  const r = await dialog.showSaveDialog(win, { title: 'Export modpack', defaultPath: `${inst.name}.mrpack`, filters: [{ name: 'Modrinth modpack', extensions: ['mrpack'] }] });
  if (r.canceled) return null;
  return instances.exportMrpack(id, r.filePath);
});

handle('instances:import', async () => {
  const r = await dialog.showOpenDialog(win, { title: 'Import modpack', properties: ['openFile'], filters: [{ name: 'Modpacks', extensions: ['mrpack'] }] });
  if (r.canceled) return null;
  return modrinth.installMrpackFile(r.filePaths[0], { onProgress: task('Import') });
});

handle('instances:addFiles', async (id, type) => {
  const r = await dialog.showOpenDialog(win, { title: 'Add files', properties: ['openFile', 'multiSelections'], filters: [{ name: type === 'mod' ? 'Mods' : 'Packs', extensions: type === 'mod' ? ['jar'] : ['zip'] }] });
  if (r.canceled) return 0;
  const dir = path.join(instances.dirOf(id), instances.CONTENT_DIRS[type]);
  for (const f of r.filePaths) fs.copyFileSync(f, path.join(dir, path.basename(f)));
  return r.filePaths.length;
});

const sources = { modrinth, curseforge };
handle('browse:search', ({ source, ...q }) => sources[source].search(q));
handle('browse:install', async ({ source, id, type, instanceId }) => {
  const label = task('Installing');
  const list = await sources[source].installProject(instanceId, id, type, label);
  label({ done: true, message: list.length ? `Installed ${list[0]}` : 'Already installed' });
  return list;
});
handle('browse:installPack', async ({ source, id, title }) => {
  const progress = task(title);
  const result = await sources[source].installModpack(id, progress);
  const inst = result.instance || result;
  progress({ done: true, message: `${inst.name} ready${result.blocked?.length ? ` · ${result.blocked.length} files skipped` : ''}` });
  return inst;
});

handle('social:get', () => require('./core/social').social().catch(() => null));
handle('social:messages', since => require('./core/social').messages(since));
handle('social:send', (uuid, text) => require('./core/social').sendMessage(uuid, text));
handle('admin:status', () => admin.status());
handle('admin:setup', pw => admin.setup(pw));
handle('admin:unlock', pw => admin.unlock(pw));
handle('admin:list', () => admin.list());
handle('admin:create', o => admin.create(o));
handle('admin:remove', c => admin.remove(c));
handle('admin:lock', () => admin.lock());
handle('admin:sales', () => admin.salesList());
handle('admin:addSale', o => admin.addSale(o));
handle('admin:removeSale', i => admin.removeSale(i));
handle('admin:release', () => ({ repo: release.repo(), hasToken: admin.hasToken(), codesUrl: config.load().codesUrl || release.codesUrl() }));
handle('admin:setRepo', r => { config.save({ updateRepo: String(r || '').trim() }); return release.repo(); });
handle('admin:setToken', t => admin.setToken(String(t || '').trim()));
handle('admin:publishFile', async notes => {
  admin.list();
  const r = await dialog.showOpenDialog(win, { title: 'Pick the mod .jar or launcher .exe to publish', properties: ['openFile'], filters: [{ name: 'Surge builds', extensions: ['jar', 'exe'] }] });
  if (r.canceled || !r.filePaths[0]) return null;
  return release.publishFile({ token: admin.getToken(), file: r.filePaths[0], notes });
});
handle('admin:locks', () => admin.locksList());
handle('admin:toggleLock', id => admin.toggleLock(id));
handle('admin:discover', () => admin.discoverList());
handle('admin:addDiscover', o => admin.addDiscover(o));
handle('admin:removeDiscover', i => admin.removeDiscover(i));
handle('admin:moveDiscover', ([i, d]) => admin.moveDiscover(i, d));
handle('admin:publishDiscover', async () => release.publishDiscover({ token: admin.getToken(), servers: admin.discoverList() }));
handle('admin:publishCodes', async () => release.publishCodes({ token: admin.getToken(), json: admin.publicJson() }));
handle('admin:export', async () => {
  const r = await dialog.showSaveDialog(win, { defaultPath: 'codes.json', filters: [{ name: 'JSON', extensions: ['json'] }] });
  return r.canceled ? null : admin.exportPublic(r.filePath);
});

handle('servers:ping', address => servers.ping(address));

let presenceTimer = null;
function presenceIdle() {
  clearInterval(presenceTimer);
  discord.set({ details: 'In the launcher' });
}
function presencePlaying() {
  clearInterval(presenceTimer);
  discord.stop();
}
function applyDiscordSettings() {
  const cfg = config.load();
  const id = cfg.discordClientId || discord.DEFAULT_ID;
  if (cfg.discord !== false && id) { discord.start(id); presenceIdle(); } else discord.stop();
}

handle('launch:start', (id, target = {}) => {
  const logTail = [];
  const startedAt = Date.now();
  presencePlaying();
  return launcher.launch(id, {
    server: target.server,
    world: target.world,
    onProgress: p => send('launch:progress', p),
    onLog: line => {
      logTail.push(...String(line).split('\n'));
      if (logTail.length > 400) logTail.splice(0, logTail.length - 400);
      send('launch:log', line);
    },
    onExit: code => {
      const crash = code && code !== 130 ? crash_.analyze(instances.dirOf(id), logTail, startedAt) : null;
      send('launch:exit', { code, crash });
      applyDiscordSettings();
      if (!alive()) { app.quit(); return; }
      if (!win.isVisible()) win.show();
      else if (win.isMinimized()) win.restore();
    },
  }).then(r => { const c = config.load(); if (alive()) { if (c.closeOnLaunch) win.hide(); else if (c.minimizeOnLaunch !== false) win.minimize(); } return r; });
});
handle('shell:openFile', file => shell.openPath(file));

const inside = (id, p) => path.resolve(p).startsWith(path.resolve(instances.dirOf(id)) + path.sep);
handle('instances:worlds', id => instances.worlds(id));
handle('instances:screenshots', id => instances.screenshots(id));
handle('files:trash', (id, p) => { if (inside(id, p)) return shell.trashItem(p); });
handle('files:show', (id, p) => { if (inside(id, p)) shell.showItemInFolder(p); });
handle('files:open', (id, p) => { if (inside(id, p)) return shell.openPath(p); });
handle('files:copyImage', (id, p) => { if (inside(id, p)) clipboard.writeImage(nativeImage.createFromPath(p)); });

handle('skins:profile', () => skins.profile());
handle('skins:library', () => skins.library());
handle('skins:upload', (name, variant) => skins.upload(name, variant));
handle('skins:cape', id => skins.setCape(id));
handle('skins:remove', name => skins.remove(name));
handle('skins:saveCurrent', () => skins.saveCurrent());
handle('skins:current', () => skins.current());
wardrobe.setPlaying(() => launcher.isRunning());
handle('wardrobe:get', async () => ({ ...(await wardrobe.get()), playing: launcher.isRunning() }));
handle('wardrobe:buy', async id => wardrobe.buy(id));
handle('wardrobe:equip', async (type, key) => wardrobe.equip(type, key));
handle('wardrobe:wheel', async key => wardrobe.toggleWheel(key));
handle('wardrobe:redeem', async code => wardrobe.redeem(code));
handle('skins:folder', () => shell.openPath(skins.LIB));
handle('skins:import', async () => {
  const r = await dialog.showOpenDialog(win, { title: 'Import skins', properties: ['openFile', 'multiSelections'], filters: [{ name: 'Skins', extensions: ['png'] }] });
  return r.canceled ? null : skins.importFiles(r.filePaths);
});

handle('updates:check', id => updates.check(id));
handle('updates:apply', async (id, list) => {
  const progress = task('Updating mods');
  const n = await updates.apply(id, list, progress);
  progress({ done: true, message: `Updated ${n} mod${n === 1 ? '' : 's'}` });
  return n;
});

handle('launch:running', () => launcher.isRunning());

handle('selfupdate:check', () => release.checkLauncher(app.getVersion()));
handle('selfupdate:apply', async url => {
  const r = await release.applyLauncher(url, p => send('launch:progress', p));
  if (r.restart) setTimeout(() => { try { discord.stop(); } catch { } app.exit(0); }, 400);
  else shell.showItemInFolder(r.file);
  return r;
});

app.whenReady().then(() => { createWindow(); applyDiscordSettings(); require('./core/social').refreshService(); });
app.on('before-quit', () => { try { discord.stop(); } catch { } });
app.on('activate', () => { if (!alive()) createWindow(); });
app.on('window-all-closed', () => { if (!launcher.isRunning()) app.quit(); });
