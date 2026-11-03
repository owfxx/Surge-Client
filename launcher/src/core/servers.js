const net = require('net');
const dns = require('dns').promises;

function varint(n) {
  const out = [];
  do { let b = n & 0x7f; n >>>= 7; if (n) b |= 0x80; out.push(b); } while (n);
  return Buffer.from(out);
}
function packet(id, ...parts) {
  const body = Buffer.concat([varint(id), ...parts]);
  return Buffer.concat([varint(body.length), body]);
}
function str(s) { const b = Buffer.from(s, 'utf8'); return Buffer.concat([varint(b.length), b]); }
function readVarint(buf, off) {
  let n = 0, shift = 0, b;
  do {
    if (off >= buf.length) return null;
    b = buf[off++];
    n |= (b & 0x7f) << shift;
    shift += 7;
  } while (b & 0x80);
  return [n, off];
}

async function resolve(address) {
  let [host, port] = address.trim().split(':');
  if (port) return { host, port: parseInt(port, 10) || 25565 };
  try {
    const srv = await dns.resolveSrv(`_minecraft._tcp.${host}`);
    if (srv.length) return { host: srv[0].name, port: srv[0].port, display: host };
  } catch { }
  return { host, port: 25565 };
}

const COLORS = { black: '0', dark_blue: '1', dark_green: '2', dark_aqua: '3', dark_red: '4', dark_purple: '5', gold: '6', gray: '7', dark_gray: '8', blue: '9', green: 'a', aqua: 'b', red: 'c', light_purple: 'd', yellow: 'e', white: 'f' };
function flatten(c) {
  if (c == null) return '';
  if (typeof c === 'string') return c;
  if (Array.isArray(c)) return c.map(flatten).join('');
  let s = '';
  if (c.color && COLORS[c.color]) s += `§${COLORS[c.color]}`;
  if (c.color && c.color.startsWith('#')) s += `§#${c.color.slice(1)}`;
  if (c.bold) s += '§l';
  if (c.italic) s += '§o';
  s += c.text || '';
  if (c.extra) s += c.extra.map(x => `§r${flatten(x)}`).join('');
  return s;
}

function ping(address, timeout = 5000) {
  return resolve(address).then(({ host, port }) => new Promise(resolvePromise => {
    const socket = net.createConnection({ host, port });
    let buf = Buffer.alloc(0);
    let status = null;
    let sentAt = 0;
    const done = result => { socket.destroy(); clearTimeout(timer); resolvePromise(result); };
    const timer = setTimeout(() => done({ online: false, error: 'Timed out' }), timeout);
    socket.on('error', e => done({ online: false, error: e.code === 'ENOTFOUND' ? 'Unknown address' : 'Can\'t reach server' }));
    socket.on('connect', () => {
      const portBuf = Buffer.alloc(2); portBuf.writeUInt16BE(port);
      socket.write(packet(0x00, varint(774), str(host), portBuf, varint(1)));
      socket.write(packet(0x00));
    });
    socket.on('data', chunk => {
      buf = Buffer.concat([buf, chunk]);
      for (;;) {
        const len = readVarint(buf, 0);
        if (!len || buf.length < len[1] + len[0]) return;
        const body = buf.subarray(len[1], len[1] + len[0]);
        buf = buf.subarray(len[1] + len[0]);
        const id = readVarint(body, 0);
        if (!id) return done({ online: false, error: 'Bad response' });
        if (id[0] === 0x00 && !status) {
          const sl = readVarint(body, id[1]);
          try { status = JSON.parse(body.subarray(sl[1], sl[1] + sl[0]).toString('utf8')); } catch { return done({ online: false, error: 'Bad response' }); }
          sentAt = Date.now();
          const t = Buffer.alloc(8); t.writeBigInt64BE(BigInt(sentAt));
          socket.write(packet(0x01, t));
        } else if (id[0] === 0x01 && status) {
          done({
            online: true,
            ping: Date.now() - sentAt,
            motd: flatten(status.description),
            players: { online: status.players?.online ?? 0, max: status.players?.max ?? 0 },
            version: status.version?.name || '',
            favicon: typeof status.favicon === 'string' && status.favicon.startsWith('data:image/') ? status.favicon : null,
          });
        }
      }
    });
  }));
}

module.exports = { ping };
