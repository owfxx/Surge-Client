const S = window.surge;
const $ = (sel, root = document) => root.querySelector(sel);
const view = $('#view');
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const fmt = n => n >= 1e6 ? `${(n / 1e6).toFixed(1)}M` : n >= 1e3 ? `${(n / 1e3).toFixed(1)}K` : String(n ?? 0);
const plural = (n, w) => `${n} ${w}${n === 1 ? '' : 's'}`;
const ICON = 'assets/icon.png';
const REDUCED = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
const I = {
  chevron: '<svg viewBox="0 0 24 24"><path d="M6 9l6 6 6-6"/></svg>',
  back: '<svg viewBox="0 0 24 24"><path d="M15 6l-6 6 6 6"/></svg>',
  play: '<svg viewBox="0 0 24 24"><path d="M7 4.5v15L19.5 12z"/></svg>',
  plus: '<svg viewBox="0 0 24 24"><path d="M10 4h4v16h-4zM4 10h16v4H4z"/></svg>',
};
const xBtn = attr => `<button class="btn sm x-btn" ${attr}>&times;</button>`;
const selectWrap = html => `<span class="select-wrap">${html}</span>`;

const state = {
  view: 'home', cfg: null, accounts: { active: null, accounts: [] }, packs: [], selected: null,
  detail: null, detailTab: 'mod',
  browse: { source: 'modrinth', type: 'mod', query: '', sort: 'relevance', offset: 0, target: null, results: null, total: 0, loading: false, error: null },
  launch: { busy: false, stage: '', current: 0, total: 0, log: [], showLog: false },
  versions: [],
};

function toast(msg, err = false) {
  sfx(err ? 'err' : 'ok');
  const t = document.createElement('div');
  t.className = `toast${err ? ' err' : ''}`;
  t.innerHTML = '<div class="tm"></div>';
  $('.tm', t).textContent = msg;
  $('#toasts').appendChild(t);
  setTimeout(() => t.remove(), err ? 6000 : 3200);
}
async function safe(fn, okMsg) {
  try { const r = await fn(); if (okMsg) toast(okMsg); return r; } catch (e) { toast(e.message, true); return undefined; }
}

let audio;
function clickSound() {
  if (state.cfg && state.cfg.uiSounds === false) return;
  try {
    audio = audio || new AudioContext();
    if (audio.state === 'suspended') audio.resume();
    const t = audio.currentTime;
    const noise = audio.createBuffer(1, Math.floor(audio.sampleRate * 0.03), audio.sampleRate);
    const d = noise.getChannelData(0);
    for (let i = 0; i < d.length; i++) d[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / d.length, 3);
    const src = audio.createBufferSource(); src.buffer = noise;
    const bp = audio.createBiquadFilter(); bp.type = 'bandpass'; bp.frequency.value = 2400; bp.Q.value = 1.4;
    const ng = audio.createGain(); ng.gain.value = 0.35;
    src.connect(bp).connect(ng).connect(audio.destination);
    const o = audio.createOscillator(), g = audio.createGain();
    o.type = 'square';
    o.frequency.setValueAtTime(220, t);
    o.frequency.exponentialRampToValueAtTime(110, t + 0.05);
    g.gain.setValueAtTime(0.08, t);
    g.gain.exponentialRampToValueAtTime(0.0001, t + 0.06);
    o.connect(g).connect(audio.destination);
    src.start(t); o.start(t); o.stop(t + 0.07);
  } catch { }
}
document.addEventListener('pointerdown', e => {
  const b = e.target.closest('button');
  if (b && !b.disabled && e.button === 0) clickSound();
});

function sfx(kind) {
  if (state.cfg && state.cfg.uiSounds === false) return;
  try {
    audio = audio || new AudioContext();
    if (audio.state === 'suspended') audio.resume();
    const t = audio.currentTime, out = audio.createGain();
    out.gain.value = 0.6;
    out.connect(audio.destination);
    const tone = (f, at, len, type = 'sine', vol = 0.12, to) => {
      const o = audio.createOscillator(), g = audio.createGain();
      o.type = type; o.frequency.setValueAtTime(f, t + at);
      if (to) o.frequency.exponentialRampToValueAtTime(to, t + at + len);
      g.gain.setValueAtTime(0.0001, t + at); g.gain.exponentialRampToValueAtTime(vol, t + at + 0.01); g.gain.exponentialRampToValueAtTime(0.0001, t + at + len);
      o.connect(g).connect(out); o.start(t + at); o.stop(t + at + len + 0.02);
    };
    const noise = (at, len, freq, vol) => {
      const b = audio.createBuffer(1, Math.floor(audio.sampleRate * len), audio.sampleRate), d = b.getChannelData(0);
      for (let i = 0; i < d.length; i++) d[i] = (Math.random() * 2 - 1) * Math.sin(Math.PI * i / d.length);
      const src = audio.createBufferSource(), f = audio.createBiquadFilter(), g = audio.createGain();
      src.buffer = b; f.type = 'bandpass'; f.frequency.setValueAtTime(freq, t + at); f.frequency.exponentialRampToValueAtTime(freq * 2.5, t + at + len); f.Q.value = 0.8; g.gain.value = vol;
      src.connect(f).connect(g).connect(out); src.start(t + at);
    };
    if (kind === 'hover') tone(1800, 0, 0.03, 'sine', 0.025);
    else if (kind === 'swish') noise(0, 0.18, 700, 0.18);
    else if (kind === 'ok') { tone(880, 0, 0.18, 'triangle', 0.08); tone(1318, 0.07, 0.25, 'triangle', 0.07); }
    else if (kind === 'err') { tone(220, 0, 0.12, 'square', 0.04); tone(165, 0.1, 0.18, 'square', 0.04); }
    else if (kind === 'coin') { tone(988, 0, 0.08, 'square', 0.05); tone(1319, 0.07, 0.3, 'square', 0.05); }
    else if (kind === 'play') { [523, 659, 784, 1047].forEach((f, i) => tone(f, i * 0.06, 0.5, 'triangle', 0.06)); noise(0, 0.4, 400, 0.08); }
    else if (kind === 'open') tone(600, 0, 0.12, 'sine', 0.06, 900);
  } catch { }
}
let lastHover = null, lastHoverAt = 0;
document.addEventListener('mouseover', e => {
  const b = e.target.closest('.nav, .btn, .ward-card, .pack-card, .seg button');
  if (!b || b === lastHover) return;
  lastHover = b;
  const now = performance.now();
  if (now - lastHoverAt > 60) { lastHoverAt = now; sfx('hover'); }
});

const tip = $('#tip');
document.addEventListener('mouseover', e => {
  const el = e.target.closest('[data-tip]');
  if (!el || !el.dataset.tip) { tip.classList.add('hidden'); return; }
  tip.textContent = el.dataset.tip;
  tip.classList.remove('hidden');
});
document.addEventListener('mousemove', e => {
  if (tip.classList.contains('hidden')) return;
  tip.style.left = `${Math.min(e.clientX + 14, window.innerWidth - tip.offsetWidth - 8)}px`;
  tip.style.top = `${e.clientY + 18}px`;
});
document.addEventListener('mousedown', () => tip.classList.add('hidden'));

function modal(html, bind) {
  sfx('open');
  $('#modalCard').innerHTML = html;
  $('#modal').classList.remove('hidden');
  const close = () => $('#modal').classList.add('hidden');
  $('#modal').onclick = e => { if (e.target.id === 'modal') close(); };
  document.onkeydown = e => { if (e.key === 'Escape' && !$('#modal').classList.contains('hidden')) close(); };
  bind?.($('#modalCard'), close);
  return close;
}
function menu(anchor, items) {
  document.querySelector('.menu')?.remove();
  const m = document.createElement('div');
  m.className = 'menu';
  m.setAttribute('role', 'menu');
  items.forEach(([label, fn, kind]) => {
    const b = document.createElement('button');
    b.textContent = label;
    if (kind) b.className = kind;
    b.onclick = () => { m.remove(); fn(); };
    m.appendChild(b);
  });
  document.body.appendChild(m);
  const r = anchor.getBoundingClientRect();
  m.style.top = `${r.bottom + 4}px`;
  m.style.left = `${Math.min(r.right - m.offsetWidth, window.innerWidth - m.offsetWidth - 8)}px`;
  setTimeout(() => document.addEventListener('mousedown', function off(e) { if (!m.contains(e.target)) { m.remove(); document.removeEventListener('mousedown', off); } }), 0);
}
const img = (url, cls = '') => url ? `<img class="${cls}" src="${esc(url)}" alt="" loading="lazy">` : `<img class="${cls}" src="${ICON}" alt="">`;

document.addEventListener('error', e => {
  const t = e.target;
  if (t.tagName !== 'IMG') return;
  if (!t.dataset.fallback) { t.dataset.fallback = '1'; t.src = ICON; }
}, true);

const tasks = new Map();
S.onTask(p => {
  const key = p.label;
  let el = tasks.get(key);
  if (!el) {
    el = document.createElement('div');
    el.className = 'task';
    el.innerHTML = '<div class="l"></div><div class="s"></div><div class="bar indeterminate"><div></div></div>';
    $('#tasks').appendChild(el);
    tasks.set(key, el);
  }
  $('.l', el).textContent = p.label;
  if (p.stage) $('.s', el).textContent = p.stage;
  const bar = $('.bar', el);
  if (p.total) { bar.classList.remove('indeterminate'); $('div', bar).style.width = `${Math.round((p.current / p.total) * 100)}%`; }
  if (p.done) {
    tasks.delete(key);
    el.remove();
    if (p.error) toast(p.error, true); else if (p.message) toast(p.message);
    refreshPacks().then(() => { if (state.view !== 'home') render(); else updateHeroText(); });
  }
});

S.onLaunchProgress(p => {
  const L = state.launch;
  if (p.stage === 'running') { L.stage = 'Playing'; L.current = L.total = 0; }
  else { L.stage = p.stage; L.current = p.current || 0; L.total = p.total || 0; }
  updateLaunchUI();
});
S.onLaunchLog(line => {
  const L = state.launch;
  L.log.push(String(line).trimEnd());
  if (L.log.length > 600) L.log.splice(0, L.log.length - 600);
  const el = $('#log');
  if (el && L.showLog) { el.textContent = L.log.join('\n'); el.scrollTop = el.scrollHeight; }
});
S.onLaunchExit(({ code, crash }) => {
  Object.assign(state.launch, { busy: false, stage: crash ? crash.title : '', current: 0, total: 0 });
  if (state.view === 'home') updateLaunchUI();
  if (crash) crashModal(crash);
});

function crashModal(c) {
  modal(`<h2>${esc(c.title)}</h2>
    <p style="margin:0 0 12px">${esc(c.fix)}</p>
    ${c.details ? `<pre class="crash-details">${esc(c.details)}</pre>` : ''}
    <div class="modal-actions">
      ${c.report ? '<button class="btn" id="crReport">Open crash report</button>' : ''}
      <button class="btn" id="crLog">Copy log</button>
      <button class="btn primary" id="crOk">OK</button>
    </div>`,
  (card, close) => {
    if ($('#crReport', card)) $('#crReport', card).onclick = () => S.openFile(c.report);
    $('#crLog', card).onclick = async () => { await navigator.clipboard.writeText(state.launch.log.join('\n')); toast('Log copied'); };
    $('#crOk', card).onclick = close;
  });
}

async function refreshPacks() {
  state.packs = (await safe(() => S.instances.list())) || [];
  const cfgSel = state.cfg?.selectedInstance;
  state.selected = state.packs.find(p => p.id === (state.selected?.id || cfgSel)) || state.packs[0] || null;
}
async function refreshAccounts() {
  state.accounts = (await safe(() => S.auth.list())) || state.accounts;
  const acc = activeAccount();
  $('#accountName').textContent = acc ? acc.name : 'Sign in';
  $('#accountBtn').dataset.tip = acc ? acc.name : 'Sign in';
  $('#accountHead').src = acc ? `https://mc-heads.net/avatar/${acc.uuid}/64` : ICON;
}
const activeAccount = () => state.accounts.accounts.find(a => a.uuid === state.accounts.active);
const modCount = p => (p?.content || []).filter(c => c.type === 'mod').length;

document.querySelectorAll('.nav[data-view]').forEach(b => b.addEventListener('click', () => go(b.dataset.view)));
const DISCORD = 'https://discord.gg/7wGxqGN86R';
$('#discordBtn').onclick = () => S.open(DISCORD);
document.querySelectorAll('[data-win]').forEach(b => b.addEventListener('click', () => S.win[b.dataset.win]()));
$('#accountBtn').addEventListener('click', openAccounts);

function go(v, opts = {}) {
  if (state.view !== v) sfx('swish');
  state.view = v;
  if (v === 'packs' && !opts.keepDetail) state.detail = null;
  document.querySelectorAll('.nav[data-view]').forEach(b => b.classList.toggle('active', b.dataset.view === v));
  render();
  if (v === 'browse' && !state.browse.results) runSearch();
}

function render() {
  view.scrollTop = 0;
  view.classList.remove('view-anim');
  void view.offsetWidth;
  if (state.view !== 'home') view.classList.add('view-anim');
  view.classList.toggle('home', state.view === 'home');
  if (state.view !== 'home') disposeScene();
  if (state.view !== 'skins') disposeSkinPreview();
  if (state.view !== 'wardrobe') disposeWardrobe();
  ({ home: renderHome, packs: renderPacks, skins: renderSkins, wardrobe: renderWardrobe, servers: renderServers, browse: renderBrowse, settings: renderSettings })[state.view]();
}

const PLACEHOLDER_SKIN = 'chroynic';
const Q_MARK = ['..XXXX..', '.XX..XX.', '.....XX.', '....XX..', '...XX...', '........', '...XX...'];

function unknownSkin() {
  const c = document.createElement('canvas');
  c.width = c.height = 64;
  const g = c.getContext('2d');
  const paint = (color, rects) => { g.fillStyle = color; rects.forEach(([x, y, w, h]) => g.fillRect(x, y, w, h)); };
  paint('#15101f', [[0, 0, 32, 16], [16, 16, 24, 16], [40, 16, 16, 16], [0, 16, 16, 16], [16, 48, 16, 16], [32, 48, 16, 16]]);
  paint('#0a0710', [[8, 8, 8, 8], [20, 20, 8, 12], [44, 20, 4, 12], [36, 52, 4, 12], [4, 20, 4, 12], [20, 52, 4, 12]]);
  paint('#211833', [[8, 0, 8, 8], [20, 16, 8, 4], [44, 16, 4, 4], [36, 48, 4, 4], [4, 16, 4, 4], [20, 48, 4, 4]]);
  g.fillStyle = '#d4d4d8';
  Q_MARK.forEach((row, r) => [...row].forEach((ch, col) => { if (ch === 'X') g.fillRect(8 + col, 8 + r, 1, 1); }));
  return c;
}

