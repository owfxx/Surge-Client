// Admin panel backend: password-protected coin code generator.
// Plain codes stay in admin.json (only on the admin's PC). codes.json holds only
// SHA-256 hashes + coin amounts, so it's safe to publish (e.g. a GitHub repo).
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const paths = require('./paths');

const ADMIN = path.join(paths.root, 'admin.json');
const PUBLIC = path.join(paths.root, 'codes.json');
const ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
let unlocked = false;
// The owner's admin password, stored only as a salted scrypt hash.
const MASTER = { salt: '5urge-adm1n-0f6c2a', hash: '57a793b7fca657d22e4fc5cc2c4185c4237c83253bebaa34991efb5a5e421df4' };

const read = () => { try { return JSON.parse(fs.readFileSync(ADMIN, 'utf8')); } catch { return { pass: null, codes: [] }; } };
const write = d => { fs.writeFileSync(ADMIN, JSON.stringify(d, null, 2)); writePublic(d); };
const normalize = c => c.toUpperCase().replace(/[^A-Z0-9]/g, '');
const sha256 = s => crypto.createHash('sha256').update(s, 'utf8').digest('hex');

// Sales made in the in-game admin panel (Ctrl+Shift+F4) live in sales.json and are published with the codes.
function sales() {
  return readSales().filter(s => !s.until || s.until > Date.now());
}

function publicJson(d = read()) {
  return { codes: d.codes.map(c => ({ hash: sha256(normalize(c.code)), coins: c.coins || 0, items: c.items || [] })), sales: sales() };
}
function writePublic(d) { fs.writeFileSync(PUBLIC, JSON.stringify(publicJson(d), null, 2)); }

function hashPass(pw, salt = crypto.randomBytes(16).toString('hex')) {
  return { salt, hash: crypto.scryptSync(pw, salt, 32).toString('hex') };
}

function status() { return { hasPassword: true, unlocked }; }

function setup(pw) {
  const d = read();
  if (d.pass) throw new Error('Already set up');
  if (!pw || pw.length < 6) throw new Error('Min 6 characters');
  d.pass = hashPass(pw);
  write(d);
  unlocked = true;
  return list();
}

function matches(pw, rec) {
  if (!rec || !pw) return false;
  const { hash } = hashPass(pw, rec.salt);
  return crypto.timingSafeEqual(Buffer.from(hash, 'hex'), Buffer.from(rec.hash, 'hex'));
}

function unlock(pw) {
  if (!matches(pw, MASTER) && !matches(pw, read().pass)) throw new Error('Wrong password');
  unlocked = true;
  return list();
}

function lock() { unlocked = false; }

// ---------- sales (shared with the in-game admin panel through sales.json) ----------
const SALES = path.join(paths.root, 'sales.json');
function readSales() { try { return JSON.parse(fs.readFileSync(SALES, 'utf8')).sales || []; } catch { return []; } }
function writeSales(list) { fs.writeFileSync(SALES, JSON.stringify({ sales: list }, null, 2)); writePublic(read()); }
function salesList() { guard(); return readSales().filter(s => !s.until || s.until > Date.now()); }
function addSale({ target = 'all', percent = 20, hours = 0 }) {
  guard();
  percent = Math.min(90, Math.max(5, Math.round(+percent || 0)));
  const list = readSales().filter(s => !s.until || s.until > Date.now());
  list.push({ target: String(target), percent, until: +hours > 0 ? Date.now() + hours * 3600000 : 0 });
  writeSales(list);
  return salesList();
}
function removeSale(i) { guard(); const list = salesList(); list.splice(i, 1); writeSales(list); return salesList(); }

// ---------- GitHub token for publishing (encrypted with the OS keychain) ----------
function tokenFile() { return path.join(paths.root, 'admin-token.bin'); }
function setToken(token) {
  guard();
  const { safeStorage } = require('electron');
  if (!token) { fs.rmSync(tokenFile(), { force: true }); return false; }
  fs.writeFileSync(tokenFile(), safeStorage.isEncryptionAvailable() ? safeStorage.encryptString(token) : Buffer.from(token));
  return true;
}
function getToken() {
  guard();
  try {
    const { safeStorage } = require('electron');
    const buf = fs.readFileSync(tokenFile());
    return safeStorage.isEncryptionAvailable() ? safeStorage.decryptString(buf) : buf.toString();
  } catch { return ''; }
}
function hasToken() { return fs.existsSync(tokenFile()); }

function guard() { if (!unlocked) throw new Error('Locked'); }

function list() { guard(); return read().codes; }

// Random codes (count of them), or one code with your own text. Each gives coins and/or unlocks items.
function create({ coins = 0, count = 1, code = '', items = [] }) {
  guard();
  coins = Math.max(0, Math.floor(Number(coins) || 0));
  items = (Array.isArray(items) ? items : []).filter(i => /^[a-z]+:[A-Za-z_]+$/.test(i));
  if (!coins && !items.length) throw new Error('Give the code some coins or an item');
  const d = read();
  if (code) {
    const n = normalize(code);
    if (n.length < 3) throw new Error('Use at least 3 letters or numbers');
    if (d.codes.some(c => normalize(c.code) === n)) throw new Error('That code already exists');
    d.codes.unshift({ code: code.toUpperCase(), coins, items, created: Date.now() });
  } else {
    for (let k = 0; k < Math.min(Math.max(1, count), 100); k++) {
      const raw = Array.from(crypto.randomBytes(12), b => ALPHABET[b % ALPHABET.length]).join('');
      d.codes.unshift({ code: raw.match(/.{4}/g).join('-'), coins, items, created: Date.now() });
    }
  }
  write(d);
  return d.codes;
}

function remove(code) {
  guard();
  const d = read();
  d.codes = d.codes.filter(c => c.code !== code);
  write(d);
  return d.codes;
}

function exportPublic(dest) { guard(); fs.writeFileSync(dest, JSON.stringify(publicJson(), null, 2)); return dest; }

module.exports = { status, setup, unlock, lock, list, create, remove, exportPublic, publicJson, salesList, addSale, removeSale, setToken, getToken, hasToken };
