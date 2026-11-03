@echo off
rem Publishes a new Surge Client version. GitHub builds the mod + launcher and every
rem player's launcher updates on its own. Needs Git and Node.js.
setlocal
set /p VER=New version (like 1.0.1): 
if "%VER%"=="" exit /b 1
node scripts\set-version.js %VER% || exit /b 1
git add -A
git commit -m "Surge Client %VER%"
git push
echo.
echo Done. GitHub is building %VER% now (Actions tab, about 10 minutes).
echo When it finishes, players get it automatically.
pause