const EASE = x => x * x * (3 - 2 * x);
const CL = v => Math.max(0, Math.min(1, v));
function emotePose(name, t) {
  const P = { rArm: [0, 0, 0], lArm: [0, 0, 0], rLeg: [0, 0, 0], lLeg: [0, 0, 0], head: [0, 0, 0], body: [0, 0, 0], lean: 0, bob: 0, sway: 0 };
  const sin = Math.sin, cos = Math.cos, abs = Math.abs;
  switch (name) {
    case 'IDLE_LOOK': { const lk = sin(t * 0.8) * 42 * Math.min(1, t); P.head = [-4 + sin(t * 0.4) * 6, lk, 0]; P.sway = sin(t * 0.8) * 0.25; P.rArm = [-4, 0, 4]; P.lArm = [-4, 0, -4]; break; }
    case 'IDLE_STRETCH': { const c = (t % 6) / 6; const up = c < 0.08 ? 0 : c < 0.22 ? EASE((c - 0.08) / 0.14) : c < 0.55 ? 1 : c < 0.7 ? EASE(1 - (c - 0.55) / 0.15) : 0; P.rArm = [0, 0, 170 * up]; P.lArm = [0, 0, -170 * up]; P.head = [-25 * up, 0, 0]; P.lean = -8 * up; P.bob = -0.6 * up; break; }
    case 'IDLE_YAWN': { const c = (t % 7) / 7; const y = EASE(CL(Math.min(c * 5, (0.6 - c) * 5))); P.rArm = [-130 * y, -30 * y, 10 * y]; P.head = [-20 * y + 6 * (1 - y), 0, 6 * (1 - y)]; P.lArm = [-6, 0, -4]; P.bob = 0.3 * (1 - y); break; }
    case 'IDLE_TAP': { const tap = Math.max(0, sin(t * 9)); P.rLeg = [-14 * tap, 0, 3]; P.lArm = [-50, 35, -6]; P.rArm = [-50, -35, 6]; P.head = [4, 0, sin(t * 4.5) * 3]; break; }
    case 'IDLE_CROSS': { const br = sin(t * 1.6); P.rArm = [-62 + br * 2, -42, 12]; P.lArm = [-58 + br * 2, 42, -12]; P.head = [-3 + br, sin(t * 0.5) * 10, 0]; P.lean = -2; break; }
    case 'IDLE_VIBE': { const b = sin(t * 2 * Math.PI); P.head = [10 * abs(b), sin(t * Math.PI) * 10, 0]; P.bob = -abs(b) * 0.6; P.rArm = [-10, 0, 8 + 6 * abs(b)]; P.lArm = [-10, 0, -8 - 6 * abs(b)]; P.sway = b * 0.4; break; }
    case 'WAVE': { const w = sin(t * 9), hi = EASE(CL(t / 0.3)); P.rArm = [-15 + w * 5, 0, (150 + w * 24) * hi]; P.lArm = [4, 0, -6]; P.head = [-8, -14, 6 + w * 3]; P.body = [0, -6, 0]; P.bob = -abs(sin(t * 4.5)) * 0.5; break; }
    case 'CLAP': { const c = abs(sin(t * 7)); P.rArm = [-72, -(4 + 17 * c), 0]; P.lArm = [-72, 4 + 17 * c, 0]; P.head = [4 - 6 * c, 0, 0]; P.bob = -c * 0.3; break; }
    case 'DAB': { const d = EASE(CL(t * 4)); P.lArm = [0, 0, -125 * d]; P.rArm = [-105 * d, -55 * d, 0]; P.head = [30 * d, 28 * d, 0]; P.lean = 8 * d; break; }
    case 'SHRUG': { const k = EASE(0.5 - 0.5 * cos(t * Math.PI * 2 / 1.2)); P.rArm = [-25 * k, 0, 32 * k]; P.lArm = [-25 * k, 0, -32 * k]; P.head = [-6 * k, 0, 14 * k]; break; }
    case 'SALUTE': P.rArm = [-118, -50, 14]; P.lArm = [0, 0, -4]; P.head = [-6, 0, 0]; break;
    case 'CHEER': { const j = abs(sin(t * 5)), pump = sin(t * 10) * 14; P.rArm = [0, 0, 160 + pump]; P.lArm = [0, 0, -160 - pump]; P.head = [-15, 0, 0]; P.bob = -j * 2.5; P.rLeg = [0, 0, 4 * j]; P.lLeg = [0, 0, -4 * j]; break; }
    case 'BOW': { const ph = (t % 3) / 3, dn = EASE(CL(ph / 0.35)) * (1 - EASE(CL((ph - 0.65) / 0.35))); P.lean = 40 * dn; P.rArm = [-70 * dn, -45 * dn, 0]; P.lArm = [25 * dn, 0, -5]; P.head = [10 * dn, 0, 0]; break; }
    case 'HEART': { const beat = Math.max(0, sin(t * 7)), k = sin(t * 2); P.rArm = [-82 - beat * 4, -36, 0]; P.lArm = [-82 - beat * 4, 36, 0]; P.head = [-6, 0, k * 6]; P.sway = k * 0.6; P.body = [0, k * 5, 0]; P.bob = -beat * 0.4; break; }
    case 'FLOSS': { const f = sin(t * 7), sw = sin(t * 14); P.rArm = [26 * sw, 0, 42 * f]; P.lArm = [-26 * sw, 0, 42 * f]; P.body = [0, 0, -7 * f]; P.sway = -2 * f; P.head = [0, 0, 7 * f]; P.rLeg = [0, 0, -4 * f]; P.lLeg = [0, 0, -4 * f]; break; }
    case 'TPOSE': P.rArm = [0, 0, 90]; P.lArm = [0, 0, -90]; break;
    case 'LAUGH': { const sh = sin(t * 20); P.lean = -14 + sh * 2; P.rArm = [-30, -30, 0]; P.lArm = [-35, 32, 0]; P.head = [-24 + sh * 5, 0, 0]; P.bob = abs(sh) * 0.5; break; }
    case 'FACEPALM': { const a = EASE(CL(t / 0.45)), sh = t > 0.6 ? sin(t * 6.5) * 9 * a : 0; P.rArm = [-128 * a, -38 * a, 0]; P.lArm = [-20 * a, 25 * a, -6]; P.head = [30 * a, sh, 0]; P.lean = 7 * a; P.bob = 0.4 * a; break; }
    case 'SIT': { const br = sin(t * 2); P.bob = 10; P.rLeg = [-88, 8, 0]; P.lLeg = [-88, -8, 0]; P.rArm = [-20, 0, 12]; P.lArm = [-20, 0, -12]; P.head = [4 + br * 2, sin(t * 0.7) * 12, 0]; break; }
    default: { const b = abs(sin(t * 4)), side = sin(t * 4); P.rArm = [-40 - 30 * side, 0, 20]; P.lArm = [-40 + 30 * side, 0, -20]; P.head = [6 * b, side * 8, 0]; P.bob = -b * 1.2; P.sway = side * 0.5; }
  }
  return P;
}
const D2R = Math.PI / 180;
function applyPose(player, P, k) {
  const s = player.skin;
  const set = (part, r) => part.rotation.set(r[0] * D2R * k, -r[1] * D2R * k, -r[2] * D2R * k);
  set(s.rightArm, P.rArm); set(s.leftArm, P.lArm); set(s.rightLeg, P.rLeg); set(s.leftLeg, P.lLeg); set(s.head, P.head);
  s.body.rotation.set((P.body[0] + P.lean) * D2R * k, -P.body[1] * D2R * k, -P.body[2] * D2R * k);
  player.position.y = -P.bob * k;
  player.position.x = P.sway * k;
}
function emoteAnimation(getName) {
  let cur = null, since = 0;
  return new skinview3d.FunctionAnimation((player, t) => {
    const name = getName();
    if (name !== cur) { cur = name; since = t; }
    if (!name) {
      const s = player.skin;
      s.rightArm.rotation.set(Math.sin(t * 0.67) * 0.04, 0, 0.06 + Math.cos(t * 0.9) * 0.035);
      s.leftArm.rotation.set(-Math.sin(t * 0.67) * 0.04, 0, -0.06 - Math.cos(t * 0.9) * 0.035);
      s.rightLeg.rotation.set(0, 0, 0); s.leftLeg.rotation.set(0, 0, 0); s.head.rotation.set(0, 0, 0); s.body.rotation.set(0, 0, 0);
      player.position.set(0, 0, 0);
      player.cape.rotation.x = 0.06 * Math.PI + Math.sin(t * 2) * 0.01;
      return;
    }
    const lt = t - since;
    applyPose(player, emotePose(name, lt), Math.min(1, lt / 0.25));
    player.cape.rotation.x = 0.06 * Math.PI + Math.max(0, player.skin.body.rotation.x) * 0.6;
  });
}

function surgeIdle() {
  return new skinview3d.FunctionAnimation((player, t) => {
    const s = player.skin;
    s.rightArm.rotation.z = 0.06 + Math.cos(t * 0.9) * 0.035;
    s.leftArm.rotation.z = -0.06 - Math.cos(t * 0.9) * 0.035;
    s.rightArm.rotation.x = Math.sin(t * 0.67) * 0.04;
    s.leftArm.rotation.x = -Math.sin(t * 0.67) * 0.04;
    s.leftLeg.rotation.x = s.rightLeg.rotation.x = 0;
    s.body.rotation.x = 0;
    s.rotation.y = 0;
    player.position.y = Math.sin(t * 1.6) * 0.12;
  });
}

let viewer = null;
let sceneRaf = 0;
const look = { x: 0, y: 0, tx: 0, ty: 0, drag: 0, dragging: false, lastX: 0 };
window.addEventListener('mousemove', e => {
  const c = document.getElementById('scene');
  if (!c) return;
  const r = c.getBoundingClientRect();
  const px = r.left + r.width * 0.68;
  look.tx = Math.max(-1, Math.min(1, (e.clientX - px) / (window.innerWidth / 2)));
  look.ty = Math.max(-1, Math.min(1, (e.clientY - (r.top + r.height * 0.35)) / (window.innerHeight / 2)));
  if (look.dragging) { look.drag += (e.clientX - look.lastX) * 0.012; look.lastX = e.clientX; }
});
window.addEventListener('mouseup', () => { look.dragging = false; });

function disposeScene() {
  cancelAnimationFrame(sceneRaf);
  if (viewer) { viewer.dispose(); viewer = null; }
}

function buildGeode(v) {
  const T = window.THREE;
  if (!T) return () => {};
  T.ColorManagement.enabled = false;
  const group = new T.Group();
  group.position.x = shift;
  v.scene.add(group);

  const rim = new T.PointLight(0xa6a6aa, 1.6, 0, 0);
  rim.position.set(shift, -4, -26);
  v.scene.add(rim);

  const plat = new T.Mesh(new T.CylinderGeometry(13, 11, 5, 6), new T.MeshLambertMaterial({ color: 0x3b3b3f, flatShading: true }));
  plat.position.y = -18.5;
  plat.rotation.y = Math.PI / 6;
  group.add(plat);
  const edges = new T.LineSegments(new T.EdgesGeometry(plat.geometry), new T.LineBasicMaterial({ color: 0xc2c2c6, transparent: true, opacity: 0.55 }));
  edges.position.copy(plat.position); edges.rotation.copy(plat.rotation);
  group.add(edges);
  const inlay = new T.Mesh(new T.CircleGeometry(10, 6), new T.MeshBasicMaterial({ color: 0xa6a6aa, transparent: true, opacity: 0.16, blending: T.AdditiveBlending, depthWrite: false }));
  inlay.rotation.x = -Math.PI / 2; inlay.rotation.z = Math.PI / 6; inlay.position.y = -15.95; inlay.scale.setScalar(0.85);
  group.add(inlay);
  const glow = new T.Mesh(new T.RingGeometry(15.2, 19, 6), new T.MeshBasicMaterial({ color: 0xa6a6aa, transparent: true, opacity: 0.07, blending: T.AdditiveBlending, depthWrite: false, side: T.DoubleSide }));
  glow.rotation.x = -Math.PI / 2; glow.rotation.z = Math.PI / 6; glow.position.y = -20.9;
  group.add(glow);

  const crystalMat = (c, e) => new T.MeshLambertMaterial({ color: c, emissive: e, flatShading: true, transparent: true, opacity: 0.88 });
  const specs = [
    [-26, -4, -10, 4.2, 0xbcbcc0, 0x151519], [24, 6, -12, 3.6, 0xd4d4d8, 0x19191d], [-18, 18, -24, 2.6, 0xc8c8cc, 0x1a1a1e],
    [30, -14, 2, 2.8, 0xaaaaae, 0x121216], [-34, 24, -46, 5.5, 0x9d9da1, 0x0e0e12], [38, 22, -40, 6, 0xb6b6ba, 0x111115],
    [10, 28, -30, 2, 0xf7f7fb, 0x1f1f23], [-20, -16, 12, 1.8, 0xdadade, 0x18181c],
  ];
  const crystals = specs.map(([x, y, z, s, c, e], i) => {
    const m = new T.Mesh(new T.OctahedronGeometry(1, 0), crystalMat(c, e));
    m.scale.set(s, s * 2.1, s);
    m.position.set(x, y, z);
    m.rotation.set(0.3 * i, i, 0.2);
    m.userData = { y, s, phase: i * 1.7, delay: i * 0.08 };
    group.add(m);
    return m;
  });

  const N = 140, pos = new Float32Array(N * 3);
  for (let i = 0; i < N; i++) { pos[i * 3] = (Math.random() - 0.5) * 120; pos[i * 3 + 1] = Math.random() * 70 - 30; pos[i * 3 + 2] = (Math.random() - 0.5) * 80 - 10; }
  const dustGeo = new T.BufferGeometry(); dustGeo.setAttribute('position', new T.BufferAttribute(pos, 3));
  const dust = new T.Points(dustGeo, new T.PointsMaterial({ color: 0xf1f1f5, size: 0.6, transparent: true, opacity: 0.6, blending: T.AdditiveBlending, depthWrite: false }));
  group.add(dust);

  const start = performance.now();
  return now => {
    const t = (now - start) / 1000;
    for (const c of crystals) {
      const d = c.userData;
      const k = REDUCED ? 1 : Math.min(1, Math.max(0, (t - d.delay) / 0.9));
      const e = 1 - Math.pow(1 - k, 3);
      c.scale.set(d.s * e, d.s * 2.1 * e, d.s * e);
      c.position.y = d.y + (REDUCED ? 0 : Math.sin(t * 0.7 + d.phase) * 1.4 - (1 - e) * 8);
      if (!REDUCED) c.rotation.y += 0.004;
    }
    const intro = REDUCED ? 1 : Math.min(1, t / 1.2);
    edges.material.opacity = 0.55 * intro;
    glow.material.opacity = 0.07 * intro + (REDUCED ? 0 : Math.sin(t * 1.3) * 0.015);
    if (!REDUCED) {
      const p = dust.geometry.attributes.position;
      for (let i = 0; i < N; i++) { let y = p.getY(i) + 0.03; if (y > 40) y = -30; p.setY(i, y); }
      p.needsUpdate = true;
    }
  };
}

let shift = 0;
async function mountScene() {
  disposeScene();
  const canvas = document.getElementById('scene');
  if (!canvas || !window.skinview3d) return;
  const box = canvas.parentElement.getBoundingClientRect();
  viewer = new skinview3d.SkinViewer({ canvas, width: box.width, height: box.height, zoom: 0.5, fov: 36 });
  viewer.animation = surgeIdle();
  viewer.controls.enabled = false;
  viewer.globalLight.intensity = 2.2;
  viewer.cameraLight.intensity = 0.9;
  shift = box.width > 900 ? Math.min(11, box.width / 100) : Math.min(30, box.width / 36);
  viewer.playerObject.position.x = shift;
  const tickGeode = buildGeode(viewer);
  canvas.onmousedown = e => { look.dragging = true; look.lastX = e.clientX; };

  const acc = activeAccount();
  const urls = acc ? [`https://mc-heads.net/skin/${acc.uuid}`, `https://crafatar.com/skins/${acc.uuid}`, `https://mc-heads.net/skin/${PLACEHOLDER_SKIN}`] : [];
  let loaded = false;
  for (const u of urls) {
    try { await viewer?.loadSkin(u); loaded = true; break; } catch { }
  }
  if (!viewer) return;
  if (!loaded) viewer.loadSkin(unknownSkin());
  if (acc) viewer.nameTag = new skinview3d.NameTagObject(acc.name, { font: '46px Monocraft', repaintAfterLoaded: true });

  const skin = viewer.playerObject.skin;
  const loop = now => {
    if (!viewer) return;
    look.x += (look.tx - look.x) * 0.1;
    look.y += (look.ty - look.y) * 0.1;
    skin.head.rotation.y = look.x * 0.7;
    skin.head.rotation.x = look.y * 0.4;
    viewer.playerObject.rotation.y = look.x * 0.3 + look.drag;
    if (!look.dragging) look.drag *= 0.96;
    tickGeode(now);
    if (viewer.nameTag) viewer.nameTag.position.x = shift;
    sceneRaf = requestAnimationFrame(loop);
  };
  sceneRaf = requestAnimationFrame(loop);
}

window.addEventListener('resize', () => {
  const c = document.getElementById('scene');
  if (!viewer || !c) return;
  const r = c.parentElement.getBoundingClientRect();
  viewer.setSize(r.width, r.height);
  mountScene();
});

