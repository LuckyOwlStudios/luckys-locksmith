@echo off
rem Double-click to build both loaders. Jars end up in release\<version>\
cd /d "%~dp0"
call gradlew.bat buildRelease
if errorlevel 1 (
    echo.
    echo BUILD FAILED. See the messages above.
) else (
    echo.
    echo Done! Jars are in the release folder.
    if exist release start "" explorer release
)
echo.
pause
