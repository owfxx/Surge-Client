@echo off
rem Publishes a new Surge Client version: GitHub builds the mod + launcher and every
rem player's launcher updates on its own. Needs Git and this folder pushed to your GitHub repo.
setlocal
set /p VER=New version (like 0.9.0): 
if "%VER%"=="" exit /b 1
git add -A
git commit -m "Surge Client %VER%"
git tag v%VER%
git push
git push origin v%VER%
echo.
echo Done. GitHub is building v%VER% now (Actions tab). When it finishes, players get it automatically.
pause
