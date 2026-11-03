# Surge Client

- `launcher/` — Windows launcher (Electron)
- `mod/` — in-game mod, Fabric 1.21.11 (Right Shift menu, B emote wheel)

## Build
Windows with JDK 21 + Node 20+: run `build.bat`.
Output: `launcher/dist/Surge-Client-Setup-<ver>.exe` and `Surge-Client-<ver>-Standalone.exe`.

Or push to GitHub — the workflow builds both and uploads the exes.

## Notes
- CurseForge needs a free API key (Settings → CurseForge key).
- Data: `%APPDATA%\.surgeclient`


## Updates through GitHub (do this once)

1. Make a free GitHub account and a new **public** repository, e.g. `surge-client` (public so every launcher can download updates without a login).
2. Install Git (git-scm.com). In this folder run:
   ```
   git init
   git add -A
   git commit -m "Surge Client"
   git branch -M main
   git remote add origin https://github.com/YOUR-NAME/surge-client.git
   git push -u origin main
   ```
3. Double-click **release.bat** and type a version like `0.9.0`.
   GitHub builds the mod and the launcher (Actions tab, ~10 minutes) and publishes a Release.
4. Give players the `.exe` from that first Release. From then on, every new `release.bat`:
   - new mod jar: downloaded automatically the next time they press Play
   - new launcher: they get an "Update available" popup and it updates itself

## Admin (launcher)

Press **Ctrl + Shift + F4** in the launcher and enter the admin password.
- **Codes**: random or your own text, giving coins and/or cosmetics and emotes. Press *Publish codes*.
- **Sales**: a percentage off one item, a category or everything. Press *Publish*.
- **Updates**: set the repo (`you/surge-client`) and a GitHub token (fine-grained, this repo only,
  *Contents: Read and write*). *Publish update…* uploads a mod `.jar` or launcher `.exe` by hand,
  without waiting for Actions.
