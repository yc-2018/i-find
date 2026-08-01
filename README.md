# I find

`I find` 是一款使用 Kotlin 和 XML Views 编写的轻量级原生安卓搜索启动器。当前只构建 `arm64-v8a` APK，最低支持 Android 8.0（API 26）。

## 主要功能

- 配置搜索项的主链接和备用链接
- 新增、编辑、隐藏、删除及拖拽排序搜索项
- 设置页分为“搜索项”和“应用设置”，避免配置开关与长列表混在一起
- 支持内置图标、已安装应用图标、相册图片、首字图标和网络图片
- 网络图片首次加载成功后缓存到本地
- 搜索历史按日期分组，可折叠、单独删除、按天清空或全部清空
- 可选用 Shizuku 自动恢复被禁用、暂停或冻结的应用
- 搜索项、设置和历史记录均保存在设备本地

## 项目结构

- `native-android/`：Kotlin 原生 Android 项目，使用 XML Views 和 ViewBinding
- `native-android/app/src/main/assets/builtin-icons/`：自动扫描的内置图片图标目录
- `.toolchains/`：项目本地 JDK、Android SDK 和构建缓存，仅提交目录说明
- `setup-dev.ps1`：Windows 开发环境一键初始化脚本
- `build-debug.bat`：无需发布签名的 Debug APK 构建入口
- `build-native.bat`：使用私有签名构建 Release APK
- `install-native.bat`：安装已构建的 Release APK 到 USB 设备
- `.github/workflows/android-release.yml`：推送 `main` 后自动构建和发布

进行 Android 开发时，请使用 Android Studio 打开 `native-android/`，不要把仓库根目录当作 Android 工程打开。

## 从零开始

### 环境要求

- Windows 10 或 Windows 11，64 位
- PowerShell 5.1 或更高版本
- 可访问 Adoptium、Google Android 仓库和 Gradle 下载服务的网络
- 首次初始化及依赖下载建议预留约 2 GB 磁盘空间

不需要预先安装 Node.js、Expo、Android Studio、JDK 或 Android SDK。命令行开发所需的 JDK 17 和 Android SDK 35 可以由项目脚本下载到 `.toolchains/`。

### 1. 克隆项目

```powershell
git clone git@github.com:yc-2018/i-find.git
cd i-find
```

没有配置 GitHub SSH Key 时，也可以使用 HTTPS：

```powershell
git clone https://github.com/yc-2018/i-find.git
cd i-find
```

### 2. 初始化本地工具链

在仓库根目录执行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\setup-dev.ps1
```

脚本会自动完成：

1. 下载并解压 JDK 17 到 `.toolchains/jdk17/`。
2. 下载 Android SDK 命令行工具到 `.toolchains/android-sdk/`。
3. 接受 Android SDK 许可证并安装 Platform Tools、API 35 和 Build Tools 35.0.0。
4. 生成已被 Git 忽略的 `native-android/local.properties`。

下载中断时可以直接重新运行。需要彻底重装时执行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\setup-dev.ps1 -Force
```

`.toolchains/` 中除 [说明文件](.toolchains/README.md) 外的所有内容均被 Git 忽略，不需要也不应该上传。

### 3. 构建 Debug APK

普通开发和 fork 测试不需要原作者的签名文件或 GitHub Secrets：

```powershell
.\build-local.ps1 -Debug
```

也可以双击：

```text
build-debug.bat
```

生成文件位于：

```text
builds/i-find-native-arm64-v8a-debug.apk
```

Debug 版包名为 `com.cgl.ifind.debug`，可以和正式版同时安装。

## 添加内置图标

把图片直接放入以下目录即可：

```text
native-android/app/src/main/assets/builtin-icons/
```

支持 `.png`、`.webp`、`.jpg`、`.jpeg` 和 `.svg`。应用构建后会自动扫描该目录，并在内置图标选择器中以每行五个图标显示。

文件名就是图标的唯一 Key，例如：

```text
zhihu.webp
```

保存到搜索项中的 `iconValue` 也会直接使用 `zhihu.webp`。不需要编辑 Kotlin 映射、不需要 JSON 配置，也不会在图标选择器中显示文件名或额外文字。文件名不要重复，建议使用简短的英文小写名称以方便跨平台维护。

旧版本使用的 `asset:douyin` 等矢量图标 Key 仍保留兼容，不会影响已经保存的搜索项。

## 使用 Android Studio

希望通过图形界面开发时，可以安装最新版 Android Studio，并按以下方式打开项目：

1. 先运行 `setup-dev.ps1`，或者自行安装 JDK 17、Android SDK Platform 35 和 Build Tools 35.0.0。
2. 在 Android Studio 中选择 **Open**，打开仓库内的 `native-android/`。
3. Gradle JDK 选择 JDK 17；使用脚本初始化时路径为 `.toolchains/jdk17/`。
4. 等待 Gradle 同步完成，选择 `app` 配置后连接真机运行。

### IDEA 和 Android Studio 的 Gradle 目录

在 **Settings -> Build, Execution, Deployment -> Build Tools -> Gradle** 中，各目录应这样选择：

| 设置项 | 推荐值 |
|---|---|
| 项目根目录 | `<仓库目录>\native-android` |
| Gradle 项目文件 | `<仓库目录>\native-android\settings.gradle.kts` |
| Gradle 用户主目录 | `<仓库目录>\.toolchains\gradle-user-home` |
| Gradle 分发 | 使用项目的 Gradle Wrapper |
| Gradle JVM | `<仓库目录>\.toolchains\jdk17` |
| Android SDK | `<仓库目录>\.toolchains\android-sdk` |