function renderHome() {
  view.innerHTML = `
    <section class="hero">
      <canvas id="scene"></canvas>
      <div class="hero-info">
        <div class="hero-chips" id="heroChips"></div>
        <button class="pack-title" id="packTitle" data-tip="Switch modpack"><h1 id="packName"></h1>${I.chevron}</button>
        <div class="play-row">
          <button class="btn primary play" id="playBtn">${I.play}<span>Play</span></button>
          ${(state.cfg.servers || []).slice(0, 3).map((sv, i) => `<button class="btn quick" data-quick="${i}" data-tip="Join ${esc(sv.address)}">${esc(sv.name)}</button>`).join('')}
        </div>
        <div class="launch-status" id="launchStatus"></div>
      </div>
      <aside class="home-side" id="homeSide">
        <div class="greet" id="greet"></div>
        <div class="side-card" id="friendsCard"><div class="side-head">Friends</div><div class="muted small">Loading…</div></div>
        <div class="side-card news" id="newsCard"><div class="side-head">What's new</div><ul></ul></div>
      </aside>
      <div class="panel log ${state.launch.showLog ? '' : 'hidden'}" id="log"></div>
    </section>`;
  updateHeroText();
  homeSide();
  loadNews();
  loadAnnouncement();
  mountScene();
  if (state.launch.showLog) { const lg = $('#log'); lg.textContent = state.launch.log.join('\n'); lg.scrollTop = lg.scrollHeight; }
  $('#packTitle').onclick = pickPack;
  $('#playBtn').onclick = () => play();
  view.querySelectorAll('[data-quick]').forEach(b => b.onclick = () => play({ server: state.cfg.servers[+b.dataset.quick].address }));
  addContinueButton();
  updateLaunchUI();
}

const STATUS_COL = { online: '#4ade80', idle: '#facc15', busy: '#f87171', offline: '#71717a' };
async function homeSide() {
  const h = new Date().getHours();
  const name = activeAccount()?.name;
  const g = $('#greet');
  if (g) g.innerHTML = `<div class="greet-time">${h < 5 ? 'Late night' : h < 12 ? 'Good morning' : h < 18 ? 'Good afternoon' : 'Good evening'}${name ? ',' : ''}</div>${name ? `<div class="greet-name">${esc(name)}</div>` : ''}`;
  const card = $('#friendsCard');
  if (!card) return;
  const data = await S.social().catch(() => null);
  if (!$('#friendsCard')) return;
  if (data && data.off) { card.innerHTML = '<div class="side-head">Friends</div><div class="muted small">Friends and chat are coming soon.</div>'; return; }
  if (!data || data.signedOut) { card.innerHTML = '<div class="side-head">Friends</div><div class="muted small">Sign in with a Microsoft account to see your friends.</div>'; return; }
  if (data.error) {
    card.innerHTML = `<div class="side-head">Friends</div><div class="muted small">${esc(data.error)}</div><button class="btn sm" id="friendsRetry" style="margin-top:8px">Try again</button>`;
    $('#friendsRetry', card).onclick = () => { card.innerHTML = '<div class="side-head">Friends</div><div class="muted small">Loading…</div>'; homeSide(); };
    return;
  }
  const fr = (data.friends || []).slice().sort((a, b) => (a.status === 'offline') - (b.status === 'offline'));
  const on = fr.filter(f => f.status !== 'offline').length;
  const req = (data.incoming || []).length;
  card.innerHTML = `<div class="side-head">Friends <span class="muted small">${on} online${req ? ` · ${req} request${req > 1 ? 's' : ''}` : ''}</span></div>
    ${fr.slice(0, 7).map(f => `<div class="friend"><img src="https://mc-heads.net/avatar/${esc(f.uuid)}/24" alt=""><span class="dot" style="background:${STATUS_COL[f.status] || STATUS_COL.offline}"></span>
      <div class="grow"><div>${esc(f.name)}</div><div class="muted small">${f.status === 'offline' ? 'Offline' : esc(f.server || (f.status[0].toUpperCase() + f.status.slice(1)))}</div></div>
      <button class="btn sm" data-chat="${esc(f.uuid)}" data-name="${esc(f.name)}" data-tip="Message">Chat${unread(f.uuid) ? ` <span class="badge">${unread(f.uuid)}</span>` : ''}</button>
      ${f.server && f.server !== 'Singleplayer' && f.status !== 'offline' ? `<button class="btn sm" data-join="${esc(f.server)}">Join</button>` : ''}</div>`).join('')
      || '<div class="muted small">No friends yet. Add them in game: pause menu, Social.</div>'}`;
  card.querySelectorAll('[data-join]').forEach(b => b.onclick = () => play({ server: b.dataset.join }));
  card.querySelectorAll('[data-chat]').forEach(b => b.onclick = () => openChat(b.dataset.chat, b.dataset.name));
  pollChats();
}

const chat = { msgs: [], since: 0, me: '', timer: null, open: null };
const normId = u => String(u || '').replace(/-/g, '').toLowerCase();
function seenKey(u) { return `surge.seen.${normId(u)}`; }
function unread(u) {
  let seen = 0;
  try { seen = Number(localStorage.getItem(seenKey(u))) || 0; } catch { }
  return chat.msgs.filter(m => normId(m.sender) === normId(u) && m.id > seen).length;
}
async function fetchChats() {
  const r = await S.messages(chat.since).catch(() => null);
  if (!r) return false;
  chat.me = normId(r.me);
  let added = false;
  for (const m of r.messages || []) {
    if (chat.msgs.some(x => x.id === m.id)) continue;
    chat.msgs.push(m);
    chat.since = Math.max(chat.since, m.id);
    added = true;
  }
  return added;
}
function pollChats() {
  if (chat.timer) return;
  const tick = async () => {
    const added = await fetchChats();
    if (added) {
      if (chat.open) renderChat();
      else if (state.view === 'home') homeSide();
    }
    chat.timer = setTimeout(tick, chat.open ? 2500 : 15000);
  };
  tick();
}
function renderChat() {
  const box = $('#chatLog');
  if (!box || !chat.open) return;
  const other = normId(chat.open.uuid);
  const list = chat.msgs.filter(m => normId(m.sender) === other || normId(m.receiver) === other);
  const atBottom = box.scrollHeight - box.scrollTop - box.clientHeight < 40;
  box.innerHTML = list.map(m => {
    const mine = normId(m.sender) === chat.me;
    const t = new Date(m.ts).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    return `<div class="cmsg ${mine ? 'mine' : ''}${m.pending ? ' pending' : ''}"><div class="cbody">${esc(m.text)}</div><div class="ctime">${m.pending ? 'sending…' : t}${m.edited ? ' · edited' : ''}</div></div>`;
  }).join('') || '<div class="muted small" style="text-align:center;margin-top:30px">No messages yet. Say hi!</div>';
  if (atBottom || !box.dataset.ready) { box.scrollTop = box.scrollHeight; box.dataset.ready = '1'; }
  const last = list.filter(m => normId(m.sender) === other).pop();
  if (last) try { localStorage.setItem(seenKey(other), String(last.id)); } catch { }
}
function openChat(uuid, name) {
  chat.open = { uuid, name };
  const close = modal(`<div class="chat-head"><img src="https://mc-heads.net/avatar/${esc(uuid)}/32" alt=""><h2>${esc(name)}</h2></div>
    <div class="chat-log" id="chatLog"></div>
    <form class="chat-send" id="chatForm"><input class="input" id="chatInput" maxlength="500" placeholder="Message ${esc(name)}" autocomplete="off"><button class="btn primary" type="submit">Send</button></form>
    <div class="muted small" id="chatErr"></div>`, (card) => {
    card.classList.add('chat-card');
    const input = $('#chatInput', card);
    setTimeout(() => input.focus(), 30);
    $('#chatForm', card).onsubmit = async e => {
      e.preventDefault();
      const text = input.value.trim();
      if (!text) return;
      input.value = '';
      const temp = { id: -Date.now(), sender: chat.me, receiver: normId(uuid), text, ts: Date.now(), pending: true };
      chat.msgs.push(temp);
      renderChat();
      try {
        const r = await S.sendMessage(uuid, text);
        temp.id = r.id; temp.ts = r.ts; temp.pending = false;
        chat.since = Math.max(chat.since, r.id);
        $('#chatErr').textContent = '';
      } catch (err) {
        chat.msgs.splice(chat.msgs.indexOf(temp), 1);
        $('#chatErr').textContent = err.message;
        input.value = text;
      }
      renderChat();
    };
  });
  const obs = new MutationObserver(() => {
    if ($('#modal').classList.contains('hidden')) { chat.open = null; obs.disconnect(); $('#modalCard').classList.remove('chat-card'); if (state.view === 'home') homeSide(); }
  });
  obs.observe($('#modal'), { attributes: true, attributeFilter: ['class'] });
  renderChat();
  fetchChats().then(renderChat);
  return close;
}

async function addContinueButton() {
  const p = state.selected;
  if (!p) return;
  const worlds = (await S.worlds(p.id).catch(() => [])) || [];
  const row = $('.play-row');
  if (!worlds.length || !row || state.selected !== p) return;
  const b = document.createElement('button');
  b.className = 'btn quick';
  b.dataset.tip = 'Continue your last world';
  b.textContent = worlds[0].folder;
  b.onclick = () => play({ world: worlds[0].folder });
  $('#playBtn').after(b);
}

function updateHeroText() {
  const p = state.selected;
  const name = $('#packName');
  if (!name) return;
  name.textContent = p ? p.name : 'Create a modpack';
  $('#heroChips').innerHTML = p
    ? `<span class="chip accent">Fabric ${esc(p.mcVersion)}</span><span class="chip">${plural(modCount(p), 'mod')}</span>`
    : '';
}

function updateLaunchUI() {
  const el = $('#launchStatus');
  if (!el) return;
  const L = state.launch;
  const pct = L.total ? Math.round((L.current / L.total) * 100) : null;
  const logBtn = L.log.length ? `<button class="log-btn" id="logBtn">${L.showLog ? 'Hide log' : 'Show log'}</button>` : '';
  el.innerHTML = (L.stage || L.busy)
    ? `<div class="row"><span>${esc(L.stage)}${pct !== null ? ` ${pct}%` : ''}</span>${logBtn}</div>${L.busy && L.stage !== 'Playing' ? `<div class="bar ${pct === null ? 'indeterminate' : ''}"><div style="width:${pct ?? 0}%"></div></div>` : ''}`
    : logBtn;
  const btn = $('#logBtn');
  if (btn) btn.onclick = () => {
    L.showLog = !L.showLog;
    const lg = $('#log');
    lg.classList.toggle('hidden', !L.showLog);
    lg.textContent = L.log.join('\n'); lg.scrollTop = lg.scrollHeight;
    updateLaunchUI();
  };
  const play = $('#playBtn');
  if (play) {
    play.disabled = L.busy;
    $('span', play).textContent = L.busy ? (L.stage === 'Playing' ? 'Playing' : 'Launching') : (state.selected ? 'Play' : 'New modpack');
  }
}

async function play(target) {
  if (!target || typeof target !== 'object' || target instanceof Event) target = {};
  if (!state.selected) { go('packs'); newPackModal(); return; }
  if (!state.accounts.active) { openAccounts(); return; }
  if (state.launch.busy) { toast('Minecraft is already running', true); return; }
  sfx('play');
  if (state.view !== 'home') go('home');
  Object.assign(state.launch, { busy: true, stage: target.server ? `Joining ${target.server}` : target.world ? `Opening ${target.world}` : 'Starting', log: [] });
  updateLaunchUI();
  try { await S.launch.start(state.selected.id, target); } catch (e) {
    Object.assign(state.launch, { busy: false, stage: '' });
    toast(e.message, true);
    updateLaunchUI();
  }
}

function pickPack() {
  if (!state.packs.length) { go('packs'); newPackModal(); return; }
  modal(`<h2>Modpacks</h2><div class="pick-list">${state.packs.map(p => `
    <button data-id="${esc(p.id)}" class="${p.id === state.selected?.id ? 'on' : ''}">${img(p.icon, 'pack-icon')}
      <div class="grow"><div>${esc(p.name)}</div><div class="muted small">Fabric ${esc(p.mcVersion)}, ${plural(modCount(p), 'mod')}</div></div></button>`).join('')}</div>`,
  (card, close) => card.querySelectorAll('[data-id]').forEach(b => b.onclick = () => {
    state.selected = state.packs.find(p => p.id === b.dataset.id);
    S.config.set({ selectedInstance: state.selected.id });
    close(); updateHeroText(); updateLaunchUI();
  }));
}

function openAccounts() {
  const { accounts, active } = state.accounts;
  modal(`<h2>Accounts</h2>
    <div class="pick-list">${accounts.map(a => `
      <button data-uuid="${esc(a.uuid)}" class="${a.uuid === active ? 'on' : ''}">
        <img class="pack-icon" src="https://mc-heads.net/avatar/${esc(a.uuid)}/64" alt="">
        <div class="grow">${esc(a.name)}</div>
        <span class="btn sm x-btn" data-out="${esc(a.uuid)}" data-tip="Sign out">&times;</span>
      </button>`).join('')}</div>
    <div class="modal-actions"><button class="btn primary" id="msLogin">Add Microsoft account</button></div>`,
  (card, close) => {
    $('#msLogin', card).onclick = async () => {
      const r = await safe(() => S.auth.login());
      if (r) { await refreshAccounts(); close(); if (state.view === 'home') renderHome(); toast(`Signed in as ${activeAccount()?.name}`); }
    };
    card.querySelectorAll('[data-uuid]').forEach(b => b.onclick = async e => {
      const out = e.target.closest('[data-out]');
      if (out) await safe(() => S.auth.logout(out.dataset.out)); else await safe(() => S.auth.setActive(b.dataset.uuid));
      await refreshAccounts(); openAccounts(); if (state.view === 'home') renderHome();
    });
  });
}

function renderPacks() {
  if (state.detail) return renderDetail();
  view.innerHTML = `<div class="page">
    <div class="view-head"><h1 class="view-title">Modpacks</h1>
      <div style="display:flex;gap:8px"><button class="btn" id="importBtn">Import</button><button class="btn primary" id="newBtn">${I.plus}New modpack</button></div></div>
    <div class="grid">
      ${state.packs.map(p => `
        <button class="panel pack-card" data-id="${esc(p.id)}">${img(p.icon, 'pack-icon')}
          <div style="min-width:0"><div class="title">${esc(p.name)}</div><div class="meta">Fabric ${esc(p.mcVersion)}, ${plural(modCount(p), 'mod')}</div></div>
        </button>`).join('')}
      <button class="panel pack-card new-card" id="newCard" aria-label="New modpack">${I.plus}</button>
    </div></div>`;
  view.querySelectorAll('[data-id]').forEach(c => c.onclick = () => openDetail(c.dataset.id));
  $('#newBtn').onclick = $('#newCard').onclick = newPackModal;
  $('#importBtn').onclick = async () => { const r = await safe(() => S.instances.importPack()); if (r) { toast(`Imported ${r.name}`); await refreshPacks(); render(); } };
}

async function newPackModal() {
  if (!state.versions.length) state.versions = (await safe(() => S.fabricVersions())) || ['1.21.11'];
  const withSurge = (await safe(() => S.surgeVersions())) || ['1.21.11'];
  const ordered = withSurge.filter(v => state.versions.includes(v)).concat(state.versions.filter(v => !withSurge.includes(v)));
  const def = ordered.find(v => withSurge.includes(v)) || ordered[0];
  modal(`<h2>New modpack</h2>
    <div class="form">
      <label>Name<input class="input" id="npName" maxlength="40" placeholder="My modpack"></label>
      <label>Minecraft version${selectWrap(`<select class="input" id="npVer" style="width:100%">${ordered.map(v => `<option value="${esc(v)}" ${v === def ? 'selected' : ''}>${esc(v)}${withSurge.includes(v) ? '' : ' (no Surge menu)'}</option>`).join('')}</select>`)}</label>
      <label class="check"><input type="checkbox" id="npOpt" checked> Add performance mods</label>
    </div>
    <div class="modal-actions"><button class="btn" id="npCancel">Cancel</button><button class="btn primary" id="npCreate">Create</button></div>`,
  (card, close) => {
    $('#npName', card).focus();
    $('#npCancel', card).onclick = close;
    const create = async () => {
      const name = $('#npName', card).value.trim() || 'My modpack';
      $('#npCreate', card).disabled = true;
      const inst = await safe(() => S.instances.create({ name, mcVersion: $('#npVer', card).value, optimized: $('#npOpt', card).checked }));
      close();
      if (inst) { await refreshPacks(); openDetail(inst.id); }
    };
    $('#npCreate', card).onclick = create;
    $('#npName', card).onkeydown = e => { if (e.key === 'Enter') create(); };
  });
}

async function openDetail(id) {
  const filter = state.detail?.id === id ? state.detail.filter : '';
  state.detail = { id, filter, content: (await safe(() => S.instances.content(id))) || [] };
  if (state.view !== 'packs') go('packs', { keepDetail: true }); else render();
}

