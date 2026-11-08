const fs = require('fs');
const path = require('path');
const os = require('os');
const crypto = require('crypto');
const paths = require('./paths');
const social = require('./social');

const CATALOG = [{"id": "wings:angel", "name": "Angel Wings", "price": 1200}, {"id": "wings:surge", "name": "Surge Wings", "price": 1500}, {"id": "wings:soul", "name": "Soul Wings", "price": 1100}, {"id": "wings:flame", "name": "Flame Wings", "price": 1100}, {"id": "wings:void", "name": "Void Wings", "price": 1300}, {"id": "wings:crystal", "name": "Crystal Wings", "price": 1400}, {"id": "wings:gold", "name": "Golden Wings", "price": 1600}, {"id": "halo:angel", "name": "Angel Halo", "price": 900}, {"id": "halo:surge", "name": "Surge Halo", "price": 1000}, {"id": "halo:fire", "name": "Fire Halo", "price": 750}, {"id": "halo:enchanted", "name": "Enchanted Halo", "price": 800}, {"id": "halo:void", "name": "Void Halo", "price": 850}, {"id": "halo:rose", "name": "Rose Halo", "price": 700}, {"id": "cape:surge", "name": "Surge Cape", "price": 1000}, {"id": "cape:void", "name": "Void Cape", "price": 800}, {"id": "cape:flame", "name": "Flame Cape", "price": 1200}, {"id": "cape:ocean", "name": "Ocean Cape", "price": 700}, {"id": "cape:aurora", "name": "Aurora Cape", "price": 900}, {"id": "cape:sakura", "name": "Sakura Cape", "price": 700}, {"id": "cape:midnight", "name": "Midnight Cape", "price": 750}, {"id": "cape:royal", "name": "Royal Cape", "price": 950}, {"id": "cape:emerald", "name": "Emerald Cape", "price": 700}, {"id": "cape:galaxy", "name": "Galaxy Cape", "price": 1100}, {"id": "cape:custom", "name": "Color Cape", "price": 300}, {"id": "cape:blinkcat", "name": "Blink Cat Cape", "price": 1500}, {"id": "cape:enchanted", "name": "Enchanted Cape", "price": 1800}, {"id": "cape:runes", "name": "Rune Cape", "price": 1600}, {"id": "cape:beta", "name": "Beta Tester Cape", "price": 0}, {"id": "cape:image", "name": "Picture Cape", "price": 0}, {"id": "emote:WAVE", "name": "Wave", "price": 600}, {"id": "emote:CLAP", "name": "Clap", "price": 600}, {"id": "emote:FACEPALM", "name": "Facepalm", "price": 650}, {"id": "emote:DAB", "name": "Dab", "price": 750}, {"id": "emote:SHRUG", "name": "Shrug", "price": 600}, {"id": "emote:SALUTE", "name": "Salute", "price": 600}, {"id": "emote:CHEER", "name": "Cheer", "price": 800}, {"id": "emote:LAUGH", "name": "Laugh", "price": 700}, {"id": "emote:BOW", "name": "Bow", "price": 650}, {"id": "emote:HEART", "name": "Heart", "price": 850}, {"id": "emote:FLOSS", "name": "Floss", "price": 1000}, {"id": "emote:TPOSE", "name": "T-Pose", "price": 600}, {"id": "emote:CRY", "name": "Cry", "price": 750}, {"id": "emote:SIT", "name": "Sit", "price": 600}, {"id": "emote:GRIDDY", "name": "Griddy", "price": 1200}, {"id": "emote:SHUFFLE", "name": "Shuffle", "price": 1100}, {"id": "emote:DISCO", "name": "Disco Fever", "price": 1000}];
const EMOTE_ORDER = CATALOG.filter(i => i.id.startsWith('emote:')).map(i => i.id.slice(6));
const PER_PAGE = 8;

const walletFile = () => path.join(paths.root, 'wallet.json');
const configFile = () => path.join(os.homedir(), '.surgeclient', 'surge.json');

