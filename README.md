# RESERV SAWAH 🌾

Aplikasi reservasi ruang rapat tema persawahan. Proyek baru, terpisah dari repositori lama.

## Fitur
- Login admin dan pengguna memakai kode akses.
- Admin mengelola pengguna, kode akses, ruang rapat, dan persetujuan reservasi.
- Pengguna membuat reservasi dan melihat riwayatnya.
- Pemeriksaan bentrok jadwal ruang dan waktu.
- Penyimpanan permanen Cloudflare D1.
- Tampilan responsif desktop dan ponsel.

## Deploy tanpa Node.js di komputer

### 1. Buat database D1
Di Cloudflare Dashboard buka **Storage & databases → D1 SQL Database → Create**.
- Nama database: `reserv-sawah-db`
- Setelah dibuat, salin **Database ID**.

### 2. Pasang tabel awal
Buka database `reserv-sawah-db` → **Console**. Salin seluruh isi file [schema.sql](schema.sql), tempel ke Console, lalu klik **Execute**. Ini membuat akun awal dan dua ruang rapat.

### 3. Hubungkan Worker ke GitHub
Di Cloudflare Dashboard buka **Workers & Pages → Create → Workers → Import a repository**, pilih repo **cahya31/reserv-sawah**.
- Build command: kosongkan / default.
- Deploy command: `npx wrangler deploy`.
- Pastikan root directory kosong (root repo).

### 4. Isi Database ID
Sebelum deploy pertama, buka file [wrangler.toml](wrangler.toml) di GitHub, klik Edit (ikon pensil), ganti teks `GANTI_DATABASE_ID_D1` dengan Database ID dari langkah 1, lalu Commit changes.

Cloudflare akan otomatis menjalankan deployment dari repositori. Setiap perubahan di branch `main` akan memicu deploy ulang.

## Login awal
- Admin: `123456`
- Pengguna demo: `0002`

Segera ubah kode admin setelah login dan jangan membagikan kode admin.

## Penting
- Aplikasi berjalan sebagai **Cloudflare Worker + D1**, bukan Pages static-only.
- Jangan memasukkan API token Cloudflare ke repositori.
- Gambar hero berasal dari Unsplash; fungsi reservasi tidak bergantung pada gambar tersebut.
