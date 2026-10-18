# A800 / iPhone 7 recurrence: KitKat multicast port sharing

On 2026-10-18 the installed `0.2.68-hud-test` again stalled after wireless authentication on iPhone 7 / iOS 15.8.8, using Existing Wi-Fi / Same LAN.

## Collected evidence

- iAP2 authentication succeeds and CarPlay StartSession is sent.
- Four observed attempts alternated IPv6 → IPv4 → IPv6 → IPv4; each hit the 30-second first-AirPlay-TCP timeout. The earlier address recovery patch is executing, but does not resolve this reproduction.
- The car received all three ICMP replies from the phone. This shows basic unicast reachability, not proof that every TCP or multicast path works.
- Both JmDNS socket construction paths fail with `BindException: EADDRINUSE`; no original DiPlay process is running, while the system `mdnsd` is present.
- `mdnsFamilies=nsd-fallback`, with no discovered/resolved control services.
- API-19 fallback registration has no TXT record. AOSP KitKat's `NsdService.registerService` also omits the TXT arguments. Thus the fallback is not equivalent to the complete bundled AirPlay advertisement. This is a concrete compatibility defect and a candidate explanation, not yet proof of the iPhone failure's complete root cause.

## Local correction

Keep JmDNS 3.6.3 pinned. Its input JAR is verified against SHA-256 `6b6eb1623cb1d9e51467312ea62c567da005c2aa5a95347f566a11b8703c0ef1`. A build task changes only the allocation and constructor call inside `JmDNSImpl.openMulticastSocket` to a small `KitKatMdnsSocket` subclass. It creates an unbound multicast socket, enables `SO_REUSEADDR`, then binds; failed construction closes the descriptor. No system daemon is stopped, and the NSD fallback remains available.

The build requires exactly one allocation and matching constructor, rejects a changed dependency, preserves every other JAR entry byte-for-byte (including licenses), and uses ASM 9.7.1 only during the Gradle build. The patch does not add ASM to the app. The version suffix `-hud-test-mdns1` distinguishes this local candidate. No public release APK is replaced by this experiment.

Regression checks cover two sockets sharing a port, preserving unbound construction, and the actual JmDNS factory using the corrected socket. An API-19 instrumentation method also registers a synthetic service with TXT fields through the real patched JmDNS call site.

References:
- [JmDNS 3.6.3 source](https://github.com/jmdns/jmdns/blob/v3.6.3/src/main/java/javax/jmdns/impl/JmDNSImpl.java)
- [AOSP KitKat NsdService](https://android.googlesource.com/platform/frameworks/base/+/android-4.4.2_r1/services/java/com/android/server/NsdService.java)

Validation results and hardware acceptance must be recorded after execution; a successful socket bind alone is not a successful CarPlay connection. Raw reports, network details and authentication inputs stay outside Git.

## Checks completed

- Full AGENTS task list plus standalone Debug and instrumentation builds: `BUILD SUCCESSFUL`, 3m44s; 275 tasks.
- shared: 1,253 tests, 1 skipped, zero failures/errors. common: 931 tests, zero failures/errors. Home: 4 tests, zero failures/errors. Total: 2,187 passing, 1 skipped.
- Verified only `javax/jmdns/impl/JmDNSImpl.class` differs between the original and generated dependency JAR; all other entry contents are identical.
- Installed `0.2.68-hud-test-mdns1` in place on A800, keeping the test app's settings and original DiPlay installation.
- Actual A800 / API-19 instrumentation `mdnsCanShareSystemPortAndPublishTxtOnKitKat`: 1 test, zero failures, zero ignored. This includes port reuse, the real patched JmDNS factory, and synthetic service registration with TXT properties while the system daemon remains running.
- Candidate APK SHA-256: `45806bb1a5a5562be55cc44e7a8e58bff8a577344cb1e47028cadc084a1a5327`.
- iPhone session acceptance is still pending; this candidate has not been published to GitHub Releases.

## Follow-up physical result

iPhone 7 / iOS 15.8.8 still fails on mdns1. Phone-side logs confirm both IPv4 and IPv6 StartSession addresses were received. Safari can reach the car's `/info` port; this manual TCP probe is explicitly not a CarPlay session. Phone packet capture also contains complete AirPlay mDNS TXT/address records. These observations do not identify the remaining root cause.

A same-Wi-Fi comparison with iPhone 16 Pro / iOS 27.0 on mdns1 succeeds: first IPv6 TCP at +0.77s, active AirPlay at +2.29s, first video frame at +3.04s after StartSession. The successful run also has bonjourAdded=0, bonjourResolved=0, connectProbes=0, so those counters alone are not a failure criterion. The user excludes built-in-hotspot testing; future investigation remains on Same LAN. Raw phone and car logs are local only.
