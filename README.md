# RESERV SAWAH

Aplikasi reservasi ruang rapat tema persawahan. Proyek ini dibuat baru dan berdiri sendiri.

## Isi
Kode aplikasi lengkap berada di arsip `reserv-sawah-lengkap.zip`. Arsip berisi Cloudflare Worker, skema database D1, konfigurasi Wrangler, dan petunjuk.

## Deploy tanpa memasang Node.js di komputer

Node.js hanya digunakan di lingkungan build Cloudflare, bukan di komputer Anda.

1. Buat database D1 di Cloudflare dengan nama `reserv-sawah-baru-db`.
2. Salin Database ID dari dashboard D1.
3. Buka **Workers & Pages → Create application → Import a repository**, pilih repo ini.
4. Atur build command:
   ```sh
   unzip -o reserv-sawah-lengkap.zip
   cd reserv-sawah-baru
   sed -i "s/GANTI_DENGAN_DATABASE_ID_D1/$D1_DATABASE_ID/" wrangler.toml
   npx wrangler deploy
   ```
5. Tambahkan environment variable build bernama `D1_DATABASE_ID` dengan Database ID dari langkah 2.
6. Hubungkan D1 binding bernama `DB` bila diminta oleh dashboard. Pastikan binding menunjuk database `reserv-sawah-baru-db`.
7. Sebelum deploy pertama, jalankan SQL pada `reserv-sawah-baru/schema.sql` melalui menu D1 → Console, atau gunakan fitur import SQL.
8. Simpan konfigurasi dan deploy.

> Penting: jangan pernah memasukkan Cloudflare API token atau password ke dalam file repo.

## Login awal
- Admin: `123456`
- User demo: `0002`

Segera ubah kode admin setelah login.