@echo off
setlocal
set "ADB=%~dp0.toolchains\android-sdk\platform-tools\adb.exe"
set "APK=%~dp0builds\i-find-native-arm64-v8a-release.apk"

if not exist "%ADB%" (
  echo ADB was not found: %ADB%
  pause
  exit /b 1
)

if not exist "%APK%" (
  echo APK not found. Building it now...
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0build-local.ps1"
  if errorlevel 1 (
    echo Build failed.
    pause
    exit /b 1
  )
)

"%ADB%" start-server >nul
"%ADB%" get-state >nul 2>&1
if errorlevel 1 (
  echo No Android device is ready.
  echo Connect the phone, enable USB debugging, and accept the authorization prompt.
  "%ADB%" devices
  pause
  exit /b 1
)

echo Installing I find...
"%ADB%" install -r "%APK%"
if errorlevel 1 (
  echo Installation failed. Check the message above.
  pause
  exit /b 1
)

echo Installation complete. You can open I find on the phone.
pause
exit /b 0