const TAB_LABEL = { mod: 'Mods', resourcepack: 'Texture packs', shader: 'Shaders' };
const DETAIL_TABS = { ...TAB_LABEL, worlds: 'Worlds', screenshots: 'Screenshots' };
const fileUrl = p => 'file:///' + p.replace(/\\/g, '/').split('/').filter(Boolean).map((seg, i) => (i === 0 && /^[A-Za-z]:$/.test(seg) ? seg : encodeURIComponent(seg))).join('/');
const ago = t => { const m = Math.round((Date.now() - t) / 60000); return m < 60 ? `${Math.max(1, m)} min ago` : m < 1440 ? `${Math.round(m / 60)} h ago` : `${Math.round(m / 1440)} days ago`; };

async function renderFilesTab(p) {
  const box = $('#dFiles');
  if (!box) return;
  const id = p.id;
  if (state.detailTab === 'worlds') {
    const list = (await S.worlds(id).catch(() => [])) || [];
    if (state.detailTab !== 'worlds' || !$('#dFiles')) return;
    box.innerHTML = list.length ? `<div class="panel content-list">${list.map((w, i) => `
      <div class="content-row">
        ${w.icon ? `<img src="${fileUrl(w.icon)}" alt="">` : '<div class="ph"></div>'}
        <div class="t"><div>${esc(w.folder)}</div><div class="muted small">Played ${ago(w.lastPlayed)}</div></div>
        <button class="btn primary sm" data-wplay="${i}">${I.play}Play</button>
        <button class="btn sm" data-wshow="${i}">Show</button>
        ${xBtn(`data-wdel="${i}" data-tip="Move to Recycle Bin"`)}
      </div>`).join('')}</div>` : '<div class="empty">No worlds yet. Worlds you create in this modpack show up here.</div>';
    box.querySelectorAll('[data-wplay]').forEach(b => b.onclick = () => { state.selected = p; S.config.set({ selectedInstance: id }); play({ world: list[+b.dataset.wplay].folder }); });
    box.querySelectorAll('[data-wshow]').forEach(b => b.onclick = () => S.files.show(id, list[+b.dataset.wshow].path));
    box.querySelectorAll('[data-wdel]').forEach(b => b.onclick = () => {
      const w = list[+b.dataset.wdel];
      modal(`<h2>Delete ${esc(w.folder)}?</h2><p class="muted" style="margin:0">The world goes to the Recycle Bin, so you can still restore it.</p>
        <div class="modal-actions"><button class="btn" id="no">Cancel</button><button class="btn danger" id="yes">Delete</button></div>`,
      (card, close) => { $('#no', card).onclick = close; $('#yes', card).onclick = async () => { await safe(() => S.files.trash(id, w.path), 'Moved to Recycle Bin'); close(); renderFilesTab(p); }; });
    });
  } else {
    const list = (await S.screenshots(id).catch(() => [])) || [];
    if (state.detailTab !== 'screenshots' || !$('#dFiles')) return;
    box.innerHTML = list.length ? `<div class="shots">${list.map((sh, i) => `
      <div class="shot" data-sopen="${i}"><img src="${fileUrl(sh.path)}" alt="" loading="lazy">
        <div class="shot-bar"><span class="small">${ago(sh.time)}</span>
          <button class="btn sm" data-scopy="${i}">Copy</button>${xBtn(`data-sdel="${i}" data-tip="Move to Recycle Bin"`)}</div></div>`).join('')}</div>`
      : '<div class="empty">No screenshots yet. Press F2 in game to take one.</div>';
    box.querySelectorAll('[data-sopen]').forEach(el => el.onclick = e => { if (!e.target.closest('button')) S.files.open(id, list[+el.dataset.sopen].path); });
    box.querySelectorAll('[data-scopy]').forEach(b => b.onclick = async () => { await safe(() => S.files.copyImage(id, list[+b.dataset.scopy].path), 'Screenshot copied'); });
    box.querySelectorAll('[data-sdel]').forEach(b => b.onclick = async () => { await safe(() => S.files.trash(id, list[+b.dataset.sdel].path)); renderFilesTab(p); });
  }
}

function renderDetail() {
  const p = state.packs.find(x => x.id === state.detail.id);
  if (!p) { state.detail = null; return renderPacks(); }
  const isContent = !!TAB_LABEL[state.detailTab];
  const q = (state.detail.filter || '').toLowerCase();
  const all = state.detail.content.filter(c => c.type === state.detailTab);
  const items = all.filter(c => !q || c.title.toLowerCase().includes(q) || c.file.toLowerCase().includes(q));
  const emptyText = !isContent ? '' : all.length ? 'No matches' : `No ${TAB_LABEL[state.detailTab].toLowerCase()} yet. Add some from Browse.`;
  view.innerHTML = `<div class="page">
    <button class="back" id="back">${I.back}Modpacks</button>
    <div class="detail-head">${img(p.icon, 'pack-icon')}
      <div style="min-width:0"><h1>${esc(p.name)}</h1>
        <div style="display:flex;gap:6px"><span class="chip accent">Fabric ${esc(p.mcVersion)}</span><span class="chip">${plural(modCount(p), 'mod')}</span></div></div>
      <div class="detail-actions">
        <button class="btn primary" id="dPlay">${I.play}Play</button>
        <button class="btn" id="dUpdates">Check for updates</button>
        <button class="btn" id="dOptimize" data-tip="Installs Sodium, Lithium and other FPS mods that are missing">Add performance mods</button>
        <button class="btn" id="dMore" aria-haspopup="menu">More</button>
      </div>
    </div>
    <div class="toolbar">
      <div class="seg" id="dTabs">${Object.entries(DETAIL_TABS).map(([k, v]) => `<button data-t="${k}" class="${state.detailTab === k ? 'on' : ''}">${v}</button>`).join('')}</div>
      ${isContent ? `<input class="input search" id="dFilter" placeholder="Search ${TAB_LABEL[state.detailTab].toLowerCase()}" value="${esc(state.detail.filter || '')}">
      <button class="btn" id="dAddFile">Add file</button>
      <button class="btn primary" id="dBrowse">${I.plus}Browse</button>` : `<div class="spacer"></div><button class="btn" id="dOpenDir">Open folder</button>`}
    </div>
    ${isContent ? '' : '<div id="dFiles"><div class="empty"><div class="bar indeterminate" style="width:180px;margin:0 auto"><div></div></div></div></div>'}
    <div class="panel content-list ${isContent ? '' : 'hidden'}">
      ${items.map(c => `
        <div class="content-row ${c.enabled ? '' : 'off'}">
          ${c.iconUrl ? `<img src="${esc(c.iconUrl)}" alt="" loading="lazy">` : '<div class="ph"></div>'}
          <div class="t"><div>${esc(c.title)}</div><div class="muted small">${esc(c.file)}</div></div>
          <button class="toggle ${c.enabled ? 'on' : ''}" data-toggle="${esc(c.file)}" aria-label="${c.enabled ? 'Disable' : 'Enable'}" data-tip="${c.enabled ? 'Disable' : 'Enable'}"></button>
          ${xBtn(`data-del="${esc(c.file)}" data-tip="Remove"`)}
        </div>`).join('') || `<div class="empty">${emptyText}</div>`}
    </div></div>`;
  const id = p.id;
  $('#back').onclick = () => { state.detail = null; render(); };
  $('#dPlay').onclick = () => { state.selected = p; S.config.set({ selectedInstance: id }); go('home'); play(); };
  $('#dMore').onclick = e => menu(e.currentTarget, [
    ['Open folder', () => S.instances.folder(id)],
    ['Export as .mrpack', async () => { const r = await safe(() => S.instances.exportPack(id)); if (r) toast('Exported'); }],
    ['Edit', () => editPackModal(p)],
    ['Delete', () => confirmDelete(), 'danger'],
  ]);
  $('#dOptimize').onclick = () => safe(() => S.instances.optimize(id));
  $('#dUpdates').onclick = async () => {
    const b = $('#dUpdates'); b.disabled = true; b.textContent = 'Checking';
    const list = await safe(() => S.updates.check(id));
    b.disabled = false; b.textContent = 'Check for updates';
    if (!list) return;
    if (!list.length) { toast('Every mod is up to date'); return; }
    modal(`<h2>${plural(list.length, 'update')} available</h2>
      <div class="pick-list" style="max-height:320px;overflow:auto">${list.map(u => `
        <div class="update-row"><div class="grow"><div>${esc(u.title)}</div><div class="muted small">${esc(u.file)} to ${esc(u.version)}</div></div></div>`).join('')}</div>
      <div class="modal-actions"><button class="btn" id="upLater">Not now</button><button class="btn primary" id="upAll">Update all</button></div>`,
    (card, close) => {
      $('#upLater', card).onclick = close;
      $('#upAll', card).onclick = async () => { close(); await safe(() => S.updates.apply(id, list)); await refreshPacks(); openDetail(id); };
    });
  };
  const confirmDelete = () => modal(`<h2>Delete ${esc(p.name)}?</h2><p class="muted" style="margin:0">Its mods, settings and worlds are deleted too.</p>
    <div class="modal-actions"><button class="btn" id="no">Cancel</button><button class="btn danger" id="yes">Delete</button></div>`,
  (card, close) => { $('#no', card).onclick = close; $('#yes', card).onclick = async () => { await safe(() => S.instances.remove(id), 'Deleted'); close(); state.detail = null; await refreshPacks(); render(); }; });
  view.querySelectorAll('#dTabs [data-t]').forEach(b => b.onclick = () => { state.detailTab = b.dataset.t; render(); });
  if (!isContent) {
    renderFilesTab(p);
    $('#dOpenDir').onclick = () => S.instances.folder(id);
    return;
  }
  $('#dFilter').oninput = e => {
    state.detail.filter = e.target.value;
    const pos = e.target.selectionStart;
    renderDetail();
    const f = $('#dFilter'); f.focus(); f.setSelectionRange(pos, pos);
  };
  $('#dAddFile').onclick = async () => { const n = await safe(() => S.instances.addFiles(id, state.detailTab)); if (n) openDetail(id); };
  $('#dBrowse').onclick = () => {
    Object.assign(state.browse, { target: id, type: state.detailTab, offset: 0, results: null });
    go('browse');
  };
  view.querySelectorAll('[data-toggle]').forEach(b => b.onclick = async () => { await safe(() => S.instances.toggle(id, state.detailTab, b.dataset.toggle)); openDetail(id); });
  view.querySelectorAll('[data-del]').forEach(b => b.onclick = async () => { await safe(() => S.instances.removeContent(id, state.detailTab, b.dataset.del)); await refreshPacks(); openDetail(id); });
}

function editPackModal(p) {
  modal(`<h2>Edit modpack</h2>
    <div class="form">
      <label>Name<input class="input" id="eName" value="${esc(p.name)}" maxlength="40"></label>
      <label>Memory in MB, leave empty for the default<input class="input" id="eRam" type="number" min="1024" step="512" value="${p.ramMb || ''}"></label>
      <label class="check"><input type="checkbox" id="eSurge" ${p.surgeMod !== false ? 'checked' : ''}> Include the Surge in-game menu</label>
    </div>
    <div class="modal-actions"><button class="btn" id="eCancel">Cancel</button><button class="btn primary" id="eSave">Save</button></div>`,
  (card, close) => {
    $('#eCancel', card).onclick = close;
    $('#eSave', card).onclick = async () => {
      const ram = parseInt($('#eRam', card).value, 10);
      await safe(() => S.instances.update(p.id, { name: $('#eName', card).value.trim() || p.name, ramMb: Number.isFinite(ram) ? ram : null, surgeMod: $('#eSurge', card).checked }), 'Saved');
      close(); await refreshPacks(); render();
    };
  });
}

function skinFront(dataUrl, canvas) {
  const im = new Image();
  im.onload = () => {
    const g = canvas.getContext('2d');
    canvas.width = 16; canvas.height = 32;
    g.imageSmoothingEnabled = false;
    const tall = im.height >= 64;
    const part = (sx, sy, w, h, dx, dy, mirror) => {
      if (mirror) { g.save(); g.translate(dx + w, dy); g.scale(-1, 1); g.drawImage(im, sx, sy, w, h, 0, 0, w, h); g.restore(); }
      else g.drawImage(im, sx, sy, w, h, dx, dy, w, h);
    };
    part(8, 8, 8, 8, 4, 0); part(20, 20, 8, 12, 4, 8);
    part(44, 20, 4, 12, 0, 8); part(4, 20, 4, 12, 4, 20);
    if (tall) { part(36, 52, 4, 12, 12, 8); part(20, 52, 4, 12, 8, 20); }
    else { part(44, 20, 4, 12, 12, 8, true); part(4, 20, 4, 12, 8, 20, true); }
    part(40, 8, 8, 8, 4, 0);
    if (tall) { part(20, 36, 8, 12, 4, 8); part(44, 36, 4, 12, 0, 8); part(52, 52, 4, 12, 12, 8); part(4, 36, 4, 12, 4, 20); part(4, 52, 4, 12, 8, 20); }
  };
  im.src = dataUrl;
}

let skinViewer = null;
const ANIM_CAPES = { phoenix: [20, 90], koi: [24, 120], bloodmoon: [20, 120], obsidian: [16, 110], lava: [20, 110], matrix: [20, 80], storm: [24, 110], frost: [16, 130], flame: [16, 70], enchanted: [16, 90], aurora: [16, 140], emerald: [16, 110], galaxy: [24, 120], midnight: [20, 160], ocean: [16, 120], royal: [16, 110], sakura: [20, 130], surge: [16, 110], void: [16, 120], blinkcat: [2, 0] };
const HALO_COLORS = { angel: 0xffec9a, surge: 0xc66bff, fire: 0xff8c28, enchanted: 0x78c8ff, void: 0x6a2bd9, rose: 0xff6fae };
const WARD_TABS = [['cape', 'Capes'], ['wings', 'Wings'], ['halo', 'Halos'], ['emote', 'Emotes'], ['idle', 'Idles']];
const ward = { data: null, tab: 'cape', preview: null, elytra: false, viewer: null, raf: 0, imgs: {}, capeCanvas: null, capeKey: '', frame: -1, extras: [] };

function disposeWardrobe() {
  cancelAnimationFrame(ward.raf);
  if (ward.viewer) { ward.viewer.dispose(); ward.viewer = null; }
}

function wardImg(src) {
  if (ward.imgs[src]) return ward.imgs[src];
  const img = new Image();
  img.src = src;
  ward.imgs[src] = img;
  return img;
}

function blinkFrame(now) {
  const cycle = Math.floor(now / 11000), at = 1500 + ((cycle * 2654435761) >>> 0) % 7500, d = now % 11000 - at;
  return d >= 0 && d < 270 ? 1 : 0;
}

function hexToRgb(h) { const n = parseInt(String(h || '8F50F2'), 16); return [(n >> 16) & 255, (n >> 8) & 255, n & 255]; }

function drawGenerated(g, key, data) {
  const w = 512, h = 256;
  g.clearRect(0, 0, w, h);
  if (key === 'custom') {
    const [r, gg, b] = hexToRgb(data.capeColor);
    g.fillStyle = `rgb(${r},${gg},${b})`; g.fillRect(0, 0, w, h);
  } else if (key === 'runes' || key === 'beta') {
    g.fillStyle = key === 'runes' ? '#111114' : '#141418'; g.fillRect(0, 0, w, h);
    g.fillStyle = '#fff';
    const t = Date.now() / 600;
    for (let r = 0; r < 8; r++) for (let c = 0; c < 5; c++) {
      g.globalAlpha = key === 'runes' ? 0.45 + 0.55 * (0.5 + 0.5 * Math.cos(t - r * 0.8)) : 0;
      const x = 12 + c * 16, y = 12 + r * 16;
      g.fillRect(x, y, 2, 10); g.fillRect(x, y, 8, 2); if ((r + c) % 2) g.fillRect(x + 6, y + 4, 2, 6); else g.fillRect(x + 2, y + 8, 6, 2);
    }
    g.globalAlpha = 1;
    if (key === 'beta') { g.font = 'bold 22px Monocraft, monospace'; g.textAlign = 'center'; g.fillText('BETA', 48, 80); }
  } else if (key === 'image') {
    const acc = activeAccount();
    const img = acc && wardImg(`${state.cfg.apiUrl || 'https://surge-client.remvuxd.workers.dev'}/v1/cape/${acc.uuid.replace(/-/g, '')}`);
    g.fillStyle = '#1b1b1f'; g.fillRect(0, 0, w, h);
    if (img && img.complete && img.naturalWidth) {
      if (img.naturalWidth === img.naturalHeight * 2) g.drawImage(img, 0, 0, w, h);
      else { const k = Math.max(80 / img.naturalWidth, 128 / img.naturalHeight); const sw = 80 / k, sh = 128 / k; g.drawImage(img, (img.naturalWidth - sw) / 2, (img.naturalHeight - sh) / 2, sw, sh, 8, 8, 80, 128); }
    }
  }
}

