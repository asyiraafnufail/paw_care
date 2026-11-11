# PawCare — Manajemen Klinik Kesehatan Hewan

Aplikasi Native Android berbasis **Kotlin + Jetpack Compose + Material Design 3 + Firebase** untuk manajemen kunjungan klinik kesehatan hewan (vaksinasi, periksa umum, konsultasi, dan sterilisasi) dengan dua role terpisah: **Pet Owner** dan **Clinic Admin**.

## Tech Stack & Arsitektur

- **Language & UI:** Kotlin 2.0, Jetpack Compose (BOM 2024.10.01), Material Design 3
- **Architecture:** Clean MVVM + Single-Activity + Navigation Compose
- **State Management:** `ViewModel` + `StateFlow` (`collectAsStateWithLifecycle`) + `sealed interface UiState<out T>`
- **Dependency Injection:** Lightweight Manual DI (`AppContainer`)
- **Backend & Storage:** Firebase Authentication (Email/Password) & Cloud Firestore (real-time snapshots + `PersistentCacheSettings` offline persistence)
- **Target Platform:** Android (`minSdk = 26`, `targetSdk = 35`) — diuji di Android Emulator

## Fitur Utama

1. **Autentikasi & Routing Role Otomatis:**
   - Pet Owner dapat melakukan registrasi mandiri (email & password; `role` terkunci sebagai `"owner"` di app & Firestore Rules).
   - Akun Admin ditentukan langsung melalui Firestore Console (`role: "admin"`).
2. **Manajemen Data Hewan (Pet Owner):**
   - Tambah, edit, dan hapus profil hewan peliharaan (nama, jenis spesies dengan avatar bawaan, ras, umur bulan, berat badan kg).
   - Validasi ketat dan pencegahan hapus hewan bila masih memiliki janji temu aktif (`PENDING`/`CONFIRMED`).
3. **Booking Klinik Real-time & Anti Double-Booking (Pet Owner):**
   - Pilih hewan, layanan (Vaksinasi, Periksa Umum, Konsultasi, Sterilisasi), tanggal (otomatis menolak tanggal sebelum hari ini), dan slot jam klinik (`09:00`–`16:00`).
   - Penguncian transaksi atomik pada koleksi `booked_slots` agar slot yang sudah diambil tidak dapat dipesan bersamaan.
   - Pembatalan mandiri untuk booking yang masih berstatus `PENDING` (mengembalikan slot jam menjadi tersedia).
4. **Manajemen Antrean & Pasien Aktif (Clinic Admin):**
   - Filter antrean masuk berdasarkan tanggal, setujui (`CONFIRMED`), atau tolak (`CANCELLED` dengan alasan wajib yang langsung mengosongkan kembali slot).
5. **Rekam Medis & Buku Vaksin Digital (Clinic Admin -> Pet Owner):**
   - Dokter/Admin mengisi diagnosa, tindakan, nama vaksin (wajib untuk layanan Vaksinasi), catatan resep, dan jadwal kontrol lanjutan.
   - Transaksi batch atomik menandai janji temu `COMPLETED` dan langsung menampilkan catatan medis di aplikasi Owner.
   - Pencarian & filter arsip riwayat pasien berdasarkan nama hewan atau email pemilik.

## Struktur Folder

```text
app/src/main/java/com/pawcare/app/
├── data/
│   ├── model/         # Data class & enum (User, Pet, Appointment, BookedSlot, MedicalRecord)
│   └── repository/    # Interface, implementasi Firebase, dan Fake repositories (untuk unit testing)
├── di/                # AppContainer dependency manual
├── ui/
│   ├── components/    # Komponen Compose reusable (PetAvatar, StatusBadge, Dialogs, EmptyState)
│   ├── navigation/    # PawCareNavHost & Screen route definitions
│   ├── screens/
│   │   ├── auth/      # SplashScreen, LoginScreen, RegisterScreen
│   │   ├── owner/     # OwnerMainScreen, PetsScreen, AddEditPetScreen, OwnerBookingsScreen, CreateBookingScreen, OwnerHistoryScreen, MedicalRecordDetailScreen
│   │   └── admin/     # AdminMainScreen, AdminQueueScreen, AdminActivePatientsScreen, InputMedicalRecordScreen, AdminArchiveScreen
│   └── theme/         # Material 3 color scheme, typography, theme
├── util/              # Validators (pure Kotlin) & UiState
├── viewmodel/         # AuthViewModel, OwnerViewModel, AdminViewModel
└── MainActivity.kt
```

