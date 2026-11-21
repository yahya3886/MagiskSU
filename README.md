<p align="center">
  <h1 align="center">MagiskSU</h1>
  <p align="center">
    <b>KernelSU esinli arayüz • Magisk tabanlı root yöneticisi</b>
  </p>
  <p align="center">
    <a href="https://github.com/yahya3886/MagiskSU/actions/workflows/android.yml"><img src="https://github.com/yahya3886/MagiskSU/actions/workflows/android.yml/badge.svg" alt="Build"></a>
    <img src="https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white" alt="Platform">
    <img src="https://img.shields.io/badge/Taban-Magisk-000000?logo=magisk" alt="Magisk">
    <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-Material%203-blue" alt="UI">
    <img src="https://img.shields.io/badge/License-GPL--3.0-blue.svg" alt="License">
  </p>
</p>

---

**MagiskSU**, arayüzü **KernelSU Manager'ın birebir düzenini** izleyen, ancak tamamen
**Magisk tabanlı** çalışan bir Android root (süper kullanıcı) yöneticisidir.
Uygulama, kök kararlarını Magisk'in kendi veritabanı üzerinden (`magisk --sqlite`)
yönetir; KernelSU gibi hafif ve tanıdık bir arayüz sunar.

> [!IMPORTANT]
> Bu proje bağımsız bir topluluk çalışmasıdır; **topjohnwu/Magisk** veya
> **tiann/KernelSU** projeleriyle resmi bir bağlantısı yoktur.
> KernelSU yalnızca arayüz (tasarım) referansıdır, Magisk ise teknik tabandır.

---

## Özellikler

### Arayüz (KernelSU ile aynı düzen)
- **4 sekmeli alt gezinme:** Ana Sayfa • Superuser • Modül • Ayarlar
- **Ana Sayfa:** durum kartı (hero), Superuser/Modül sayaç kartları, sistem ve Magisk bilgi kartları
- **Superuser:** uygulama ikonu + ad + paket adı + durum rozeti + izin anahtarı (switch), arama alanı
- **Modül:** modül kartları, etkin/devre dışı anahtarı, kaldırma, zip ile kurulum, yeniden başlatma
- **Ayarlar:** tema (Sistem/Açık/Koyu), Material You dinamik renkler, kök ayarları, log görüntüleyici
- Material 3 tasarım, açık/koyu tema, dinamik renk (Android 12+) desteği
- Alt kenar (edge-to-edge) ve sisteme uyumlu renklendirme

### Kök yönetimi (Magisk tabanlı)
- **Süper kullanıcı listesi:** Magisk `policies` tablosundan okunur, izin kaldırma/verme doğrudan `magisk --sqlite` ile yapılır
- **Modül yönetimi:** `/data/adb/modules` okunur; `disable` / `remove` bayrak dosyalarıyla devre dışı bırakma ve kaldırma
- **Modül kurulumu:** `module.prop` doğrulaması ve Zip-Slip koruması ile modül `/data/adb/modules_update` dizinine hazırlanır, Magisk açılışta etkinleştirir
- **Zygisk ve Denylist** anahtarları (`settings` tablosu)
- **Log görüntüleyici:** Magisk daemon logu + su istekleri (logcat), temizleme/yenileme, seçilebilir metin
- **Cihaz bilgisi:** çekirdek (`uname -r`), Android sürümü, API, model, Magisk sürüm/yol

---

## Gereksinimler

| Gereksinim | Ayrıntı |
|---|---|
| Android | 8.0 (API 26) ve üzeri |
| Kök | Cihazda **Magisk** kurulu ve çalışır durumda olmalı |
| İzin | Bu uygulamaya Magisk üzerinden **su izni** verilmiş olmalı |
| Derleme | JDK 17+, Android SDK 35, Gradle 8.14.3 (wrapper dahil) |

---

## Kurulum

### 1) APK'yi kendiniz derleyin

```bash
git clone https://github.com/yahya3886/MagiskSU.git
cd MagiskSU
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Release (imzalı) sürüm için:

```bash
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

> `local.properties` dosyasında `sdk.dir` tanımlı olmalı (Android Studio bunu
> otomatik oluşturur) ya da `ANDROID_HOME` ortam değişkeni tanımlı olmalıdır.

### 2) GitHub Actions ile otomatik derleme

Depo her `push` işleminde otomatik derlenir; APK'ler **Actions → Artifacts**
bölümünden indirilebilir.

### 3) Kurulum

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

İlk açılışta Magisk su isteğini onaylayın.

---

## Kullanım

1. **Ana Sayfa** — root durumu, Magisk/Cihaz bilgileri ve sayaçlar.
   Kök erişimi görünmüyorsa Magisk Manager'dan bu uygulamaya izin verin.
