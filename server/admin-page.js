export const ADMIN_PAGE = `<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="robots" content="noindex"><title>Surge Admin</title>
<link href="https://fonts.googleapis.com/css2?family=Pixelify+Sans:wght@600&family=Inter:wght@400;600&display=swap" rel="stylesheet">
<style>
:root{--bg:#121214;--panel:#1c1c1f;--line:#34343a;--violet:#f4f4f5;--lilac:#e4e4e7;--text:#fafafa;--muted:#a1a1aa;--bad:#ff8c8c;--good:#8cffa0}
*{box-sizing:border-box}html,body{margin:0}body{background:var(--bg);color:var(--text);font:15px/1.5 Inter,system-ui,sans-serif;min-height:100vh}
button,input,select{font:inherit;color:inherit}
.wrap{max-width:860px;margin:0 auto;padding:28px 16px 60px}
h1{font:600 30px "Pixelify Sans",sans-serif;margin:0}
header{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:22px}
.tabs{display:flex;gap:6px;flex-wrap:wrap;margin-bottom:18px}
.tab{background:transparent;border:2px solid var(--line);padding:7px 14px;cursor:pointer;color:var(--muted)}
.tab.on{background:var(--violet);border-color:var(--violet);color:#18181b}
.card{background:var(--panel);border:2px solid var(--line);padding:16px;margin-bottom:14px}
.card h2{font-size:16px;margin:0 0 12px}
.row{display:flex;gap:10px;flex-wrap:wrap;align-items:flex-end}
label{display:flex;flex-direction:column;gap:4px;font-size:13px;color:var(--muted);flex:1;min-width:120px}
input,select{background:#121214;border:2px solid var(--line);padding:8px 10px;width:100%}
input:focus,select:focus{outline:none;border-color:var(--violet)}
.btn{background:var(--violet);border:0;padding:9px 16px;cursor:pointer;color:#18181b;font-weight:600;white-space:nowrap}
.btn.ghost{background:transparent;border:2px solid var(--line);color:var(--text)}
.btn.small{padding:5px 10px;font-size:13px}
.btn:focus-visible,.tab:focus-visible{outline:2px solid var(--lilac);outline-offset:2px}
.list{display:flex;flex-direction:column;gap:6px;margin-top:12px}
.item{display:flex;align-items:center;gap:10px;padding:9px 10px;background:#18181b;border:1px solid var(--line)}
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
.pill{background:transparent;border:2px solid var(--line);padding:4px 10px;cursor:pointer;color:var(--muted);font-size:13px}
.pill.on{border-color:var(--violet);color:var(--text)}
.pick-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:6px;max-height:300px;overflow:auto;padding-right:4px}
.pick{border:2px solid var(--line);background:#18181b;padding:8px 10px;cursor:pointer;text-align:left;display:flex;gap:8px;align-items:center}
.pick:hover{border-color:#55555c}
.pick.on{border-color:var(--violet);background:#27272a}
.pick .dot{width:10px;height:10px;flex:none;border:2px solid var(--line)}
.pick.on .dot{background:var(--violet);border-color:var(--violet)}
.pick b{font-weight:600;font-size:14px;display:block}.pick small{color:var(--muted);font-size:12px}
.pick.group{border-style:dashed}
.uses{font-size:12px;color:var(--muted);white-space:nowrap}
@media (max-width:560px){.item{flex-wrap:wrap}}
</style></head><body><div class="wrap" id="app"></div><div class="toast" id="toast"></div>
<script>
const $ = s => document.querySelector(s);
let token = '';
try { token = sessionStorage.getItem('surgeAdmin') || ''; } catch {}
let data = null, tab = 'codes', picked = [];
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
const cleanName = n => n.endsWith(' (emote)') ? n.slice(0, -8) : n;
function picker(el, opts) {
  let cat = 'all', q = '';
  const draw = () => {
    const sel = new Set(opts.value());
    const groups = (opts.groups || []).filter(([, l]) => !q || l.toLowerCase().includes(q));
    const items = data.items.filter(i => (cat === 'all' || kindOf(i.id) === cat) && (!q || i.name.toLowerCase().includes(q)));
    el.innerHTML = '<div class="pick-bar"><input placeholder="Search items" value="' + esc(q) + '">' + CATS.map(([k, l]) => '<button class="pill ' + (cat === k ? 'on' : '') + '" data-cat="' + k + '">' + l + '</button>').join('') + (opts.multi ? '<span class="muted">' + sel.size + ' ' + (opts.countWord || 'picked') + '</span>' : '') + '</div>'
      + '<div class="pick-grid">' + (cat === 'all' ? groups.map(([v, l]) => '<button class="pick group ' + (sel.has(v) ? 'on' : '') + '" data-v="' + esc(v) + '"><span class="dot"></span><span><b>' + esc(l) + '</b><small>Group</small></span></button>').join('') : '')
      + items.map(i => '<button class="pick ' + (sel.has(i.id) ? 'on' : '') + '" data-v="' + esc(i.id) + '"><span class="dot"></span><span><b>' + esc(cleanName(i.name)) + '</b><small>' + (opts.note ? esc(opts.note(i.id)) : KIND_LABEL[kindOf(i.id)] || '') + '</small></span></button>').join('')
      + (groups.length + items.length ? '' : '<p class="muted">Nothing matches.</p>') + '</div>';
    const inp = el.querySelector('input');
    inp.oninput = e => { q = e.target.value.trim().toLowerCase(); const pos = e.target.selectionStart; draw(); const n = el.querySelector('input'); n.focus(); n.setSelectionRange(pos, pos); };
    el.querySelectorAll('[data-cat]').forEach(b => b.onclick = () => { cat = b.dataset.cat; draw(); });
    el.querySelectorAll('[data-v]').forEach(b => b.onclick = async () => { await opts.pick(b.dataset.v); draw(); });
  };
  draw();
  return draw;
}
const TARGETS = () => [['all', 'Everything'], ['cosmetic', 'All cosmetics'], ['emote', 'All emotes'], ['sub:wings', 'All wings'], ['sub:halo', 'All halos'], ['sub:cape', 'All capes'], ...data.items.map(i => [i.id, i.name])];

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
  const tabs = [['codes', 'Codes'], ['sales', 'Sales'], ['locks', 'Locked items'], ['servers', 'Discover']];
  $('#app').innerHTML = '<header><h1>Surge Admin</h1><button class="btn ghost small" id="out">Sign out</button></header>'
    + '<nav class="tabs">' + tabs.map(([k, l]) => '<button class="tab ' + (tab === k ? 'on' : '') + '" data-tab="' + k + '">' + l + '</button>').join('') + '</nav><div id="page"></div>';
  document.querySelectorAll('[data-tab]').forEach(b => b.onclick = () => { tab = b.dataset.tab; render(); });
  $('#out').onclick = async () => { try { await api('logout', {}); } catch {} token = ''; try { sessionStorage.removeItem('surgeAdmin'); } catch {} login(); };
  ({ codes, sales, locks, servers })[tab]();
}

function codes() {
  $('#page').innerHTML = '<div class="card"><h2>Make codes</h2><div class="row">'
    + '<label>Your own code (optional)<input id="cCode" maxlength="24" placeholder="Leave empty for random codes"></label>'
    + '<label style="max-width:120px">Coins<input id="cCoins" type="number" min="0" value="0"></label>'
    + '<label style="max-width:130px">Max uses<input id="cUses" type="number" min="0" value="0" title="0 means unlimited"></label>'
    + '<label style="max-width:110px" id="cCountL">How many<input id="cCount" type="number" min="1" max="100" value="1"></label></div>'
    + '<p class="muted" style="margin:8px 0 0">Max uses counts players, each player can redeem a code once. 0 means unlimited.</p>'
    + '<h2 style="margin-top:16px">Unlocks</h2><div class="picker" id="cPick"></div>'
    + '<div class="row" style="margin-top:12px"><button class="btn" id="cMake">Create</button></div></div>'
    + '<div class="card"><h2>Codes (' + data.codes.length + ')</h2><div class="list">' + (data.codes.map(c => '<div class="item"><div class="grow"><div class="code">' + esc(c.code) + '</div><div class="sub">' + (c.coins ? c.coins + ' coins' : '') + (c.coins && c.items.length ? ' + ' : '') + c.items.map(name).map(cleanName).map(esc).join(', ') + '</div></div><span class="uses">' + (c.max_uses ? (c.used || 0) + ' / ' + c.max_uses + ' used' : (c.used || 0) + ' used') + '</span><button class="btn ghost small" data-copy="' + esc(c.code) + '">Copy</button><button class="btn ghost small" data-del="' + c.hash + '">Delete</button></div>').join('') || '<p class="muted">No codes yet.</p>') + '</div></div>';
  picker($('#cPick'), { multi: true, value: () => picked, pick: v => { picked = picked.includes(v) ? picked.filter(x => x !== v) : [...picked, v]; } });
  $('#cCode').oninput = e => { $('#cCountL').style.display = e.target.value.trim() ? 'none' : ''; };
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
    + '<label style="max-width:120px">Percent off<input id="sP" type="number" min="5" max="90" value="20"></label>'
    + '<label style="max-width:140px">Length<select id="sL"><option value="24">1 day</option><option value="72">3 days</option><option value="168">1 week</option><option value="0">No end</option></select></label>'
    + '</div><h2 style="margin-top:16px">On <span id="sOn" style="color:var(--text)">Everything</span></h2><div class="picker" id="sPick"></div>'
    + '<div class="row" style="margin-top:12px"><button class="btn" id="sAdd">Start sale</button></div></div>'
    + '<div class="card"><h2>Running sales</h2><div class="list">' + (data.sales.map(s => '<div class="item"><div class="grow"><div>' + s.percent + '% off ' + esc(tn(s.target)) + '</div><div class="sub">' + (s.until ? 'Ends ' + new Date(s.until).toLocaleString() : 'No end') + '</div></div><button class="btn ghost small" data-end="' + s.id + '">End</button></div>').join('') || '<p class="muted">No sales running.</p>') + '</div></div>';
  let target = 'all';
  picker($('#sPick'), { groups: TARGETS().slice(0, 6), value: () => [target], pick: v => { target = v; $('#sOn').textContent = tn(v); } });
  $('#sAdd').onclick = async () => { try { await api('sales', { target, percent: +$('#sP').value, hours: +$('#sL').value }); toast('Sale started'); load(); } catch (e) { toast(e.message, true); } };
  document.querySelectorAll('[data-end]').forEach(b => b.onclick = async () => { try { await api('sales/delete', { id: +b.dataset.end }); load(); } catch (e) { toast(e.message, true); } });
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

token ? load() : login();
</script></body></html>`;
