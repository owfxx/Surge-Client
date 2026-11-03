const fs = require('fs');
const { safeStorage } = require('electron');
const { Auth } = require('msmc');
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

async function login() {
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
  const auth = new Auth('select_account');
  const xbox = await auth.refresh(unseal(acc.token));
  const mc = await xbox.getMinecraft();
  acc.token = seal(xbox.save());
  acc.name = mc.profile.name;
  writeStore(s);
  return mc.mclc();
}

module.exports = { login, logout, setActive, publicAccounts, getLaunchAuth };