function capeFrameCanvas(key, data) {
  if (!ward.capeCanvas) { ward.capeCanvas = document.createElement('canvas'); ward.capeCanvas.width = 512; ward.capeCanvas.height = 256; }
  const g = ward.capeCanvas.getContext('2d');
  const now = Date.now();
  const anim = ANIM_CAPES[key];
  let frame = 0;
  if (anim) frame = anim[1] === 0 ? blinkFrame(now) : Math.floor(now / anim[1]) % anim[0];
  if (['custom', 'runes', 'beta', 'image'].includes(key)) { drawGenerated(g, key, data); return { canvas: ward.capeCanvas, changed: key === 'runes' || key !== ward.capeKey || key === 'image' }; }
  const img = wardImg(`assets/cosmetics/cape_${key}.png`);
  if (!img.complete || !img.naturalWidth) return { canvas: null, changed: false };
  if (key === ward.capeKey && frame === ward.frame) return { canvas: ward.capeCanvas, changed: false };
  g.clearRect(0, 0, 512, 256);
  const fh = img.naturalWidth / 2;
  g.drawImage(img, 0, frame * fh, img.naturalWidth, fh, 0, 0, 512, 256);
  ward.frame = frame;
  return { canvas: ward.capeCanvas, changed: true };
}

function wardThumb(item, data) {
  const c = document.createElement('canvas');
  const g = c.getContext('2d');
  g.imageSmoothingEnabled = true;
  if (item.type === 'cape') {
    c.width = 80; c.height = 128;
    if (['custom', 'runes', 'beta', 'image'].includes(item.key)) {
      const t = document.createElement('canvas'); t.width = 512; t.height = 256;
      drawGenerated(t.getContext('2d'), item.key, data);
      g.drawImage(t, 8, 8, 80, 128, 0, 0, 80, 128);
    } else {
      const img = wardImg(`assets/cosmetics/cape_${item.key}.png`);
      const draw = () => g.drawImage(img, 8, 8, 80, 128, 0, 0, 80, 128);
      if (img.complete && img.naturalWidth) draw(); else img.addEventListener('load', draw, { once: true });
    }
  } else if (item.type === 'wings') {
    c.width = 256; c.height = 128;
    const img = wardImg(`assets/cosmetics/wings_${item.key}.png`);
    const draw = () => { g.save(); g.translate(128, 0); g.drawImage(img, 0, 0, 128, 128 * img.naturalHeight / img.naturalWidth * 2); g.scale(-1, 1); g.drawImage(img, 0, 0, 128, 128 * img.naturalHeight / img.naturalWidth * 2); g.restore(); };
    if (img.complete && img.naturalWidth) draw(); else img.addEventListener('load', draw, { once: true });
  } else if (item.type === 'halo') {
    c.width = 192; c.height = 128;
    const col = '#' + (HALO_COLORS[item.key] || 0xffffff).toString(16).padStart(6, '0');
    g.shadowColor = col; g.shadowBlur = 28; g.strokeStyle = col; g.lineWidth = 12;
    g.beginPath(); g.ellipse(96, 64, 68, 24, 0, 0, Math.PI * 2); g.stroke();
  } else {
    c.width = 128; c.height = 128;
    g.fillStyle = '#e4e4e7'; g.font = '60px Monocraft, monospace'; g.textAlign = 'center'; g.textBaseline = 'middle';
    g.fillText(item.name.slice(0, 1), 64, 68);
  }
  return c;
}

function wardExtras(data) {
  const T = window.THREE, v = ward.viewer;
  if (!T || !v) return;
  for (const o of ward.extras) o.parent?.remove(o);
  ward.extras = [];
  const look = ward.preview || {};
  const wings = look.wings !== undefined ? look.wings : data.equipped.wings;
  const halo = look.halo !== undefined ? look.halo : data.equipped.halo;
  if (wings && wings !== 'none' && !ward.elytra) {
    const tex = new T.TextureLoader().load(`assets/cosmetics/wings_${wings}.png`);
    tex.magFilter = T.LinearFilter;
    for (const side of [-1, 1]) {
      const pivot = new T.Group();
      pivot.position.set(side * 1, 6, -2.6);
      const mat = new T.MeshLambertMaterial({ map: tex, transparent: true, side: T.DoubleSide, alphaTest: 0.05, emissive: 0x111111 });
      const mesh = new T.Mesh(new T.PlaneGeometry(18, 9), mat);
      mesh.position.set(side * 9, -2, 0);
      mesh.scale.x = side;
      pivot.add(mesh);
      pivot.rotation.y = side * -0.55;
      pivot.rotation.z = side * 0.22;
      pivot.userData.side = side;
      v.playerObject.add(pivot);
      ward.extras.push(pivot);
    }
  }
  if (halo && halo !== 'none') {
    const color = HALO_COLORS[halo] || 0xffffff;
    const ring = new T.Mesh(new T.TorusGeometry(4.2, 0.5, 10, 40), new T.MeshBasicMaterial({ color }));
    ring.rotation.x = Math.PI / 2 - 0.15;
    ring.position.y = 10.5;
    const glow = new T.Mesh(new T.RingGeometry(3.2, 5.8, 40), new T.MeshBasicMaterial({ color, transparent: true, opacity: 0.25, side: T.DoubleSide, depthWrite: false }));
    glow.rotation.x = -Math.PI / 2 - 0.15;
    glow.position.y = 10.5;
    v.playerObject.skin.head.add(ring); v.playerObject.skin.head.add(glow);
    ward.extras.push(ring, glow);
  }
}

function crisp(v) {
  const T = window.THREE, tx = v.capeTexture;
  if (!T || !tx) return;
  tx.magFilter = T.NearestFilter;
  tx.minFilter = T.LinearMipmapLinearFilter;
  tx.generateMipmaps = true;
  try { tx.anisotropy = v.renderer.capabilities.getMaxAnisotropy(); } catch { }
  tx.needsUpdate = true;
}

async function mountWardrobeViewer(data) {
  disposeWardrobe();
  const canvas = $('#wdCanvas');
  if (!canvas || !window.skinview3d) return;
  const box = canvas.parentElement.getBoundingClientRect();
  const v = ward.viewer = new skinview3d.SkinViewer({ canvas, width: box.width, height: box.height, zoom: 0.8, fov: 40 });
  v.pixelRatio = Math.max(2, window.devicePixelRatio || 1);
  v.animation = emoteAnimation(() => ward.emote || (ward.data && ward.data.equipped.idle && ward.data.equipped.idle !== 'none' ? ward.data.equipped.idle : null));
  v.autoRotate = true;
  v.autoRotateSpeed = 0.55;
  v.controls.enableZoom = false;
  v.playerObject.rotation.y = Math.PI;
  const acc = activeAccount();
  let ok = false;
  for (const u of acc ? [`https://mc-heads.net/skin/${acc.uuid}`, `https://crafatar.com/skins/${acc.uuid}`] : []) { try { await v.loadSkin(u); ok = true; break; } catch { } }
  if (!ward.viewer) return;
  if (!ok) v.loadSkin(unknownSkin());
  ward.capeKey = ''; ward.frame = -1;
  wardExtras(data);
  const loop = () => {
    if (!ward.viewer) return;
    const look = ward.preview || {};
    const cape = look.cape !== undefined ? look.cape : data.equipped.cape;
    if (cape && cape !== 'none') {
      const { canvas: cc, changed } = capeFrameCanvas(cape, data);
      if (cc && (changed || ward.capeKey !== cape)) {
        v.loadCape(cc, { backEquipment: ward.elytra ? 'elytra' : 'cape' });
        crisp(v);
        ward.capeKey = cape;
      }
    } else if (ward.capeKey) { v.loadCape(null); ward.capeKey = ''; }
    const t = performance.now() / 1000;
    for (const e of ward.extras) if (e.userData.side) e.rotation.y = e.userData.side * (-0.55 - Math.sin(t * 1.6) * 0.12);
    ward.raf = requestAnimationFrame(loop);
  };
  ward.raf = requestAnimationFrame(loop);
}

function showGiftModal(title, coins, items, note) {
  modal(`<h2>${esc(title)}</h2>${note ? `<p class="muted" style="margin:0 0 10px">${esc(note)}</p>` : ''}
    <div class="gift-list">${coins > 0 ? `<div class="gift-row"><b>+${coins}</b> Surge Coins</div>` : ''}${items.map(i => `<div class="gift-row">${esc(i.name)}</div>`).join('')}</div>
    <div style="display:flex;justify-content:flex-end;margin-top:14px"><button class="btn primary" id="gOk">Awesome</button></div>`, (card, close) => { card.querySelector('#gOk').onclick = close; });
}

async function renderWardrobe() {
  view.innerHTML = `<div class="page">
    <div class="view-head"><h1 class="view-title">Wardrobe</h1>
      <div class="ward-top"><span class="chip accent" id="wdCoins">… coins</span>
        <input class="input" id="wdCode" placeholder="Redeem a code" style="width:170px" aria-label="Code">
        <button class="btn" id="wdRedeem">Redeem</button></div></div>
    <div class="skins-layout">
      <div class="panel skin-preview">
        <div class="skin-stage"><canvas id="wdCanvas"></canvas></div>
        <div class="skin-actions"><div class="seg" id="wdBack"><button data-v="cape" class="${ward.elytra ? '' : 'on'}">Cape</button><button data-v="elytra" class="${ward.elytra ? 'on' : ''}">Elytra</button></div></div>
        <div class="muted small" id="wdNote" style="text-align:center">Changes show the next time you play</div>
      </div>
      <div class="skins-side">
        <div class="seg" id="wdTabs">${WARD_TABS.map(([k, l]) => `<button data-t="${k}" class="${ward.tab === k ? 'on' : ''}">${l}</button>`).join('')}</div>
        <div class="ward-grid" id="wdGrid"><div class="muted small">Loading…</div></div>
      </div>
    </div></div>`;
  const data = await safe(() => S.wardrobe.get());
  if (state.view !== 'wardrobe') return;
  if (!data) { $('#wdGrid').innerHTML = '<div class="muted small">Your wardrobe could not be loaded</div>'; return; }
  ward.data = data;
  if (data.playing) $('#wdNote').textContent = 'Minecraft is open. Use the Wardrobe in the game, or close it to change things here.';
  if ((data.gifts || []).length) {
    const coins = data.gifts.reduce((a, g) => a + (g.coins || 0), 0), items = data.gifts.flatMap(g => g.items), note = data.gifts.map(g => g.note).filter(Boolean).join(' ');
    showGiftModal(items.length === 1 ? `You've been gifted ${items[0].name}!` : items.length ? `You've been gifted ${items.length} items!` : "You've been gifted coins!", coins, items, note);
  }
  view.querySelectorAll('#wdTabs [data-t]').forEach(b => b.onclick = () => { ward.tab = b.dataset.t; ward.preview = null; view.querySelectorAll('#wdTabs button').forEach(x => x.classList.toggle('on', x === b)); drawWardGrid(); });
  view.querySelectorAll('#wdBack [data-v]').forEach(b => b.onclick = () => { ward.elytra = b.dataset.v === 'elytra'; view.querySelectorAll('#wdBack button').forEach(x => x.classList.toggle('on', x === b)); ward.capeKey = ''; wardExtras(ward.data); });
  const redeem = async () => {
    const code = $('#wdCode').value.trim();
    if (!code) return;
    const r = await safe(() => S.wardrobe.redeem(code));
    if (!r) return;
    $('#wdCode').value = '';
    ward.data = r;
    showGiftModal('Code redeemed!', r.redeemed.coins, r.redeemed.items, '');
    drawWardGrid();
  };
  $('#wdRedeem').onclick = redeem;
  $('#wdCode').onkeydown = e => { if (e.key === 'Enter' && !e.repeat) redeem(); };
  drawWardGrid();
  mountWardrobeViewer(data);
}

function drawWardGrid() {
  const data = ward.data, grid = $('#wdGrid');
  if (!data || !grid) return;
  $('#wdCoins').textContent = `${data.coins.toLocaleString()} coins`;
  const items = data.items.filter(i => i.type === ward.tab && (!i.locked || i.owned || i.id === 'cape:beta'));
  grid.innerHTML = '';
  for (const it of items) {
    const card = document.createElement('div');
    const equipped = it.type === 'emote' ? data.wheel.includes(it.key) : data.equipped[it.type] === it.key;
    card.className = `ward-card${equipped ? ' on' : ''}`;
    const thumb = wardThumb(it, data);
    const price = it.discount ? `<s>${it.base}</s> ${it.price}` : `${it.price}`;
    const status = it.owned ? (equipped ? (it.type === 'emote' ? 'On your wheel' : 'Wearing') : 'Owned') : it.locked ? (it.id === 'cape:beta' ? 'Beta testers only' : 'Not for sale') : `${price} coins`;
    let action = '';
    if (it.owned) action = it.type === 'emote' ? (equipped ? 'Take off wheel' : 'Put on wheel') : it.type === 'idle' ? (equipped ? 'Stop using' : 'Use') : (equipped ? 'Take off' : 'Wear');
    else if (!it.locked) action = 'Buy';
    card.innerHTML = `<div class="ward-thumb"></div><div class="ward-name">${esc(it.name)}</div><div class="ward-status${it.discount && !it.owned ? ' sale' : ''}">${status}</div>${action ? `<button class="btn sm ${action === 'Buy' ? 'primary' : ''}">${action}</button>` : ''}`;
    card.querySelector('.ward-thumb').appendChild(thumb);
    if (['cape', 'wings', 'halo'].includes(it.type)) card.onmouseenter = () => { ward.preview = { [it.type]: it.key }; ward.capeKey = '-'; wardExtras(data); };
    else card.onmouseenter = () => { ward.emote = it.key; };
    card.onmouseleave = () => { ward.preview = null; ward.emote = null; ward.capeKey = '-'; wardExtras(ward.data); };
    const btn = card.querySelector('button');
    if (btn) btn.onclick = async () => {
      let r;
      if (action === 'Buy') {
        r = await safe(() => S.wardrobe.buy(it.id), `Unlocked ${it.name}`);
        if (r) sfx('coin');
        if (r && it.type !== 'emote') r = (await safe(() => S.wardrobe.equip(it.type, it.key))) || r;
      } else if (it.type === 'emote') r = await safe(() => S.wardrobe.wheel(it.key));
      else r = await safe(() => S.wardrobe.equip(it.type, equipped ? 'none' : it.key));
      if (r) { ward.data = r; ward.preview = null; ward.capeKey = '-'; wardExtras(r); drawWardGrid(); }
    };
    grid.appendChild(card);
  }
  if (!items.length) grid.innerHTML = '<div class="muted small">Nothing here yet</div>';
}

function disposeSkinPreview() { if (skinViewer) { skinViewer.dispose(); skinViewer = null; } }
const skinState = { selected: null, variant: 'classic', profile: null, library: [] };