## Dokumentasi & Proposal

- Proposal Proyek Minggu 1: [`docs/PROPOSAL.md`](docs/PROPOSAL.md)
- Dokumen Spesifikasi Desain: [`docs/superpowers/specs/2026-11-11-pawcare-design.md`](docs/superpowers/specs/2026-11-11-pawcare-design.md)
- Aturan Keamanan Database: [`firestore.rules`](firestore.rules)

## Cara Menjalankan Project (Setup Firebase)

> **Keamanan Rahasia:** `app/google-services.json` sengaja dimasukkan ke `.gitignore` agar tidak ada secret/API key yang bocor ke repositori Git.

1. Clone repository ini lalu buka di **Android Studio** (Ladybug / terbaru).
2. Buat project baru di [Firebase Console](https://console.firebase.google.com).
3. Aktifkan **Authentication** -> Sign-in method **Email/Password**.
4. Aktifkan **Cloud Firestore** (pilih lokasi terdekat, mulai dalam Production Mode).
5. Salin isi [`firestore.rules`](firestore.rules) ke menu **Firestore Database -> Rules** lalu klik **Publish**.
6. Daftarkan aplikasi Android pada Firebase Project Settings dengan Package Name: `com.pawcare.app`.
7. Unduh `google-services.json` dan simpan ke dalam folder `app/google-services.json` (gunakan `app/google-services.json.example` sebagai referensi struktur).
8. **Membuat Akun Admin Klinik:**
   - Daftar melalui halaman Register di aplikasi atau tambah user di Firebase Console Authentication.
   - Buka **Firestore Database**, temukan dokumen user tersebut di koleksi `users/{uid}`, lalu ubah field `role` dari `"owner"` menjadi `"admin"`.
9. Sync Gradle lalu jalankan pada **Android Emulator** (`API 26+` / direkomendasikan `API 34` atau `API 35`).

## Menjalankan Unit Test & Build APK

### 1. Jalankan Seluruh Unit Test (JVM — Tidak Membutuhkan `google-services.json`)
```bash
./gradlew test
```
Suite pengujian mencakup:
- `ValidatorsTest` — Validasi email, password, data hewan, tanggal & jam slot booking, alasan penolakan, rekam medis & vaksin.
- `FakeRepositoriesTest` — Integrasi repository palsu (role lock, double-booking lock, pembatalan yang mengosongkan slot, proteksi hapus hewan aktif, penyelesaian rekam medis atomik).
- `NavigationRouteTest` — Verifikasi kontrak rute navigasi Compose.
- `AuthViewModelTest` — Alur login, registrasi, pemetaan role, dan error handling.
- `OwnerViewModelTest` — Manajemen pet, slot booking terblokir/lampau, validasi & pembatalan booking.
- `AdminViewModelTest` — Persetujuan, penolakan dengan alasan wajib, validasi vaksin rekam medis, dan filter pencarian arsip.

### 2. Build Debug APK (Membutuhkan Android SDK & `google-services.json`)
```bash
./gradlew assembleDebug
```
Hasil APK akan tersedia di `app/build/outputs/apk/debug/app-debug.apk`.

## Screenshots Aplikasi

> Tambahkan 3–5 screenshot aplikasi dari Android Emulator ke folder `docs/screenshots/` sebelum pengumpulan akhir:

| Login & Register | Daftar Hewan (Owner) | Buat Booking (Slot Lock) |
|---|---|---|
| `![Login](docs/screenshots/01_login.png)` | `![Pets](docs/screenshots/02_pets.png)` | `![Booking](docs/screenshots/03_booking.png)` |

| Antrean Masuk (Admin) | Rekam Medis & Buku Vaksin |
|---|---|
| `![Admin Queue](docs/screenshots/04_admin_queue.png)` | `![Medical Record](docs/screenshots/05_medical_record.png)` |