function readJson(file) {
  try { return JSON.parse(fs.readFileSync(file, 'utf8')); } catch { return null; }
}

function writeJson(file, data) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  const tmp = `${file}.tmp`;
  fs.writeFileSync(tmp, JSON.stringify(data, null, 2));
  fs.renameSync(tmp, file);
}

function wallet() {
  const w = readJson(walletFile()) || {};
  if (typeof w.coins !== 'number') w.coins = 1000;
  if (!Array.isArray(w.owned)) w.owned = [];
  if (!Array.isArray(w.redeemed)) w.redeemed = [];
  return w;
}

function owns(w, id) {
  if (w.owned.includes(id)) return true;
  return id === 'cape:custom' && w.owned.some(o => o.startsWith('cape:classic_'));
}

let remoteCache = null, remoteAt = 0, personal = [];

async function remote() {
  if (remoteCache && Date.now() - remoteAt < 60000) return remoteCache;
  const api = social.apiUrl();
  if (!api) return remoteCache || {};
  try {
    const r = await fetch(`${api}/v1/public`, { signal: AbortSignal.timeout(8000) });
    if (r.ok) { remoteCache = await r.json(); remoteAt = Date.now(); }
  } catch { }
  return remoteCache || {};
}

function matches(target, item) {
  const type = item.id.split(':')[0];
  if (target === 'all') return true;
  if (target === 'emote') return type === 'emote';
  if (target === 'cosmetic') return type !== 'emote';
  if (target.startsWith('sub:')) return type === target.slice(4);
  return target === item.id;
}

function priceOf(item, r) {
  const base = r.prices && Number.isFinite(r.prices[item.id]) ? r.prices[item.id] : item.price;
  let pct = 0;
  for (const s of [...(r.sales || []), ...personal]) {
    if (s.until && s.until < Date.now()) continue;
    if (matches(s.target, item)) pct = Math.max(pct, Math.min(100, s.percent || 0));
  }
  return { base, discount: pct, price: pct ? Math.max(0, Math.round(base * (100 - pct) / 100)) : base };
}

function lockedOf(item, r) {
  if (item.id === 'cape:image') return true;
  return (Array.isArray(r.locked) ? r.locked : ['cape:beta']).includes(item.id);
}

function wheel(conf, w) {
  if (Array.isArray(conf.emotePages) && conf.emotePages.length) return conf.emotePages;
  const all = EMOTE_ORDER.filter(e => owns(w, `emote:${e}`));
  const pages = [];
  for (let i = 0; i < Math.max(1, all.length); i += PER_PAGE) {
    const p = [];
    for (let j = 0; j < PER_PAGE; j++) p.push(all[i + j] || '');
    pages.push(p);
  }
  return pages;
}

let claimed = [];
let badge = '';

async function checkInbox() {
  if (!social.token()) { try { await social.social(); } catch { } }
  if (!social.token()) return;
  try {
    const box = await social.call('GET', '/v1/inbox');
    personal = Array.isArray(box.sales) ? box.sales : [];
    badge = box.badge || '';
    if (Array.isArray(box.gifts) && box.gifts.length) {
      const res = await social.call('POST', '/v1/inbox/claim', { ids: box.gifts.map(g => g.id) });
      const w = wallet();
      for (const g of res.claimed || []) {
        w.coins += Math.max(0, g.coins || 0);
        for (const i of g.items || []) if (!w.owned.includes(i)) w.owned.push(i);
        claimed.push({ coins: g.coins || 0, items: (g.items || []).map(id => ({ id, name: (CATALOG.find(c => c.id === id) || { name: id }).name })), note: g.note || '' });
      }
      writeJson(walletFile(), w);
    }
  } catch { }
}