Gradle 用户主目录用于保存 Gradle 下载文件和依赖缓存。也可以保留 IDEA 默认的 `C:\Users\用户名\.gradle`，但会和项目本地缓存分别占用空间并可能重复下载依赖。

不要把 `native-android/.gradle` 选为 Gradle 用户主目录。它只是当前 Android 工程自动生成的项目级缓存目录，可以随时重新生成，并且已被 Git 忽略。

如果使用自己安装的 Android SDK，请在不提交的 `native-android/local.properties` 中写入：

```properties
sdk.dir=C:/Users/your-name/AppData/Local/Android/Sdk
```

macOS 或 Linux 用户需要自行安装 JDK 17 和 Android SDK 35，然后在 `native-android/` 中运行：

```bash
./gradlew assembleDebug
```

## Release 签名构建

Release 构建用于升级正式安装版本，必须由发布者使用自己的私有签名。fork 项目不需要、也无法获得本仓库的 Release 私钥。

本地 Release 构建需要：

```text
android/keystores/release.keystore
credentials.json
```

可以将 [credentials.example.json](credentials.example.json) 复制为 `credentials.json`，再填写自己的签名信息。签名文件可使用 JDK 自带的 `keytool` 创建，例如：

```powershell
New-Item -ItemType Directory -Force .\android\keystores
& .\.toolchains\jdk17\bin\keytool.exe -genkeypair -v -keystore .\android\keystores\release.keystore -alias ifind -keyalg RSA -keysize 2048 -validity 10000
Copy-Item .\credentials.example.json .\credentials.json
```

请妥善保存 keystore、别名和密码。丢失原签名后，无法通过覆盖安装升级原应用。

配置完成后执行：

```powershell
.\build-local.ps1
```

或双击 `build-native.bat`。生成文件位于：

```text
builds/i-find-native-arm64-v8a-release.apk
```

`credentials.json`、`*.keystore`、APK、`.toolchains/` 实际工具文件和构建目录均已被 Git 忽略。

## 真机安装

手机开启开发者选项和 USB 调试，连接电脑并接受授权提示后，正式版可以双击：

```text
install-native.bat
```

Debug APK 可以使用以下命令安装：

```powershell
.\.toolchains\android-sdk\platform-tools\adb.exe install -r .\builds\i-find-native-arm64-v8a-debug.apk
```

## GitHub 自动构建和发布

推送代码到 `main` 分支后，[GitHub Actions 工作流](.github/workflows/android-release.yml) 会自动配置 JDK 17 和 Android SDK 35，构建签名的 `arm64-v8a` APK，上传 Actions Artifact，并创建 GitHub Release。

### 配置 GitHub Actions Secrets

自动发布使用的签名来自本地这两个私有文件：

```text
android/keystores/release.keystore
credentials.json
```

进入 GitHub 仓库的 **Settings -> Secrets and variables -> Actions**，在 **Repository secrets** 区域点击 **New repository secret**，依次创建以下四项：

| Secret 名称 | 填写内容 |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | `release.keystore` 文件转换后的完整 Base64 文本 |
| `ANDROID_KEYSTORE_PASSWORD` | `credentials.json` 中的 `android.keystore.keystorePassword` |
| `ANDROID_KEY_ALIAS` | `credentials.json` 中的 `android.keystore.keyAlias` |
| `ANDROID_KEY_PASSWORD` | `credentials.json` 中的 `android.keystore.keyPassword` |

在 Windows PowerShell 中，可以使用以下命令把 keystore 的 Base64 内容直接复制到剪贴板，不会在终端打印：

```powershell
$keystore = Resolve-Path .\android\keystores\release.keystore
[Convert]::ToBase64String([IO.File]::ReadAllBytes($keystore.Path)) | Set-Clipboard
```

执行后，将剪贴板内容粘贴为 `ANDROID_KEYSTORE_BASE64` 的值。

其余三个值也可以从 `credentials.json` 安全复制到剪贴板：

```powershell
$credentials = Get-Content -Raw -Encoding UTF8 .\credentials.json | ConvertFrom-Json

# ANDROID_KEYSTORE_PASSWORD
$credentials.android.keystore.keystorePassword | Set-Clipboard

# ANDROID_KEY_ALIAS
$credentials.android.keystore.keyAlias | Set-Clipboard

# ANDROID_KEY_PASSWORD
$credentials.android.keystore.keyPassword | Set-Clipboard
```

每次只执行需要的那一条复制命令，然后立即粘贴到对应的 GitHub Secret。保存 Secret 后，GitHub 不允许再次查看原值，只能更新或删除。

四项配置完成后，可以在仓库的 **Actions** 页面选择 **构建并发布 Android APK**，点击 **Run workflow** 手动测试；以后推送到 `main` 会自动构建并发布。

fork 不会继承原仓库的 Secrets。其他维护者如果需要自动发布，必须使用自己的 keystore 和密码重新配置以上四项；只做普通开发时直接构建 Debug APK 即可。

不要把 keystore、Base64 内容或密码写入 README、工作流、Issue、Actions 日志或 Git 历史。Base64 只是编码，不是加密。

## 分支说明

- `main`：当前 Kotlin 原生 Android 版本
- `legacy`：旧 Expo 和单文件 H5 版本归档

## 许可证与依赖

项目通过 Gradle 使用 AndroidX 和 Shizuku 等第三方依赖。第三方组件仍适用其各自的开源许可证。
