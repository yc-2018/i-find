# I find

`I find` 是一款使用 Kotlin 和 XML Views 编写的轻量级原生安卓搜索启动器。

## 主要功能

- 配置搜索项的主链接和备用链接
- 新增、编辑、隐藏、删除及拖拽排序搜索项
- 支持内置图标、已安装应用图标、相册图片、首字图标和网络图片
- 网络图片首次加载成功后缓存到本地
- 搜索历史按日期分组，可折叠、单独删除或按天清空
- 可选用 Shizuku 自动恢复被禁用、暂停或冻结的应用
- 搜索项、设置和历史记录均保存在设备本地

## 项目目录

- `native-android/`：当前 Android Studio 原生项目
- `build-native.bat`：在 Windows 本地构建签名 APK
- `install-native.bat`：把现有 APK 安装到已连接的安卓设备
- `build-local.ps1`：Windows 本地构建脚本

进行安卓开发时，请使用 Android Studio 打开 `native-android/` 目录。

## 本地构建

本地构建脚本需要以下文件和工具：

- 项目本地 JDK 和 Android SDK：`.toolchains/`
- 安卓签名配置：`credentials.json`
- Release 签名文件：`android/keystores/release.keystore`

这些本地工具、凭据和签名文件均已被 Git 忽略，不会上传到仓库。

双击 `build-native.bat` 可以构建 APK。生成的安装包位于：

```text
builds/i-find-native-arm64-v8a-release.apk
```

手机开启 USB 调试并连接电脑后，双击 `install-native.bat` 可以直接安装或覆盖更新应用。

## GitHub 自动构建和发布

推送代码到 `main` 分支后，[GitHub Actions 工作流](.github/workflows/android-release.yml) 会自动：

1. 配置 JDK 17 和 Android SDK 35。
2. 使用与本地版本相同的 Release 签名构建 `arm64-v8a` APK。
3. 上传 APK 到本次 Actions 运行的构件中。
4. 创建 GitHub Release，并将 APK 标记为最新发布版本。

自动发布标签格式为：

```text
v版本号-build.工作流运行编号
```

仓库需要配置以下 GitHub Actions Secrets：

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

签名信息只保存在 GitHub Secrets 中，不会写入代码、工作流日志或 Git 历史。

## 分支说明

- `main`：当前 Kotlin 原生安卓版本
- `legacy`：旧 Expo 和单文件 H5 版本归档
