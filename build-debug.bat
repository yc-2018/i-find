@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0build-local.ps1" -Debug %*
exit /b %errorlevel%
