# I find

`I find` is a lightweight native Android search launcher built with Kotlin and XML Views.

## Features

- Configurable search targets with primary and fallback links
- Add, edit, hide, delete, and drag to reorder targets
- Built-in, installed-app, gallery, generated, and cached remote icons
- Search history grouped by day
- Optional Shizuku-assisted defrost for disabled or suspended apps
- Local-only settings and history storage

## Project Layout

- `native-android/`: current Android Studio project
- `build-native.bat`: build the signed local APK
- `install-native.bat`: install the existing APK on a connected device
- `build-local.ps1`: local Windows build implementation

Open `native-android/` in Android Studio when developing the app.

## Local Build

The local build scripts expect the project-local JDK/Android SDK under `.toolchains/`, signing settings in `credentials.json`, and the release keystore at `android/keystores/release.keystore`. These local and sensitive files are intentionally excluded from Git.

The generated APK is written to `builds/i-find-native-arm64-v8a-release.apk`.

## Branches

- `main`: current native Kotlin version
- `legacy`: archived Expo and single-page web versions
