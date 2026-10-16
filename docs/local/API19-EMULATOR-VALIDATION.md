# Android 4.4.2 / API 19 emulator validation

Date: 2026-10-16 (Asia/Shanghai).

## Environment and provenance

Apple Silicon host; isolated official Intel Android Emulator 33.1.1 (build 9529220), Rosetta, software CPU emulation (`-accel off`), headless SwiftShader, 1024×600, 768 MiB RAM. The guest is the official Android 4.4.2 / API 19 **x86** image, not an API 25+ compatibility sandbox. The car's ARMv7 ABI, Bluetooth hardware and kernel drivers are not emulated.

- Image: Google `sys-img/android/x86-19_r06.zip`, SHA-1 `2ac82153aae97f7eae4c5a0761224fe04321d03d` verified.
- Emulator: Google `emulator-darwin_x64-9529220.zip`, SHA-256 `cbfcfd6914d1e9cb6065e822611fae4d12d30fabd1b3c56b996bde7421e3fd71` verified against the official archive page.
- Source core: `bf4abe82` (Legacy v0.2.68 plus the wireless backport); this follow-up adds runtime tests and a test navigation correction only.
- Package: `com.shihab.diplay.hudtest`, version `0.2.68-hud-test`, API floor 17, x86 native libraries.
- Tested standalone Debug APK SHA-256: `c89c76b1eea0c72ea688dec16e2ca8ad5ac00abb0080cf708f7b2b481579a625`.

Runtime authentication inputs were copied locally from the user's existing, SHA-256-verified v0.2.68 Legacy APK. Exactly the two expected assets were selected; they remain outside this repository in a private local work directory and were not displayed or committed. No release signing key was used. The standalone authentication preflight passed six local signatures with zero remote requests. This only validates local operations, not an iPhone's acceptance.

## Results

Both application and instrumentation APK installed successfully. **15 runtime checks passed individually, each in a fresh instrumentation process**. Every invocation reported `Tests run: 1, failures: 0, ignored: 0`.

- PASS `wirelessAddressRecoverySwitchesFamiliesOnDalvik`
- PASS `bundledAuthenticationLoadsAndSignsLocally`
- PASS `wirelessBackportPermissionsKeepKitKatOnInstallTimeGrants`
- PASS `settingsOpensEveryDriverCategoryOnKitKat`
- PASS `nativeTunBlockingUsesAuthorizedFdOnKitKat`
- PASS `connectionEntryScreensLaunchAndCloseWithoutModernApis`
- PASS `pairingCryptoRoundTripsOnDalvik`
- PASS `chachaRejectsTamperedCiphertextWithoutLegacyClassLinkageFailure`
- PASS `kitKatUsbDescriptorAdapterPreservesRealConfigurationAndAlternates`
- PASS `pcmAudioStartsWritesAndClosesWithLegacyFocus`
- PASS `h264FirstFrameRendersAndDecoderReleasesOnKitKat`
- PASS `airPlayListenerAnswersInfoAndRecoversOccupiedPortTwice`
- PASS `lwipUserspaceStackInitializesOnDalvik`
- PASS `lwipUserspaceFullPipelineTcpRelay`
- PASS `lwipUserspaceFullPipelineUdpRelay`

The new tests execute the real shared-module family policy/recovery on Dalvik, including delayed scoped IPv6 and the return to IPv4, and confirm that API 19 needs no runtime wireless permissions while the API-37 local-network permission branch remains executable. Reflection is used only to access Kotlin-internal production helpers across modules; the policy is not reimplemented in the test.

The legacy settings test had assumed Back always returns to the category list. The current wide-screen layout has a persistent category rail and Back leaves Settings. The test now uses the rail when present and Back only for compact layouts. No production navigation behavior was changed.

## Important observed limitations

- **Whole-suite, same-process execution did not complete.** UDP relay ran, its lwIP context was destroyed, and the following TCP relay initialized another context but then stopped making progress. The test was manually stopped; the final instrumentation `Process crashed` message resulted from that stop. Individual UDP, TCP and initialization checks all pass. This is an observed native lifecycle/test-isolation issue; the root cause is not established and the production native code has not been changed. Do not describe the whole class as passing in one process.
- The first source-only Debug package had no authentication inputs, so connection entry returned to setup as designed. The final standalone Debug package passes that entry check and the local authentication test.
- Unaccelerated x86-on-ARM emulation is slow. A standalone activity launch returned `Status: timeout`, but the ActivityManager subsequently recorded the real home screen displayed at **+14s688ms**. The screenshot confirms rendering. The final isolated launch log contains no `FATAL EXCEPTION` or `ANR in com.shihab` record. This is not a car-performance result.
- No physical iPhone, car Bluetooth/RFCOMM, car hotspot or USB device was connected. A complete CarPlay session, iPhone 7/iOS 15 acceptance and ARMv7 driver behavior remain unverified.

## Build checks

The full AGENTS.md task list passed again after the final instrumentation correction (`BUILD SUCCESSFUL`, 245 tasks). JVM suites remain shared: 1,250 tests (1 skipped); common: 931; Home: 4. Zero failures/errors. Mobile/Home/MapHost lint and Debug assemble all passed. `assembleStandaloneDebug` and `assembleDebugAndroidTest` also passed.

## Reproduce

Install the locally built standalone Debug APK and its Debug instrumentation APK on the API-19 device. Run every test method separately to avoid the observed same-process lwIP lifecycle wait:

```sh
adb -s emulator-5580 shell am instrument -w \
  -e class 'com.shilapi.xcertplay.T3LegacyRuntimeTest#wirelessAddressRecoverySwitchesFamiliesOnDalvik' \
  com.shihab.diplay.hudtest.test/com.shilapi.xcertplay.T3LegacyInstrumentation
```

Use the method names above for the other checks. The isolated SDK and AVD are retained under the task's `work/api19-sdk` and `work/api19-avd` directories. Runtime logs and the screenshot remain in the local work/output directories; no reports were uploaded. The emulator is stopped after evidence collection to release resources.
