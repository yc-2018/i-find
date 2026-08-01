# 本地工具链目录

这个目录用于存放项目私有的 JDK、Android SDK 和 Gradle 缓存。除本说明文件外，目录中的内容都不会提交到 Git。

在 Windows 仓库根目录执行以下命令即可自动初始化：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\setup-dev.ps1
```

脚本会按需创建：

```text
.toolchains/
├── android-sdk/       Android SDK、Platform Tools、API 35 和 Build Tools
├── downloads/         下载缓存
├── gradle-user-home/  Gradle Wrapper 下载及依赖缓存
└── jdk17/             JDK 17
```

这些文件体积较大且包含机器相关状态，不应提交。需要重新安装时，可以删除对应的子目录后再次运行 `setup-dev.ps1`。

如果不使用项目本地工具链，也可以自行安装 JDK 17 和 Android SDK 35，然后在 `native-android/local.properties` 中配置本机的 `sdk.dir`。
