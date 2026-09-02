# SM-S921B / S921BXXSDCZB2

Hardware-owned profile for Galaxy S24 `e1s`. Offsets come from this
firmware’s own raw kernel Image and BTF. Sibling S921B records (DZE1 /
DZF2, kernel `6.1.157`) were used only as a cross-check.

## Identity

| | |
| --- | --- |
| Model | SM-S921B |
| Codename | e1s |
| Firmware | S921BXXSDCZB2 / S921BOXMDCZB2 |
| Fingerprint | `samsung/e1sxeea/e1s:16/BP2A.250605.031.A3/S921BXXSDCZB2:user/release-keys` |
| Kernel | `6.1.138-android14-11` `#1 SMP PREEMPT Fri Feb 13 05:28:07 UTC 2026` |
| Bug | CVE-2026-43499 (futex PI UAF) |
| Root | RAM-only; reboot drops KernelSU Next |

FUS package (region BOG):

```
SM-S921B_3_20260214070110_lkp8g99x0d_fac.zip
AP_S921BXXSDCZB2_…_MQB106573322_…
BL_S921BXXSDCZB2_…_MQB106573322_…
```

## Image hashes

```
boot.img           67108864  SHA-256 812209eaae12e9f96944bde6b709f4c4f0fbd80ea52c629fafe78f2c4d77af9e
kernel             37849600  SHA-256 8e51680cbfd66895803df9900413c74d2bd8cbec99adb418a231fecc2ea57e96
vendor_boot.img              SHA-256 a5eadc6ad136ecc02e8c301f47d2d0f67f82b21d94b75a72c50ae6e760586148
sboot.bin                    SHA-256 5eaaeab3e93620048a11021d9c82cb657e0f8442ed372d80d3f9f11f3f9184b4
vmlinux.elf                  SHA-256 375a28625f395a307b08db0aa79f1508c87e665aba2292870450a3c9bceba2dc
vmlinux.btf                  SHA-256 a4d071ca07f177116a2a3ad90dd5918aa3a26386f1282771bc949bb99960bf14
```

`vmlinux-to-elf` recovered 112,979 symbols at `KIMAGE_TEXT_BASE`
`0xffffffc008000000`. Raw BTF is kernel file interval
`[0x17dc6d8, 0x1d7669f)` (5,873,607 bytes).

`sboot.bin` (`Starting kernel...` at file `0x248848`) loads the Image at
physical `0x80000000` (`text_offset == 0`).

## Layout (this BTF)

- `file_operations` `0x110`
- `page` `0x40`
- compact `rt_mutex_waiter` `0x58`, `task@0x30` → `COMPACT_RT_MUTEX_WAITER=1`,
  `SLIDE_PSELECT_WORD_SHIFT=3`
- live `sched_blocked_reason` id `106`
- `/proc/config.gz`: `CONFIG_ARM64_MTE=y`, `CONFIG_KASAN_HW_TAGS=y` →
  `KERNELSNITCH_MTE_ENABLED=1`

## Exploit-relevant offsets

Address minus `KIMAGE_TEXT_BASE`:

| Macro | Offset |
| --- | ---: |
| `CALL_USERMODEHELPER_EXEC_WORK_OFF` | `0x000d3a70` |
| `SLIDE_TRACEFS_WORKER_CALLER_OFF` | `0x000db420` |
| `NOOP_LLSEEK_OFF` | `0x003a0664` |
| `COPY_SPLICE_READ_OFF` | `0x003ee1ac` |
| `CONFIGFS_READ_ITER_OFF` | `0x0046f5f4` |
| `CONFIGFS_BIN_WRITE_ITER_OFF` | `0x0046fb24` |
| `ASHMEM_IOCTL_OFF` | `0x00cd6e50` |
| `ASHMEM_COMPAT_IOCTL_OFF` | `0x00cd7788` |
| `ASHMEM_MMAP_OFF` | `0x00cd77e0` |
| `ASHMEM_OPEN_OFF` | `0x00cd7a0c` |
| `ASHMEM_RELEASE_OFF` | `0x00cd7a94` |
| `ASHMEM_SHOW_FDINFO_OFF` | `0x00cd7bb4` |
| `ANON_PIPE_BUF_OPS_OFF` | `0x011aaed0` |
| `ASHMEM_FOPS_OFF` | `0x01357530` |
| `SLIDE_NFULNL_LOGGER_NAME_OFF` | `0x01643cc9` |
| `KMALLOC_CACHES_OFF` | `0x016fcf78` |
| `SYSTEM_UNBOUND_WQ_OFF` | `0x0220ae58` |
| `SLIDE_NFULNL_LOGGER_OBJECT_OFF` | `0x02212a00` |
| `INIT_TASK_OFF` | `0x0221f800` |
| `SLIDE_RANDOM_TABLE_BOOT_ID_DATA_PTR_OFF` | `0x023537b8` |
| `ASHMEM_MISC_FOPS_OFF` | `0x02399030` |
| `ROOT_TASK_GROUP_OFF` | `0x02425cc0` |
| `SELINUX_ENFORCING_OFF` | `0x024fa328` |
| `SLIDE_SYSCTL_BOOTID_OFF` | `0x025a0a60` |

`p0_fingerprint.h` is 32 slides `0x000000`–`0x1f0000` from this Image.

## Runtime (this firmware)

Physical P0 oracle: KernelSnitch `mm_struct` leak → order-3 skb reclaim →
pselect waiter into `pipe_buffer.page` → KASLR from Image pages. Physical
slide equals virt slide (`APP_DATA_ALIAS_USES_SLIDE=1`).

Owner `sleep()`/`schedule()` panics via `task_top_pi_waiter(owner)->task`.
Mitigation: `APP_OWNER_NO_SCHEDULE`. One stack writer per boot. Payload
waits until boot uptime ≥ 120 s. `DEFAULT_EXPLOIT_ATTEMPTS=1`.

After CFI ARW + pipe physrw, UMH root (client uid of the caller, typically
2000 from adb/Shizuku) late-loads KernelSU Next. Do not kill leftover
`cve-2026-43499-root` keepers after a write.

Proven on device (run138 / isolated-tree run139): helper exit 0, `kernelsu`
Live size `221184`, manager package `com.rifsxd.ksunext`.
