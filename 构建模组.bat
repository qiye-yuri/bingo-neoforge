@echo off
chcp 65001 >nul
cd /d "%~dp0"
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\build-jar.ps1"
set "BUILD_EXIT_CODE=%ERRORLEVEL%"
echo.
if not "%BUILD_EXIT_CODE%"=="0" echo 构建未完成，请查看上方错误信息。
pause
exit /b %BUILD_EXIT_CODE%
