# 🕌 Tempat Sambung (Jokam App)

[![Android](https://img.shields.io/badge/Platform-Android%20(API%2026--36)-3DDC84?style=flat&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2B%20Material%203-4285F4?style=flat&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Google Play](https://img.shields.io/badge/Google%20Play-Published-414141?style=flat&logo=googleplay&logoColor=white)](https://play.google.com/store/apps/details?id=com.jokam.tempatsambung)
[![AdMob Policy](https://img.shields.io/badge/AdMob-100%25%20Policy%20Compliant-34A853?style=flat&logo=googleadmob&logoColor=white)](https://admob.google.com)

Aplikasi Android resmi **Tempat Sambung** (`com.jokam.tempatsambung`) — utilitas cepat, ringan, dan modern untuk mencari masjid dan tempat sambung LDII (Lembaga Dakwah Islam Indonesia) terdekat di seluruh Indonesia.

Dibangun dengan arsitektur **Zero-Backend** berbasis **Kotlin + Jetpack Compose**, aplikasi ini mengutamakan kecepatan akses, kemudahan navigasi bagi para musafir/traveler, privasi penuh tanpa login, serta kepatuhan 100% terhadap Google Play Store & Google AdMob Policies.

---

## 📌 Daftar Isi
1. [Latar Belakang & Potensi Pasar (ASO / SEO)](#-latar-belakang--potensi-pasar-aso--seo)
2. [Filosofi Arsitektur (Zero-Backend & Ultra-Lightweight)](#-filosofi-arsitektur-zero-backend--ultra-lightweight)
3. [Fitur Utama Aplikasi](#-fitur-utama-aplikasi)
4. [Sistem Monetisasi & Kepatuhan Kebijakan AdMob](#-sistem-monetisasi--kepatuhan-kebijakan-admob)
5. [Pipeline Data & Automasi Scraper](#-pipeline-data--automasi-scraper)
6. [Pipeline Wallpaper & Aset Desain](#-pipeline-wallpaper--aset-desain)
7. [Struktur Direktori Repositori](#-struktur-direktori-repositori)
8. [Setup & Panduan Menjalankan Proyek](#-setup--panduan-menjalankan-proyek)
9. [Deklarasi Keamanan Data (Play Console Data Safety)](#-deklarasi-keamanan-data-play-console-data-safety)

---

## 🎯 Latar Belakang & Potensi Pasar (ASO / SEO)

Setiap harinya, ribuan jamaah dan musafir melakukan perjalanan antar kota di seluruh Indonesia dan membutuhkan informasi cepat mengenai tempat ibadah (tempat sambung) serta kontak pengurus setempat.

Berdasarkan riset kata kunci pencarian organik Google ([keywords.ms](file:///c:/Users/webhe/Downloads/MSI/Local%20Sites/jokam-app/keywords.ms)):

| Target Keyword | Estimasi Volume Pencarian Bulanan | Intent Pengguna |
| :--- | :---: | :--- |
| **masjid ldii terdekat** | **90.500** | Kebutuhan navigasi GPS rute instan saat bepergian |
| **masjid ldii** | **49.500** | Informasi profil lokasi & alamat tempat sambung |
| **lembaga dakwah islam indonesia** | **49.500** | Pencarian resmi organisasi & legalitas |
| **ldii terdekat** | **27.100** | Pencarian cepat berbasis radius lokasi |

**Solusi Aplikasi Tempat Sambung:**
- Mengubah kebutuhan pencarian tinggi tersebut menjadi aplikasi Android utilitas yang solutif, cepat dibuka (< 1 detik), tanpa jeda login, dan langsung menampilkan tempat ibadah terdekat berdasarkan jarak GPS riil.

---

## 🏗️ Filosofi Arsitektur (Zero-Backend & Ultra-Lightweight)

Proyek ini sengaja dirancang dengan prinsip **Simplicity, Speed, and Zero Maintenance Cost**:

```
+-------------------------------------------------------------+
|                     Tempat Sambung App                      |
|                                                             |
|  +--------------------+             +--------------------+  |
|  |   Jetpack Compose  | <---------> | FusedLocation      |  |
|  |     Material 3     |             | (Haversine Sorted) |  |
|  +--------------------+             +--------------------+  |
|            |                                  |             |
|            v                                  v             |
|  +--------------------+             +--------------------+  |
|  | Single Activity    |             | Local DataStore    |  |
|  | (AppCompatDelegate)|             | (Favorites, Theme) |  |
|  +--------------------+             +--------------------+  |
+-------------------------------------------------------------+
         |                                  |
         | (HTTP Fetch - Raw CDN)           | (External Intents)
         v                                  v
+-------------------------------+   +-------------------------+
| GitHub Raw Storage (Free CDN) |   | 1-Click Intents:        |
| - scraper/places.json (6.262) |   | - Google Maps (Lat,Lng) |
| - scraper/pengurus.json(2.080)|   | - WhatsApp API (wa.me)  |
| - wallpaper/wallpapers.json   |   +-------------------------+
| - ads_config.json             |
+-------------------------------+
```

### Keunggulan Desain:
1. **Tanpa Backend Server Khusus**: Tidak memerlukan sewa server VM, database SQL, atau VPS. Seluruh dataset statis (`places.json`, `pengurus.json`, `wallpapers.json`) disajikan via GitHub Raw CDN.
2. **Tanpa Framework Berat**:
   - ❌ *No Dependency Injection* (Tidak memakai Hilt / Koin yang memperlambat build time).
   - ❌ *No Room Database* (Data dimuat ke memory runtime session, menghemat memori & storage).
   - ❌ *No Firebase SDK / Analytics* (Tidak ada pelacakan user, menjaga privasi 100%).
   - ❌ *No Google Maps SDK* (Tidak ada map view berat yang menghabiskan kuota & API billing).
3. **Kalkulasi On-Device**: Perhitungan jarak menggunakan formula matematika **Haversine** langsung di perangkat pengguna secara realtime.
4. **Target Modern**: `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`, R8 Minification + Resource Shrinking aktif, kompatibel dengan standar ukuran halaman memori Android 16 KB.

---

## 📱 Fitur Utama Aplikasi

### 1. 🏠 Beranda (Pencarian & Rute Tempat Sambung)
- **Deteksi Lokasi Otomatis**: Memanfaatkan `FusedLocationProviderClient` (akurasi balanced power). Mendukung izin lokasi *Approximate* maupun *Precise*.
- **Pengurutan Jarak Presisi**: Menghitung jarak terdekat ke terjauh (format jarak lokal: `850 m` atau `2,3 km`).
- **Mode Pencarian Kota (Fallback Tanpa GPS)**: Jika GPS mati atau izin ditolak, tersedia filter pencarian autocomplete 514 Kota/Kabupaten. Daftar diurutkan berdasarkan titik tengah (*centroid*) kota terpilih.
- **Sistem Favorit**: Simpan tempat sambung pilihan ke bagian teratas Beranda via Preferences DataStore.
- **Navigasi 1-Klik**: Tombol *"Buka Tempat Sambung"* langsung membuka aplikasi Google Maps rute koordinat tujuan (`https://www.google.com/maps/dir/?api=1&destination=LAT,LNG`).
- **Salin Alamat Cepat**: Salin alamat lengkap ke clipboard dalam satu sentuhan.

### 2. 👥 Pengurus (Kontak Sambung & Koordinasi)
- Menampilkan daftar kontak pengurus di seluruh Indonesia (2.080+ data terverifikasi).
- Secara cerdas mengurutkan pengurus di kota terdekat dengan lokasi pengguna terlebih dahulu.
- Tombol *"Hubungi Pengurus"* memicu Intent WhatsApp langsung (`https://wa.me/62...`) dengan nomor yang telah disanitasi otomatis.
- Fitur salin nomor telepon.

### 3. 🖼️ Wallpaper Islami
- Galeri wallpaper resolusi tinggi dalam grid 2-kolom dengan thumbnail WebP ringan (Coil async loader + crossfade).
- Pratinjau layar penuh (Full-screen preview).
- **Pasang Wallpaper**: Terapkan langsung ke layar utama atau layar kunci melalui `WallpaperManager`.
- **Simpan ke Galeri**: Simpan gambar ke album `Pictures/Tempat Sambung` menggunakan API modern `MediaStore` (bebas izin penyimpanan pada Android 10+ / API 29+; fallback aman untuk API 26–28).

### 4. ⚙️ Pengaturan & Preferensi
- **Dukungan Multi-Bahasa**: Bahasa Indonesia & English. Terintegrasi dengan `AppCompatDelegate.setApplicationLocales` dan persistensi otomatis.
- **Tema Tampilan**: Pilihan tema *Ikuti Sistem*, *Terang*, atau *Gelap* (palet Material 3 Green islami elegan).
- **Legal & Privasi**: Tautan kebijakan privasi resmi dan pengaturan konsensus privasi iklan Google UMP.

---

## 💰 Sistem Monetisasi & Kepatuhan Kebijakan AdMob

Aplikasi ini dirancang dengan kepatuhan ketat terhadap kebijakan **Google Play Store Spam & Policy** serta **Google AdMob Better Ads Experiences**:

### 1. Remote Config Terpusat (`ads_config.json`)
Konfigurasi iklan dikontrol secara dinamis dari remote GitHub repository tanpa perlu update rilis APK:
- `isAdsEnabled`: Saklar utama (*master switch*). Jika `false`, seluruh iklan nonaktif dan fitur langsung terbuka.
- `bannerAdUnitId`, `nativeAdUnitId`, `rewardedAdUnitId`: ID unit iklan produksi.
- Bendera kontrol fitur: `isBannerEnabled`, `isNativeEnabled`, `isRewardedRouteEnabled`, `isRewardedContactEnabled`, `isRewardedWallpaperEnabled`.
- **Keamanan Release**: Timeout 5 detik, non-blocking. Pada mode Debug menggunakan ID tes Google; pada mode Release jika gagal mengambil config, iklan tidak akan ditampilkan sama sekali (mencegah banned AdMob).

### 2. Google UMP Consent (Wajib GDPR/EEA)
- Menggunakan `UserMessagingPlatform` (UMP SDK).
- Form konsensus diperiksa dan ditampilkan saat aplikasi pertama kali dibuka sebelum inisialisasi SDK iklan dilakukan.
- Tombol *"Pengaturan privasi iklan"* tersedia di menu Pengaturan apabila pengguna berada di wilayah yurisdiksi GDPR/UK.

### 3. Penerapan Format Iklan:
- **Anchored Adaptive Banner**: Ditempatkan di bagian bawah layar (Beranda, Pengurus, Wallpaper grid) dengan divider dan margin pemisah minimal 8 dp dari elemen navigasi (mencegah klik tidak sengaja). Tidak ada banner di Pengaturan atau Preview Wallpaper.
- **Native Ads Elegan**: Disisipkan di antara daftar Beranda dan Pengurus (posisi ke-3 dan kelipatan 8, maksimal 3 per layar). Menggunakan desain kartu khusus yang kontras, dilengkapi badge *"Iklan / Ad"* dan ikon AdChoices resmi.
- **Rewarded Ads (Opt-in Sukarela)**:
  - Dipasang pada aksi: Buka Rute, Hubungi Pengurus, dan Pasang/Simpan Wallpaper.
  - Menampilkan dialog konfirmasi transparan dengan opsi *"Tonton Iklan"* atau *"Batal"*.
  - **Graceful Fallback**: Jika iklan gagal dimuat atau jaringan lemah, aksi pengguna **tetap dijalankan langsung** agar pengguna tidak terjebak.
  - **Zero Interstitials**: Bebas dari iklan interstitial pop-up yang mengganggu pengalaman pengguna.

---

## 🔄 Pipeline Data & Automasi Scraper

Seluruh data tempat ibadah diproses melalui pipeline otomatis di folder [`scraper/`](file:///c:/Users/webhe/Downloads/MSI/Local%20Sites/jokam-app/scraper/):

```
+--------------------------------------------------------------+
| 1. Query Generator (generate_queries.py)                     |
|    Mencakup seluruh 514 Kota & Kabupaten di Indonesia        |
+--------------------------------------------------------------+
                               |
                               v
+--------------------------------------------------------------+
| 2. Scraper Engine (google-maps-scraper.exe)                  |
|    - 52 Batch File (10 kueri per batch)                      |
|    - Proxy Rotator & Pool (proxies.txt)                      |
|    - PowerShell Automation (automasi_scraper.ps1)            |
|    - Auto-Resume Checkpoint (checkpoint.txt)                 |
|    - Live Monitoring Dashboard (monitor.ps1)                 |
|    - Cooldown jeda 90 menit per batch untuk proteksi IP      |
+--------------------------------------------------------------+
                               |
                               v
+--------------------------------------------------------------+
| 3. Data Cleaning & Normalizer (build_data.py)                |
|    Input: hasil_ldii_indonesia.csv (~49 MB)                  |
|    - Parsing sel JSON raksasa (field_size_limit 100 MB)      |
|    - Deduplikasi place_id, pembersihan koordinat             |
|    - Normalisasi nama Kota & Provinsi (514 wilayah standar)  |
|    - Standarisasi format nomor WhatsApp (format 62...)       |
|    - Filtering status tutup permanen / sementara             |
|    - Menghapus review, foto, & profil pribadi pengguna       |
+--------------------------------------------------------------+
                               |
            +------------------+------------------+
            v                                     v
+-----------------------+             +-----------------------+
|  scraper/places.json  |             | scraper/pengurus.json |
|    (6.262 Lokasi)     |             |    (2.080 Pengurus)   |
+-----------------------+             +-----------------------+
```

### Cara Memperbarui Data:
```bash
# 1. Jalankan pembersihan dan kompilasi CSV ke JSON
python scraper/build_data.py

# 2. Periksa ringkasan output data yang dihasilkan
# Output: scraper/places.json dan scraper/pengurus.json
```

---

## 🎨 Pipeline Wallpaper & Aset Desain

### 1. Wallpaper Optimizer ([`wallpaper/generate_json.py`](file:///c:/Users/webhe/Downloads/MSI/Local%20Sites/jokam-app/wallpaper/generate_json.py))
- Mengambil gambar mentah di `wallpaper/result/*.webp`.
- Membuat thumbnail proporsional berukuran 480 px di `wallpaper/thumbs/` menggunakan resampling Lanczos berkualitas tinggi.
- Menghasilkan katalog `wallpaper/wallpapers.json` yang siap diakses aplikasi.

### 2. Generator Aset Ikon ([`tools/generate_icons.py`](file:///c:/Users/webhe/Downloads/MSI/Local%20Sites/jokam-app/tools/generate_icons.py))
- Memproses gambar sumber menjadi aset Google Play dan aplikasi Android:
  - `store-assets/app-icon-512.png` (512×512 px, 32-bit PNG, full-bleed).
  - `store-assets/feature-graphic-1024x500.png` (1024×500 px banner).
  - Vektor Adaptive Icons untuk berbagai densitas layar (`mipmap-*`).
- **Desain Netral**: Menggunakan siluet kubah dalam pin lokasi berwarna hijau tua dan putih tanpa mencantumkan logo hak cipta pihak ketiga mana pun.

---

## 📁 Struktur Direktori Repositori

```
jokam-app/
├── app/                              # Modul utama aplikasi Android
│   ├── src/main/
│   │   ├── java/com/jokam/tempatsambung/
│   │   │   ├── MainActivity.kt       # Single Activity host & permission handler
│   │   │   ├── ads/                  # AdsManager & ConsentManager (Google UMP)
│   │   │   ├── data/                 # Model, RemoteConfig, dan Repositories
│   │   │   └── ui/                   # Jetpack Compose Screens (Home, Pengurus, Wallpaper, Settings)
│   │   ├── res/                      # Drawables, Mipmap, Strings (EN & ID), Styles
│   │   └── AndroidManifest.xml       # Deklarasi permission & konfigurasi AdMob App ID
│   ├── build.gradle.kts              # Konfigurasi dependensi modul app
│   └── proguard-rules.pro            # R8 rules untuk serialisasi & model data
├── scraper/                          # Mesin & skrip automasi pengumpul data
│   ├── automasi_scraper.ps1          # Automasi scraping Windows PowerShell
│   ├── automasi_scraper.sh           # Automasi scraping Linux / macOS
│   ├── monitor.ps1                   # Dashboard monitoring live terminal
│   ├── build_data.py                 # Skrip ETL: CSV -> places.json & pengurus.json
│   ├── hasil_ldii_indonesia.csv      # Raw dataset hasil scraping Google Maps
│   ├── places.json                   # Dataset publik 6.262 tempat sambung
│   └── pengurus.json                 # Dataset publik 2.080 kontak pengurus
├── wallpaper/                        # Pengelolaan aset wallpaper
│   ├── result/                       # Wallpaper resolusi penuh (.webp)
│   ├── thumbs/                       # Thumbnail ringan 480px (.webp)
│   ├── generate_json.py              # Script pembuat thumbnail & catalog JSON
│   └── wallpapers.json               # Metadata wallpaper publik
├── store-assets/                     # Aset publikasi Google Play Store
│   ├── app-icon-512.png              # Ikon 512x512
│   ├── feature-graphic-1024x500.png  # Feature Graphic 1024x500
│   ├── app-icon.svg                  # Master vector icon
│   └── feature-graphic.svg           # Master vector banner
├── tools/                            # Utility generator aset internal
│   └── generate_icons.py             # Script otomatisasi aset icon & mipmap
├── gradle/                           # Gradle Wrapper & Version Catalog (libs.versions.toml)
├── keywords.ms                       # Analisis kata kunci SEO/ASO target
├── prompt.md                         # Dokumen spesifikasi kebutuhan & arsitektur proyek
├── SETUP.md                          # Panduan deployment & konfigurasi manual
├── build.gradle.kts                  # Root build script Gradle
├── settings.gradle.kts               # Root settings Gradle
└── README.md                         # Dokumentasi lengkap proyek
```

---

## 🚀 Setup & Panduan Menjalankan Proyek

### Prasyarat:
- **Android Studio**: Ladybug (2024.2.1) atau versi lebih baru.
- **JDK**: Java Development Kit 17.
- **Python**: Versi 3.10+ (dengan paket `Pillow` terinstal: `pip install Pillow`).

### 1. Menjalankan / Build Aplikasi Android
```bash
# Clone repository
git clone https://github.com/susantohenri/jokam-app.git
cd jokam-app

# Build APK Debug (menggunakan test ad unit AdMob)
./gradlew assembleDebug

# Jalankan uji Lint
./gradlew lint

# Build Release APK / App Bundle (R8 Minified)
./gradlew assembleRelease
```
*File output APK akan berada di folder `app/build/outputs/apk/`.*

### 2. Memperbarui Data & Wallpaper
```bash
# Perbarui data JSON dari hasil scraping
python scraper/build_data.py

# Perbarui thumbnail dan catalog wallpaper
python wallpaper/generate_json.py

# Push perubahan ke remote GitHub repository
git add scraper/places.json scraper/pengurus.json wallpaper/
git commit -m "chore: update places, pengurus, and wallpapers data"
git push origin main
```

---

## 🔒 Deklarasi Keamanan Data (Play Console Data Safety)

Saat merilis aplikasi di Google Play Console, deklarasi formulir keamanan data adalah sebagai berikut:

1. **Lokasi Pengguna (Approximate & Precise Location)**:
   - **Tujuan**: Fungsionalitas aplikasi inti (menghitung jarak ke tempat sambung terdekat).
   - **Penyimpanan**: **Tidak disimpan**. Perhitungan dilakukan murni di memori lokal (*on-device*). Data lokasi tidak pernah dikirim ke server mana pun.
2. **ID Perangkat (Device / Advertising ID)**:
   - **Tujuan**: Monetisasi dan penayangan iklan melalui Google Mobile Ads SDK (AdMob).
   - **Penggunaan**: Sesuai dengan izin dan konsensus Google UMP yang dipilih oleh pengguna.

---

## 📄 Lisensi & Hak Cipta

Proyek ini dikembangkan untuk tujuan kemaslahatan jamaah dan musafir. Hak cipta kode sumber berada di bawah kepemilikan pengembang repositori [susantohenri/jokam-app](https://github.com/susantohenri/jokam-app).
