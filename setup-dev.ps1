param(
  [switch]$Force
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$root = $PSScriptRoot
$toolchainsRoot = Join-Path $root ".toolchains"
$downloadsRoot = Join-Path $toolchainsRoot "downloads"
$jdkRoot = Join-Path $toolchainsRoot "jdk17"
$sdkRoot = Join-Path $toolchainsRoot "android-sdk"
$nativeRoot = Join-Path $root "native-android"

$jdkArchive = Join-Path $downloadsRoot "jdk17-adoptium.zip"
$androidToolsArchive = Join-Path $downloadsRoot "commandlinetools-win.zip"
$jdkUrl = "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk"
$androidToolsUrl = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"

function Remove-ToolchainDirectory {
  param([Parameter(Mandatory = $true)][string]$Path)

  $resolvedRoot = [System.IO.Path]::GetFullPath($toolchainsRoot).TrimEnd('\') + '\'
  $resolvedPath = [System.IO.Path]::GetFullPath($Path).TrimEnd('\') + '\'
  if (-not $resolvedPath.StartsWith($resolvedRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to remove a directory outside .toolchains: $Path"
  }

  if (Test-Path -LiteralPath $Path) {
    Remove-Item -LiteralPath $Path -Recurse -Force
  }
}

function Download-File {
  param(
    [Parameter(Mandatory = $true)][string]$Uri,
    [Parameter(Mandatory = $true)][string]$Destination
  )

  if ((Test-Path -LiteralPath $Destination) -and -not $Force) {
    Write-Host "Using cached download: $Destination"
    return
  }

  Write-Host "Downloading: $Uri"
  Invoke-WebRequest -UseBasicParsing -Uri $Uri -OutFile $Destination
}

New-Item -ItemType Directory -Path $toolchainsRoot, $downloadsRoot -Force | Out-Null

$javaExe = Join-Path $jdkRoot "bin\java.exe"
if ($Force -or -not (Test-Path -LiteralPath $javaExe)) {
  Download-File -Uri $jdkUrl -Destination $jdkArchive

  $jdkStaging = Join-Path $toolchainsRoot "jdk17-staging"
  Remove-ToolchainDirectory -Path $jdkStaging
  Remove-ToolchainDirectory -Path $jdkRoot
  New-Item -ItemType Directory -Path $jdkStaging -Force | Out-Null
  Expand-Archive -LiteralPath $jdkArchive -DestinationPath $jdkStaging -Force

  $extractedJdk = Get-ChildItem -LiteralPath $jdkStaging -Directory |
    Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName "bin\java.exe") } |
    Select-Object -First 1
  if (-not $extractedJdk) {
    throw "The downloaded JDK archive does not contain bin\java.exe"
  }

  Move-Item -LiteralPath $extractedJdk.FullName -Destination $jdkRoot
  Remove-ToolchainDirectory -Path $jdkStaging
  Write-Host "JDK 17 installed: $jdkRoot"
} else {
  Write-Host "JDK 17 is ready: $jdkRoot"
}

$sdkManager = Join-Path $sdkRoot "cmdline-tools\latest\bin\sdkmanager.bat"
if ($Force -or -not (Test-Path -LiteralPath $sdkManager)) {
  Download-File -Uri $androidToolsUrl -Destination $androidToolsArchive

  $androidStaging = Join-Path $toolchainsRoot "android-tools-staging"
  $latestToolsRoot = Join-Path $sdkRoot "cmdline-tools\latest"
  Remove-ToolchainDirectory -Path $androidStaging
  Remove-ToolchainDirectory -Path $latestToolsRoot
  New-Item -ItemType Directory -Path $androidStaging -Force | Out-Null
  New-Item -ItemType Directory -Path (Split-Path $latestToolsRoot -Parent) -Force | Out-Null
  Expand-Archive -LiteralPath $androidToolsArchive -DestinationPath $androidStaging -Force

  $extractedTools = Join-Path $androidStaging "cmdline-tools"
  if (-not (Test-Path -LiteralPath (Join-Path $extractedTools "bin\sdkmanager.bat"))) {
    throw "The downloaded Android command-line tools archive is invalid"
  }

  Move-Item -LiteralPath $extractedTools -Destination $latestToolsRoot
  Remove-ToolchainDirectory -Path $androidStaging
  Write-Host "Android command-line tools installed: $latestToolsRoot"
} else {
  Write-Host "Android command-line tools are ready: $sdkManager"
}

$env:JAVA_HOME = $jdkRoot
$env:ANDROID_HOME = $sdkRoot
$env:ANDROID_SDK_ROOT = $sdkRoot
$env:Path = "$(Join-Path $jdkRoot 'bin');$(Join-Path $sdkRoot 'platform-tools');$env:Path"

$requiredSdkPaths = @(
  (Join-Path $sdkRoot "platform-tools\adb.exe"),
  (Join-Path $sdkRoot "platforms\android-35\android.jar"),
  (Join-Path $sdkRoot "build-tools\35.0.0\aapt2.exe")
)
$missingSdkPackages = $requiredSdkPaths | Where-Object { -not (Test-Path -LiteralPath $_) }

if ($Force -or $missingSdkPackages.Count -gt 0) {
  Write-Host "Accepting Android SDK licenses..."
  1..100 | ForEach-Object { "y" } | & $sdkManager "--sdk_root=$sdkRoot" --licenses | Out-Null
  if ($LASTEXITCODE -ne 0) {
    throw "Android SDK license acceptance failed with exit code $LASTEXITCODE"
  }

  Write-Host "Installing Android SDK 35 packages..."
  & $sdkManager "--sdk_root=$sdkRoot" "platform-tools" "platforms;android-35" "build-tools;35.0.0"
  if ($LASTEXITCODE -ne 0) {
    throw "Android SDK package installation failed with exit code $LASTEXITCODE"
  }
} else {
  Write-Host "Android SDK 35 packages are ready: $sdkRoot"
}

$localPropertiesPath = Join-Path $nativeRoot "local.properties"
$sdkProperty = "sdk.dir=$($sdkRoot.Replace('\', '/'))`n"
[System.IO.File]::WriteAllText(
  $localPropertiesPath,
  $sdkProperty,
  [System.Text.UTF8Encoding]::new($false)
)

Write-Host ""
Write-Host "Development environment is ready."
Write-Host "Build a Debug APK without signing credentials:"
Write-Host "  .\build-local.ps1 -Debug"
