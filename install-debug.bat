@echo off
setlocal

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0build-local.ps1" -Debug -Install %*
if errorlevel 1 (
  echo Debug build or installation failed. Check the message above.
  pause
  exit /b 1
)

echo Debug app installed successfully.
pause
exit /b 0
