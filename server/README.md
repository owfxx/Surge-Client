# Surge Client online service

Makes cosmetics, emotes and the Surge badge visible to every Surge player. Runs free on Cloudflare.

1. Make a free account at https://dash.cloudflare.com and install Node.js.
2. In this folder run:
   - `npx wrangler login`
   - `npx wrangler d1 create surge-client`  → copy the `database_id` it prints into `wrangler.toml`
   - `npx wrangler deploy` → it prints your URL, like `https://surge-client.yourname.workers.dev`
3. In the Surge launcher: Settings → Online → paste that URL.

Everyone who uses your launcher build with that URL now sees each other's wings, halos, capes, emotes and badges.