async function get() {
  await checkInbox();
  const r = await remote();
  const w = wallet();
  const conf = readJson(configFile()) || {};
  const items = CATALOG.map(i => {
    const p = priceOf(i, r);
    return { ...i, type: i.id.split(':')[0], key: i.id.slice(i.id.indexOf(':') + 1), owned: owns(w, i.id), locked: lockedOf(i, r), ...p };
  });
  const gifts = claimed;
  claimed = [];
  return {
    coins: w.coins, items, badge, gifts,
    equipped: { cape: conf.cape || 'none', wings: conf.wings || 'none', halo: conf.halo || 'none' },
    wheel: wheel(conf, w).flat().filter(Boolean),
    capeColor: conf.capeColor || '8F50F2',
  };
}

async function buy(id) {
  const item = CATALOG.find(i => i.id === id);
  if (!item) throw new Error('Unknown item');
  const r = await remote();
  const w = wallet();
  if (owns(w, id)) return get();
  if (lockedOf(item, r)) throw new Error(id === 'cape:beta' ? 'This cape is only for beta testers' : "This can't be bought right now");
  const { price } = priceOf(item, r);
  if (w.coins < price) throw new Error(`You need ${price - w.coins} more coins`);
  w.coins -= price;
  w.owned.push(id);
  writeJson(walletFile(), w);
  if (id.startsWith('emote:')) {
    const conf = readJson(configFile());
    if (conf && Array.isArray(conf.emotePages) && conf.emotePages.length) {
      const key = id.slice(6);
      const page = conf.emotePages.find(p => p.includes('') || p.length < PER_PAGE);
      if (page) { const i = page.indexOf(''); if (i >= 0) page[i] = key; else page.push(key); writeJson(configFile(), conf); }
    }
  }
  return get();
}

function equip(type, key) {
  if (!['cape', 'wings', 'halo'].includes(type)) throw new Error('Unknown slot');
  const w = wallet();
  if (key !== 'none' && !owns(w, `${type}:${key}`)) throw new Error("You don't own this yet");
  const conf = readJson(configFile()) || {};
  conf[type] = key;
  writeJson(configFile(), conf);
  return get();
}

function toggleWheel(key) {
  const w = wallet();
  if (!owns(w, `emote:${key}`)) throw new Error("You don't own this emote yet");
  const conf = readJson(configFile()) || {};
  const pages = wheel(conf, w).map(p => [...p]);
  const at = pages.findIndex(p => p.includes(key));
  if (at >= 0) pages[at][pages[at].indexOf(key)] = '';
  else {
    let page = pages.find(p => p.includes(''));
    if (!page) { if (pages.length >= 9) throw new Error('Your emote wheel is full'); page = Array(PER_PAGE).fill(''); pages.push(page); }
    page[page.indexOf('')] = key;
  }
  conf.emotePages = pages;
  writeJson(configFile(), conf);
  return get();
}

async function redeem(raw) {
  const code = String(raw || '').toUpperCase().replace(/[^A-Z0-9]/g, '');
  if (code.length < 4) throw new Error("That code doesn't exist. Check the spelling, or ask for a new one.");
  const hash = crypto.createHash('sha256').update(code).digest('hex');
  const w = wallet();
  if (w.redeemed.includes(hash)) throw new Error('You already redeemed this code');
  let res;
  try { res = await social.call('POST', '/v1/redeem', { code: raw }); }
  catch (e) {
    if (/already/i.test(e.message)) throw new Error('You already redeemed this code');
    if (/used up/i.test(e.message)) throw new Error('This code has been used up');
    if (/not found/i.test(e.message)) throw new Error("That code doesn't exist. Check the spelling, or ask for a new one.");
    throw e;
  }
  const fresh = (res.items || []).filter(i => !w.owned.includes(i));
  if (!(res.coins > 0) && !fresh.length) throw new Error('You already have everything this code gives');
  w.redeemed.push(hash);
  w.coins += Math.max(0, res.coins || 0);
  w.owned.push(...fresh);
  writeJson(walletFile(), w);
  const data = await get();
  data.redeemed = { coins: Math.max(0, res.coins || 0), items: fresh.map(id => ({ id, name: (CATALOG.find(c => c.id === id) || { name: id }).name })) };
  return data;
}

module.exports = { CATALOG, get, buy, equip, toggleWheel, redeem };