async function renderSkins() {
  const acc = activeAccount();
  view.innerHTML = `<div class="page">
    <div class="view-head"><h1 class="view-title">Skins</h1>
      <div style="display:flex;gap:8px">
        <button class="btn" id="skFolder">Open folder</button>
        ${acc ? '<button class="btn" id="skSave">Save current skin</button>' : ''}
        <button class="btn primary" id="skImport">${I.plus}Import skin</button>
      </div></div>
    <div class="skins-layout">
      <div class="panel skin-preview">
        <div class="skin-stage"><canvas id="skCanvas"></canvas></div>
        <div class="skin-actions">
          <div class="seg" id="skVariant"><button data-v="classic" class="${skinState.variant === 'classic' ? 'on' : ''}">Classic</button><button data-v="slim" class="${skinState.variant === 'slim' ? 'on' : ''}">Slim</button></div>
          <button class="btn primary" id="skApply" ${acc ? '' : 'disabled'}>Apply skin</button>
        </div>
        ${acc ? '' : '<div class="muted small" style="text-align:center">Sign in to change your skin and cape</div>'}
      </div>
      <div class="skins-side">
        <div class="skin-grid" id="skGrid"></div>
        <h2 class="section-title">Cape</h2>
        <div class="cape-row" id="skCapes">${acc ? '<span class="muted small">Loading capes…</span>' : '<span class="muted small">Sign in to see your capes</span>'}</div>
      </div>
    </div></div>`;
  disposeSkinPreview();
  const canvas = $('#skCanvas');
  const box = canvas.parentElement.getBoundingClientRect();
  skinViewer = new skinview3d.SkinViewer({ canvas, width: box.width, height: box.height, zoom: 0.85, fov: 40 });
  skinViewer.animation = new skinview3d.IdleAnimation();
  skinViewer.autoRotate = true;
  skinViewer.autoRotateSpeed = 0.6;
  skinViewer.controls.enableZoom = false;
  skinViewer.loadSkin(unknownSkin());

  $('#skFolder').onclick = () => S.skins.folder();
  $('#skImport').onclick = async () => { const lib = await safe(() => S.skins.importFiles()); if (lib) { skinState.library = lib; drawSkinGrid(); } };
  if ($('#skSave')) $('#skSave').onclick = async () => { const lib = await safe(() => S.skins.saveCurrent(), 'Saved your current skin'); if (lib) { skinState.library = lib; drawSkinGrid(); } };
  view.querySelectorAll('#skVariant [data-v]').forEach(b => b.onclick = () => {
    skinState.variant = b.dataset.v;
    if (skinState.selected) rememberVariant(skinState.selected, skinState.variant);
    view.querySelectorAll('#skVariant button').forEach(x => x.classList.toggle('on', x === b));
    showSelectedSkin();
  });
  $('#skApply').onclick = async () => {
    if (!skinState.selected) { toast('Pick a skin from your library first', true); return; }
    const b = $('#skApply'); b.disabled = true; b.textContent = 'Applying';
    const entry = skinState.library.find(x => x.name === skinState.selected);
    if (entry && skinState.variant !== 'slim' && await detectSlim(entry.data)) { skinState.variant = 'slim'; rememberVariant(entry.name, 'slim'); setVariantButtons(); showSelectedSkin(); toast('This skin has thin arms, so it was set to Slim'); }
    const p = await safe(() => S.skins.upload(skinState.selected, skinState.variant), 'Skin changed. It shows in game after you rejoin.');
    b.disabled = false; b.textContent = 'Apply skin';
    if (p) { skinState.profile = p; refreshAccounts(); }
  };

  skinState.library = (await safe(() => S.skins.library())) || [];
  if (state.view !== 'skins') return;
  drawSkinGrid();
  if (acc) {
    skinState.profile = await S.skins.profile().catch(() => null);
    if (state.view !== 'skins') return;
    drawCapes();
    const cur = await S.skins.current().catch(() => null);
    if (state.view !== 'skins') return;
    if (cur && cur.name) {
      skinState.library = cur.library;
      skinState.selected = cur.name;
      skinState.variant = cur.variant;
      rememberVariant(cur.name, cur.variant);
      setVariantButtons();
      drawSkinGrid();
      showSelectedSkin();
    } else if (!skinState.selected && skinState.profile?.skin) {
      skinState.variant = skinState.profile.skin.variant === 'SLIM' ? 'slim' : 'classic';
      setVariantButtons();
      skinViewer?.loadSkin(skinState.profile.skin.url, { model: skinState.variant === 'slim' ? 'slim' : 'default' }).catch(() => {});
    }
  }
}

function setVariantButtons() {
  document.querySelectorAll('#skVariant button').forEach(x => x.classList.toggle('on', x.dataset.v === skinState.variant));
}
function rememberVariant(name, v) {
  try { const m = JSON.parse(localStorage.getItem('surge.skinVariants') || '{}'); m[name] = v; localStorage.setItem('surge.skinVariants', JSON.stringify(m)); } catch { }
}
function rememberedVariant(name) {
  try { return JSON.parse(localStorage.getItem('surge.skinVariants') || '{}')[name] || null; } catch { return null; }
}
function detectSlim(dataUrl) {
  return new Promise(res => {
    const img = new Image();
    img.onload = () => {
      if (img.height < 64) return res(false);
      const c = document.createElement('canvas'); c.width = 64; c.height = 64;
      const g = c.getContext('2d'); g.drawImage(img, 0, 0);
      const d = g.getImageData(0, 0, 64, 64).data;
      const empty = (x, y) => { const i = (y * 64 + x) * 4; return d[i + 3] === 0 || (d[i] === 0 && d[i + 1] === 0 && d[i + 2] === 0); };
      let n = 0, hit = 0;
      for (let y = 20; y < 32; y++) for (const x of [54, 55]) { n++; if (empty(x, y)) hit++; }
      for (let y = 52; y < 64; y++) for (const x of [46, 47]) { n++; if (empty(x, y)) hit++; }
      res(hit >= n * 0.9);
    };
    img.onerror = () => res(false);
    img.src = dataUrl;
  });
}

function showSelectedSkin() {
  const s = skinState.library.find(x => x.name === skinState.selected);
  if (s && skinViewer) skinViewer.loadSkin(s.data, { model: skinState.variant === 'slim' ? 'slim' : 'default' });
}

function drawSkinGrid() {
  const grid = $('#skGrid');
  if (!grid) return;
  grid.innerHTML = skinState.library.map(s => `
    <div class="skin-card ${s.name === skinState.selected ? 'on' : ''}" data-skin="${esc(s.name)}">
      <canvas></canvas>
      <span class="skin-name">${esc(s.name.replace(/\.png$/i, ''))}</span>
      <button class="btn sm x-btn" data-del="${esc(s.name)}" aria-label="Delete">&times;</button>
    </div>`).join('') || '<div class="empty" style="padding:30px 10px">Import a skin PNG to start your library.</div>';
  grid.querySelectorAll('[data-skin]').forEach(card => {
    skinFront(skinState.library.find(x => x.name === card.dataset.skin).data, $('canvas', card));
    card.onclick = e => {
      if (e.target.closest('[data-del]')) return;
      skinState.selected = card.dataset.skin;
      grid.querySelectorAll('.skin-card').forEach(c => c.classList.toggle('on', c === card));
      const remembered = rememberedVariant(skinState.selected);
      const pick = v => { skinState.variant = v; setVariantButtons(); showSelectedSkin(); };
      if (remembered) pick(remembered);
      else detectSlim(skinState.library.find(x => x.name === skinState.selected).data).then(slim => { const v = slim ? 'slim' : 'classic'; rememberVariant(skinState.selected, v); pick(v); });
    };
  });
  grid.querySelectorAll('[data-del]').forEach(b => b.onclick = async () => {
    const lib = await safe(() => S.skins.remove(b.dataset.del));
    if (lib) { if (skinState.selected === b.dataset.del) skinState.selected = null; skinState.library = lib; drawSkinGrid(); }
  });
}

function drawCapes() {
  const row = $('#skCapes');
  const p = skinState.profile;
  if (!row) return;
  if (!p) { row.innerHTML = '<span class="muted small">Couldn\'t load your capes</span>'; return; }
  if (!p.capes.length) { row.innerHTML = '<span class="muted small">This account has no capes</span>'; return; }
  const active = p.capes.find(c => c.active);
  row.innerHTML = `<button class="btn sm ${!active ? 'primary' : ''}" data-cape="">No cape</button>` +
    p.capes.map(c => `<button class="btn sm ${c.active ? 'primary' : ''}" data-cape="${esc(c.id)}">${esc(c.name)}</button>`).join('');
  if (active && skinViewer) skinViewer.loadCape(active.url).catch(() => {});
  row.querySelectorAll('[data-cape]').forEach(b => b.onclick = async () => {
    const np = await safe(() => S.skins.cape(b.dataset.cape || null), 'Cape changed');
    if (np) { skinState.profile = np; drawCapes(); if (!b.dataset.cape && skinViewer) skinViewer.loadCape(null); }
  });
}

