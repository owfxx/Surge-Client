const { contextBridge, ipcRenderer } = require('electron');

const call = async (ch, ...args) => {
  const r = await ipcRenderer.invoke(ch, ...args);
  if (!r.ok) throw new Error(r.error);
  return r.data;
};
const on = (ch, fn) => {
  const h = (_e, d) => fn(d);
  ipcRenderer.on(ch, h);
  return () => ipcRenderer.removeListener(ch, h);
};

contextBridge.exposeInMainWorld('surge', {
  win: { min: () => ipcRenderer.send('win:min'), max: () => ipcRenderer.send('win:max'), close: () => ipcRenderer.send('win:close') },
  config: { get: () => call('config:get'), set: p => call('config:set', p), pickJava: () => call('dialog:java'), openDataDir: () => call('shell:dataDir') },
  open: url => call('shell:open', url),
  auth: { list: () => call('auth:list'), login: () => call('auth:login'), logout: u => call('auth:logout', u), setActive: u => call('auth:active', u) },
  fabricVersions: () => call('fabric:versions'),
  instances: {
    list: () => call('instances:list'),
    ensureDefault: () => call('instances:ensureDefault'),
    create: o => call('instances:create', o),
    update: (id, p) => call('instances:update', id, p),
    remove: id => call('instances:remove', id),
    content: id => call('instances:content', id),
    toggle: (id, t, f) => call('instances:toggle', id, t, f),
    removeContent: (id, t, f) => call('instances:removeContent', id, t, f),
    folder: id => call('instances:folder', id),
    exportPack: id => call('instances:export', id),
    importPack: () => call('instances:import'),
    addFiles: (id, t) => call('instances:addFiles', id, t),
    optimize: id => call('instances:optimize', id),
  },
  browse: {
    search: q => call('browse:search', q),
    install: o => call('browse:install', o),
    installPack: o => call('browse:installPack', o),
  },
  admin: {
    status: () => call('admin:status'), setup: p => call('admin:setup', p), unlock: p => call('admin:unlock', p),
    list: () => call('admin:list'), create: o => call('admin:create', o), remove: c => call('admin:remove', c), exportCodes: () => call('admin:export'),
    lock: () => call('admin:lock'), sales: () => call('admin:sales'), addSale: o => call('admin:addSale', o), removeSale: i => call('admin:removeSale', i),
    release: () => call('admin:release'), setRepo: r => call('admin:setRepo', r), setToken: t => call('admin:setToken', t),
    publishFile: notes => call('admin:publishFile', notes), publishCodes: () => call('admin:publishCodes'),
  },
  selfUpdate: { check: () => call('selfupdate:check'), apply: url => call('selfupdate:apply', url) },
  launch: { start: (id, target) => call('launch:start', id, target), running: () => call('launch:running') },
  worlds: id => call('instances:worlds', id),
  screenshots: id => call('instances:screenshots', id),
  files: { trash: (id, p) => call('files:trash', id, p), show: (id, p) => call('files:show', id, p), open: (id, p) => call('files:open', id, p), copyImage: (id, p) => call('files:copyImage', id, p) },
  servers: { ping: address => call('servers:ping', address) },
  openFile: file => call('shell:openFile', file),
  skins: {
    profile: () => call('skins:profile'), library: () => call('skins:library'), upload: (n, v) => call('skins:upload', n, v),
    cape: id => call('skins:cape', id), remove: n => call('skins:remove', n), saveCurrent: () => call('skins:saveCurrent'),
    folder: () => call('skins:folder'), importFiles: () => call('skins:import'),
  },
  updates: { check: id => call('updates:check', id), apply: (id, list) => call('updates:apply', id, list) },
  onTask: fn => on('task:progress', fn),
  onLaunchProgress: fn => on('launch:progress', fn),
  onLaunchLog: fn => on('launch:log', fn),
  onLaunchExit: fn => on('launch:exit', fn),
});
