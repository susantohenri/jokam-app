# Automasi Scraper Google Maps - LDII Indonesia

Sistem scraping otomatis untuk mengumpulkan data Google Maps pencarian `"ldii in [Kabupaten/Kota], Indonesia"` untuk seluruh **514 Kota & Kabupaten di Indonesia**.

---

## 📁 Struktur Direktori

```
scraper/
├── google-maps-scraper.exe   # Binary scraper (v1.18.1 - sudah terunduh & siap pakai)
├── generate_queries.py       # Script Python (514 wilayah resmi Indonesia)
├── queries.txt               # 514 kueri pencarian (hasil generate)
├── batches/                  # 52 file batch (masing-masing 10 kueri)
│   ├── batch_001.txt
│   ├── ...
│   └── batch_052.txt
├── proxies.txt               # Tempat menaruh 10 proxy Anda
├── automasi_scraper.ps1      # Script utama Automasi Windows (PowerShell)
├── automasi_scraper.sh       # Script alternatif untuk Bash / WSL / Linux
├── monitor.ps1               # Script dashboard monitoring progress live
├── checkpoint.txt            # Menyimpan daftar batch yang sudah selesai (auto-resume)
├── scraper.log               # Log riwayat proses & timestamp
└── hasil_ldii_indonesia.csv  # File CSV hasil scraping gabungan
```

---

## 🚀 Langkah Menjalankan

### Langkah 1: Masukkan 10 Proxy Anda
Buka file `proxies.txt` lalu paste 10 proxy Anda (1 proxy per baris), contoh:
```text
http://user:password@103.152.118.1:8080
http://user:password@103.152.118.2:8080
...
```
*(Bisa juga format tanpa username/password jika IP di-whitelist: `http://ip:port`)*

---

### Langkah 2: Jalankan Script Automasi (Windows)
Buka terminal **PowerShell**, masuk ke folder `scraper`:
```powershell
cd "c:\Users\webhe\Downloads\MSI\Local Sites\jokam-app\scraper"
.\automasi_scraper.ps1
```

> **Catatan jika muncul error Execution Policy di PowerShell:**
> Jalankan perintah ini sekali saja:
> ```powershell
> Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
> .\automasi_scraper.ps1
> ```

---

## 📊 Cara Memonitor Progress & Status

Ada 3 cara mudah untuk memantau proses:

### 1. Layar Terminal Utama (Live Countdown & Progress)
Script utama menampilkan status batch yang sedang berjalan dan **countdown timer realtime** saat sedang jeda mendinginkan proxy:
```text
[BATCH 12 / 52] (23.1% SELESAI)
Memproses : batch_012.txt (10 kueri)
[JEDA PROXY] Sisa waktu: 01:29:45 [===>           ] 15% | Lanjut pukul 22:35:10 WIB
```

### 2. Dashboard Monitor (Buka di Terminal Lain)
Buka tab atau jendela terminal PowerShell baru, lalu jalankan:
```powershell
.\monitor.ps1 -Watch
```
Ini akan menampilkan dashboard interaktif yang otomatis refresh tiap 3 detik:
- Status terkini (`SCRAPING`, `SLEEPING`, `FINISHED`)
- Persentase penyelesaian (misal `25%`) & progress bar visual
- Estimasi jam mulai batch berikutnya
- Jumlah baris data yang sudah tersimpan di `hasil_ldii_indonesia.csv`
- 5 baris log aktivitas terakhir

### 3. Mengintip File Log secara Real-Time
Anda juga bisa memantau aliran log langsung menggunakan:
```powershell
Get-Content scraper.log -Wait -Tail 20
```

---

## 🛡️ Fitur Unggulan (Resilience & Auto-Resume)

* **Auto-Resume Tanpa Duplikasi**: Jika proses terhenti di tengah jalan (misal PC mati, internet putus, atau Anda tekan `Ctrl+C`), jalankan kembali `.\automasi_scraper.ps1`. Script membaca `checkpoint.txt` dan **langsung melanjutkan dari batch yang belum selesai**.
* **Filter Proxy Otomatis**: Baris komentar (`#`) atau spasi kosong di `proxies.txt` dibersihkan otomatis sehingga binary scraper tidak error.
* **Pencegah Rate-Limit**: Otomatis sleep 5400 detik (90 menit) setelah tiap 10 kueri untuk mengistirahatkan IP proxy.
* **Output Tunggal**: Semua batch di-append ke satu file `hasil_ldii_indonesia.csv` menggunakan flag `-resume` bawaan engine scraper.
