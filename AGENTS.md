# AGENTS.md

本文件适用于仓库根目录及其所有子目录，供参与本项目的编码代理使用。

## 项目定位

- 应用名称：`I find`
- Android 包名：`com.cgl.ifind`
- 当前版本：`2.1.0`（`versionCode = 3`）
- 当前实现：Kotlin + XML Views + ViewBinding
- 当前主项目：`native-android/`
- 最低 Android：API 26
- 编译及目标 Android：API 35
- Java/Kotlin JVM：17
- 发布产物：仅配置 `arm64-v8a` 的签名 APK
- 构建方式：Windows 本地构建或 GitHub Actions 自动构建，不使用 EAS

`main` 只保存当前 Kotlin 原生版本。旧 Expo 和单文件 H5 版本位于 `legacy` 分支，除非用户明确要求，否则不要将 `legacy` 合并回 `main`，也不要在 `main` 重新引入 Expo、React Native、Node 或网页运行入口。

## 重要目录

- `native-android/app/src/main/java/com/cgl/ifind/data/`：数据模型、默认搜索项和本地持久化
- `native-android/app/src/main/java/com/cgl/ifind/ui/`：Activity、RecyclerView Adapter 和界面交互
- `native-android/app/src/main/java/com/cgl/ifind/util/`：搜索启动、图标加载、网络图标缓存和应用状态检测
- `native-android/app/src/main/java/com/cgl/ifind/shizuku/`：Shizuku 状态、授权和解冻命令
- `native-android/app/src/main/res/layout/`：XML 布局
- `native-android/app/src/main/res/values/strings.xml`：用户可见中文文案
- `build-local.ps1`：完整本地 Release 构建脚本
- `build-native.bat`：双击构建入口
- `install-native.bat`：双击安装入口

## 架构约束

### 搜索项模型

`SearchTarget` 的稳定字段为：

- `id`
- `name`
- `primaryTemplate`
- `fallbackTemplate?`
- `androidPackageName?`
- `iconMode`
- `iconValue`
- `hidden`
- `sortOrder`

搜索链接必须使用 `{keyword}` 作为占位符。启动搜索时通过 `Uri.encode` 编码关键词，先打开主链接，主链接不可用时再打开备用链接。

不要直接删除 `Models.kt` 中针对旧字段 `launchMode`、`schemeTemplate` 和 `webFallbackTemplate` 的读取迁移逻辑。已经安装过 Expo 版或旧原生版的用户依赖这些迁移保持数据可用。

### 排序和持久化

所有配置保存在 `AppStore` 使用的 `SharedPreferences` 中。修改搜索项、显示状态、排序和全局开关后必须立即持久化。

`AppStore.normalizeTargets()` 会先按 `sortOrder` 排序再重新编号。因此改变列表顺序时，必须先按新顺序更新每项的 `sortOrder`，再调用 `saveTargets()`。否则界面看似完成拖拽，但进入设置页或重新加载后会恢复旧顺序。

首页只对可见搜索项拖拽。保存首页顺序时必须使用 `saveVisibleTargetOrder()`，保留隐藏项在完整列表中的槽位。设置页排序作用于完整列表。

### 历史记录

- 历史记录默认开启，最多保留 500 条。
- 历史页按设备本地日期分组，默认展开。
- 支持单条删除、清除当天和清空全部。
- 关闭记录开关后不得删除已有历史。

### 图标

支持以下五种 `iconMode`：

- `builtin`
- `installedApp`
- `gallery`
- `generated`
- `remote`

相册图片必须复制到应用私有目录，不能长期保存系统临时 URI。替换或删除相册图标时，只允许删除应用私有 `files/icons/` 目录内由本应用管理的文件。

网络图标仅接受 `http` 或 `https`，先显示首字图标作为回退。首次下载成功后缓存到 `files/icons/remote/`，后续优先使用缓存。保留现有连接超时、读取超时、2.5 MB 大小上限、采样解码和并发请求合并逻辑，不要在主线程进行网络或大图解码。

### Shizuku

Shizuku 功能是可选增强，不能成为普通搜索的前置条件。

搜索目标存在 Android 包名且用户开启自动解冻时，先通过 `PackageStateInspector` 检查应用状态。正常可用的应用必须直接打开，不能每次都绑定 Shizuku 或执行 shell 命令。只有检测到禁用、暂停或被设备所有者隐藏等冻结迹象时，才调用 `ShizukuBridge.attemptDefrost()`。

解冻失败后仍需尝试现有主链接和备用链接。不要扩大 `ShizukuCommandService` 的 shell 命令白名单，除非需求明确且完成安全审查。

`androidPackageName` 无法从任意自定义 URL 可靠推断。内置项和“选择已安装应用”可以自动填写；其他自定义项保持可选手动输入。

## 界面约束

