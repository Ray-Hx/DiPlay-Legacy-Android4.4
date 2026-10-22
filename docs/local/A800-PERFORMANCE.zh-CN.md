# A800 Android 4.4.2 性能优化测试版

分支：`optimize/android-4.4.2-carplay`。基础提交：`5918906e`。版本：`0.2.68-a800-perf1`，包名沿用 `com.shihab.diplay.hudtest`。

## 改动与适用范围

- 按用户要求移除应用内更新：设置/关于页不再显示检查更新和自动更新，移除后台更新调度器及 APK 安装权限、更新文件提供器。此版本需手动安装后续 APK。

- 同一网卡的 mDNS 控制服务探测改用延迟重试队列。某个地址失败后，已发现的其他地址可以先尝试；不再等待前一个地址用尽七次重试。单次连接/读取超时及重试预算保持原样。只有走该发现路径且存在失败候选地址时才有对应收益，不代表所有连接都能缩短数十秒，也不解决旧 iPhone 在 StartSession 后不建立 TCP 的所有原因。
- AAC/Opus 直接从 RTP 数据写入解码器输入，AAC 复用 ADTS 头；LPCM 在现有 PCM 缓冲区中转换字节序，减少逐包数组分配。音乐抗抖缓冲大小保持原值。
- 音视频解码器复用输出元数据；音频缓存 KitKat 的缓冲区数组并在系统通知变化时刷新。视频在收到缓冲区变化通知后继续读取已完成帧，避免额外等待一次轮询。
- 保留 30 fps、旧设备默认 SurfaceView、完整视频参考帧链及恢复策略。不通过强制丢帧或缩短音频缓冲宣称降低延迟。

进一步追踪发现，视频网络接收层本已原地转换 Annex B，常规视频路径不存在最初怀疑的每帧完整转换副本。因此本版没有引入额外的视频格式转换重构。

减少高频临时对象用于降低旧系统回收压力；这是优化机制，实车帧率、延迟与断音改善幅度尚未测定。参考 [Android 内存管理说明](https://developer.android.com/topic/performance/memory-overview) 和 [MediaCodec 缓冲区协议](https://developer.android.com/reference/android/media/MediaCodec)。

## 安装与实车对照

这是 Debug 测试包，可与正式版共存，会更新相同包名、相同签名的旧测试版。启动测试版前停止其他 DiPlay，避免争用蓝牙和网络端口。保留原有连接方式以及画面、音频设置进行对照。

在同一台 A800、同一 iPhone、同一网络下分别测试原版和新版：各连接五次，记录打开应用到第一幅 CarPlay 画面的时间；再播放音乐并连续操作地图十分钟，记录触控响应、画面停顿和断音次数。正常手机成功不能代替旧 iPhone 的兼容性验证。

## 构建

依照 AGENTS.md 执行 shared/common/home 单元测试、mobile/home/maphost lint 与 Debug 打包。独立测试包使用既有本地运行认证材料以及 `:mobile:assembleStandaloneDebug` 构建；认证材料不进入 Git。真实运行检查使用独立 API 19 x86 模拟器，不能代替 A800 ARMv7 驱动、蓝牙、Wi-Fi 和 iPhone 完整会话测试。
