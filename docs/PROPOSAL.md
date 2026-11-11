# Proposal Proyek Akhir — PawCare
**Aplikasi Manajemen Klinik Hewan Berbasis Native Android (Jetpack Compose & Firebase)**

---

## 1. Judul Aplikasi & Informasi Mahasiswa
- **Nama Aplikasi:** PawCare (Veterinary Clinic & Pet Health Management)
- **Platform:** Native Android (Min SDK 26, Target SDK 35)
- **Teknologi Utama:** Kotlin 2.0, Jetpack Compose, Material Design 3, Firebase Authentication & Cloud Firestore
- **Model Arsitektur:** Model-View-ViewModel (MVVM) dengan Reactive `StateFlow`
- **Jadwal Pengerjaan:** 5 Minggu (Sesuai Final Project Guidelines 2026)

---

## 2. Latar Belakang & Problem Statement
Pemilik hewan peliharaan (*pet owner*) seringkali kesulitan memantau riwayat kesehatan anabul mereka secara teratur. Jadwal vaksinasi berkala sering terlewat, buku catatan vaksin fisik mudah hilang atau rusak, dan proses pendaftaran antrean klinik hewan konvensional masih mengharuskan telepon atau datang langsung tanpa kepastian slot waktu.

Di sisi pengelola klinik (*clinic admin / staff*), pencatatan rekam medis yang masih berbasis kertas menimbulkan risiko salah resep, pencarian riwayat pasien yang lambat, dan tumpang tindih jadwal kunjungan (*double-booking*).

PawCare hadir sebagai solusi digital terintegrasi yang menghubungkan pemilik hewan dan pihak klinik hewan dalam satu ekosistem *real-time* berbasis cloud.

---

## 3. Tujuan Proyek (*Objectives*)
1. Mempermudah pemilik hewan mendaftarkan profil anabul mereka dan melakukan reservasi kunjungan secara *self-service* dengan validasi slot waktu yang akurat.
2. Mencegah bentrok jadwal (*double-booking*) pada jam operasional klinik melalui penguncian slot transaksi di database.
3. Mengotomatisasi manajemen antrean kunjungan bagi admin klinik untuk menyetujui (*confirm*) atau menolak (*cancel with reason*) janji temu secara terstruktur.
4. Menyediakan buku rekam medis dan catatan vaksinasi digital yang dapat diakses seketika oleh pemilik hewan setelah pemeriksaan dokter selesai.
5. Memenuhi seluruh rubrik penilaian mata kuliah Mobile Programming: UI/UX modern (Material 3), arsitektur bersih (Clean MVVM), pengelolaan state reaktif, persistensi data cloud & luring (*offline*), dan kebersihan repositori tanpa *hardcoded secrets*.

---

## 4. Target Pengguna & Pembagian Peran (*User Roles*)
PawCare memiliki dua peran pengguna yang terisolasi secara ketat:

### A. Pet Owner (Pemilik Hewan)
- Melakukan registrasi mandiri akun baru menggunakan email dan password (role otomatis dikunci sebagai `owner`).
- Mengelola data hewan peliharaan (tambah, edit, dan hapus profil dengan avatar visual jenis spesies).
- Membuat booking janji temu dengan pemilihan layanan (Vaksinasi, Pemeriksaan Umum, Konsultasi, Sterilisasi), tanggal (tidak boleh tanggal lampau), dan slot jam yang belum terisi.
- Membatalkan permohonan janji temu selama masih berstatus `PENDING`.
- Melihat buku rekam medis digital (diagnosa, terapi, merk vaksin, dan jadwal kontrol berikutnya).

### B. Clinic Admin (Staf / Dokter Klinik)
- Akun admin dibuat langsung melalui Firebase Console dan diberikan otorisasi `role: "admin"` pada dokumen pengguna di Firestore.
- Memantau permohonan janji temu masuk dengan filter tanggal, menyetujui (*confirm*) atau menolak (*cancel*) disertai alasan wajib yang mengosongkan kembali slot jam.
- Menangani daftar pasien aktif hari ini dan menginput rekam medis digital (otomatis mengubah status janji temu menjadi `COMPLETED`).
- Mencari dan memfilter arsip riwayat kunjungan berdasarkan nama hewan atau email pemilik.

---

## 5. Fitur Utama MVP (5 Fitur Terintegrasi)

1. **Role-based Authentication & Route Gate:**
   Sistem login dan register menggunakan Firebase Auth dengan pembatasan hak akses berbasis Firestore. Splash screen secara otomatis mengarahkan user ke dashboard Owner atau Admin.
2. **Manajemen Profil Hewan (Pet CRUD):**
   Penyimpanan data hewan (nama, ras, umur bulan, berat badan kg, dan spesies). Proteksi penghapusan diterapkan jika hewan memiliki janji temu yang sedang aktif.
3. **Pemesanan Janji Temu Bebas Bentrok (Atomic Slot Lock Booking):**
   Formulir reservasi dengan pemilihan kalender dan jam klinik (`09:00` s.d. `16:00`). Dilengkapi transaksi atomik pada koleksi `booked_slots` untuk mencegah dua pengguna memesan jam yang sama di detik yang bersamaan.
