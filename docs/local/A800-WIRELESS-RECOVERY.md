# A800 / Android 4.4 wireless connection backport

## Baseline and scope

- Legacy baseline: `v0.2.68`, commit `96598336fbc39485ffedff2fbdc30b04b8112883`.
- Upstream reviewed: `1b1ac5f2` (main at fetch time), plus upstream v0.2.13/v0.2.16.
- Local branch: `fix/same-lan-address-fallback`.
- Target: Oushang A800, S401AUTO-MX6Q, Android 4.4.2/API 19, ARMv7.
- Observed iPhone 7 / iOS 15.8.8 failure: iAP2/MFi authentication and StartSession succeed; no first AirPlay TCP arrives within 30 seconds.

This is a targeted connection backport, not a merge of upstream's modern Android UI or USB implementation.

## Audit against the maintainer's reply

1. **IPv4 first (upstream `b4db0ad7`, v0.2.13): already present.**
   Legacy `HotspotAddressPolicy` already selects usable IPv4 before link-local IPv6. Preserve that default.
2. **Retry the other family after first-TCP timeout (upstream `7b6f4585`, integrated as `e197d5c7`, v0.2.16): missing.**
   Port `HotspotAddressRecovery`, family-aware selection and controller integration from the integrated revision, including its delayed-IPv6 and cancellation checks.
3. **Android 17 local-network permission (upstream `a2b3d54d`): missing.**
   Add the manifest declaration and API-37-gated wireless request. Preserve Legacy's manual-hotspot/Same LAN exemptions and use `realSdkInt()` at the activity boundary. API 19 requests no runtime permissions; this permission cannot fix the A800's API-19 connection failure.

Sources:
- https://github.com/shihabal3amri/DiPlay/issues/151
- https://github.com/shihabal3amri/DiPlay/commit/b4db0ad7
- https://github.com/shihabal3amri/DiPlay/commit/e197d5c7
- https://github.com/shihabal3amri/DiPlay/commit/a2b3d54d

## Resulting behavior

- Start with IPv4 unless a prior first-TCP timeout saved a usable alternative preference.
- On that specific timeout, re-sample only the selected network interface and save the other family only when it currently exists.
- The next automatic retry reads the saved family; preferred IPv6 may wait up to 15 seconds for a delayed scoped link-local address, then uses available IPv4 if necessary.
- Use current, interface-scoped candidates for the listener and Bonjour. Preserve Legacy's existing restriction on additional mDNS families for old Same LAN firmware; an explicitly selected IPv6 address remains the primary responder on the IPv6 retry.
- Keep the original 30-second first-TCP deadline and bounded retry budget. An accepted AirPlay connection cancels the timeout and does not trigger a family flip.
- Serialize cancellation with preference writes. Stale generations cannot update the next attempt's preference.
- New logs include `preference=`, `alternate=`, `wireless address family flip published=IPv4 next=IPv6`, or `flip skipped` when the interface has no live alternative.

## Preserved Android 4.4 compatibility

No changes to minSdk, dependencies, traditional View UI, Java desugaring, multidex, ARMv7/native build settings, KitKat USB descriptors/usbfs handling, legacy Bluetooth adapters, legacy station network monitor, audio or decoder implementations. No new setting or automatic network reconfiguration is introduced.

## Verification and device acceptance

Prepare the pinned native sources with `python3 scripts/prepare-libopus.py` and `python3 scripts/prepare-lwip.py`, then run the AGENTS.md test/lint/assemble task list using `bash ./gradlew` (the baseline wrapper has no executable bit). A fresh checkout needs the explicit preparation: the baseline task wiring does not run these scripts before all `configureNdkBuildDebug` tasks. Keep their pinned commits unchanged.

One existing test needed a platform-portability correction: `DiagnosticExportFallbackTest.selectedWritableUsbVolumeReceivesTheSameUtf8Report` compared an absolute `/var/...` test path with the exporter's canonical `/private/var/...` result on macOS. Compare `copy.canonicalPath`, matching the unchanged exporter's contract. This is a test-only fix; diagnostic export behavior is unchanged.

Results are recorded below after execution.

New/regression coverage includes IPv4 preference, preferred IPv6 selection, delayed IPv6, absent/disappearing alternates, timeout-to-next-attempt flow, accepted TCP suppressing the flip, cancellation races and the API-19/23/29/31/33/36/37 wireless permission matrix.

A source-only debug APK is not a standalone CarPlay acceptance result: runtime authentication assets are external inputs. A separately signed build cannot replace the installed release in place unless the signing certificate matches. Do not uninstall the working app to test this patch.

Hardware acceptance remains necessary: on Same LAN, verify the first failed IPv4 attempt logs a family flip, the next advertises scoped IPv6, and whether an actual `airplay TCP accepted` / active session / rendered frame follows. If no live IPv6 exists, the patch intentionally keeps IPv4. This patch does not resolve iOS 15's separate USB mode-request refusal and does not establish the root cause of the wireless failure.

## Completed local validation (2026-10-16)

Full AGENTS.md task list: **BUILD SUCCESSFUL**, 3m 7s, after the macOS-only test assertion correction.

| Module | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| shared | 1250 | 0 | 0 | 1 |
| common | 931 | 0 | 0 | 0 |
| samples/home | 4 | 0 | 0 | 0 |

Mobile, Home and MapHost Debug assemble and lint all passed. Lint retains existing warnings; error counts are zero.
The separately run real-source network/JUnit subset passed 37 tests. The full Gradle run includes the four wireless permission tests and the 11 address recovery tests.

No runtime authentication files or release signing keys were committed. At the time of the local validation above, car acceptance was pending. On 2026-10-17 the standalone Debug APK was installed on the A800 and the user reported it effective with the iPhone 7 / iOS 15.8.8. No successful-session trace was collected to establish which address family was used. See [the Chinese device guide](../../README-A800.zh-CN.md) for the current scope. The follow-up [API 19 emulator report](API19-EMULATOR-VALIDATION.md) records the locally provisioned standalone Debug run, its individual passing checks and the same-process lwIP limitation.
