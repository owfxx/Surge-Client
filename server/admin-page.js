export const ADMIN_PAGE = `<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="robots" content="noindex"><title>Surge Admin</title>
<link href="https://fonts.googleapis.com/css2?family=Pixelify+Sans:wght@600&family=Inter:wght@400;600&display=swap" rel="stylesheet">
<style>
:root{--bg:#0e0e11;--panel:#16161a;--line:#27272e;--violet:#f2c430;--lilac:#ffe07a;--text:#f4f4f5;--muted:#a1a1aa;--bad:#ff8c8c;--good:#4ade80}
*{box-sizing:border-box}html,body{margin:0}body{background:var(--bg);color:var(--text);font:15px/1.5 Inter,system-ui,sans-serif;min-height:100vh}
button,input,select{font:inherit;color:inherit}
.wrap{max-width:960px;margin:0 auto;padding:28px 16px 60px}
h1{font:600 30px "Pixelify Sans",sans-serif;margin:0}
header{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:22px}
.tabs{display:flex;gap:6px;flex-wrap:wrap;margin-bottom:18px}
.tab{background:transparent;border:1px solid var(--line);padding:7px 14px;cursor:pointer;color:var(--muted);border-radius:999px;transition:color .15s,border-color .15s}
.tab:hover{color:var(--text);border-color:#3a3a44}
.tab.on{background:var(--violet);border-color:var(--violet);color:#1a1405;font-weight:600}
.card{background:var(--panel);border:1px solid var(--line);border-radius:10px;padding:18px;margin-bottom:14px}
.card h2{font-size:16px;margin:0 0 12px}
.row{display:flex;gap:10px;flex-wrap:wrap;align-items:flex-end}
label{display:flex;flex-direction:column;gap:4px;font-size:13px;color:var(--muted);flex:1;min-width:120px}
input,select,textarea{background:#0e0e11;border:1px solid var(--line);border-radius:6px;padding:9px 11px;width:100%}
input:focus,select:focus{outline:none;border-color:var(--violet)}
.btn{background:var(--violet);border:0;border-radius:6px;padding:9px 16px;cursor:pointer;color:#1a1405;font-weight:600;white-space:nowrap;transition:filter .15s}.btn:hover{filter:brightness(1.08)}
.btn.ghost{background:transparent;border:1px solid var(--line);color:var(--text)}.btn.ghost:hover{border-color:#3a3a44;filter:none}
.btn.small{padding:5px 10px;font-size:13px}
.btn:focus-visible,.tab:focus-visible{outline:2px solid var(--lilac);outline-offset:2px}
.list{display:flex;flex-direction:column;gap:6px;margin-top:12px}
.item{display:flex;align-items:center;gap:10px;padding:10px 12px;background:#121216;border:1px solid var(--line);border-radius:8px}
.item .grow{flex:1;min-width:0}.item .sub{color:var(--muted);font-size:13px}
.code{font-family:ui-monospace,Consolas,monospace;font-weight:600;letter-spacing:.5px;word-break:break-all}
.chips{display:flex;gap:6px;flex-wrap:wrap;margin-top:8px}
.chip{background:#27272a;border:1px solid var(--line);padding:2px 8px;font-size:13px;cursor:pointer}
.muted{color:var(--muted);font-size:13.5px}
.toast{position:fixed;left:50%;bottom:20px;transform:translateX(-50%);background:#27272a;border:2px solid var(--violet);padding:10px 16px;display:none}
.login{max-width:360px;margin:12vh auto 0}
.locked{color:var(--lilac);font-weight:600}
.picker{margin-top:10px}
.pick-bar{display:flex;gap:6px;flex-wrap:wrap;align-items:center;margin-bottom:8px}
.pick-bar input{max-width:220px}
.pill{background:transparent;border:1px solid var(--line);border-radius:999px;padding:4px 10px;cursor:pointer;color:var(--muted);font-size:13px}
.pill.on{border-color:var(--violet);color:var(--text)}
.pick-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:6px;max-height:300px;overflow:auto;padding-right:4px}
.pick{border:1px solid var(--line);border-radius:6px;background:#121216;padding:8px 10px;cursor:pointer;text-align:left;display:flex;gap:8px;align-items:center}
.pick:hover{border-color:#55555c}
.pick.on{border-color:var(--violet);background:#27272a}
.pick .dot{width:10px;height:10px;flex:none;border:2px solid var(--line)}
.pick.on .dot{background:var(--violet);border-color:var(--violet)}
.pick b{font-weight:600;font-size:14px;display:block}.pick small{color:var(--muted);font-size:12px}
.pick.group{border-style:dashed}
.uses{font-size:12px;color:var(--muted);white-space:nowrap}
.stats{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:10px;margin-bottom:14px}
.stat{background:var(--panel);border:1px solid var(--line);border-radius:10px;padding:16px}
.stat b{display:block;font:600 30px "Pixelify Sans",sans-serif;color:var(--text)}.stat span{color:var(--muted);font-size:13px}
.dot{display:inline-block;width:8px;height:8px;border-radius:50%;margin-right:6px;background:var(--good)}
.kv{display:grid;grid-template-columns:140px 1fr;gap:6px 12px;font-size:14px}.kv span{color:var(--muted)}
.check{flex-direction:row;align-items:center;gap:8px;flex:0 0 auto;min-width:0;color:var(--text);font-size:14px}.check input{width:auto}
.banner{border:1px solid #f2c43055;background:#f2c43012;border-radius:8px;padding:12px 14px}
.th{width:36px;height:36px;flex:none;border-radius:7px;background:radial-gradient(circle at 50% 30%,#22222a,#0b0b0e);border:1px solid var(--line);display:grid;place-items:center;overflow:hidden;position:relative}
.th.big{width:72px;height:72px;border-radius:10px}
.th .cp{background-repeat:no-repeat;box-shadow:0 2px 6px #0008}
.th .wg{display:flex;align-items:center;justify-content:center;width:100%;height:100%}
.th .wg img{height:62%;width:auto;image-rendering:auto}.th .wg img:first-child{transform:scaleX(-1)}
.th .hl{width:62%;height:26%;border-radius:50%;border:3px solid currentColor;box-shadow:0 0 10px currentColor,inset 0 0 6px currentColor}
.th .em{width:58%;height:58%;color:#e4e4e7}
.th .gl{font:600 16px "Pixelify Sans",sans-serif;color:#fff;opacity:.85}
.pick .th{width:32px;height:32px}
.gp{border:1px solid #3a3a44;border-radius:14px;padding:18px;background:linear-gradient(180deg,#1d1d24,#141418);box-shadow:0 10px 30px #0006;max-width:520px}
.gp h3{margin:0;font:600 20px "Pixelify Sans",sans-serif}.gp .gsub{color:var(--muted);font-size:13.5px;margin:4px 0 14px}
.gp .gitems{display:flex;flex-wrap:wrap;gap:10px}
.gp .gi{display:flex;flex-direction:column;align-items:center;gap:6px;width:86px;text-align:center;font-size:12.5px;animation:pop .35s cubic-bezier(.2,1.4,.4,1) both}
.gp .gi span{color:var(--muted)}
.coin{width:46%;height:46%;border-radius:50%;background:radial-gradient(circle at 35% 30%,#fff3b0,#f2c430 45%,#a77a08);box-shadow:0 0 12px #f2c43088}
.gp .to{margin-top:14px;color:var(--muted);font-size:13px}
.gp .empty{color:var(--muted);font-size:13.5px}
.prevrow{display:flex;gap:14px;align-items:center}
@keyframes pop{from{transform:scale(.6);opacity:0}to{transform:none;opacity:1}}
.feed .item{padding:8px 10px}
@media (max-width:560px){.item{flex-wrap:wrap}}
</style></head><body><div class="wrap" id="app"></div><div class="toast" id="toast"></div>
<script>
const $ = s => document.querySelector(s);
let token = '';
try { token = sessionStorage.getItem('surgeAdmin') || ''; } catch {}
let data = null, tab = 'overview', picked = [];
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
function toast(t, bad) { const el = $('#toast'); el.textContent = t; el.style.borderColor = bad ? 'var(--bad)' : 'var(--violet)'; el.style.display = 'block'; clearTimeout(toast.t); toast.t = setTimeout(() => el.style.display = 'none', 3200); }
async function api(path, body) {
  const r = await fetch('/admin/api/' + path, { method: body ? 'POST' : 'GET', headers: { 'content-type': 'application/json', authorization: 'Bearer ' + token }, body: body ? JSON.stringify(body) : undefined });
  const d = await r.json().catch(() => ({}));
  if (r.status === 401 && path !== 'login') { token = ''; try { sessionStorage.removeItem('surgeAdmin'); } catch {} login(); throw new Error(d.error || 'Signed out'); }
  if (!r.ok) throw new Error(d.error || 'Something went wrong');
  return d;
}
const name = id => (data.items.find(i => i.id === id) || { name: id }).name;
const CATS = [['all', 'All'], ['wings', 'Wings'], ['halo', 'Halos'], ['cape', 'Capes'], ['emote', 'Emotes']];
const kindOf = id => id.split(':')[0];
const KIND_LABEL = { wings: 'Wings', halo: 'Halo', cape: 'Cape', emote: 'Emote' };
const cleanName = n => n.replace(/ \\(emote\\)$/, '');
function picker(el, opts) {
  let cat = 'all', q = '';
  const draw = () => {
    const sel = new Set(opts.value());
    const groups = (opts.groups || []).filter(([, l]) => !q || l.toLowerCase().includes(q));
    const items = data.items.filter(i => (cat === 'all' || kindOf(i.id) === cat) && (!q || i.name.toLowerCase().includes(q)));
    el.innerHTML = '<div class="pick-bar"><input placeholder="Search items" value="' + esc(q) + '">' + CATS.map(([k, l]) => '<button class="pill ' + (cat === k ? 'on' : '') + '" data-cat="' + k + '">' + l + '</button>').join('') + (opts.multi ? '<span class="muted">' + sel.size + ' ' + (opts.countWord || 'picked') + '</span>' : '') + '</div>'
      + '<div class="pick-grid">' + (cat === 'all' ? groups.map(([v, l]) => '<button class="pick group ' + (sel.has(v) ? 'on' : '') + '" data-v="' + esc(v) + '"><span class="dot"></span><span><b>' + esc(l) + '</b><small>Group</small></span></button>').join('') : '')
      + items.map(i => '<button class="pick ' + (sel.has(i.id) ? 'on' : '') + '" data-v="' + esc(i.id) + '">' + thumb(i.id) + '<span><b>' + esc(cleanName(i.name)) + '</b><small>' + (opts.note ? esc(opts.note(i.id)) : KIND_LABEL[kindOf(i.id)] || '') + '</small></span></button>').join('')
      + (groups.length + items.length ? '' : '<p class="muted">Nothing matches.</p>') + '</div>';
    const inp = el.querySelector('input');
    inp.oninput = e => { q = e.target.value.trim().toLowerCase(); const pos = e.target.selectionStart; draw(); const n = el.querySelector('input'); n.focus(); n.setSelectionRange(pos, pos); };
    el.querySelectorAll('[data-cat]').forEach(b => b.onclick = () => { cat = b.dataset.cat; draw(); });
    el.querySelectorAll('[data-v]').forEach(b => b.onclick = async () => { await opts.pick(b.dataset.v); draw(); });
  };
  draw();
  return draw;
}

const ANIM = { runes: 24, flame: 16, enchanted: 16, blinkcat: 2, aurora: 16, emerald: 16, galaxy: 24, midnight: 20, ocean: 16, royal: 16, sakura: 20, surge: 16, void: 16, lava: 20, matrix: 20, storm: 24, phoenix: 20, koi: 24, bloodmoon: 20, obsidian: 16, frost: 16, sunset: 18, ender: 24, diamond: 20, toxic: 20 };
const HALO = { angel: '#ffec9a', surge: '#c66bff', fire: '#ff8c28', enchanted: '#78c8ff', void: '#8a4bff', rose: '#ff6fae', crown: '#ffd54a', orbit: '#7af0ff', rainbow: '#ffffff' };
const EMOTE_SVG = '<svg class="em" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="4.5" r="2.2"/><path d="M12 7v7M12 14l-3.5 6M12 14l3.5 6M6 9.5l6 1.5 6-3"/></svg>';
function thumb(id, big) {
  const k = kindOf(id), key = id.slice(k.length + 1), box = big ? 72 : 36;
  let inner = '';
  if (k === 'cape') {
    const u = box * 0.82 / 16, w = Math.round(10 * u), h = Math.round(16 * u);
    const base = 'width:' + w + 'px;height:' + h + 'px;';
    if (key === 'custom') inner = '<div class="cp" style="' + base + 'background:linear-gradient(#8f50f2,#ec68b0)"></div>';
    else if (key === 'image') inner = '<div class="cp" style="' + base + 'background:linear-gradient(135deg,#3a3a44,#1b1b20);display:grid;place-items:center;color:#a1a1aa;font-size:' + Math.round(u * 3.4) + 'px">IMG</div>';
    else {
      const src = key === 'beta' ? 'cape_overlay_logo' : 'cape_' + key;
      const url = key === 'runes' ? '/img/cape/runes.png' : '/img/cosmetic/' + src + '.png';
      const bg = key === 'beta' ? 'background-color:#141418;' : '';
      inner = '<div class="cp" data-n="' + (ANIM[key] || 1) + '" data-u="' + u + '" style="' + base + bg + 'background-image:url(' + url + ');background-size:' + (64 * u) + 'px auto;background-position:' + (-u) + 'px ' + (-u) + 'px"></div>';
    }
  } else if (k === 'wings') inner = '<div class="wg"><img alt="" src="/img/cosmetic/wings_' + esc(key) + '.png"><img alt="" src="/img/cosmetic/wings_' + esc(key) + '.png"></div>';
  else if (k === 'halo') inner = '<div class="hl" style="color:' + (HALO[key] || '#fff') + (key === 'rainbow' ? ';border-color:transparent;background:conic-gradient(red,orange,yellow,lime,cyan,blue,magenta,red) border-box;-webkit-mask:radial-gradient(farthest-side,transparent calc(100% - 3px),#000 calc(100% - 3px))' : '') + '"></div>';
  else if (k === 'emote') inner = EMOTE_SVG;
  else inner = '<span class="gl">?</span>';
  return '<span class="th' + (big ? ' big' : '') + '" title="' + esc(cleanName(name(id))) + '">' + inner + '</span>';
}
let animFrame = 0;
setInterval(() => {
  animFrame++;
  document.querySelectorAll('.cp[data-n]').forEach(el => {
    const n = +el.dataset.n, u = +el.dataset.u;
    if (n > 1) el.style.backgroundPosition = (-u) + 'px ' + (-u - (animFrame % n) * 32 * u) + 'px';
  });
}, 110);
function giftCard(coins, items, note, who) {
  const n = items.length;
  const title = n && coins ? "You've been gifted " + coins + ' coins and ' + n + ' item' + (n > 1 ? 's' : '') + '!' : n ? "You've been gifted " + n + ' item' + (n > 1 ? 's' : '') + '!' : coins ? "You've been gifted " + coins + ' coins!' : 'Nothing picked yet';
  const tiles = (coins ? '<div class="gi"><span class="th big"><span class="coin"></span></span><b>' + coins + '</b><span>Coins</span></div>' : '')
    + items.map(id => '<div class="gi">' + thumb(id, true) + '<b>' + esc(cleanName(name(id))) + '</b><span>' + (KIND_LABEL[kindOf(id)] || 'Idle') + '</span></div>').join('');
  return '<div class="gp"><h3>' + esc(title) + '</h3><div class="gsub">' + esc(note || 'A gift from Surge') + '</div><div class="gitems">' + (tiles || '<div class="empty">Add coins or pick items to see what they get.</div>') + '</div>' + (who ? '<div class="to">' + esc(who) + '</div>' : '') + '</div>';
}
function itemsLine(coins, items) {
  return '<div class="prevrow">' + (coins ? '<span class="th"><span class="coin"></span></span>' : '') + items.map(id => thumb(id)).join('') + '<span class="sub">' + [coins ? coins + ' coins' : '', ...items.map(name).map(cleanName)].filter(Boolean).map(esc).join(', ') + '</span></div>';
}
const TARGETS = () => [['all', 'Everything'], ['cosmetic', 'All cosmetics'], ['emote', 'All emotes'], ['sub:wings', 'All wings'], ['sub:halo', 'All halos'], ['sub:cape', 'All capes'], ...data.items.map(i => [i.id, i.name])];

async function setupCheck() {
  try {
    const r = await fetch('/admin/api/status');
    const d = await r.json();
    if (d.configured) return true;
  } catch { return true; }
  $('#app').innerHTML = '<div class="login card"><h1>Surge Admin</h1><p>The admin password isn\\'t set up yet.</p><ol class="muted" style="line-height:1.7"><li>Open your GitHub repository, then Settings, Secrets and variables, Actions.</li><li>Add a secret named <b>ADMIN_PASSWORD</b> with the password you want.</li><li>Release a new version (or re-run the latest build). Then reload this page.</li></ol></div>';
  return false;
}

function login(msg) {
  $('#app').innerHTML = '<div class="login card"><h1>Surge Admin</h1><p class="muted">Enter your admin password.</p><form id="lf"><label>Password<input id="pw" type="password" autocomplete="current-password" required></label><p id="lerr" style="color:var(--bad);min-height:1.5em;margin:8px 0">' + esc(msg || '') + '</p><button class="btn" style="width:100%">Sign in</button></form></div>';
  $('#pw').focus();
  $('#lf').onsubmit = async e => {
    e.preventDefault();
    try { const r = await api('login', { password: $('#pw').value }); token = r.token; try { sessionStorage.setItem('surgeAdmin', token); } catch {} load(); }
    catch (err) { $('#lerr').textContent = err.message; }
  };
}
async function load() { try { data = await api('state'); render(); } catch (e) { if (token) toast(e.message, true); } }

function render() {
  const tabs = [['overview', 'Overview'], ['lookup', 'Players'], ['announce', 'Announcement'], ['codes', 'Codes'], ['gifts', 'Gifts'], ['sales', 'Sales'], ['prices', 'Prices'], ['players', 'Icons and capes'], ['locks', 'Locked items'], ['servers', 'Discover']];
  $('#app').innerHTML = '<header><h1>Surge Admin</h1><button class="btn ghost small" id="out">Sign out</button></header>'
    + '<nav class="tabs">' + tabs.map(([k, l]) => '<button class="tab ' + (tab === k ? 'on' : '') + '" data-tab="' + k + '">' + l + '</button>').join('') + '</nav><div id="page"></div>';
  document.querySelectorAll('[data-tab]').forEach(b => b.onclick = () => { tab = b.dataset.tab; render(); });
  $('#out').onclick = async () => { try { await api('logout', {}); } catch {} token = ''; try { sessionStorage.removeItem('surgeAdmin'); } catch {} login(); };
  ({ overview, lookup, announce, codes, gifts, sales, prices, players, locks, servers })[tab]();
}

function ago(t) { if (!t) return 'never'; const s = Math.round((Date.now() - t) / 1000); if (s < 90) return 'just now'; if (s < 5400) return Math.round(s / 60) + ' minutes ago'; if (s < 129600) return Math.round(s / 3600) + ' hours ago'; return Math.round(s / 86400) + ' days ago'; }
function overview() {
  const o = data.overview || {};
  const stat = (n, l) => '<div class="stat"><b>' + (n ?? 0) + '</b><span>' + l + '</span></div>';
  $('#page').innerHTML = '<div class="stats">' + stat(o.online, 'Online now') + stat(o.day, 'Played today') + stat(o.week, 'Played this week') + stat(o.players, 'Players in total') + stat(o.redeemed, 'Codes redeemed today') + stat(o.waiting, 'Gifts not opened yet') + stat(data.codes.length, 'Codes made') + stat(data.sales.length, 'Sales running') + stat(data.locked.length, 'Locked items') + stat(data.items.length, 'Items in the shop') + '</div>'
    + (data.announce ? '<div class="card"><h2>Live announcement</h2><div class="banner">' + esc(data.announce.text) + '</div></div>' : '')
    + '<div class="card"><h2>Online now</h2><div class="list">' + ((o.list || []).map(u => '<div class="item"><span class="dot" style="background:' + (u.status === 'busy' ? '#f87171' : u.status === 'idle' ? '#facc15' : 'var(--good)') + '"></span><div class="grow"><b>' + esc(u.name) + '</b></div><span class="uses">' + esc(u.server || 'In the menus') + '</span><button class="btn ghost small" data-look="' + esc(u.name) + '">Open</button></div>').join('') || '<p class="muted">Nobody is online right now.</p>') + '</div>'
    + '<div class="row" style="margin-top:12px"><button class="btn ghost small" id="oRefresh">Refresh</button></div></div>'
    + '<div class="card"><h2>Latest gifts</h2><div class="list feed">' + (data.gifts.slice(0, 6).map(g => '<div class="item"><div class="grow"><b>' + esc(g.uuid === '*' ? 'Everyone' : g.name) + '</b>' + itemsLine(g.coins, g.items) + '</div><span class="uses">' + (g.uuid === '*' ? (g.got || 0) + ' received' : g.claimed ? 'Received' : 'Waiting') + '</span></div>').join('') || '<p class="muted">No gifts yet.</p>') + '</div></div>'
    + '<div class="card"><h2>Newest codes</h2><div class="list feed">' + (data.codes.slice(0, 6).map(c => '<div class="item"><div class="grow"><div class="code">' + esc(c.code) + '</div>' + itemsLine(c.coins, c.items) + '</div><span class="uses">' + (c.used || 0) + ' used</span></div>').join('') || '<p class="muted">No codes yet.</p>') + '</div></div>';
  $('#oRefresh').onclick = load;
  document.querySelectorAll('[data-look]').forEach(b => b.onclick = () => { lookName = b.dataset.look; tab = 'lookup'; render(); });
}
let lookName = '', lookData = null;
function lookup() {
  const p = lookData;
  $('#page').innerHTML = '<div class="card"><h2>Find a player</h2><form class="row" id="lForm"><label>Minecraft name or UUID<input id="lWho" maxlength="36" value="' + esc(lookName) + '"></label><button class="btn">Look up</button></form></div><div id="lOut"></div>';
  $('#lForm').onsubmit = async e => { e.preventDefault(); lookName = $('#lWho').value.trim(); await fetchPlayer(); };
  if (lookName && (!p || p.query !== lookName)) fetchPlayer(); else if (p) drawPlayer();
}
async function fetchPlayer() {
  try { lookData = await api('player', { player: lookName }); lookData.query = lookName; drawPlayer(); }
  catch (e) { $('#lOut').innerHTML = '<div class="card"><p class="muted">' + esc(e.message) + '</p></div>'; }
}
function drawPlayer() {
  const p = lookData;
  const online = p.seen && Date.now() - p.seen < 180000;
  $('#lOut').innerHTML = '<div class="card"><div class="row" style="align-items:center;margin-bottom:14px"><img alt="" width="48" height="48" style="border-radius:6px;image-rendering:pixelated" src="https://mc-heads.net/avatar/' + esc(p.uuid) + '/48"><div class="grow" style="flex:1"><h2 style="margin:0">' + esc(p.name) + '</h2><div class="muted">' + (p.known ? (online ? '<span class="dot"></span>Online' + (p.server ? ' on ' + esc(p.server) : '') : 'Last seen ' + ago(p.seen)) : 'Has never opened Surge') + '</div></div></div>'
    + '<div class="kv"><span>UUID</span><div class="code" style="font-size:13px">' + esc(p.uuid) + '</div><span>Icon</span><div>' + esc(BADGE_LABEL[p.badge] || 'None') + '</div><span>Picture cape</span><div>' + (p.cape ? 'Yes' : 'No') + '</div><span>Friends</span><div>' + p.friends + '</div>' + (p.country ? '<span>Country</span><div>' + esc(p.country) + '</div>' : '') + '<span>Personal sales</span><div>' + (p.sales.map(s => s.percent + '% off ' + esc(name(s.target) || s.target)).join(', ') || 'None') + '</div></div></div>'
    + '<div class="card"><h2>Quick gift</h2><div class="row"><label style="max-width:140px">Coins<input id="qCoins" type="number" min="1" value="500"></label><label>Message<input id="qNote" maxlength="140" placeholder="Optional"></label><button class="btn" id="qGift">Send</button></div></div>'
    + '<div class="card"><h2>Icon</h2><div class="row"><label style="max-width:240px">Icon<select id="qBadge"><option value="">None</option>' + data.badgeKinds.map(k => '<option value="' + k + '"' + (p.badge === k ? ' selected' : '') + '>' + esc(BADGE_LABEL[k]) + '</option>').join('') + '</select></label><button class="btn" id="qBadgeSet">Save</button></div></div>'
    + '<div class="card"><h2>Gifts</h2><div class="list">' + (p.gifts.map(g => '<div class="item"><div class="grow">' + itemsLine(g.coins, g.items) + (g.note ? '<div class="sub">' + esc(g.note) + '</div>' : '') + '</div><span class="uses">' + (g.claimed ? 'Received' : 'Waiting') + '</span></div>').join('') || '<p class="muted">No gifts yet.</p>') + '</div></div>'
    + '<div class="card"><h2>Codes used</h2><div class="list">' + (p.codes.map(c => '<div class="item"><div class="grow code">' + esc(c.code || 'Deleted code') + '</div><span class="uses">' + ago(c.at) + '</span></div>').join('') || '<p class="muted">No codes used.</p>') + '</div></div>';
  $('#qGift').onclick = async () => { try { await api('gifts', { player: p.uuid, coins: +$('#qCoins').value, note: $('#qNote').value.trim(), items: [] }); toast('Gift sent to ' + p.name); fetchPlayer(); } catch (e) { toast(e.message, true); } };
  $('#qBadgeSet').onclick = async () => { const v = $('#qBadge').value; try { await api('badges', v ? { player: p.uuid, badge: v } : { player: p.uuid, remove: true }); toast('Saved'); fetchPlayer(); } catch (e) { toast(e.message, true); } };
}
function announce() {
  const a = data.announce;
  $('#page').innerHTML = '<div class="card"><h2>Announcement</h2><p class="muted">Shows at the top of the launcher and as a popup in game, once per announcement.</p>'
    + (a ? '<div class="banner" style="margin:10px 0">' + esc(a.text) + '<div class="muted" style="margin-top:6px">' + (a.until ? 'Ends ' + new Date(a.until).toLocaleString() : 'No end date') + '</div></div>' : '')
    + '<div class="row"><label>Message<input id="aText" maxlength="200" placeholder="Double coins weekend is live!"></label><label style="max-width:150px">Show for (hours)<input id="aHours" type="number" min="0" value="48" title="0 means until you remove it"></label></div>'
    + '<div class="row" style="margin-top:12px"><button class="btn" id="aSave">Publish</button>' + (a ? '<button class="btn ghost" id="aClear">Remove</button>' : '') + '</div></div>';
  $('#aSave').onclick = async () => { const t = $('#aText').value.trim(); if (!t) return toast('Write a message first', true); try { await api('announce', { text: t, hours: +$('#aHours').value }); toast('Published'); load(); } catch (e) { toast(e.message, true); } };
  if (a) $('#aClear').onclick = async () => { try { await api('announce', { text: '' }); toast('Removed'); load(); } catch (e) { toast(e.message, true); } };
}

function codes() {
  $('#page').innerHTML = '<div class="card"><h2>Make codes</h2><div class="row">'
    + '<label>Your own code (optional)<input id="cCode" maxlength="24" placeholder="Leave empty for random codes"></label>'
    + '<label style="max-width:120px">Coins<input id="cCoins" type="number" min="0" value="0"></label>'
    + '<label style="max-width:130px">Max uses<input id="cUses" type="number" min="0" value="0" title="0 means unlimited"></label>'
    + '<label style="max-width:110px" id="cCountL">How many<input id="cCount" type="number" min="1" max="100" value="1"></label></div>'
    + '<p class="muted" style="margin:8px 0 0">Players type the code in the game under Wardrobe, Coins, Redeem a code. It works right away, no publishing needed. Each player can use a code once. Max uses limits how many players can use it, 0 means no limit.</p>'
    + '<h2 style="margin-top:16px">Unlocks</h2><div class="picker" id="cPick"></div>'
    + '<h2 style="margin-top:16px">What players get</h2><div id="cPrev"></div>'
    + '<div class="row" style="margin-top:12px"><button class="btn" id="cMake">Create</button></div></div>'
    + '<div class="card"><h2>Codes (' + data.codes.length + ')</h2><div class="list">' + (data.codes.map(c => '<div class="item"><div class="grow"><div class="code">' + esc(c.code) + '</div>' + itemsLine(c.coins, c.items) + '</div><span class="uses">' + (c.max_uses ? (c.used || 0) + ' / ' + c.max_uses + ' used' : (c.used || 0) + ' used') + '</span><button class="btn ghost small" data-copy="' + esc(c.code) + '">Copy</button><button class="btn ghost small" data-del="' + c.hash + '">Delete</button></div>').join('') || '<p class="muted">No codes yet.</p>') + '</div></div>';
  const prevC = () => { const code = $('#cCode').value.trim().toUpperCase(); const uses = +$('#cUses').value; $('#cPrev').innerHTML = giftCard(+$('#cCoins').value, picked, 'Redeemed with code ' + (code || 'XXXX-XXXX'), (uses ? uses + ' players can use it' : 'Anyone can use it once') + (code || +$('#cCount').value <= 1 ? '' : ', ' + $('#cCount').value + ' different codes')); };
  picker($('#cPick'), { multi: true, value: () => picked, pick: v => { picked = picked.includes(v) ? picked.filter(x => x !== v) : [...picked, v]; prevC(); } });
  ['#cCoins', '#cUses', '#cCount'].forEach(q => $(q).oninput = prevC);
  $('#cCode').oninput = e => { $('#cCountL').style.display = e.target.value.trim() ? 'none' : ''; prevC(); };
  prevC();
  $('#cMake').onclick = async () => {
    try {
      const r = await api('codes', { code: $('#cCode').value.trim(), coins: +$('#cCoins').value, uses: +$('#cUses').value, count: +$('#cCount').value, items: picked });
      try { await navigator.clipboard.writeText(r.made.join('\\n')); } catch {}
      toast(r.made.length === 1 ? 'Created ' + r.made[0] + ' (copied)' : 'Created ' + r.made.length + ' codes (copied)');
      picked = []; load();
    } catch (e) { toast(e.message, true); }
  };
  document.querySelectorAll('[data-copy]').forEach(b => b.onclick = async () => { try { await navigator.clipboard.writeText(b.dataset.copy); toast('Copied'); } catch { toast('Copy it by hand: ' + b.dataset.copy); } });
  document.querySelectorAll('[data-del]').forEach(b => b.onclick = async () => { if (!confirm('Delete this code? Players who already redeemed it keep their rewards.')) return; try { await api('codes/delete', { hash: b.dataset.del }); load(); } catch (e) { toast(e.message, true); } });
}

function sales() {
  const tn = t => (TARGETS().find(x => x[0] === t) || [t, t])[1];
  $('#page').innerHTML = '<div class="card"><h2>Start a sale</h2><div class="row">'
    + '<label style="max-width:120px">Percent off<input id="sP" type="number" min="5" max="100" value="20"></label>'
    + '<label>Only for one player (optional)<input id="sWho" maxlength="36" placeholder="Minecraft name, empty for everyone"></label>'
    + '<label style="max-width:140px">Length<select id="sL"><option value="24">1 day</option><option value="72">3 days</option><option value="168">1 week</option><option value="0">No end</option></select></label>'
    + '</div><h2 style="margin-top:16px">On <span id="sOn" style="color:var(--text)">Everything</span></h2><div class="picker" id="sPick"></div>'
    + '<h2 style="margin-top:16px">Preview</h2><div id="sPrev"></div>'
    + '<div class="row" style="margin-top:12px"><button class="btn" id="sAdd">Start sale</button></div></div>'
    + '<div class="card"><h2>Running sales</h2><div class="list">' + (data.sales.map(s => '<div class="item"><div class="grow"><div>' + s.percent + '% off ' + esc(tn(s.target)) + (s.name ? ' for ' + esc(s.name) : '') + '</div><div class="sub">' + (s.until ? 'Ends ' + new Date(s.until).toLocaleString() : 'No end') + '</div></div><button class="btn ghost small" data-end="' + s.id + '">End</button></div>').join('') || '<p class="muted">No sales running.</p>') + '</div></div>';
  let target = 'all';
  const price = id => data.prices[id] ?? (data.items.find(i => i.id === id) || {}).price ?? 0;
  const inTarget = t => data.items.filter(i => t === 'all' || (t === 'cosmetic' && ['wings', 'halo', 'cape'].includes(kindOf(i.id))) || (t === 'emote' && kindOf(i.id) === 'emote') || (t.startsWith('sub:') && kindOf(i.id) === t.slice(4)) || i.id === t);
  const prevS = () => {
    const pct = Math.max(0, Math.min(100, +$('#sP').value || 0)), list = inTarget(target);
    $('#sPrev').innerHTML = '<div class="gp"><h3>' + pct + '% off ' + esc(tn(target)) + '</h3><div class="gsub">' + list.length + ' item' + (list.length === 1 ? '' : 's') + ($('#sWho').value.trim() ? ', only for ' + esc($('#sWho').value.trim()) : ', for everyone') + '</div><div class="gitems">'
      + list.slice(0, 10).map(i => '<div class="gi">' + thumb(i.id, true) + '<b>' + esc(cleanName(i.name)) + '</b><span><s>' + price(i.id) + '</s> ' + Math.round(price(i.id) * (100 - pct) / 100) + '</span></div>').join('')
      + (list.length > 10 ? '<div class="gi"><span class="th big"><span class="gl">+' + (list.length - 10) + '</span></span><b>more</b></div>' : '') + '</div></div>';
  };
  picker($('#sPick'), { groups: TARGETS().slice(0, 6), value: () => [target], pick: v => { target = v; $('#sOn').textContent = tn(v); prevS(); } });
  ['#sP', '#sWho'].forEach(q => $(q).oninput = prevS);
  prevS();
  $('#sAdd').onclick = async () => { try { await api('sales', { target, percent: +$('#sP').value, hours: +$('#sL').value, player: $('#sWho').value.trim() }); toast('Sale started'); load(); } catch (e) { toast(e.message, true); } };
  document.querySelectorAll('[data-end]').forEach(b => b.onclick = async () => { try { await api('sales/delete', { id: +b.dataset.end }); load(); } catch (e) { toast(e.message, true); } });
}

const BADGE_LABEL = { owner: 'Owner', staff: 'Staff', booster: 'Server Booster', beta: 'Beta Tester', media: 'Creator', vip: 'VIP', early: 'Early Supporter', dev: 'Developer' };
const BADGE_COLOR = { owner: '#f2c430', staff: '#ff6060', booster: '#fa8cff', beta: '#5aa0ff', media: '#ff4f6d', vip: '#ffd25a', early: '#5fd67a', dev: '#ff9a3c' };
let giftPicked = [];
function gifts() {
  $('#page').innerHTML = '<div class="card"><h2>Send a gift</h2><div class="row">'
    + '<label id="gWhoL">Player<input id="gWho" maxlength="36" placeholder="Minecraft name"></label>'
    + '<label style="max-width:120px">Coins<input id="gCoins" type="number" min="0" value="0"></label>'
    + '<label class="check"><input type="checkbox" id="gAll"> Everyone</label>'
    + '<label style="max-width:150px;display:none" id="gHoursL">Can claim for (hours)<input id="gHours" type="number" min="0" value="72" title="0 means forever"></label></div>'
    + '<div class="row" style="margin-top:10px"><label>Message (optional)<input id="gNote" maxlength="140" placeholder="Thanks for boosting the server!"></label></div>'
    + '<p class="muted" style="margin:8px 0 0">The player gets it the next time they open Surge, with a popup that shows what they got.</p>'
    + '<h2 style="margin-top:16px">Items</h2><div class="picker" id="gPick"></div>'
    + '<h2 style="margin-top:16px">What they will see</h2><div id="gPrev"></div>'
    + '<div class="row" style="margin-top:12px"><button class="btn" id="gSend">Send gift</button></div></div>'
    + '<div class="card"><h2>Recent gifts</h2><div class="list">' + (data.gifts.map(g => '<div class="item"><div class="grow"><div><b>' + esc(g.name) + '</b></div>' + itemsLine(g.coins, g.items) + (g.note ? '<div class="sub">' + esc(g.note) + '</div>' : '') + '</div><span class="uses">' + (g.uuid === '*' ? (g.got || 0) + ' received' + (g.claimed ? (g.claimed > Date.now() ? ', ends ' + new Date(g.claimed).toLocaleDateString() : ', ended') : '') : (g.claimed ? 'Received' : 'Waiting')) + '</span>' + (g.claimed && g.uuid !== '*' ? '' : '<button class="btn ghost small" data-gdel="' + g.id + '">' + (g.uuid === '*' ? 'Stop' : 'Cancel') + '</button>') + '</div>').join('') || '<p class="muted">No gifts yet.</p>') + '</div></div>';
  const prevG = () => { const all = $('#gAll').checked, who = $('#gWho').value.trim(); $('#gPrev').innerHTML = giftCard(+$('#gCoins').value, giftPicked, $('#gNote').value.trim(), all ? 'Goes to every Surge player' + (+$('#gHours').value ? ' for ' + $('#gHours').value + ' hours' : '') : who ? 'Goes to ' + who : 'Type a player name'); };
  picker($('#gPick'), { multi: true, value: () => giftPicked, pick: v => { giftPicked = giftPicked.includes(v) ? giftPicked.filter(x => x !== v) : [...giftPicked, v]; prevG(); } });
  ['#gWho', '#gCoins', '#gNote', '#gHours'].forEach(q => $(q).oninput = prevG);
  $('#gAll').onchange = () => { const all = $('#gAll').checked; $('#gWhoL').style.display = all ? 'none' : ''; $('#gHoursL').style.display = all ? '' : 'none'; prevG(); };
  prevG();
  $('#gSend').onclick = async () => {
    const all = $('#gAll').checked;
    if (all && !confirm('Send this gift to every Surge player?')) return;
    try { const r = await api('gifts', { everyone: all, hours: +$('#gHours').value, player: $('#gWho').value.trim(), coins: +$('#gCoins').value, note: $('#gNote').value.trim(), items: giftPicked }); toast(all ? 'Gift sent to everyone' : 'Gift sent to ' + r.name); giftPicked = []; load(); }
    catch (e) { toast(e.message, true); }
  };
  document.querySelectorAll('[data-gdel]').forEach(b => b.onclick = async () => { try { await api('gifts/delete', { id: +b.dataset.gdel }); load(); } catch (e) { toast(e.message, true); } });
}
function prices() {
  let sel = data.items[0].id;
  const cur = id => data.prices[id] ?? (data.items.find(i => i.id === id) || {}).price;
  $('#page').innerHTML = '<div class="card"><h2>Change a price</h2><p class="muted">The new price shows for everyone right away. Sales are taken off the new price.</p>'
    + '<div class="row"><span id="pThumb"></span><label>Item<select id="pItem">' + data.items.map(i => '<option value="' + esc(i.id) + '">' + esc(cleanName(i.name)) + '</option>').join('') + '</select></label>'
    + '<label style="max-width:140px">Price<input id="pVal" type="number" min="0"></label><button class="btn" id="pSet">Save</button><button class="btn ghost" id="pReset">Back to normal</button></div>'
    + '<p class="muted" id="pNote"></p></div>'
    + '<div class="card"><h2>Change all prices</h2><div class="row"><label style="max-width:160px">Percent of normal price<input id="pPct" type="number" min="1" max="1000" value="100"></label><button class="btn" id="pAll">Apply to everything</button></div><p class="muted">100% puts every price back to normal.</p></div>'
    + '<div class="card"><h2>Changed prices</h2><div class="list">' + (Object.keys(data.prices).map(id => '<div class="item">' + thumb(id) + '<div class="grow">' + esc(cleanName(name(id))) + '</div><span class="uses">' + data.prices[id] + ' coins (normally ' + ((data.items.find(i => i.id === id) || {}).price ?? '?') + ')</span></div>').join('') || '<p class="muted">Every item has its normal price.</p>') + '</div></div>';
  const show = () => { sel = $('#pItem').value; $('#pThumb').innerHTML = thumb(sel, true); $('#pVal').value = cur(sel); const it = data.items.find(i => i.id === sel); $('#pNote').textContent = 'Normal price: ' + it.price + ' coins'; };
  $('#pItem').onchange = show; show();
  $('#pSet').onclick = async () => { try { await api('prices', { item: sel, price: +$('#pVal').value }); toast('Price saved'); load(); } catch (e) { toast(e.message, true); } };
  $('#pReset').onclick = async () => { try { await api('prices', { item: sel, price: null }); toast('Price reset'); load(); } catch (e) { toast(e.message, true); } };
  $('#pAll').onclick = async () => { const base = {}; data.items.forEach(i => base[i.id] = i.price); try { await api('prices/scale', { percent: +$('#pPct').value, base }); toast('Prices updated'); load(); } catch (e) { toast(e.message, true); } };
}
function players() {
  let badge = 'booster';
  $('#page').innerHTML = '<div class="card"><h2>Give a name icon</h2><p class="muted">Shows next to their name above their head and in the tab list, instead of the normal Surge icon.</p>'
    + '<div class="row"><label>Player<input id="bWho" maxlength="36" placeholder="Minecraft name"></label></div><div class="chips" id="bKinds" style="margin-top:10px"></div>'
    + '<div class="row" style="margin-top:12px"><button class="btn" id="bGive">Give icon</button></div>'
    + '<div class="list">' + (data.badges.map(b => '<div class="item"><span style="width:12px;height:12px;background:' + (BADGE_COLOR[b.badge] || '#fff') + '"></span><div class="grow"><b>' + esc(b.name) + '</b> <span class="sub">' + esc(BADGE_LABEL[b.badge] || b.badge) + '</span></div><button class="btn ghost small" data-brm="' + esc(b.uuid) + '">Remove</button></div>').join('') || '<p class="muted">Nobody has a special icon yet.</p>') + '</div></div>'
    + '<div class="card"><h2>Picture cape</h2><p class="muted">Upload any picture and it becomes that player\\'s own cape. A 64 by 32 or 512 by 256 cape texture is used as it is, any other picture is fitted on the back. Only you can give these out.</p>'
    + '<div class="row"><label>Player<input id="cWho" maxlength="36" placeholder="Minecraft name"></label><label>Picture<input id="cFile" type="file" accept="image/png,image/jpeg,image/webp"></label><button class="btn" id="cUp">Upload and gift</button></div>'
    + '<div class="list">' + (data.capes.map(c => '<div class="item"><img alt="" width="40" height="64" style="object-fit:cover;background:#000" src="/v1/cape/' + esc(c.uuid) + '?v=' + c.v + '"><div class="grow"><b>' + esc(c.name) + '</b></div><button class="btn ghost small" data-crm="' + esc(c.uuid) + '">Remove</button></div>').join('') || '<p class="muted">No picture capes yet.</p>') + '</div></div>';
  const drawKinds = () => { $('#bKinds').innerHTML = data.badgeKinds.map(k => '<button class="pill ' + (k === badge ? 'on' : '') + '" data-k="' + k + '"><span style="display:inline-block;width:10px;height:10px;margin-right:6px;background:' + BADGE_COLOR[k] + '"></span>' + BADGE_LABEL[k] + '</button>').join(''); document.querySelectorAll('[data-k]').forEach(b => b.onclick = () => { badge = b.dataset.k; drawKinds(); }); };
  drawKinds();
  $('#bGive').onclick = async () => { try { const r = await api('badges', { player: $('#bWho').value.trim(), badge }); toast(r.name + ' got the ' + BADGE_LABEL[badge] + ' icon'); load(); } catch (e) { toast(e.message, true); } };
  document.querySelectorAll('[data-brm]').forEach(b => b.onclick = async () => { try { await api('badges', { player: b.dataset.brm, badge: '' }); load(); } catch (e) { toast(e.message, true); } });
  document.querySelectorAll('[data-crm]').forEach(b => b.onclick = async () => { try { await api('capes/delete', { uuid: b.dataset.crm }); load(); } catch (e) { toast(e.message, true); } });
  $('#cUp').onclick = async () => {
    const f = $('#cFile').files[0];
    if (!f) { toast('Pick a picture first', true); return; }
    try {
      const png = await toPng(f);
      const r = await api('capes', { player: $('#cWho').value.trim(), png });
      toast('Picture cape sent to ' + r.name); load();
    } catch (e) { toast(e.message, true); }
  };
}
function toPng(file) {
  return new Promise((res, rej) => {
    const img = new Image();
    img.onload = () => {
      let w = img.width, h = img.height;
      const max = w === h * 2 ? 1024 : 512;
      const k = Math.min(1, max / Math.max(w, h));
      w = Math.max(1, Math.round(w * k)); h = Math.max(1, Math.round(h * k));
      const c = document.createElement('canvas'); c.width = w; c.height = h;
      const x = c.getContext('2d'); x.imageSmoothingEnabled = k < 1; x.drawImage(img, 0, 0, w, h);
      res(c.toDataURL('image/png').split(',')[1]);
    };
    img.onerror = () => rej(new Error('That file is not a picture'));
    img.src = URL.createObjectURL(file);
  });
}

function locks() {
  $('#page').innerHTML = '<div class="card"><h2>Locked items</h2><p class="muted">Click an item to lock or unlock it. Locked items can\\'t be bought, players only get them from a code.</p><div class="picker" id="lPick"></div></div>';
  picker($('#lPick'), { multi: true, countWord: 'locked', value: () => data.locked, note: id => data.locked.includes(id) ? 'Locked' : 'In shop', pick: async v => { try { const r = await api('locks/toggle', { item: v }); data.locked = r.locked; } catch (e) { toast(e.message, true); } } });
}

function servers() {
  $('#page').innerHTML = '<div class="card"><h2>Add a partnered server</h2><p class="muted">These show on the Discover tab next to the server list.</p><div class="row">'
    + '<label>Name<input id="dN" maxlength="32" placeholder="Example Network"></label><label>Address<input id="dI" placeholder="play.example.net"></label><label style="max-width:130px">Tag<input id="dT" maxlength="16" placeholder="Partner"></label></div>'
    + '<div class="row" style="margin-top:10px"><label>Short description<input id="dD" maxlength="80" placeholder="Bedwars, Skywars and Duels"></label><button class="btn" id="dAdd">Add</button></div></div>'
    + '<div class="card"><h2>Servers</h2><div class="list">' + (data.servers.map(s => '<div class="item"><div class="grow"><div><b>' + esc(s.name) + '</b> <span class="sub">' + esc(s.ip) + (s.tag ? ' · ' + esc(s.tag) : '') + '</span></div><div class="sub">' + esc(s.description) + '</div></div><button class="btn ghost small" data-rm="' + s.id + '">Remove</button></div>').join('') || '<p class="muted">No servers yet.</p>') + '</div></div>';
  $('#dAdd').onclick = async () => { try { await api('servers', { name: $('#dN').value, ip: $('#dI').value, tag: $('#dT').value, description: $('#dD').value }); toast('Server added'); load(); } catch (e) { toast(e.message, true); } };
  document.querySelectorAll('[data-rm]').forEach(b => b.onclick = async () => { try { await api('servers/delete', { id: +b.dataset.rm }); load(); } catch (e) { toast(e.message, true); } });
}

setupCheck().then(ok => { if (ok) token ? load() : login(); });
</script></body></html>`;