4. **Pusat Antrean & Manajemen Pasien Admin:**
   Daftar antrean masuk dengan peninjauan cepat, filter per tanggal kunjungan, serta formulir penolakan yang mencatat alasan pembatalan ke pemilik.
5. **Rekam Medis & Buku Vaksin Digital:**
   Input diagnosa medis, tindakan terapi, merk vaksin (wajib pada layanan vaksinasi), catatan resep dokter, dan tanggal kontrol lanjutan. Mengubah status janji temu secara atomik menjadi selesai (*completed*).

---

## 6. Struktur UI/UX & Interkoneksi Layar (Total 10+ Layar)

Aplikasi dibangun menggunakan Single-Activity dengan Jetpack Compose Navigation:
- **Alur Autentikasi:**
  - `SplashScreen`: Verifikasi sesi login dan routing role otomatis.
  - `LoginScreen`: Masuk menggunakan email dan password.
  - `RegisterScreen`: Pendaftaran khusus Pet Owner.
- **Alur Pet Owner (Bottom Navigation 3 Tab):**
  - Tab 1: `PetsScreen` (Daftar kartu anabul, FAB tambah)
    - Sub-layar: `AddEditPetScreen` (Formulir tambah/edit dengan konfirmasi hapus)
  - Tab 2: `OwnerBookingsScreen` (Daftar janji temu aktif, tombol batal)
    - Sub-layar: `CreateBookingScreen` (Pemilihan pet, jenis layanan, DatePicker, slot jam dinamis)
  - Tab 3: `OwnerHistoryScreen` (Tab segmented: Buku Medis & Riwayat Selesai/Batal)
    - Sub-layar: `MedicalRecordDetailScreen` (Tampilan detail diagnosa dan buku vaksin)
- **Alur Clinic Admin (Bottom Navigation 3 Tab):**
  - Tab 1: `AdminQueueScreen` (Filter tanggal, kartu persetujuan / penolakan berdialog)
  - Tab 2: `AdminActivePatientsScreen` (Daftar pasien menunggu pemeriksaan)
    - Sub-layar: `InputMedicalRecordScreen` (Form input diagnosa, vaksin, resep, dan kontrol)
  - Tab 3: `AdminArchiveScreen` (Pencarian nama/email dan filter status selesai/batal)

---

## 7. Rencana Teknis & Skema Database

### Skema Cloud Firestore
1. `users/{userId}`: Data akun (`uid`, `email`, `role`, `createdAt`). Role terkunci di tingkat keamanan database (`firestore.rules`).
2. `pets/{petId}`: Profil hewan (`petId`, `ownerId`, `name`, `species`, `breed`, `ageMonths`, `weightKg`, `createdAt`).
3. `appointments/{appointmentId}`: Transaksi janji temu (`appointmentId`, `ownerId`, `ownerEmail`, `petId`, `petName`, `petSpecies`, `serviceType`, `date`, `timeSlot`, `slotKey`, `notes`, `status`, `rejectionReason`, `createdAt`, `updatedAt`).
4. `booked_slots/{date_slot}`: Dokumen pengunci slot waktu unik (`appointmentId`, `ownerId`, `date`, `timeSlot`).
5. `medical_records/{appointmentId}`: Dokumen rekam medis 1:1 terhadap janji temu (`appointmentId`, `petId`, `petName`, `ownerId`, `ownerEmail`, `serviceType`, `diagnosis`, `treatment`, `vaccineName`, `doctorNotes`, `visitDate`, `nextVisitDate`, `createdAt`).

### Kualitas Kode & Penanganan Error
- **Validation:** Validasi pure-Kotlin teruji unit test (format email, kekuatan password, batas umur & berat pet, proteksi tanggal lampau, alasan penolakan, mandatory vaksin).
- **State Handling:** `sealed interface UiState<T>` untuk `Loading`, `Success`, dan `Error`.
- **Offline First:** Firestore `PersistentCacheSettings` aktif agar data yang telah di-*load* tetap dapat dibaca saat koneksi terputus.

---

## 8. Jadwal Pengerjaan 5 Minggu

| Minggu | Agenda & Target Capaian | Status |
|---|---|---|
| **Minggu 1** | Penyusunan Proposal Proyek, inisialisasi arsitektur Kotlin + Jetpack Compose, konfigurasi Firebase Auth & Firestore, aturan keamanan `firestore.rules`. | **Selesai (Task 1–3)** |
| **Minggu 2** | Implementasi autentikasi (Login/Register/Splash), perancangan tema Material 3, komponen UI umum, dan navigasi `PawCareNavHost`. | **Selesai (Task 4–5)** |
| **Minggu 3** | Implementasi alur Pet Owner: CRUD hewan, visual avatar spesies, reservasi janji temu dengan kunci slot anti double-booking, dan riwayat janji. | **Selesai (Task 6)** |
| **Minggu 4** | Implementasi alur Clinic Admin: peninjauan antrean, persetujuan/penolakan booking, input rekam medis/buku vaksin, dan pencarian arsip. | **Selesai (Task 7)** |
| **Minggu 5** | Pengujian menyeluruh (*all unit tests pass*), build APK rilis (`app-debug.apk`), dokumentasi `README.md`, rekaman video demo 2–4 menit, dan demo langsung. | **Selesai (Task 8)** |
