const fs = require('fs');
const { safeStorage } = require('electron');
const { Auth, lexicon } = require('msmc');
const paths = require('./paths');

function readStore() {
  try { return JSON.parse(fs.readFileSync(paths.accounts, 'utf8')); } catch { return { active: null, accounts: [] }; }
}
function writeStore(s) { fs.writeFileSync(paths.accounts, JSON.stringify(s, null, 2)); }

function seal(text) {
  return safeStorage.isEncryptionAvailable() ? { enc: safeStorage.encryptString(text).toString('base64') } : { raw: text };
}
function unseal(rec) {
  return rec.enc ? safeStorage.decryptString(Buffer.from(rec.enc, 'base64')) : rec.raw;
}

function publicAccounts() {
  const s = readStore();
  return { active: s.active, accounts: s.accounts.map(a => ({ uuid: a.uuid, name: a.name })) };
}

const FRIENDLY = {
  'error.gui.closed': 'The sign-in window was closed before you finished.',
  'error.auth.microsoft': 'Microsoft sign-in failed. Try again, and check that the date and time on your PC are correct.',
  'error.auth.xboxLive': "Couldn't connect to Xbox Live. Check your internet connection and try again.",
  'error.auth.xsts': "Xbox Live refused the sign-in. Open xbox.com in your browser, sign in once with this account, then try again.",
  'error.auth.xsts.userNotFound': "This Microsoft account doesn't have an Xbox profile yet. Sign in once at xbox.com to create one, then try again.",
  'error.auth.xsts.bannedCountry': "Xbox Live isn't available in this account's country, so it can't sign in to Minecraft.",
  'error.auth.xsts.child': 'This account is under 18. An adult has to add it to a Microsoft family group first (account.microsoft.com/family).',
  'error.auth.xsts.child.SK': 'This account needs parental permission on the Xbox website before it can sign in.',
  'error.auth.minecraft': "Couldn't sign in to Minecraft. Try again in a minute.",
  'error.auth.minecraft.login': "Minecraft's sign-in servers rejected the request. Wait a minute and try again.",
  'error.auth.minecraft.profile': "This account doesn't own Minecraft: Java Edition, or hasn't picked a player name yet. Open minecraft.net, sign in and set up your profile first.",
  'error.auth.minecraft.entitlements': "Couldn't check if this account owns Minecraft. Try again in a minute.",
};

function authError(err) {
  if (err instanceof Error && !err.ts) return err;
  let code = '';
  let status = 0;
  if (typeof err === 'string') code = err;
  else if (err && typeof err === 'object') { code = String(err.ts || err.name || ''); status = err.response?.status || 0; }
  let msg = '';
  for (let c = code; c && !msg; c = c.includes('.') ? c.slice(0, c.lastIndexOf('.')) : '') msg = FRIENDLY[c] || '';
  if (!msg) {
    try { const m = code ? lexicon.wrapError(code).message : ''; msg = m && m !== code ? m : ''; } catch { }
  }
  if (status === 429) msg = 'Too many sign-in attempts right now. Wait a few minutes and try again.';
  if (!msg) msg = code ? `Sign-in failed (${code}). Please try again.` : 'Sign-in failed. Please try again.';
  if (status && status !== 429) msg += ` (code ${status})`;
  const e = new Error(msg);
  e.code = code;
  return e;
}

async function logResponse(err) {
  try {
    if (err && err.response && typeof err.response.text === 'function') console.error('[auth]', err.ts, err.response.status, (await err.response.text()).slice(0, 500));
    else console.error('[auth]', err);
  } catch { }
}

async function login() {
  try {
    return await loginInner();
  } catch (err) {
    await logResponse(err);
    const e = authError(err);
    if (e.code === 'error.gui.closed') return publicAccounts();
    throw e;
  }
}

async function loginInner() {
  const auth = new Auth('select_account');
  const xbox = await auth.launch('electron', { title: 'Sign in to Microsoft — Surge Client', width: 520, height: 640, resizable: false, backgroundColor: '#140b20' });
  const mc = await xbox.getMinecraft();
  if (!mc.profile) throw new Error("This Microsoft account doesn't own Minecraft: Java Edition.");
  const s = readStore();
  s.accounts = s.accounts.filter(a => a.uuid !== mc.profile.id);
  s.accounts.push({ uuid: mc.profile.id, name: mc.profile.name, token: seal(xbox.save()) });
  s.active = mc.profile.id;
  writeStore(s);
  return publicAccounts();
}

function logout(uuid) {
  const s = readStore();
  s.accounts = s.accounts.filter(a => a.uuid !== uuid);
  if (s.active === uuid) s.active = s.accounts[0]?.uuid || null;
  writeStore(s);
  return publicAccounts();
}

function setActive(uuid) {
  const s = readStore();
  if (s.accounts.find(a => a.uuid === uuid)) { s.active = uuid; writeStore(s); }
  return publicAccounts();
}

async function getLaunchAuth() {
  const s = readStore();
  const acc = s.accounts.find(a => a.uuid === s.active);
  if (!acc) throw new Error('Sign in with your Microsoft account first.');
  let xbox, mc;
  try {
    const auth = new Auth('select_account');
    xbox = await auth.refresh(unseal(acc.token));
    mc = await xbox.getMinecraft();
  } catch (err) {
    await logResponse(err);
    const e = authError(err);
    if (String(e.code).startsWith('error.auth.microsoft')) throw new Error('Your Microsoft sign-in expired. Remove the account in Accounts and sign in again.');
    throw e;
  }
  acc.token = seal(xbox.save());
  acc.name = mc.profile.name;
  writeStore(s);
  return mc.mclc();
}

function activeUuid() { const s = readStore(); return String(s.active || '').replace(/-/g, '').toLowerCase(); }

module.exports = { login, logout, setActive, publicAccounts, getLaunchAuth, activeUuid };
