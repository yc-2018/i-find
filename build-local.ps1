param(
  [switch]$Install
)

$ErrorActionPreference = "Stop"

$root = $PSScriptRoot
$nativeRoot = Join-Path $root "native-android"
$jdkRoot = Join-Path $root ".toolchains\jdk17"
$sdkRoot = Join-Path $root ".toolchains\android-sdk"
$gradleFallback = Join-Path $root ".toolchains\gradle\gradle-8.9\bin\gradle.bat"
$credentialsPath = Join-Path $root "credentials.json"
$outputDirectory = Join-Path $root "builds"
$finalApk = Join-Path $outputDirectory "i-find-native-arm64-v8a-release.apk"

if (-not (Test-Path (Join-Path $jdkRoot "bin\java.exe"))) {
  throw "JDK 17 is not ready: $jdkRoot"
}

if (-not (Test-Path (Join-Path $sdkRoot "platforms\android-35\android.jar"))) {
  throw "Android SDK 35 is not ready: $sdkRoot"
}

if (-not (Test-Path $credentialsPath)) {
  throw "Missing signing configuration: $credentialsPath"
}

$credentials = Get-Content -LiteralPath $credentialsPath -Raw | ConvertFrom-Json
$keystore = $credentials.android.keystore
if (-not $keystore.keystorePassword -or -not $keystore.keyAlias -or -not $keystore.keyPassword) {
  throw "Android signing configuration is incomplete in credentials.json"
}

$env:JAVA_HOME = $jdkRoot
$env:ANDROID_HOME = $sdkRoot
$env:ANDROID_SDK_ROOT = $sdkRoot
$env:IFIND_KEYSTORE_PASSWORD = [string]$keystore.keystorePassword
$env:IFIND_KEY_ALIAS = [string]$keystore.keyAlias
$env:IFIND_KEY_PASSWORD = [string]$keystore.keyPassword
$env:Path = "$(Join-Path $jdkRoot 'bin');$(Join-Path $sdkRoot 'platform-tools');$env:Path"

$localPropertiesPath = Join-Path $nativeRoot "local.properties"
$sdkProperty = "sdk.dir=$($sdkRoot.Replace('\', '/'))`n"
[System.IO.File]::WriteAllText(
  $localPropertiesPath,
  $sdkProperty,
  [System.Text.UTF8Encoding]::new($false)
)

$wrapper = Join-Path $nativeRoot "gradlew.bat"
if (Test-Path $gradleFallback) {
  $gradleCommand = $gradleFallback
} elseif (Test-Path $wrapper) {
  $gradleCommand = $wrapper
} else {
  throw "Gradle wrapper and project-local Gradle 8.9 are both unavailable"
}

Push-Location $nativeRoot
try {
  & $gradleCommand --no-daemon clean assembleRelease
  if ($LASTEXITCODE -ne 0) {
    throw "Gradle release build failed with exit code $LASTEXITCODE"
  }
} finally {
  Pop-Location
}

$apkDirectory = Join-Path $nativeRoot "app\build\outputs\apk\release"
$builtApk = Get-ChildItem -LiteralPath $apkDirectory -Filter "*.apk" -File |
  Where-Object { $_.Name -match "arm64-v8a" } |
  Sort-Object Length -Descending |
  Select-Object -First 1

if (-not $builtApk) {
  $builtApk = Get-ChildItem -LiteralPath $apkDirectory -Filter "*.apk" -File |
    Sort-Object Length -Descending |
    Select-Object -First 1
}

if (-not $builtApk) {
  throw "Build completed but no APK was found in $apkDirectory"
}

New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
Copy-Item -LiteralPath $builtApk.FullName -Destination $finalApk -Force

Write-Host "APK created: $finalApk"
Write-Host ("APK size: {0:N2} MB" -f ((Get-Item -LiteralPath $finalApk).Length / 1MB))

if ($Install) {
  $adb = Join-Path $sdkRoot "platform-tools\adb.exe"
  if (-not (Test-Path $adb)) {
    throw "adb is unavailable: $adb"
  }
  & $adb install -r $finalApk
  if ($LASTEXITCODE -ne 0) {
    throw "APK installation failed with exit code $LASTEXITCODE"
  }
}