const MC_COLORS = { 0: '#000000', 1: '#0000aa', 2: '#00aa00', 3: '#00aaaa', 4: '#aa0000', 5: '#aa00aa', 6: '#ffaa00', 7: '#aaaaaa', 8: '#555555', 9: '#5555ff', a: '#55ff55', b: '#55ffff', c: '#ff5555', d: '#ff55ff', e: '#ffff55', f: '#ffffff' };
function motdHtml(text) {
  let color = '#aaaaaa', bold = false, out = '';
  const parts = String(text).split(/(§#[0-9a-fA-F]{6}|§[0-9a-fk-orA-FK-OR])/);
  for (const part of parts) {
    if (/^§#/.test(part)) { color = `#${part.slice(2)}`; continue; }
    if (/^§/.test(part)) {
      const c = part[1].toLowerCase();
      if (MC_COLORS[c]) { color = MC_COLORS[c]; bold = false; } else if (c === 'l') bold = true; else if (c === 'r') { color = '#aaaaaa'; bold = false; }
      continue;
    }
    if (part) out += `<span style="color:${color}${bold ? ';font-weight:700' : ''}">${esc(part)}</span>`;
  }
  return out.replace(/\n/g, '<br>');
}
const pingCache = new Map();
function pingBars(ms) {
  const n = ms == null ? 0 : ms < 80 ? 5 : ms < 150 ? 4 : ms < 250 ? 3 : ms < 400 ? 2 : 1;
  return `<span class="bars" data-tip="${ms == null ? 'Offline' : `${ms} ms`}">${[1, 2, 3, 4, 5].map(i => `<i class="${i <= n ? 'on' : ''}" style="height:${3 + i * 3}px"></i>`).join('')}</span>`;
}

function renderServers() {
  const list = state.cfg.servers || [];
  view.innerHTML = `<div class="page">
    <div class="view-head"><h1 class="view-title">Servers</h1>
      <div style="display:flex;gap:8px">
        <button class="btn" id="svRefresh">Refresh</button>
        <button class="btn primary" id="svAdd">${I.plus}Add server</button>
      </div></div>
    <div class="toolbar"><span class="muted small">Joins with</span>${selectWrap(`<select class="input" id="svPack" aria-label="Modpack">${state.packs.map(p => `<option value="${esc(p.id)}" ${p.id === state.selected?.id ? 'selected' : ''}>${esc(p.name)}</option>`).join('')}</select>`)}</div>
    <div class="servers">${list.map((sv, i) => `
      <div class="panel server" data-i="${i}">
        <img class="server-icon" id="svIcon${i}" src="${ICON}" alt="">
        <div class="server-info">
          <div class="server-top"><span class="server-name">${esc(sv.name)}</span><span class="muted small" id="svPlayers${i}"></span></div>
          <div class="server-motd" id="svMotd${i}"><span class="muted">Pinging…</span></div>
        </div>
        <span id="svPing${i}">${pingBars(null)}</span>
        <button class="btn primary" data-join="${i}">${I.play}Join</button>
        <button class="btn sm" data-edit="${i}">Edit</button>
        ${xBtn(`data-del="${i}" data-tip="Remove"`)}
      </div>`).join('') || `<div class="empty">Add the servers you play on to see who's online and join in one click.<br><button class="btn primary" id="svAddEmpty">${I.plus}Add server</button></div>`}
    </div></div>`;
  const saveList = async l => { state.cfg = { ...state.cfg, ...(await safe(() => S.config.set({ servers: l }))) }; };
  $('#svAdd').onclick = () => serverModal();
  if ($('#svAddEmpty')) $('#svAddEmpty').onclick = () => serverModal();
  $('#svRefresh').onclick = () => { pingCache.clear(); renderServers(); };
  if ($('#svPack')) $('#svPack').onchange = e => { state.selected = state.packs.find(p => p.id === e.target.value); S.config.set({ selectedInstance: state.selected.id }); };
  view.querySelectorAll('[data-join]').forEach(b => b.onclick = () => play({ server: list[+b.dataset.join].address }));
  view.querySelectorAll('[data-edit]').forEach(b => b.onclick = () => serverModal(+b.dataset.edit));
  view.querySelectorAll('[data-del]').forEach(b => b.onclick = async () => { const l = list.filter((_, i) => i !== +b.dataset.del); await saveList(l); renderServers(); });
  list.forEach(async (sv, i) => {
    let r = pingCache.get(sv.address);
    if (!r) { r = (await S.servers.ping(sv.address).catch(() => null)) || { online: false }; pingCache.set(sv.address, r); }
    if (state.view !== 'servers' || !$(`#svMotd${i}`)) return;
    $(`#svMotd${i}`).innerHTML = r.online ? motdHtml(r.motd) : `<span style="color:#ff5555">${esc(r.error || 'Offline')}</span>`;
    $(`#svPlayers${i}`).textContent = r.online ? `${r.players.online.toLocaleString('en-US')} / ${r.players.max.toLocaleString('en-US')}` : '';
    $(`#svPing${i}`).innerHTML = pingBars(r.online ? r.ping : null);
    if (r.favicon) $(`#svIcon${i}`).src = r.favicon;
  });
}

function serverModal(index) {
  const list = state.cfg.servers || [];
  const sv = index != null ? list[index] : { name: '', address: '' };
  modal(`<h2>${index != null ? 'Edit server' : 'Add server'}</h2>
    <div class="form">
      <label>Name<input class="input" id="svName" value="${esc(sv.name)}" maxlength="32" placeholder="My server"></label>
      <label>Address<input class="input" id="svAddr" value="${esc(sv.address)}" placeholder="play.example.net"></label>
    </div>
    <div class="modal-actions"><button class="btn" id="svCancel">Cancel</button><button class="btn primary" id="svSave">Save</button></div>`,
  (card, close) => {
    $(index != null ? '#svAddr' : '#svName', card).focus();
    $('#svCancel', card).onclick = close;
    const save = async () => {
      const address = $('#svAddr', card).value.trim();
      if (!address) { $('#svAddr', card).focus(); return; }
      const entry = { name: $('#svName', card).value.trim() || address, address };
      const l = [...list];
      if (index != null) l[index] = entry; else l.push(entry);
      state.cfg = { ...state.cfg, ...(await safe(() => S.config.set({ servers: l }))) };
      pingCache.delete(address);
      close(); renderServers();
    };
    $('#svSave', card).onclick = save;
    card.querySelectorAll('input').forEach(i => { i.onkeydown = e => { if (e.key === 'Enter') save(); }; });
  });
}

const TYPE_LABEL = { mod: 'Mods', modpack: 'Modpacks', resourcepack: 'Texture packs', shader: 'Shaders' };

function renderBrowse() {
  const B = state.browse;
  const target = state.packs.find(p => p.id === B.target) || state.selected;
  if (target) B.target = target.id;
  const forPack = B.type !== 'modpack';
  const hasCf = !!(state.cfg && state.cfg.curseforgeKey);
  if (!hasCf) B.source = 'modrinth';
  view.innerHTML = `<div class="page">
    <div class="view-head"><h1 class="view-title">Browse</h1>
      <div class="seg" id="bSource" ${hasCf ? '' : 'hidden'}><button data-s="modrinth" class="${B.source === 'modrinth' ? 'on' : ''}">Modrinth</button><button data-s="curseforge" class="${B.source === 'curseforge' ? 'on' : ''}">CurseForge</button></div></div>
    <div class="toolbar">
      <div class="seg" id="bType">${Object.entries(TYPE_LABEL).map(([k, v]) => `<button data-t="${k}" class="${B.type === k ? 'on' : ''}">${v}</button>`).join('')}</div>
      <input class="input search" id="bQuery" placeholder="Search ${TYPE_LABEL[B.type].toLowerCase()}" value="${esc(B.query)}">
      ${selectWrap(`<select class="input" id="bSort" aria-label="Sort">${['relevance', 'downloads', 'follows', 'updated', 'newest'].map(s => `<option value="${s}" ${B.sort === s ? 'selected' : ''}>${s[0].toUpperCase() + s.slice(1)}</option>`).join('')}</select>`)}
      ${forPack ? selectWrap(`<select class="input" id="bTarget" data-tip="Installs into this modpack">${state.packs.map(p => `<option value="${esc(p.id)}" ${p.id === B.target ? 'selected' : ''}>${esc(p.name)} (${esc(p.mcVersion)})</option>`).join('')}</select>`) : ''}
    </div>
    <div id="bResults" class="results"></div>
    <div class="pager" id="bPager"></div></div>`;
  view.querySelectorAll('#bSource [data-s]').forEach(b => b.onclick = () => { B.source = b.dataset.s; B.offset = 0; renderBrowse(); runSearch(); });
  view.querySelectorAll('#bType [data-t]').forEach(b => b.onclick = () => { B.type = b.dataset.t; B.offset = 0; renderBrowse(); runSearch(); });
  let timer;
  $('#bQuery').onkeydown = e => { if (e.key === 'Enter') { clearTimeout(timer); B.query = e.target.value; B.offset = 0; runSearch(); } };
  $('#bQuery').oninput = e => { clearTimeout(timer); timer = setTimeout(() => { B.query = e.target.value; B.offset = 0; runSearch(); }, 300); };
  $('#bSort').onchange = e => { B.sort = e.target.value; B.offset = 0; runSearch(); };
  if ($('#bTarget')) $('#bTarget').onchange = e => { B.target = e.target.value; B.offset = 0; runSearch(); };
  drawResults();
}

async function runSearch() {
  const B = state.browse;
  const target = state.packs.find(p => p.id === B.target) || state.selected;
  if (target) B.target = target.id;
  B.loading = true; B.error = null; drawResults();
  try {
    const r = await S.browse.search({ source: B.source, type: B.type, query: B.query, sort: B.sort, offset: B.offset, mcVersion: B.type === 'modpack' ? '' : (target?.mcVersion || '') });
    B.results = r.hits; B.total = r.total;
  } catch (e) { B.error = e.message; B.results = []; }
  B.loading = false;
  drawResults();
}

function drawResults() {
  const B = state.browse;
  const box = $('#bResults');
  if (!box) return;
  if (B.loading) { box.innerHTML = '<div class="empty"><div class="bar indeterminate" style="width:180px;margin:0 auto"><div></div></div></div>'; $('#bPager').innerHTML = ''; return; }
  if (B.error) {
    box.innerHTML = `<div class="empty">${esc(B.error)}${B.source === 'curseforge' ? '<br><button class="btn" id="toSettings">Open settings</button>' : ''}</div>`;
    if ($('#toSettings')) $('#toSettings').onclick = () => go('settings');
    return;
  }
  if (!B.results) return;
  const target = state.packs.find(p => p.id === B.target);
  const have = new Set((target?.content || []).filter(c => c.source === B.source).map(c => String(c.projectId)));
  box.innerHTML = B.results.map((r, i) => `
    <div class="panel result">
      ${r.iconUrl ? `<img src="${esc(r.iconUrl)}" alt="" loading="lazy">` : '<div class="ph"></div>'}
      <div class="info"><div class="name">${esc(r.title)}</div>
        <div class="by">${esc(r.author)}, ${fmt(r.downloads)} downloads</div>
        <div class="desc">${esc(r.description)}</div></div>
      <div class="actions">${B.type !== 'modpack' && have.has(String(r.id))
        ? '<button class="btn sm" disabled>Installed</button>'
        : `<button class="btn primary sm" data-install="${i}">Install</button>`}
        ${r.url ? `<button class="btn sm" data-page="${i}">Page</button>` : ''}</div>
    </div>`).join('') || '<div class="empty">Nothing found. Try another search or version.</div>';
  box.querySelectorAll('[data-install]').forEach(b => b.onclick = () => installResult(B.results[+b.dataset.install], b));
  box.querySelectorAll('[data-page]').forEach(b => b.onclick = () => S.open(B.results[+b.dataset.page].url));
  const pages = Math.ceil(B.total / 24);
  const page = Math.floor(B.offset / 24) + 1;
  $('#bPager').innerHTML = pages > 1 ? `<button class="btn sm" id="prev" ${page <= 1 ? 'disabled' : ''}>Previous</button><span>Page ${page} of ${pages}</span><button class="btn sm" id="next" ${page >= pages ? 'disabled' : ''}>Next</button>` : '';
  if ($('#prev')) $('#prev').onclick = () => { B.offset = Math.max(0, B.offset - 24); runSearch(); view.scrollTop = 0; };
  if ($('#next')) $('#next').onclick = () => { B.offset += 24; runSearch(); view.scrollTop = 0; };
}

async function installResult(r, btn) {
  const B = state.browse;
  btn.disabled = true; btn.textContent = 'Installing';
  if (B.type === 'modpack') {
    const inst = await safe(() => S.browse.installPack({ source: r.source, id: r.id, title: r.title }));
    btn.textContent = inst ? 'Installed' : 'Install';
    btn.disabled = !!inst;
    await refreshPacks();
    return;
  }
  if (!B.target) { toast('Create a modpack first', true); btn.disabled = false; btn.textContent = 'Install'; return; }
  const list = await safe(() => S.browse.install({ source: r.source, id: r.id, type: B.type, instanceId: B.target }));
  btn.textContent = list ? 'Installed' : 'Install';
  btn.disabled = !!list;
  await refreshPacks();
}

function renderSettings() {
  const c = state.cfg;
  const maxRam = Math.max(4, c.totalGb) * 1024;
  const pct = v => `${((v - 1024) / (maxRam - 1024)) * 100}%`;
  const row = (label, control) => `<div class="setting"><div class="label">${label}</div><div class="control">${control}</div></div>`;
  view.innerHTML = `<div class="page">
    <div class="view-head"><h1 class="view-title">Settings</h1></div>
    <div class="settings">
      <section><h2>Game</h2><div class="panel">
        ${row('Memory', `<div class="slider"><input type="range" id="sRam" min="1024" max="${maxRam}" step="512" value="${c.ramMb}" style="--p:${pct(c.ramMb)}" aria-label="Memory"><span id="sRamV">${(c.ramMb / 1024).toFixed(1)} GB</span></div>`)}
        ${row('Java flags', `<div class="seg" id="sJvm"><button data-v="surge" class="${c.jvmPreset === 'surge' ? 'on' : ''}">Optimized</button><button data-v="vanilla" class="${c.jvmPreset === 'vanilla' ? 'on' : ''}">Default</button></div>`)}
        ${row('Extra Java arguments', `<input class="input" id="sArgs" style="width:280px" value="${esc(c.extraJvmArgs)}" aria-label="Extra Java arguments">`)}
        ${row('Java', `<span class="muted small" style="max-width:240px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${c.javaPath ? esc(c.javaPath) : 'Downloaded automatically'}</span><button class="btn sm" id="sJava">Choose</button>${c.javaPath ? '<button class="btn sm" id="sJavaReset">Use automatic</button>' : ''}`)}
        ${row('Window size', `<input class="input" id="sW" type="number" style="width:92px" value="${c.width}" aria-label="Width"><span class="muted">by</span><input class="input" id="sH" type="number" style="width:92px" value="${c.height}" aria-label="Height">`)}
      </div></section>
      <section><h2>Launcher</h2><div class="panel">
        ${row('Hide launcher while playing', `<button class="toggle ${c.closeOnLaunch ? 'on' : ''}" id="sHide" aria-label="Hide launcher while playing"></button>`)}
        ${row('Minimize launcher while playing', `<button class="toggle ${c.minimizeOnLaunch !== false ? 'on' : ''}" id="sMin" aria-label="Minimize launcher while playing"></button>`)}
        ${row('Click sounds', `<button class="toggle ${c.uiSounds !== false ? 'on' : ''}" id="sSound" aria-label="Click sounds"></button>`)}
        ${row('Game folder', `<button class="btn sm" id="sData">Open folder</button>`)}
        ${row('Help and news', `<button class="btn sm" id="sDiscord">Join our Discord</button>`)}
      </div></section>
    </div></div>`;
  const save = async patch => { state.cfg = { ...state.cfg, ...(await safe(() => S.config.set(patch))) }; };
  $('#sRam').oninput = e => { $('#sRamV').textContent = `${(e.target.value / 1024).toFixed(1)} GB`; e.target.style.setProperty('--p', pct(+e.target.value)); };
  $('#sRam').onchange = e => save({ ramMb: +e.target.value });
  view.querySelectorAll('#sJvm [data-v]').forEach(b => b.onclick = async () => { await save({ jvmPreset: b.dataset.v }); renderSettings(); });
  $('#sArgs').onchange = e => save({ extraJvmArgs: e.target.value });
  $('#sJava').onclick = async () => { const p = await safe(() => S.config.pickJava()); if (p) { await save({ javaPath: p }); renderSettings(); } };
  if ($('#sJavaReset')) $('#sJavaReset').onclick = async () => { await save({ javaPath: '' }); renderSettings(); };
  $('#sW').onchange = e => save({ width: +e.target.value || 1280 });
  $('#sH').onchange = e => save({ height: +e.target.value || 720 });
  $('#sHide').onclick = async () => { await save({ closeOnLaunch: !state.cfg.closeOnLaunch }); renderSettings(); };
  $('#sMin').onclick = async () => { await save({ minimizeOnLaunch: state.cfg.minimizeOnLaunch === false }); renderSettings(); };
  $('#sSound').onclick = async () => { await save({ uiSounds: state.cfg.uiSounds === false }); renderSettings(); };
  $('#sData').onclick = () => S.config.openDataDir();
  $('#sDiscord').onclick = () => S.open(DISCORD);
}

window.addEventListener('keydown', e => { if (e.ctrlKey && e.shiftKey && e.code === 'F4') { e.preventDefault(); openAdmin(); } });

async function openAdmin() {
  const cfg = await S.config.get();
  const base = String(cfg.apiUrl || cfg.autoApiUrl || '').replace(/\/+$/, '');
  if (!base) return toast('The Surge service is not set up yet.');
  S.open(base + '/admin');
}

const SHOP_ITEMS = [{"id": "wings:angel", "name": "Angel Wings"}, {"id": "wings:surge", "name": "Surge Wings"}, {"id": "wings:soul", "name": "Soul Wings"}, {"id": "wings:flame", "name": "Flame Wings"}, {"id": "wings:void", "name": "Void Wings"}, {"id": "wings:crystal", "name": "Crystal Wings"}, {"id": "wings:gold", "name": "Golden Wings"}, {"id": "halo:angel", "name": "Angel Halo"}, {"id": "halo:surge", "name": "Surge Halo"}, {"id": "halo:fire", "name": "Fire Halo"}, {"id": "halo:enchanted", "name": "Enchanted Halo"}, {"id": "halo:void", "name": "Void Halo"}, {"id": "halo:rose", "name": "Rose Halo"}, {"id": "cape:surge", "name": "Surge Cape"}, {"id": "cape:void", "name": "Void Cape"}, {"id": "cape:flame", "name": "Flame Cape"}, {"id": "cape:ocean", "name": "Ocean Cape"}, {"id": "cape:sakura", "name": "Sakura Cape"}, {"id": "cape:midnight", "name": "Midnight Cape"}, {"id": "cape:royal", "name": "Royal Cape"}, {"id": "cape:emerald", "name": "Emerald Cape"}, {"id": "cape:lava", "name": "Lava Cape"}, {"id": "cape:matrix", "name": "Matrix Cape"}, {"id": "cape:storm", "name": "Storm Cape"}, {"id": "cape:phoenix", "name": "Phoenix Cape"}, {"id": "cape:koi", "name": "Koi Cape"}, {"id": "cape:bloodmoon", "name": "Blood Moon Cape"}, {"id": "cape:obsidian", "name": "Obsidian Cape"}, {"id": "cape:frost", "name": "Frost Cape"}, {"id": "cape:galaxy", "name": "Galaxy Cape"}, {"id": "cape:aurora", "name": "Aurora Cape"}, {"id": "cape:custom", "name": "Color Cape"}, {"id": "cape:blinkcat", "name": "Blink Cat Cape"}, {"id": "cape:enchanted", "name": "Enchanted Cape"}, {"id": "cape:runes", "name": "Rune Cape"}, {"id": "cape:beta", "name": "Beta Tester Cape"}, {"id": "cape:image", "name": "Picture Cape"}, {"id": "emote:WAVE", "name": "Wave (emote)"}, {"id": "emote:CLAP", "name": "Clap (emote)"}, {"id": "emote:FACEPALM", "name": "Facepalm (emote)"}, {"id": "emote:DAB", "name": "Dab (emote)"}, {"id": "emote:SHRUG", "name": "Shrug (emote)"}, {"id": "emote:SALUTE", "name": "Salute (emote)"}, {"id": "emote:CHEER", "name": "Cheer (emote)"}, {"id": "emote:LAUGH", "name": "Laugh (emote)"}, {"id": "emote:BOW", "name": "Bow (emote)"}, {"id": "emote:HEART", "name": "Heart (emote)"}, {"id": "emote:FLOSS", "name": "Floss (emote)"}, {"id": "emote:TPOSE", "name": "T-Pose (emote)"}, {"id": "emote:CRY", "name": "Cry (emote)"}, {"id": "emote:SIT", "name": "Sit (emote)"}, {"id": "emote:GRIDDY", "name": "Griddy (emote)"}, {"id": "emote:SHUFFLE", "name": "Shuffle (emote)"}, {"id": "emote:DISCO", "name": "Disco Fever (emote)"}, {"id": "idle:IDLE_LOOK", "name": "Look Around (idle)"}, {"id": "idle:IDLE_STRETCH", "name": "Big Stretch (idle)"}, {"id": "idle:IDLE_YAWN", "name": "Sleepy (idle)"}, {"id": "idle:IDLE_TAP", "name": "Foot Tap (idle)"}, {"id": "idle:IDLE_CROSS", "name": "Arms Crossed (idle)"}, {"id": "idle:IDLE_VIBE", "name": "Vibing (idle)"}];
const shopName = id => (SHOP_ITEMS.find(i => i.id === id) || { name: id }).name;

const SALE_TARGETS = [['all', 'Everything'], ['cosmetic', 'All cosmetics'], ['emote', 'All emotes'], ['sub:wings', 'All wings'], ['sub:halo', 'All halos'], ['sub:cape', 'All capes'], ...SHOP_ITEMS.map(i => [i.id, i.name])];
const saleName = t => (SALE_TARGETS.find(x => x[0] === t) || [t, t])[1];

async function adminPanel(tab = 'codes') {
  const tabs = `<div class="seg" id="aTabs" style="margin-bottom:12px">${[['codes', 'Codes'], ['sales', 'Sales'], ['locks', 'Locked'], ['discover', 'Discover'], ['updates', 'Updates']]
    .map(([v, l]) => `<button data-v="${v}" class="${tab === v ? 'on' : ''}">${l}</button>`).join('')}</div>`;
  const wire = (card, close) => {
    card.querySelectorAll('#aTabs [data-v]').forEach(b => b.onclick = () => { close(); adminPanel(b.dataset.v); });
    $('#aLock', card).onclick = async () => { await safe(() => S.admin.lock()); close(); toast('Admin locked'); };
  };
  const foot = `<div class="modal-actions"><button class="btn" id="aLock">Lock</button>${tab === 'codes' ? '<button class="btn" id="aExport">Export codes.json</button>' : ''}</div>`;

  if (tab === 'codes') {
    const codes = (await safe(() => S.admin.list())) || [];
    const picked = new Set();
    modal(`<h2>Admin</h2>${tabs}
      <div class="form">
        <div style="display:flex;gap:8px;align-items:flex-end">
          <label style="flex:1">Your own code (optional)<input class="input" id="aCustom" maxlength="24" placeholder="Leave empty for random codes"></label>
          <label style="width:110px">Coins<input class="input" id="aCoins" type="number" min="0" value="1000"></label>
          <label style="width:80px" id="aCountWrap">How many<input class="input" id="aCount" type="number" min="1" max="100" value="1"></label>
        </div>
        <label>Also unlocks
          <select class="input" id="aItem"><option value="">Pick a cosmetic or emote…</option>${SHOP_ITEMS.map(i => `<option value="${esc(i.id)}">${esc(i.name)}</option>`).join('')}</select></label>
        <div id="aPicked" class="muted small">Only coins</div>
        <div class="modal-actions" style="margin:0"><button class="btn primary" id="aGen">Create</button><button class="btn" id="aPub" data-tip="Upload codes and sales so every player can use them">Publish codes</button></div>
        <div class="pick-list" style="max-height:220px;overflow:auto">${codes.map(c => `
          <button data-code="${esc(c.code)}" data-tip="Copy code"><div class="grow code">${esc(c.code)}</div>
            <span class="muted small">${c.coins || 0} coins${(c.items || []).length ? ' + ' + c.items.map(shopName).map(esc).join(', ') : ''}</span><span class="btn sm x-btn" data-del="${esc(c.code)}" data-tip="Delete code">&times;</span></button>`).join('') || '<div class="muted small">No codes yet</div>'}</div>
      </div>${foot}`,
    (card, close) => {
      wire(card, close);
      const showPicked = () => {
        $('#aPicked', card).innerHTML = picked.size
          ? [...picked].map(id => `<span class="chip" data-unpick="${esc(id)}">${esc(shopName(id))} &times;</span>`).join(' ')
          : 'Only coins';
        card.querySelectorAll('[data-unpick]').forEach(ch => ch.onclick = () => { picked.delete(ch.dataset.unpick); showPicked(); });
      };
      $('#aItem', card).onchange = e => { if (e.target.value) picked.add(e.target.value); e.target.value = ''; showPicked(); };
      $('#aCustom', card).oninput = e => { $('#aCountWrap', card).style.display = e.target.value.trim() ? 'none' : ''; };
      $('#aGen', card).onclick = async () => {
        const r = await safe(() => S.admin.create({ coins: +$('#aCoins', card).value, count: +$('#aCount', card).value, code: $('#aCustom', card).value.trim(), items: [...picked] }));
        if (r) { close(); adminPanel('codes'); toast('Code created. Press Publish codes so players can use it.'); }
      };
      $('#aPub', card).onclick = async () => { const u = await safe(() => S.admin.publishCodes()); if (u) toast('Codes and sales published'); };
      card.querySelectorAll('[data-code]').forEach(b => b.onclick = async e => {
        const del = e.target.closest('[data-del]');
        if (del) { const r = await safe(() => S.admin.remove(del.dataset.del)); if (r) { close(); adminPanel('codes'); } return; }
        await navigator.clipboard.writeText(b.dataset.code); toast('Code copied');
      });
      $('#aExport', card).onclick = () => safe(() => S.admin.exportCodes());
    });
    return;
  }

  if (tab === 'sales') {
    const sales = (await safe(() => S.admin.sales())) || [];
    modal(`<h2>Admin</h2>${tabs}
      <div class="form">
        <div style="display:flex;gap:8px;align-items:flex-end">
          <label style="flex:1">On<select class="input" id="sTarget">${SALE_TARGETS.map(([v, l]) => `<option value="${esc(v)}">${esc(l)}</option>`).join('')}</select></label>
          <label style="width:90px">Percent off<input class="input" id="sPct" type="number" min="5" max="90" value="20"></label>
          <label style="width:120px">Length<select class="input" id="sLen"><option value="24">1 day</option><option value="72">3 days</option><option value="168">1 week</option><option value="0">No end</option></select></label>
        </div>
        <div class="modal-actions" style="margin:0"><button class="btn primary" id="sAdd">Start sale</button><button class="btn" id="aPub">Publish</button></div>
        <div class="pick-list" style="max-height:240px;overflow:auto">${sales.map((x, i) => `
          <button data-i="${i}"><div class="grow">${x.percent}% off ${esc(saleName(x.target))}</div>
            <span class="muted small">${x.until ? 'ends ' + new Date(x.until).toLocaleString() : 'no end'}</span><span class="btn sm x-btn" data-end="${i}" data-tip="End sale">&times;</span></button>`).join('') || '<div class="muted small">No sales running</div>'}</div>
      </div>${foot}`,
    (card, close) => {
      wire(card, close);
      $('#sAdd', card).onclick = async () => {
        const r = await safe(() => S.admin.addSale({ target: $('#sTarget', card).value, percent: +$('#sPct', card).value, hours: +$('#sLen', card).value }));
        if (r) { close(); adminPanel('sales'); toast('Sale started. Press Publish so every player sees it.'); }
      };
      $('#aPub', card).onclick = async () => { const u = await safe(() => S.admin.publishCodes()); if (u) toast('Sales published'); };
      card.querySelectorAll('[data-end]').forEach(b => b.onclick = async e => { e.stopPropagation(); const r = await safe(() => S.admin.removeSale(+b.dataset.end)); if (r) { close(); adminPanel('sales'); } });
    });
    return;
  }

  if (tab === 'locks') {
    const locked = new Set((await safe(() => S.admin.locks())) || []);
    modal(`<h2>Admin</h2>${tabs}
      <div class="form">
        <p class="muted small" style="margin:0">Locked items can't be bought. Players only get them from a code. Press Publish so everyone gets the change.</p>
        <input class="input" id="lFind" placeholder="Search">
        <div class="pick-list" id="lList" style="max-height:300px;overflow:auto">${SHOP_ITEMS.map(i => `
          <button data-id="${esc(i.id)}"><div class="grow">${esc(i.name)}</div><span class="chip ${locked.has(i.id) ? 'accent' : ''}">${locked.has(i.id) ? 'Locked' : 'In shop'}</span></button>`).join('')}</div>
        <div class="modal-actions" style="margin:0"><button class="btn primary" id="aPub">Publish</button></div>
      </div>${foot}`,
    (card, close) => {
      wire(card, close);
      $('#lFind', card).oninput = e => { const q = e.target.value.toLowerCase(); card.querySelectorAll('#lList [data-id]').forEach(b => { b.style.display = b.textContent.toLowerCase().includes(q) ? '' : 'none'; }); };
      card.querySelectorAll('#lList [data-id]').forEach(b => b.onclick = async () => {
        const list = await safe(() => S.admin.toggleLock(b.dataset.id));
        if (!list) return;
        const on = list.includes(b.dataset.id);
        const chip = b.querySelector('.chip');
        chip.textContent = on ? 'Locked' : 'In shop';
        chip.classList.toggle('accent', on);
      });
      $('#aPub', card).onclick = async () => { const u = await safe(() => S.admin.publishCodes()); if (u) toast('Published'); };
    });
    return;
  }

  if (tab === 'discover') {
    const list = (await safe(() => S.admin.discover())) || [];
    modal(`<h2>Admin</h2>${tabs}
      <div class="form">
        <p class="muted small" style="margin:0">Partnered servers on the in-game Discover tab (next to Friends on the server list).</p>
        <div style="display:flex;gap:8px;align-items:flex-end">
          <label style="flex:1">Name<input class="input" id="dName" maxlength="32" placeholder="Example Network"></label>
          <label style="flex:1">Address<input class="input" id="dIp" placeholder="play.example.net"></label>
          <label style="width:100px">Tag<input class="input" id="dTag" maxlength="16" placeholder="Partner"></label>
        </div>
        <label>Short description<input class="input" id="dDesc" maxlength="80" placeholder="Bedwars, Skywars and Duels"></label>
        <div class="modal-actions" style="margin:0"><button class="btn primary" id="dAdd">Add server</button><button class="btn" id="dPub" data-tip="Upload the list so every player sees it">Publish</button></div>
        <div class="pick-list" style="max-height:240px;overflow:auto">${list.map((x, i) => `
          <button data-i="${i}"><div class="grow"><b>${esc(x.name)}</b> <span class="muted small">${esc(x.ip)}${x.tag ? ' · ' + esc(x.tag) : ''}</span><div class="muted small">${esc(x.description || '')}</div></div>
            <span class="btn sm" data-up="${i}" data-tip="Move up">&uarr;</span><span class="btn sm x-btn" data-rm="${i}" data-tip="Remove">&times;</span></button>`).join('') || '<div class="muted small">No servers yet</div>'}</div>
      </div>${foot}`,
    (card, close) => {
      wire(card, close);
      $('#dAdd', card).onclick = async () => {
        const r = await safe(() => S.admin.addDiscover({ name: $('#dName', card).value, ip: $('#dIp', card).value, tag: $('#dTag', card).value, description: $('#dDesc', card).value }));
        if (r) { close(); adminPanel('discover'); toast('Server added. Press Publish so players see it.'); }
      };
      $('#dPub', card).onclick = async () => { const u = await safe(() => S.admin.publishDiscover()); if (u) toast('Discover list published'); };
      card.querySelectorAll('[data-rm]').forEach(b => b.onclick = async e => { e.stopPropagation(); if (await safe(() => S.admin.removeDiscover(+b.dataset.rm))) { close(); adminPanel('discover'); } });
      card.querySelectorAll('[data-up]').forEach(b => b.onclick = async e => { e.stopPropagation(); if (await safe(() => S.admin.moveDiscover(+b.dataset.up, -1))) { close(); adminPanel('discover'); } });
    });
    return;
  }

  const rel = (await safe(() => S.admin.release())) || {};
  modal(`<h2>Admin</h2>${tabs}
    <div class="form">
      <label>Update repo (GitHub owner/name)<input class="input" id="uRepo" value="${esc(rel.repo || '')}" placeholder="you/surge-client"></label>
      <label>GitHub token ${rel.hasToken ? '<span class="muted small">(saved)</span>' : ''}<input class="input" id="uTok" type="password" autocomplete="off" placeholder="${rel.hasToken ? 'Saved. Paste a new one to replace it' : 'Fine-grained token with Contents: Read and write'}"></label>
      <label>What's new (optional)<input class="input" id="uNotes" placeholder="Shown on the release page"></label>
      <div class="modal-actions" style="margin:0">
        <button class="btn primary" id="uPub" data-tip="Pick surge-client-X+mc1.21.11.jar or Surge-Client-X-Standalone.exe">Publish update…</button>
        <button class="btn" id="uTokLink">Make a token</button>
      </div>
      <p class="muted small" style="margin:0">Players get a new mod .jar automatically the next time they press Play. A new launcher .exe is offered to them on start.
      Codes link: ${esc(rel.codesUrl || 'set a repo first')}</p>
    </div>${foot}`,
  (card, close) => {
    wire(card, close);
    $('#uRepo', card).onchange = async e => { await safe(() => S.admin.setRepo(e.target.value)); toast('Repo saved'); };
    $('#uTok', card).onchange = async e => { await safe(() => S.admin.setToken(e.target.value)); e.target.value = ''; toast('Token saved (encrypted)'); };
    $('#uTokLink', card).onclick = () => S.open('https://github.com/settings/personal-access-tokens/new');
    $('#uPub', card).onclick = async () => {
      toast('Publishing…');
      const r = await safe(() => S.admin.publishFile($('#uNotes', card).value.trim()));
      if (r) toast(`Published ${r.tag}`);
    };
  });
}

let announceCache = null;
async function loadAnnouncement() {
  try {
    if (!announceCache || Date.now() - announceCache.at > 120000) {
      const api = (state.cfg && state.cfg.apiUrl) || 'https://surge-client.remvuxd.workers.dev';
      const r = await fetch(`${api}/v1/public`, { signal: AbortSignal.timeout(6000) });
      const d = await r.json();
      announceCache = { at: Date.now(), a: d && d.announce };
    }
    const a = announceCache.a, hero = $('.hero');
    if (!a || !hero || $('.announce', hero)) return;
    let gone = '';
    try { gone = localStorage.getItem('surgeAnnounce') || ''; } catch { }
    if (String(a.id) === gone) return;
    const el = document.createElement('div');
    el.className = 'announce';
    el.innerHTML = `<b>News</b><div class="grow">${esc(a.text)}</div><button class="btn sm x-btn" aria-label="Dismiss">&times;</button>`;
    el.querySelector('button').onclick = () => { try { localStorage.setItem('surgeAnnounce', String(a.id)); } catch { } el.remove(); };
    hero.appendChild(el);
  } catch { }
}

let newsCache = null;
async function loadNews() {
  if (!newsCache) {
    try {
      const t = (await S.changelog()) || '';
      const m = t.match(/^## ([\d.]+)\s*\n([\s\S]*?)(?=^## |$(?![\s\S]))/m);
      newsCache = m ? { version: m[1], items: m[2].split('\n').filter(l => l.startsWith('- ')).map(l => l.slice(2)) } : { items: [] };
    } catch { newsCache = { items: [] }; }
  }
  const card = $('#newsCard');
  if (!card) return;
  const items = newsCache.items;
  card.innerHTML = `<div class="side-head">What's new${newsCache.version ? ` <span class="muted small">${esc(newsCache.version)}</span>` : ''}</div>
    <ul>${items.slice(0, 5).map(i => `<li>${esc(i)}</li>`).join('')}</ul>${items.length > 5 ? `<button class="btn sm" id="newsMore">See all ${items.length}</button>` : ''}`;
  const more = $('#newsMore', card);
  if (more) more.onclick = () => modal(`<h2>What's new in ${esc(newsCache.version)}</h2><ul class="news-full">${items.map(i => `<li>${esc(i)}</li>`).join('')}</ul><div class="modal-actions"><button class="btn primary" id="nmOk">Nice</button></div>`, (c, close) => { $('#nmOk', c).onclick = close; });
}

function changeLines(text) {
  return String(text || '').split(/\r?\n/)
    .map(l => l.trim())
    .filter(l => l && !/https?:\/\/|full changelog|what's changed|new contributors| by @/i.test(l) && !/^#/.test(l))
    .map(l => l.replace(/^[-*•]\s*/, '').replace(/\*\*|__|`/g, ''))
    .filter(Boolean)
    .slice(0, 20);
}

async function checkSelfUpdate() {
  const u = await S.selfUpdate.check().catch(() => null);
  if (!u) return;
  const items = changeLines(u.notes);
  modal(`<h2>Update available</h2><p class="muted" style="margin:0">Surge Client ${esc(u.version)} is out.</p>
    ${items.length ? `<details class="changelog"><summary>What's new in ${esc(u.version)}</summary><ul>${items.map(l => `<li>${esc(l)}</li>`).join('')}</ul></details>` : ''}
    <div class="modal-actions"><button class="btn" id="uLater">Later</button><button class="btn primary" id="uNow">Update and restart</button></div>`,
  (card, close) => {
    $('#uLater', card).onclick = close;
    $('#uNow', card).onclick = async () => { $('#uNow', card).disabled = true; $('#uNow', card).textContent = 'Downloading…'; await safe(() => S.selfUpdate.apply(u.url)); };
  });
}

function backgroundFx() {
  const c = document.getElementById('bgfx');
  if (!c) return;
  const ctx = c.getContext('2d');
  const still = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  let w = 0, h = 0, dpr = 1, stars = [], last = 0;
  const resize = () => {
    dpr = Math.min(2, window.devicePixelRatio || 1);
    w = c.clientWidth; h = c.clientHeight;
    c.width = Math.round(w * dpr); c.height = Math.round(h * dpr);
    const n = Math.round((w * h) / 9000);
    stars = Array.from({ length: n }, () => ({ x: Math.random() * w, y: Math.random() * h, z: 0.25 + Math.random() * 0.75, p: Math.random() * Math.PI * 2 }));
  };
  const draw = t => {
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, w, h);
    const s = t / 1000;
    for (const [ox, oy, r, a] of [[0.25, 0.3, 0.55, 0.05], [0.8, 0.75, 0.5, 0.04]]) {
      const gx = w * (ox + Math.sin(s * 0.05 + ox * 9) * 0.08), gy = h * (oy + Math.cos(s * 0.04 + oy * 7) * 0.08);
      const g = ctx.createRadialGradient(gx, gy, 0, gx, gy, Math.max(w, h) * r);
      g.addColorStop(0, `rgba(255,255,255,${a})`);
      g.addColorStop(1, 'rgba(255,255,255,0)');
      ctx.fillStyle = g;
      ctx.fillRect(0, 0, w, h);
    }
    for (const st of stars) {
      const y = ((st.y - s * 6 * st.z) % h + h) % h;
      const x = (st.x + Math.sin(s * 0.2 + st.p) * 4 * st.z + w) % w;
      const tw = 0.35 + 0.65 * (0.5 + 0.5 * Math.sin(s * (0.6 + st.z) + st.p));
      const size = st.z > 0.85 ? 2 : 1;
      ctx.fillStyle = `rgba(255,255,255,${(0.12 + 0.5 * st.z) * tw})`;
      ctx.fillRect(Math.round(x), Math.round(y), size, size);
    }
  };
  const loop = t => {
    requestAnimationFrame(loop);
    if (document.hidden || state.launch.stage === 'Playing' || t - last < 33) return;
    last = t;
    draw(t);
  };
  resize();
  window.addEventListener('resize', resize);
  if (still) draw(0); else requestAnimationFrame(loop);
}

function calmDown() {
  const quiet = document.hidden || state.launch.stage === 'Playing';
  for (const v of [viewer, skinViewer, ward.viewer]) if (v && v.renderPaused !== quiet) v.renderPaused = quiet;
}
setInterval(calmDown, 1000);
document.addEventListener('visibilitychange', calmDown);

(async function boot() {
  backgroundFx();
  state.cfg = await safe(() => S.config.get());
  await refreshAccounts();
  await safe(() => S.instances.ensureDefault());
  await refreshPacks();
  state.launch.busy = !!(await safe(() => S.launch.running()));
  render();
  setTimeout(checkSelfUpdate, 2500);
})();
