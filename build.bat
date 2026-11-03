@echo off
cd /d "%~dp0mod" || exit /b 1
call gradlew.bat build || exit /b 1
cd /d "%~dp0launcher" || exit /b 1
call npm install || exit /b 1
call npm run dist || exit /b 1
echo.
echo Done: %~dp0launcher\dist
