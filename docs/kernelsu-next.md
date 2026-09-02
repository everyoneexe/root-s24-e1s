# KernelSU Next LKM (this firmware only)

Late-load module for `6.1.138-android14-11` / S921BXXSDCZB2. Not a generic
6.1.157 Samsung KDP image and not official KernelSU Manager
(`me.weishu.kernelsu`).

## Artifacts

| File | SHA-256 | Notes |
| --- | --- | --- |
| `kernelsu/android14-6.1_kernelsu-e1s-S921BXXSDCZB2-kdp-next.ko` | `05c391c7614d217c3cec9e8c11e85be3685ed1c5f8f29db4819237826e2b6c38` | 406888 bytes; Live size 221184 |
| `kernelsu/ksud-e1s-S921BXXSDCZB2-kdp-next` | `acd98abdbdaf5f50ff811ce55a0f9beaac74667af87fcad56dec6f5a409939f7` | 4897856 bytes |

On device the helper copies ksud to `/data/local/tmp/ksud-dczb2-next` and
bind-mounts it over `/system/bin/logcat` for:

```
logcat late-load --package-name com.rifsxd.ksunext
```

## Build

KernelSU `v3.2.5` commit `b0bc817b4e966aa6aa830834eaf6ef765d821d40` plus
`kernelsu/patches/KernelSU-v3.2.5-samsung-kdp-rkp-defex.patch`.

- DDK: `ghcr.io/ylarod/ddk-min:android14-6.1-20260313`
- vermagic: `6.1.138-android14-11`
- empty `__versions`, no `stop_machine`
- `CONFIG_KSU_SAMSUNG_{KDP,RKP,DEFEX,NO_PATCH_TEXT}=y`
- `KSU_VERSION=33214`, `KERNEL_SU_VERSION_TAG=v3.3.0`
- ioctl 98/99: `GET_HOOK_MODE` / `GET_VERSION_TAG`
- manager cert: size `0x3e6` hash `79e59011…` (Next); size2 `0x033b` hash
  `c371061b…` (official, unused by this app)

Stock KernelSU text-patching panics under Samsung EL2/KDP. This `.ko` uses
the no-patch-text Samsung hooks.

## After late-load

KernelSU sepolicy hides `/proc/modules` from untrusted_app. The Next manager
still sees Working LKM through `ksu_driver`. The app treats `late-load ok=1`
as success and keeps a per-boot receipt.

Zygisk Next is **not** supported on this RAM-only path: zygote is already
up when the LKM arrives, so `post-fs-data.sh` never runs.
