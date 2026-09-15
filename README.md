# VoxenTopUp

Plugin Paper 1.21.x/Java 21 untuk pengumuman top-up rank LuckPerms dan Buku Kontrak VoxenSMP.

## Instalasi
1. Build dengan `mvn clean package`, lalu salin `target/VoxenTopUp.jar` ke folder `plugins` server Paper.
2. Instal LuckPerms terlebih dahulu. VoxenTopUp memakai API resmi LuckPerms dan tidak akan aktif tanpa dependency tersebut.
3. Edit `plugins/VoxenTopUp/config.yml`, lalu gunakan `/vt reload`.

## Command dan permission
| Command | Permission | Keterangan |
|---|---|---|
| `/topup <nama_buku> <nick>` | `voxentopup.topup` (op) | Mengumumkan kontrak; tidak memberi item/balance/rank. |
| `/voxentopup reload` (`/vt reload`) | `voxentopup.reload` (op) | Memvalidasi dan menerapkan konfigurasi baru. |

Jika `/topup` sudah dipakai plugin lain, gunakan command namespace Bukkit, misalnya `/voxentopup:topup`, atau ubah command yang konflik pada plugin lain.

## Rank dan contract
Tambahkan key pada `ranks` atau `contracts` dengan `enabled`, `display-name`, dan harga integer non-negatif. Harga dirender memakai `settings.price-format`; `%price%` menjadi angka format Indonesia seperti `10.000`.

Contoh rank: `/lp user Reii parent set prime`. Hanya bentuk tepat `lp`/`luckperms user <nick> parent set <group>` yang dideteksi (case-insensitive untuk keyword). Plugin menunggu beberapa tick lalu memuat user melalui LuckPerms API dan hanya mengumumkan jika primary group benar-benar cocok. Target offline didukung untuk rank.

Contoh contract: `/topup basic Reii`. Target offline dapat diproses selama pernah dikenal server bila `settings.contract.require-online-player` bernilai `false`.

## Placeholder broadcast
Rank: `%player%`, `%uuid%`, `%group%`, `%rank%`, `%prefix%`, `%price%`.

Contract: `%player%`, `%uuid%`, `%contract%`, `%price%`.

Semua line broadcast dan messages menerima MiniMessage; warna legacy seperti `&6&l` juga didukung. Prefix diambil dari cached meta LuckPerms, dengan fallback `display-name` config.

## Reload, batasan, troubleshooting
Reload bersifat atomik pada runtime: konfigurasi baru divalidasi sebelum menggantikan konfigurasi aktif. Bila invalid, konfigurasi lama tetap digunakan dan console mencatat alasannya. Cache deduplikasi rank tetap hidup saat reload dan menekan pengumuman sama dalam window konfigurasi.

Chat vanilla tidak dapat menampilkan PNG skin besar secara portabel. Karena itu broadcast menggunakan komponen Adventure, nama/prefix, dan visual Unicode yang kompatibel, tanpa NMS atau layanan eksternal. Aktifkan `settings.debug` untuk diagnosis command dan verifikasi group.
