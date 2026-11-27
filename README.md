# Root S24 (e1s / S921BXXSDCZB2)

Per-boot local root on **Galaxy S24 SM-S921B** (`e1s`) firmware `S921BXXSDCZB2`
(kernel `6.1.138-android14-11`), late-loading **KernelSU** so
`me.weishu.kernelsu` (or `com.rifsxd.ksunext`) reports Working LKM.

A specialized downstream fork of [Root My Galaxy](https://github.com/BuSung-dev/Root-My-Galaxy)
by [BuSung-dev](https://github.com/BuSung-dev), pre-configured and optimized specifically
for this single device & firmware combination with bundled offline payloads.

> [!WARNING]
> **Disclaimer — Educational & Research Purposes Only**
>
> This project, its source code, and any prebuilt binaries are provided strictly for **educational and authorized security research purposes only**. Use this software only on devices that you own and have explicit permission to test.
>
> Kernel exploitation and loading out-of-tree kernel modules carry inherent risks, including kernel panics, instability, bootloops, data loss, or voided warranties. **The authors and contributors accept no responsibility or liability for any direct or indirect damage, data loss, device malfunction, or misuse resulting from the use of this repository.** You proceed entirely at your own risk.

## Download

Install the APK from **[Releases](https://github.com/everyoneexe/root-s24-e1s/releases)**.

KernelSU manager (required):
[v3.3.0](https://github.com/tiann/KernelSU/releases/download/v3.3.0/KernelSU_v3.3.0_32601-release.apk)

## Device & Exploits

| | |
| --- | --- |
| Model | SM-S921B |
| Codename | e1s (Samsung Exynos 2400) |
| Firmware | S921BXXSDCZB2 |
| Kernel | 6.1.138-android14-11 |
| Exploits | **CVE-2026-43499** (GhostLock / futex PI UAF)<br>**CVE-2026-98260** (SamTimer / posix-cpu-timers UAF) |
| Root Mode | RAM-only (reboot drops it) |
| Manager | KernelSU v3.3.0 / KernelSU-Next |

## Layout

```
src/                 GhostLock (CVE-2026-43499) exploit + UMH helper (Cortex-A720 pinned)
cve-2026-98260/      SamTimer (CVE-2026-98260) tracepoint KASLR + UMH pipe exploit
docs/                Target firmware analysis + Next LKM notes
kernelsu/            Next-compat .ko + ksud, GPL module source
app/                 Root S24 Android application (Kotlin), bundled DCZB2 payload
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

### GhostLock (CVE-2026-43499) manual execution:

```sh
export EXPLOIT_ATTEMPTS=1 P0_ATTEMPT_TIMEOUT_SEC=300 EXPLOIT_ATTEMPT_TIMEOUT_SEC=420
/data/local/tmp/cve-2026-43499-root --run-payload \
  /data/local/tmp/cve-2026-43499-app.so \
  /data/local/tmp/cve-2026-43499-root \
  /data/local/tmp/dczb2.log
```

### SamTimer (CVE-2026-98260) manual execution:

```sh
/data/local/tmp/samtimer/libtimer.so umhpipe
```

## License

Exploit/helper: Apache-2.0 (see LICENSE and NOTICE).
`kernelsu/kernel`: GPL-2.0.

## Credits & Acknowledgements

This standalone port is built on the research, tooling, and exploit foundations of the open-source Android security community:

- **[BuSung-dev](https://github.com/BuSung-dev)** — Author of [Root My Galaxy](https://github.com/BuSung-dev/Root-My-Galaxy) and [Root-My-Galaxy-Payloads](https://github.com/BuSung-dev/Root-My-Galaxy-Payloads). The exploit architecture, Samsung UMH helper integration, and late-load patterns in this repository are directly derived from their pioneering work.
- **[NebuSec / CyberMeowfia](https://github.com/NebuSec/CyberMeowfia)** — For the original [CVE-2026-43499 (IonStack)](https://github.com/NebuSec/CyberMeowfia/tree/main/IonStack/CVE-2026-43499/exploit) futex PI research and exploit proof of concept.
- **[KernelSU](https://github.com/tiann/KernelSU)** (`tiann`) & **[KernelSU-Next](https://github.com/rifsxd/KernelSU-Next)** (`rifsxd`) — For the underlying kernel-assisted su and LKM framework.

