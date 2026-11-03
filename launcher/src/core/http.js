// Small fetch helpers: JSON requests, verified file downloads and a concurrency pool.
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const UA = 'SurgeClient/0.1.0 (github.com/surge-client)';

async function getJson(url, headers = {}) {
  const res = await fetch(url, { headers: { 'User-Agent': UA, Accept: 'application/json', ...headers } });
  if (!res.ok) {
    const body = await res.text().catch(() => '');
    throw new Error(`HTTP ${res.status} for ${url}${body ? `: ${body.slice(0, 200)}` : ''}`);
  }
  return res.json();
}

async function postJson(url, data, headers = {}) {
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'User-Agent': UA, 'Content-Type': 'application/json', Accept: 'application/json', ...headers },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status} for ${url}`);
  return res.json();
}

function hashFile(file, algo = 'sha1') {
  return crypto.createHash(algo).update(fs.readFileSync(file)).digest('hex');
}

// Downloads to a temp name, checks sha1 if given, then moves into place.
async function download(url, dest, sha1) {
  if (sha1 && fs.existsSync(dest) && hashFile(dest) === sha1) return dest;
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  const res = await fetch(url, { headers: { 'User-Agent': UA } });
  if (!res.ok) throw new Error(`Download failed (${res.status}): ${url}`);
  const buf = Buffer.from(await res.arrayBuffer());
  if (sha1) {
    const got = crypto.createHash('sha1').update(buf).digest('hex');
    if (got !== sha1) throw new Error(`Checksum mismatch for ${path.basename(dest)}`);
  }
  const tmp = `${dest}.part`;
  fs.writeFileSync(tmp, buf);
  fs.renameSync(tmp, dest);
  return dest;
}

// Runs async jobs with at most `limit` in flight; reports progress.
async function pool(items, limit, worker, onProgress) {
  let done = 0;
  let i = 0;
  const results = new Array(items.length);
  async function next() {
    while (i < items.length) {
      const idx = i++;
      results[idx] = await worker(items[idx], idx);
      done++;
      if (onProgress) onProgress(done, items.length);
    }
  }
  await Promise.all(Array.from({ length: Math.min(limit, items.length) }, next));
  return results;
}

module.exports = { UA, getJson, postJson, download, pool, hashFile };
