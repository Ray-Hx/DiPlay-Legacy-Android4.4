# DiPlay Legacy：Android 4.4 无线 CarPlay 连接修复

这个分支基于 [DiPlay Legacy Android v0.2.68](https://github.com/programmerguohuajing/DiPlay-Legacy-Android)，保留旧安卓车机兼容层，补入 [DiPlay 原仓库](https://github.com/shihabal3amri/DiPlay)的无线连接地址恢复修复。

**2026-10-17：测试用户已在 2019 款长安欧尚 A800（Android 4.4.2）与 iPhone 7（iOS 15.8.8）上反馈此测试版有效。** 这是单套设备的用户实测结果，不代表所有 Android 4.4 车机或 iPhone 均兼容；尚未根据成功连接日志确定本次实际采用了哪一种地址族。

## 适合哪些用户

- 原车或后装车机实际系统为 Android 4.4，依赖 Legacy 版本才能运行 DiPlay。
- 无线 CarPlay 能完成蓝牙配对、认证，iPhone 的“CarPlay 车载”中也能看到 DiPlay，但车机一直停在“控制通道运行中”，最终断开。
- 日志含 `no airplay tcp after carplay startsession`，或发送 StartSession 后 30 秒内没有第一条 AirPlay TCP 连接。
- 使用原车热点或同一 Wi-Fi / Same LAN，希望尝试上游 IPv4／IPv6 地址恢复机制。

优先推荐给与上述症状相同的用户。只遇到画面模糊、卡顿、导航静音、USB 不识别或认证失败的用户，本修复并不针对这些原因。Android 5/6 及其他旧车机保留原 Legacy 代码路径，但没有新增真机验证。清单最低 API 17 不等于 Android 4.2/4.3 已经验证。

## 为什么需要这个分支

上游作者在 [issue #151](https://github.com/shihabal3amri/DiPlay/issues/151)说明：这类报告的特征是认证成功后 iPhone 没有经 Wi-Fi 建立 AirPlay 连接；0.2.13 优先使用热点 IPv4，0.2.16 在首次超时后尝试另一地址族，并请求新 Android 的本地网络权限。

不能直接以最新版主线替换旧车机版本：本分支需要保留 Legacy 的旧系统适配。核对发现 Legacy v0.2.68 **已经有 IPv4 优先**，缺少的是超时后的地址族恢复及新 Android 权限处理。因此没有把“IPv4 优先”写成此次新增，也没有整体合并主线。

## 修改了哪些

| 项目 | 修改前 | 本分支行为 |
| --- | --- | --- |
| 默认地址 | 已优先 IPv4 | 保留默认策略 |
| 首次 AirPlay TCP 超时 | 缺少对应的地址族恢复流程 | 在既有 30 秒期限到达时重新读取当前接口；有可用另一地址族才保存偏好，供下一次既有自动重试使用 |
| 延迟出现的 IPv6 | 缺少恢复等待 | 偏好 IPv6 时最多等待 15 秒，使用带接口作用域的有效地址；必要时回退到可用 IPv4 |
| 地址新鲜度 | 重试缺少相应恢复 | 重新读取选定网络接口，更新监听与 Bonjour 候选，避免复用消失的地址 |
| 取消与竞态 | 缺少恢复流程的同步保护 | 用户取消、会话过期或已有 TCP 连接时，不让旧超时流程覆盖下一次的地址偏好 |
| Android 17 本地网络权限 | 缺少声明和请求 | 补充 `ACCESS_LOCAL_NETWORK` 声明与 API 37 请求，保留旧版手动热点／Same LAN 例外；Android 4.4 不走运行时权限请求 |
| 自动化验证 | 没有本次恢复的回归覆盖 | 补充地址族、延迟／消失地址、取消、已有连接及不同系统权限测试；增加 Dalvik 运行检查 |

上游对应改动：[IPv4 优先 b4db0ad7](https://github.com/shihabal3amri/DiPlay/commit/b4db0ad7)、[地址恢复 e197d5c7](https://github.com/shihabal3amri/DiPlay/commit/e197d5c7)、[权限 a2b3d54d](https://github.com/shihabal3amri/DiPlay/commit/a2b3d54d)。详细迁移核对见 [A800-WIRELESS-RECOVERY.md](docs/local/A800-WIRELESS-RECOVERY.md)。

**Android 17 权限不是 Android 4.4 连接改善的原因。** 没有可用 IPv6 时不会凭空创建 IPv6，也不会强制修改路由器、热点或手机网络配置。正常连接已被接受时不会为了切换地址而断开它。

## 保留了哪些 Android 4.4 兼容能力

保留原 Legacy 的传统 View 界面、系统版本判断、旧蓝牙适配、KitKat USB 描述符和 usbfs、旧版网络监听、音频和视频解码路径、Java 兼容处理、Multidex、ARMv7 支持及原生库 API 19 构建设置。没有升级依赖或最低系统版本，也没有引入主线现代界面替换。

改动范围为 13 个源码／测试／记录文件，核心连接修复提交 `bf4abe82`，Android 4.4 运行验证提交 `f05d198e`。其中还修正了一个 macOS `/var` 与 `/private/var` 规范化路径的测试断言，以及设置运行测试的宽屏导航假设；两项均为测试修正，没有改变用户功能。

## 测试版安装与使用

本次有效 APK 是 **Debug 独立测试包**，不是原作者签名的正式版本：

- 版本：`0.2.68-hud-test`，versionCode `88`。
- 包名：`com.shihab.diplay.hudtest`；原版为 `com.shihab.diplay`，两者可以共存。
- 已验证车机：S401AUTO-MX6Q，Android 4.4.2 / API 19，ARMv7，2019 款欧尚 A800。
- 已反馈有效的手机：iPhone 7，iOS 15.8.8；本轮测试沿用同一 Wi-Fi 场景，其他网络拓扑需分别验证。
- APK SHA-256：`c89c76b1eea0c72ea688dec16e2ca8ad5ac00abb0080cf708f7b2b481579a625`。

**不用卸载原版。** 使用测试版前先停止原版，避免两个投屏应用同时争用蓝牙和连接资源。测试版有自己的设置，需要重新选择 iPhone、连接方式，并按系统提示配对或授权。

通过 ADB 安装及启动（请将文件名替换为实际下载的名称；多设备时使用 `adb -s 车机序列号`）：

```sh
adb install -r DiPlay-A800-wireless-test.apk
adb shell am force-stop com.shihab.diplay
adb shell am start -n com.shihab.diplay.hudtest/com.shilapi.xcertplay.DiPlayActivity --es page home
```

启动后让手机和车机使用所选连接方式要求的网络，保持蓝牙开启，按 DiPlay 提示连接。首次 IPv4 尝试仍可能超时，地址族恢复发生在随后的自动重试中。

### 开机自启与悬浮窗

这两个功能是 **Legacy 原有功能，本次没有新写**。已在本次测试车机上开启并读取确认保存值：

- 设置 → 连接 → 自动连接 → **车机启动后打开**。
- 设置 → 高级 → 仪表盘地图相关区域 → **悬浮小白点**。离开 DiPlay 后显示，可拖动并点击返回；在 DiPlay 内部会隐藏。

开机自启仍受原车系统启动、休眠及后台管理影响；本次只确认开关保存，尚未记录完整断电再启动验证。新安装设备默认不会自动继承这些设置。原车应用列表不显示第三方图标时，可以先经 ADB 启动，再使用悬浮点进入。

### 恢复原版

停止测试版，再打开原版即可。需要时执行：

```sh
adb shell am force-stop com.shihab.diplay.hudtest
adb shell am start -n com.shihab.diplay/com.shilapi.xcertplay.DiPlayActivity --es page home
```

## 验证情况与限制

1. **A800 实车：** 安装成功、启动成功；用户反馈测试版有效。未取得成功会话的完整脱敏日志，因此不宣称已经通过日志证明 IPv6 切换是成功原因。
2. **源码检查：** shared/common/home 共 2,184 项测试通过、1 项跳过；Mobile/Home/MapHost 的 lint 零错误，三个 Debug 构建通过。
3. **Android 4.4.2 模拟器：** 15 项运行检查独立进程逐项通过，涵盖地址恢复、权限、设置、加密、USB 描述符、本地音频、H.264、AirPlay 监听及 lwIP TCP/UDP。见 [验证记录](docs/local/API19-EMULATOR-VALIDATION.md)。
4. **已知测试限制：** 全套同进程运行在第二次 lwIP 初始化附近停止推进，根因未确定；不能表述为整组同进程通过。x86 模拟器也不能替代 ARMv7 车机驱动及实体蓝牙验证。
5. **未解决或未验证：** iOS 15 单独存在的 USB 模式请求拒绝、所有车型的音频路由、其他手机／iOS 版本、长期连续运行、完整冷启动及休眠唤醒。

音乐、导航没有声音时需另查原车音源及应用音频路由。不要将本连接修复当作通用音频或性能补丁。

## 从源码构建

基线：Legacy v0.2.68，`96598336fbc39485ffedff2fbdc30b04b8112883`。保留原仓库的 Gradle、SDK／NDK 和原生依赖要求，详见 [构建说明](docs/BUILD.md)及 `mobile/build.gradle.kts`。

```sh
python3 scripts/prepare-libopus.py
python3 scripts/prepare-lwip.py
bash ./gradlew :shared:testDebugUnitTest :common:testDebugUnitTest :home:testDebugUnitTest \
  :mobile:lintDebug :home:lintDebug :maphost:lintDebug \
  :mobile:assembleDebug :home:assembleDebug :maphost:assembleDebug
```

普通源码构建不包含运行认证身份，不能据此保证完整 CarPlay 可用。独立测试包需要经合法取得的本地运行材料，通过 `DIPLAY_AUTH_ASSETS_DIR` 提供，并使用 `:mobile:assembleStandaloneDebug`；不要把身份私钥、证书、签名密钥、热点密码或原始诊断日志提交到源码仓库。测试 APK 已含实验性运行身份，转发前应理解上游对此身份的说明。不要假冒原作者签名发布。

## 致谢与许可

本分支基于 [programmerguohuajing/DiPlay-Legacy-Android](https://github.com/programmerguohuajing/DiPlay-Legacy-Android)，连接修复来自 [shihabal3amri/DiPlay](https://github.com/shihabal3amri/DiPlay)。原始项目来自 [xcertplay](https://github.com/shilapi/xcertplay)，界面及网站包含 DiAuto 衍生成果。保留 [LICENSE](LICENSE)、[第三方声明](docs/THIRD_PARTY_NOTICES.md)及 `docs/licenses` 中的许可。此分支为非官方修改，不代表原作者或 Apple，也不是 Apple 认证产品。
