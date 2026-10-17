# DiPlay Legacy · Android 4.4

针对 **iOS 15.8.8 + Android 4.4 车机**的无线 CarPlay 连接问题，基于 Legacy v0.2.68 保留旧系统兼容层，补入 DiPlay 上游的连接恢复修复。

## 适用问题

iPhone 已完成蓝牙配对，CarPlay 中能看到 DiPlay，但车机停在“控制通道运行中”，随后提示：

```text
no airplay tcp after carplay startsession
```

相关背景见 [DiPlay issue #455](https://github.com/shihabal3amri/DiPlay/issues/455)。该问题还涉及旧 iOS 的 USB 模式切换失败，**本版针对无线连接，不宣称解决 USB 问题**。Issue 中的手机／系统与本次实测不同，仅作为相关故障参考。

## 改了什么

- 保留 Legacy 的 Android 4.4、ARMv7、旧蓝牙和音视频兼容实现。
- 保留 IPv4 优先；首次 AirPlay 连接超时后，有可用另一地址族时切换偏好，供下一次自动重试使用。
- 补充延迟 IPv6 等待、地址重新读取和取消保护。
- 同步新 Android 的本地网络权限处理，不影响 Android 4.4。

## 下载与使用

**[到 Releases 下载 APK](https://github.com/Ray-Hx/DiPlay-Legacy-Android4.4/releases/tag/v0.2.68-android4.4-wireless-test)**

已反馈有效：**iPhone 7 / iOS 15.8.8 + 2019 款欧尚 A800 / Android 4.4.2**。其他设备需自行验证。

这是独立 Debug 测试包（`com.shihab.diplay.hudtest`），可与原版共存。安装后先停止原版，再在测试版中重新配对、选择无线连接方式；使用“同一 Wi-Fi”时，手机和车机应连接同一网络。

- 开机自启：设置 → 连接 → 自动连接 → 车机启动后打开。
- 悬浮入口：设置 → 高级 → 悬浮小白点。

更多改动、测试与限制见[详细说明](docs/local/ANDROID44-WIRELESS-DETAILS.zh-CN.md)。

## Contributors / 来源致谢

- [shihabal3amri / DiPlay](https://github.com/shihabal3amri/DiPlay)：DiPlay 原作者、无线连接修复来源。
- [programmerguohuajing / DiPlay-Legacy-Android](https://github.com/programmerguohuajing/DiPlay-Legacy-Android)：Android 4.4 兼容版本来源。

本仓库为非官方修改版，保留原项目[许可证](LICENSE)和[第三方声明](docs/THIRD_PARTY_NOTICES.md)。测试 APK 沿用上游公开的实验性运行身份，并非 Apple 认证产品。
