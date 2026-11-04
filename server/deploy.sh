#!/usr/bin/env bash
set -euo pipefail
if [ -z "${CLOUDFLARE_API_TOKEN:-}" ]; then echo "No CLOUDFLARE_API_TOKEN secret, skipping"; exit 0; fi
API=https://api.cloudflare.com/client/v4
cf() { curl -sS -H "Authorization: Bearer $CLOUDFLARE_API_TOKEN" -H "Content-Type: application/json" "$@"; }
json() { node -e "let d='';process.stdin.on('data',c=>d+=c).on('end',()=>{const o=JSON.parse(d);const v=($1);process.stdout.write(v==null?'':String(v))})"; }

echo "Token: $(cf "$API/user/tokens/verify" | json "o.result&&o.result.status")"
ACC="${CLOUDFLARE_ACCOUNT_ID:-}"
[ -z "$ACC" ] && ACC=$(cf "$API/accounts" | json "o.result&&o.result[0]&&o.result[0].id")
if [ -z "$ACC" ]; then echo "Token can't see a Cloudflare account"; cf "$API/accounts"; exit 1; fi
export CLOUDFLARE_ACCOUNT_ID=$ACC

DB=$(cf "$API/accounts/$ACC/d1/database?name=surge-client" | json "(o.result||[]).find(x=>x.name==='surge-client')?.uuid")
if [ -z "$DB" ]; then
  DB=$(cf -X POST "$API/accounts/$ACC/d1/database" -d '{"name":"surge-client"}' | json "o.result&&o.result.uuid")
fi
if [ -z "$DB" ]; then echo "Couldn't create the database (token needs D1 Edit)"; exit 1; fi

SUB=$(cf "$API/accounts/$ACC/workers/subdomain" | json "o.result&&o.result.subdomain")
if [ -z "$SUB" ]; then
  WANT="surge-${ACC:0:6}"
  SUB=$(cf -X PUT "$API/accounts/$ACC/workers/subdomain" -d "{\"subdomain\":\"$WANT\"}" | json "o.result&&o.result.subdomain")
fi
if [ -z "$SUB" ]; then echo "Couldn't set up a workers.dev address"; exit 1; fi

cat > wrangler.toml <<TOML
name = "surge-client"
main = "worker.js"
compatibility_date = "2025-01-01"
workers_dev = true

[[d1_databases]]
binding = "DB"
database_name = "surge-client"
database_id = "$DB"
TOML

KEYS=$(curl -sS --max-time 15 https://api.minecraftservices.com/publickeys | node -e "let d='';process.stdin.on('data',c=>d+=c).on('end',()=>{try{const o=JSON.parse(d);process.stdout.write(JSON.stringify((o.playerCertificateKeys||[]).map(k=>k.publicKey)))}catch{process.stdout.write('[]')}})" || echo '[]')
echo "Mojang keys: $(echo "$KEYS" | node -e "let d='';process.stdin.on('data',c=>d+=c).on('end',()=>process.stdout.write(String(JSON.parse(d).length)))")"
if [ "$KEYS" != "[]" ]; then
  printf '\n[vars]\nMOJANG_KEYS = %s\n' "$(node -e "process.stdout.write(JSON.stringify(process.argv[1]))" "$KEYS")" >> wrangler.toml
fi

npx --yes wrangler@4 deploy
if [ -n "${ADMIN_PASSWORD:-}" ]; then printf '%s' "$ADMIN_PASSWORD" | npx --yes wrangler@4 secret put ADMIN_PASSWORD >/dev/null && echo "Admin password set"; else echo "No ADMIN_PASSWORD secret: the website admin stays locked"; fi
URL="https://surge-client.$SUB.workers.dev"
echo "Online service: $URL"
echo "url=$URL" >> "${GITHUB_OUTPUT:-/dev/null}"
sleep 8
for p in / /download/mod "/v1/players?ids=0" /admin /v1/public; do
  echo "Check $p: $(curl -s -o /dev/null -w '%{http_code}' -r 0-1000 "$URL$p")"
done