2. **Superuser** — izin verilmiş/reddedilmiş uygulamalar; anahtarla izin kaldırın
   veya verin, satıra dokunarak detayı görün, üstteki alanda arama yapın.
3. **Modül** — modülleri açıp kapatın, kaldırın (yeniden başlatmada silinir),
   **Kur** ile `zip` modül yükleyin.
4. **Ayarlar** — tema, dinamik renk, **Zygisk**, **Denylist**,
   **Log görüntüleyici** ve yeniden başlatma.

---

## Mimari

```
MagiskSU/
├── app/src/main/java/com/magisksu/manager/
│   ├── MainActivity.kt              # Compose giriş noktası + kenar-kenara
│   ├── MagiskSUApp.kt               # Application
│   ├── root/
│   │   ├── RootShell.kt             # tek girişli `su -c` kabuk katmanı
│   │   └── MagiskCore.kt            # magisk CLI + veritabanı (sqlite) sarmalayıcı
│   ├── data/
│   │   ├── Models.kt                # SuperuserApp, ModuleInfo, ikon yardımcıları
│   │   ├── SuperuserRepository.kt   # policies + PackageManager eşleme
│   │   ├── ModuleRepository.kt      # /data/adb/modules + zip kurulumu
│   │   ├── LogRepository.kt         # magisk.log + logcat
│   │   └── SettingsStore.kt         # DataStore (tema, dinamik renk)
│   └── ui/
│       ├── MagiskSUScaffold.kt      # alt gezinme + sekmeler
│       ├── theme/                   # Material 3 renk/tema/tipografi
│       ├── components/              # kartlar, rozetler, diyaloglar
│       └── screens/                 # Home, Superuser, Module, Log, Settings
├── .github/workflows/android.yml    # CI (debug APK derlemesi)
├── LICENSE                          # GPL-3.0
├── README.md                        # bu dosya (Türkçe)
└── README_EN.md                     # İngilizce
```

---

## Teknik notlar

### Magisk veritabanı

Uygulama, Magisk'in kendi CLI'sını kullanır:

```bash
magisk --sqlite "SELECT uid, policy, until, logging, notification FROM policies"
magisk --sqlite "REPLACE INTO settings (key, value) VALUES ('zygisk', 1)"
magisk -v    # çalışan daemon sürümü
magisk -V    # sürüm kodu
magisk --path
```

Gerçek tablo şeması (`native/src/core/sqlite.cpp`):

```sql
CREATE TABLE policies (uid INT, policy INT, until INT, logging INT,
                       notification INT, PRIMARY KEY(uid));
CREATE TABLE settings (key TEXT, value INT, PRIMARY KEY(key));
CREATE TABLE denylist (package_name TEXT, process TEXT,
                       PRIMARY KEY(package_name, process));
```

`policy` değerleri: `0 = QUERY`, `1 = DENY`, `2 = ALLOW`, `3 = RESTRICT`.

### Modül kurulumu nasıl çalışır?

1. Zip, uygulama önbelleğine Zip-Slip korumasıyla açılır.
2. `module.prop` doğrulanır ve `id` temizlenir.
3. Dosyalar `su` ile `/data/adb/modules_update/<id>` dizinine kopyalanır.
4. **Yeniden başlatmadan sonra** Magisk modülü yerine taşır ve etkinleştirir.

> Not: Modül `customize.sh` betiği Magisk'in kendi kurucusu tarafından
> açılışta çalıştırılır; MagiskSU yalnızca dosyaları hazırlar.

---

## Sınırlamar

- Kök **istek penceresini** (dialog) Magisk kendisi gösterir; MagiskSU
  isteklerin **kararlarını** (izin/ret) yönetir ve `policies` tablosunu düzenler.
- KernelSU'nun çekirdek sürücüsüne (GKI ksjmi) **sahip değildir** — bu proje
  yalnızca arayüzü onun düzeninde, motoru Magisk'tedir.
- Play Store politikaları gereği `QUERY_ALL_PACKAGES` izni kullanılır;
  yalnızca kendi cihazınızda/kişisel kullanım için derlemeniz önerilir.

---

## Güvenlik

- Depoya **imzalama anahtarı, token veya gizli bilgi koymayın**.
- Fork/PR süreçlerinde GitHub token'larınızı paylaşmayın ve kullandıktan
  sonra hemen iptal edin.

---

## Katkı

Hata raporları ve pull request'ler açıktır. Değişiklik öncesi
`./gradlew assembleDebug` ile derlemeyi doğrulayın.

## Lisans

[GPL-3.0](LICENSE) — aynı lisans Magisk projesi için de geçerlidir.