- 保持 XML Views 和 ViewBinding，不迁移到 Jetpack Compose。
- 保持竖屏设计。
- 所有 Activity 都要处理状态栏、导航栏和水滴屏安全区域；新增页面应复用 `applySystemBarInsets()`。
- 首页启动后自动聚焦搜索框并弹出输入法。
- 首页搜索项支持长按拖拽，设置页使用拖拽手柄排序。
- 首页搜索按钮文字使用较柔和的灰色，并受“显示搜索按钮文字”开关控制。
- 设置页列表底部必须留出可滚动空间，不能让最后一项被系统导航栏或固定区域遮挡。
- 内置图标选择器保持每行五列并直接显示图标，不要退回纯文字下拉框。
- 用户可见文案统一放在 `strings.xml`，保持 UTF-8 中文。不要把中文写成字面量 `\uXXXX`，也不要引入乱码或替换字符。

## 代码风格

- Kotlin 使用现有两空格缩进和当前命名风格。
- XML 延续现有四空格缩进。
- 优先使用现有 AndroidX 依赖和平台 API，不为简单功能引入大型库。
- 保持 Activity/Adapter/Store 的现有职责边界；不要把持久化或网络逻辑塞进 ViewHolder。
- 只为不直观的复杂逻辑添加简短注释。
- 修改数据模型时必须同时检查默认数据、编辑页、持久化、旧数据迁移和搜索启动流程。
- 不要无故修改应用包名、签名配置、版本号、最低 SDK 或 ABI 策略。

## 构建与验证

### 完整 Release 构建

在仓库根目录执行：

```powershell
.\build-local.ps1
```

或双击：

```text
build-native.bat
```

脚本执行 `clean assembleRelease`，并把最终 APK 复制到：

```text
builds/i-find-native-arm64-v8a-release.apk
```

完整 Release 构建依赖本机的 `.toolchains/`、`credentials.json` 和 `android/keystores/release.keystore`。如果这些本地文件不存在，应明确说明无法完成签名构建，不要创建或提交伪造凭据。

### Lint

配置好 JDK、Android SDK 和 `native-android/local.properties` 后，在 `native-android/` 执行：

```powershell
.\gradlew.bat --no-daemon :app:lintRelease
```

新增代码至少应通过 Kotlin 编译和相关资源处理。发布前优先完成 `lintRelease` 与完整 Release 构建。

### GitHub Actions

`.github/workflows/android-release.yml` 在每次推送到 `main` 后自动构建并创建 GitHub Release。工作流必须继续使用 JDK 17、Android SDK 35、现有 Release 签名和 `arm64-v8a` APK。

工作流依赖以下仓库 Secrets：

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

不要把这些值改成工作流明文，也不要在日志中输出它们。自动发布标签使用 `v版本号-build.运行编号`，同一次工作流重新运行时应覆盖原 Release 资源，而不是因标签已存在而失败。

### 真机验证

手机开启 USB 调试并授权后，可双击：

```text
install-native.bat
```

关键回归场景：

1. 首页自动聚焦、清空按钮和搜索跳转正常。
2. 首页与设置页拖拽排序保持一致，重启后顺序不变。
3. 新增、编辑、隐藏、删除和恢复默认均能持久化。
4. 五种图标来源均可回退，网络和相册图标重启后仍可显示。
5. 历史记录按天展开、折叠和清除正常。
6. 正常应用打开时不等待 Shizuku；冻结应用才触发解冻。
7. 水滴屏、状态栏和底部导航区域没有遮挡内容。

## 发布要求

- 发布新安装包时同步递增 `versionCode`，并按需更新 `versionName`。
- 推送到 `main` 会自动创建 GitHub Release；提交前确认当前改动适合公开发布。
- 保持 Release 的 R8 压缩和资源缩减。
- 使用现有签名升级安装，避免破坏用户本地数据。
- 校验 APK 包名为 `com.cgl.ifind`、应用名为 `I find`、签名有效，并确认输出元数据为 `arm64-v8a`。
- 不要提交 APK、构建目录、本地 SDK/JDK、凭据或签名密钥。

## Git 与安全

- `main`：当前 Kotlin 原生版本。
- `legacy`：旧 Expo/H5 归档，不作为当前开发基础。
- 远程仓库：`git@github.com:yc-2018/i-find.git`
- 保持 `credentials.json`、`*.keystore`、`.toolchains/`、`builds/`、`local.properties`、Gradle 构建目录和 IDE 配置处于忽略状态。
- 提交前检查暂存文件，确认没有密钥、密码、APK 或大体积生成物。
- 不覆盖或回退用户未提交的修改；遇到冲突时先理解并保留现有工作。

## 完成标准

任务完成时应说明：

- 实际修改了什么用户行为。
- 执行了哪些编译、Lint、构建或真机验证。
- 最终 APK 的位置（如果已构建）。
- 仍未验证的真机条件或外部依赖。
