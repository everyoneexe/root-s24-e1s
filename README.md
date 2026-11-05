# Root S24 (e1s / S921BXXSDCZB2)

Per-boot local root on **Galaxy S24 SM-S921B** firmware `S921BXXSDCZB2`
(kernel `6.1.138-android14-11`), then late-load **KernelSU** so
`me.weishu.kernelsu` (or `com.rifsxd.ksunext`) reports Working LKM.

Not affiliated with [Root My Galaxy](https://github.com/BuSung-dev/Root-My-Galaxy).
No GitHub payload feed, no multi-device catalog. One firmware, bundled
payload. Use a device you own.

## Download

Install the APK from **[Releases](https://github.com/everyoneexe/root-s24-e1s/releases)**.

KernelSU manager (required):
[v3.3.0](https://github.com/tiann/KernelSU/releases/download/v3.3.0/KernelSU_v3.3.0_32601-release.apk)

## Device

| | |
| --- | --- |
| Model | SM-S921B |
| Codename | e1s |
| Firmware | S921BXXSDCZB2 |
| Kernel | 6.1.138-android14-11 |
| Bug | CVE-2026-43499 (futex PI UAF) |
| Root | RAM-only; reboot drops it |
| Manager | KernelSU v3.3.0 (or Next) |

## Layout

```
src/                 exploit + UMH helper (this target only)
src/targets/e1s-S921BXXSDCZB2/
docs/                this firmware + Next LKM notes
kernelsu/            Next-compat .ko + ksud, GPL module source
app/                 Root S24 (Kotlin), bundled DCZB2 payload
```

## App

Settings matches Root My Galaxy (theme, language menu, Shizuku). Shizuku is
the reliable transport on this phone (uid 2000). Without it, app UID can
SIGKILL the helper (exit 137).

Keep the screen on during install. Do not kill leftover
`cve-2026-43499-root` keepers after a write.

## Build from source

```sh
export ANDROID_NDK_HOME=/path/to/android-ndk
make

cd app
./gradlew :app:assembleDebug
```

KernelSU rebuild: DDK `ghcr.io/ylarod/ddk-min:android14-6.1-20260313`,
vermagic `6.1.138-android14-11`. See [docs/kernelsu-next.md](docs/kernelsu-next.md).

adb (clean boot), same payload the app bundles:

```sh
export EXPLOIT_ATTEMPTS=1 P0_ATTEMPT_TIMEOUT_SEC=300 EXPLOIT_ATTEMPT_TIMEOUT_SEC=420
/data/local/tmp/cve-2026-43499-root --run-payload \
  /data/local/tmp/cve-2026-43499-app.so \
  /data/local/tmp/cve-2026-43499-root \
  /data/local/tmp/dczb2.log
```

## License

Exploit/helper: Apache-2.0 (see LICENSE and NOTICE).
`kernelsu/kernel`: GPL-2.0.
