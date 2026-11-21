<p align="center">
  <h1 align="center">MagiskSU</h1>
  <p align="center"><b>KernelSU-style UI • Magisk-based root manager</b></p>
  <p align="center">
    <a href="https://github.com/yahya3886/MagiskSU/actions/workflows/android.yml"><img src="https://github.com/yahya3886/MagiskSU/actions/workflows/android.yml/badge.svg" alt="Build"></a>
    <img src="https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white" alt="Platform">
    <img src="https://img.shields.io/badge/Taban-Magisk-000000" alt="Magisk">
    <img src="https://img.shields.io/badge/License-GPL--3.0-blue.svg" alt="License">
  </p>
</p>

---

**MagiskSU** is a Android superuser (root) manager that follows the **exact
layout of KernelSU Manager** while being **100% Magisk based**. All su
decisions are stored and edited in Magisk's own database via `magisk --sqlite`.

> [!IMPORTANT]
> Independent community project. Not affiliated with **topjohnwu/Magisk** or
> **tiann/KernelSU**. KernelSU is only the UI reference; Magisk is the engine.

## Features

**UI (mirrors KernelSU)**
- Four bottom tabs: Home • Superuser • Module • Settings
- Home: hero status card, superuser/module counters, system + Magisk info cards
- Superuser: icon, label, package, policy chip, grant/revoke switch, search
- Module: enable switch, staged removal, zip installer, reboot button
- Settings: theme (System/Light/Dark), Material You colors, log viewer
- Material 3, light/dark themes, dynamic color (Android 12+)

**Root management (Magisk based)**
- Reads/writes the Magisk `policies` table through `magisk --sqlite`
- Manages `/data/adb/modules` (`disable`, `remove`, `update` flag files)
- Stages zip modules into `/data/adb/modules_update` (applied on next boot)
- Toggles **Zygisk** and **Denylist** (`settings` table)
- Log viewer: Magisk daemon log + su requests from logcat
- Device info: kernel (`uname -r`), Android, API, model, Magisk version/path

## Requirements

- Android 8.0 (API 26)+ with **Magisk installed**
- Magisk must grant **su permission** to this app
- JDK 17+, Android SDK 35 to build

## Build

```bash
git clone https://github.com/yahya3886/MagiskSU.git
cd MagiskSU
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease    # app/build/outputs/apk/release/app-release.apk
```

CI builds on every push; download APKs from **Actions → Artifacts**.

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## How it works

```
magisk --sqlite "SELECT uid, policy, until, logging, notification FROM policies"
magisk --sqlite "REPLACE INTO settings (key, value) VALUES ('zygisk', 1)"
magisk -v ; magisk -V ; magisk --path
```

Schema (`native/src/core/sqlite.cpp`):

```sql
CREATE TABLE policies (uid INT, policy INT, until INT, logging INT,
                       notification INT, PRIMARY KEY(uid));
CREATE TABLE settings (key TEXT, value INT, PRIMARY KEY(key));
CREATE TABLE denylist (package_name TEXT, process TEXT,
                       PRIMARY KEY(package_name, process));
```

`policy`: `0 = QUERY`, `1 = DENY`, `2 = ALLOW`, `3 = RESTRICT`.

Module install: zip is extracted with Zip-Slip protection, `module.prop` is
validated, files are copied to `/data/adb/modules_update/<id>` and Magisk moves
them into place on the next reboot.

## Limitations

- The su **prompt dialog** is shown by Magisk itself; MagiskSU manages the
  stored decisions.
- This project does **not** contain a kernel driver (like KernelSU's GKI
  patch) — only the UI layout is borrowed.
- Uses `QUERY_ALL_PACKAGES`; intended for personal use on your own device.

## License

[GPL-3.0](LICENSE) — the same license as Magisk.
