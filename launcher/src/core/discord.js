// Discord Rich Presence over Discord's local IPC pipe (no extra dependencies).
// Needs a Discord application ID (discord.com/developers > New Application). Upload
// the Surge icon there under Rich Presence > Art Assets with the name "surge".
const net = require('net');
const path = require('path');
const os = require('os');

let sock = null;
let ready = false;
let clientId = '';
let pending = null;
let retryTimer = null;
const startedAt = Date.now();

function pipePath(i) {
  if (process.platform === 'win32') return `\\\\?\\pipe\\discord-ipc-${i}`;
  const dir = process.env.XDG_RUNTIME_DIR || process.env.TMPDIR || process.env.TMP || process.env.TEMP || os.tmpdir();
  return path.join(dir, `discord-ipc-${i}`);
}

function frame(op, data) {
  const json = Buffer.from(JSON.stringify(data), 'utf8');
  const head = Buffer.alloc(8);
  head.writeInt32LE(op, 0);
  head.writeInt32LE(json.length, 4);
  return Buffer.concat([head, json]);
}

function tryConnect(i = 0) {
  if (!clientId || i > 9) { scheduleRetry(); return; }
  const s = net.createConnection(pipePath(i));
  s.once('connect', () => {
    sock = s;
    s.write(frame(0, { v: 1, client_id: clientId }));
  });
  s.on('data', buf => {
    // op 1 = frame; the first one is the READY dispatch
    try {
      const op = buf.readInt32LE(0);
      const body = JSON.parse(buf.slice(8).toString('utf8'));
      if (op === 1 && body.evt === 'READY') { ready = true; if (pending) push(pending); }
      if (op === 2) { s.destroy(); } // close
    } catch { /* partial frame, ignore */ }
  });
  s.once('error', () => { s.destroy(); if (!sock) tryConnect(i + 1); });
  s.once('close', () => { if (sock === s) { sock = null; ready = false; scheduleRetry(); } });
}

function scheduleRetry() {
  clearTimeout(retryTimer);
  if (!clientId) return;
  retryTimer = setTimeout(() => { if (!sock) tryConnect(0); }, 15000);
}

function push(activity) {
  if (!sock || !ready) return;
  sock.write(frame(1, { cmd: 'SET_ACTIVITY', args: { pid: process.pid, activity }, nonce: String(Date.now()) + Math.random() }));
}

/** Starts (or restarts with a new app ID). Empty ID turns presence off. */
function start(id) {
  if (id === clientId && sock) return;
  stop();
  clientId = (id || '').trim();
  if (clientId) tryConnect(0);
}

function stop() {
  clearTimeout(retryTimer);
  if (sock) { try { push(null); sock.destroy(); } catch { /* already gone */ } }
  sock = null;
  ready = false;
  clientId = '';
}

/** details/state lines plus an optional start time (ms). */
function set({ details, state, since } = {}) {
  const activity = {
    details: details || undefined,
    state: state || undefined,
    timestamps: { start: Math.floor((since || startedAt) / 1000) },
    assets: { large_image: 'surge', large_text: 'Surge Client' },
    instance: false,
  };
  pending = activity;
  push(activity);
}

module.exports = { start, stop, set };
