Bertindaklah sebagai Automation Engineer dan Bash Scripting Expert. Saya ingin membuat sistem scraping otomatis menggunakan tool open-source `gosom/google-maps-scraper`. 

Tujuan saya adalah melakukan scraping lokasi pencarian dengan kata kunci "ldii in [Nama Kota/Kabupaten], Indonesia" untuk seluruh 514 kota/kabupaten di Indonesia. Karena saya menggunakan 10 datacenter proxy gratisan, saya harus menghindari rate-limit Google dengan strategi "pelan tapi pasti" (batching & sleeping).

Berikut adalah spesifikasi sistem yang harus kamu buatkan untuk saya:

1. WORKFLOW BATCHING:
- Total kueri (~514 baris) harus dipecah menjadi file-file kecil (batch) yang masing-masing berisi maksimal 10 baris.
- Scraper akan dijalankan untuk 1 batch, kemudian sistem harus jeda (sleep) selama 90 menit (5400 detik) untuk mendinginkan IP proxy.
- Setelah jeda, sistem otomatis melanjutkan ke batch berikutnya.
- Semua hasil dari setiap batch harus digabungkan ke dalam satu file output utama bernama `hasil_ldii_indonesia.csv`.

2. COMMAND SCRAPER:
Command dasar scraper yang digunakan (asumsi menggunakan Docker, atau command line biasa):
`./google-maps-scraper -input [NAMA_FILE_BATCH] -results hasil_ldii_indonesia.csv -proxies-file proxies.txt -c 1 -depth 10 -resume`
(Pastikan flag `-resume` digunakan agar hasil baru ditambahkan ke CSV yang sama tanpa menimpa data sebelumnya).

TUGAS KAMU:
Tolong buatkan dan berikan saya kode/file berikut secara lengkap:
1. Sebuah script Python singkat (`generate_queries.py`) yang berisi daftar array 514 kota/kabupaten di Indonesia, lalu secara otomatis melakukan looping untuk men-generate file `queries.txt` dengan format "ldii in [NAMA KOTA], Indonesia".
2. Sebuah Bash Script (`automasi_scraper.sh`) yang melakukan logika automasi: 
   - Memecah `queries.txt` menjadi file-file batch (misal menggunakan command `split`).
   - Melakukan looping untuk membaca setiap file batch.
   - Menjalankan command scraper untuk file batch tersebut.
   - Melakukan echo progres (menampilkan info di terminal sedang di tahap mana).
   - Melakukan jeda (sleep 5400) sebelum lanjut ke loop berikutnya.
   - Membersihkan file batch sementara jika sudah selesai semua.
3. Berikan instruksi singkat langkah demi langkah (1, 2, 3) tentang cara menjalankan kedua script tersebut di terminal Linux/Mac/WSL.

Jangan berikan penjelasan bertele-tele, langsung berikan script-nya saja dengan komentar kode yang jelas.