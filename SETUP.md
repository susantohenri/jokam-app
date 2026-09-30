# SETUP & PANDUAN DEPLOYMENT APLIKASI "TEMPAT SAMBUNG"

Aplikasi Android **Tempat Sambung** telah selesai dibangun menggunakan Kotlin, Jetpack Compose, Material 3, serta integrasi AdMob (Banner, Native, Rewarded) dan Google UMP Consent.

---

## 1. Commit & Push Data ke GitHub
Pastikan seluruh file data JSON dan thumbnail di-push ke remote GitHub repository (`susantohenri/jokam-app` branch `main`) agar aplikasi dapat mengunduhnya secara online:
```bash
git add scraper/places.json scraper/pengurus.json wallpaper/wallpapers.json wallpaper/thumbs/
git commit -m "feat: add places, pengurus, and wallpaper thumbs data"
git push origin main
```

---

## 2. Ganti AdMob Application ID ke Production ID
Di file [app/build.gradle.kts](file:///c:/Users/webhe/Downloads/MSI/Local%20Sites/jokam-app/app/build.gradle.kts):
Ganti sample App ID `ca-app-pub-3940256099942544~3347511713` pada baris:
```kotlin
manifestPlaceholders["admobAppId"] = "ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY"
```
dengan ID Aplikasi AdMob produksimu.

---

## 3. Masukkan Production Ad Unit IDs ke `ads_config.json`
Pada repository remote `susantohenri/admob-remote-configs` di file `tempatsambung/ads_config.json`:
- Masukkan production Ad Unit ID untuk:
  - `bannerAdUnitId`
  - `rewardedAdUnitId`
  - `nativeAdUnitId`
- Aktifkan iklan dengan mengubah:
  ```json
  "isAdsEnabled": true
  ```

---

## 4. Konfigurasi Pesan GDPR / Privasi di Konsol Google AdMob
1. Masuk ke **Google AdMob Console** &rarr; **Privacy & messaging** (Privasi & pengiriman pesan).
2. Buat pesan **European regulations (GDPR)** dan pilih aplikasi Tempat Sambung.
3. Pastikan statusnya **Published** agar form consent UMP dapat muncul otomatis saat dibuka pengguna di wilayah EEA/UK.

---

## 5. Tambahkan Hash Perangkat Uji (Test Device Hash)
Untuk testing AdMob di perangkat fisikmu tanpa risiko pembatasan akun (invalid traffic):
1. Jalankan aplikasi pada perangkat fisik via Android Studio.
2. Cari string `ConsentDebugSettings.Builder().addTestDeviceHashedId("...")` di logcat.
3. Perbarui nilai test device hash di [app/src/main/java/com/jokam/tempatsambung/ads/ConsentManager.kt](file:///c:/Users/webhe/Downloads/MSI/Local%20Sites/jokam-app/app/src/main/java/com/jokam/tempatsambung/ads/ConsentManager.kt).

---

## 6. Formulir Keamanan Data (Data Safety) di Google Play Console
Saat mengunggah build release ke Google Play Console, deklarasikan penggunaan data berikut:
1. **Lokasi (Location)**:
   - *Approximate location* & *Precise location*.
   - Tujuan: Fungsionalitas aplikasi (menghitung jarak ke tempat sambung terdekat secara on-device).
   - Lokasi **tidak disimpan** di server dan **tidak dibagikan** kepada pihak ketiga.
2. **ID Perangkat (Device or other IDs)**:
   - *Advertising ID* (dikumpulkan oleh Google Mobile Ads SDK untuk monetisasi iklan).

---

## 7. Build Perintah (Gradle)
- Build Debug:
  ```bash
  ./gradlew assembleDebug
  ```
- Build Release (Minified & Obfuscated via R8):
  ```bash
  ./gradlew assembleRelease
  ```
- Jalankan Lint:
  ```bash
  ./gradlew lint
  ```
Output APK berada di `app/build/outputs/apk/`.
