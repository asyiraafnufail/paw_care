# PawCare — Veterinary Clinic Management Application

Aplikasi Native Android berbasis **Kotlin 2.0 + Jetpack Compose + Material Design 3 + Firebase Authentication & Cloud Firestore** untuk manajemen kunjungan klinik kesehatan hewan (vaksinasi, periksa umum, konsultasi, dan sterilisasi) dengan pemisahan peran **Pet Owner** dan **Clinic Admin**.

> **Pusat Dokumentasi & Setup Lengkap:** Seluruh panduan setup Android Studio, konfigurasi Firebase, cara build & install APK (Emulator/ADB/HP fisik), user manual kedua peran, dan troubleshooting tersedia terpusat di **[`docs/SETUP_GUIDE.md`](docs/SETUP_GUIDE.md)**.

---

## Ringkasan Tech Stack

- **Bahasa & UI:** Kotlin `2.0.21`, Jetpack Compose BOM `2024.10.01`, Material Design 3
- **Arsitektur:** Clean MVVM + Single-Activity (`MainActivity`) + Navigation Compose `2.8.4`
- **State & DI:** Reactive `StateFlow` + `sealed interface UiState<T>` + Manual `AppContainer`
- **Backend & Offline Cache:** Firebase Auth & Cloud Firestore (`33.6.0`, `PersistentCacheSettings`)
- **Platform:** Android (`minSdk = 26`, `targetSdk = 35`, `compileSdk = 35`, `Java 17`)

---

## Fitur Utama

1. **Autentikasi & Routing Peran Otomatis (`Role Gate`):**
   - **Pet Owner:** Registrasi mandiri (role dikunci `"owner"` di app & `firestore.rules`).
   - **Clinic Admin:** Akun dipromosikan langsung di Cloud Firestore Console (`role: "admin"`).
2. **Manajemen Profil Hewan (Pet Owner):**
   - Tambah, edit, dan hapus profil anabul (avatar spesies bawaan, ras, umur bulan, berat kg).
   - Proteksi hapus hewan jika masih memiliki janji temu aktif (`PENDING`/`CONFIRMED`).
3. **Booking Klinik Real-time & Anti Double-Booking:**
   - Validasi slot jam klinik (`09:00`–`16:00`) dan blokir tanggal/jam lampau.
   - Transaksi atomik pada koleksi `booked_slots/{YYYY-MM-DD}_{HH:mm}` mencegah dua pengguna memesan jam yang sama; pembatalan otomatis mengosongkan kembali slot.
4. **Antrean & Pemeriksaan Pasien (Clinic Admin):**
   - Filter antrean per tanggal, setujui (`CONFIRMED`), atau tolak (`CANCELLED`) dengan alasan wajib.
   - Input rekam medis digital & merk vaksin (wajib untuk layanan Vaksinasi) yang secara atomik menandai janji temu `COMPLETED`.
5. **UX & Stabilitas:**
   - Live search (`PawSearchBar`), animasi shimmer skeleton (`SkeletonCardList`), tab backstack (`BackHandler`), layout responsif maks `600.dp` (`ResponsiveScreenBox`), dan penyesuaian keyboard (`imePadding`).

---

## Pusat Dokumentasi & Panduan Lengkap

Gunakan indeks terpusat berikut untuk membuka panduan sesuai kebutuhan:

| Kategori Panduan | Tautan Langsung ke Pusat Dokumentasi |
|---|---|
| **1. Spesifikasi & Prasyarat Sistem** | [Spesifikasi & Persyaratan](docs/SETUP_GUIDE.md#1-spesifikasi-teknis--persyaratan-sistem) |
| **2. Setup Android Studio & JDK 17** | [Setup Android Studio, JDK 17, `local.properties`, Sync](docs/SETUP_GUIDE.md#2-panduan-setup-android-studio--jdk-17) |
| **3. Konfigurasi Firebase & Rules** | [Setup Firebase Auth, Firestore, `google-services.json`, Akun Admin](docs/SETUP_GUIDE.md#3-panduan-konfigurasi-firebase-authentication--cloud-firestore) |
| **4. Setup & Run Android Emulator** | [Konfigurasi AVD Pixel & Jalankan App](docs/SETUP_GUIDE.md#4-panduan-menyiapkan--menjalankan-android-emulator) |
| **5. Kompilasi & Build APK** | [Build Debug & Release APK (CLI & GUI)](docs/SETUP_GUIDE.md#5-panduan-kompilasi--build-apk-debug--release) |
| **6. Instalasi APK (3 Metode)** | [Drag-and-Drop Emulator, ADB CLI, & HP Fisik](docs/SETUP_GUIDE.md#6-panduan-instalasi-apk-ke-emulator--smartphone-fisik-3-metode) |
| **7. Automated Unit Tests** | [Jalankan 7 Test Suites (100% JVM) & Laporan HTML](docs/SETUP_GUIDE.md#7-panduan-menjalankan-automated-unit-tests-100-jvm) |
| **8. User Manual (Owner & Admin)** | [Alur Pengoperasian Pet Owner & Clinic Admin](docs/SETUP_GUIDE.md#8-panduan-pengoperasian-aplikasi-user-manual-pet-owner--clinic-admin) |
| **9. Skema Database & Security** | [Struktur Koleksi Firestore & Anti Composite-Index Crash](docs/SETUP_GUIDE.md#9-arsitektur-data--aturan-keamanan-database-firestorerules) |
| **10. Troubleshooting FAQ** | [Solusi Error Umum (JDK, XML v4, Permission Denied, Emulator)](docs/SETUP_GUIDE.md#10-matriks-troubleshooting--solusi-error-lengkap) |

### Dokumen Pendukung Lainnya
- **Proposal Proyek (Minggu 1):** [`docs/PROPOSAL.md`](docs/PROPOSAL.md)
- **Spesifikasi Desain Sistem:** [`docs/superpowers/specs/2026-11-11-pawcare-design.md`](docs/superpowers/specs/2026-11-11-pawcare-design.md)
- **Rencana Implementasi TDD:** [`docs/superpowers/plans/2026-11-11-pawcare.md`](docs/superpowers/plans/2026-11-11-pawcare.md)
- **Aturan Keamanan Cloud Firestore:** [`firestore.rules`](firestore.rules)
- **Contoh Konfigurasi Firebase:** [`app/google-services.json.example`](app/google-services.json.example)

---

## Quick Start (Terminal)

```bash
# 1. Jalankan seluruh unit test JVM (tidak butuh emulator atau google-services.json)
./gradlew testDebugUnitTest --continue

# 2. Build Debug APK (membutuhkan app/google-services.json dari Firebase Console)
./gradlew assembleDebug

# 3. Install APK ke emulator atau perangkat Android yang terhubung via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Struktur Folder Proyek

```text
app/src/
├── main/java/com/pawcare/app/
│   ├── data/
│   │   ├── model/         # Appointment, BookedSlot, Enums, MedicalRecord, Pet, User
│   │   └── repository/    # Interface, implementasi Firebase, & Fake repositories
│   ├── di/                # AppContainer (manual dependency injection)
│   ├── ui/
│   │   ├── components/    # CommonComponents (PawTextField, PawSearchBar, ResponsiveScreenBox, SkeletonCardList) & PetAvatar
│   │   ├── navigation/    # PawCareNavHost, Screen routes, OwnerTab & AdminTab
│   │   ├── screens/
│   │   │   ├── auth/      # SplashScreen, LoginScreen, RegisterScreen
│   │   │   ├── owner/     # OwnerMainScreen, PetsScreen, AddEditPetScreen, OwnerBookingsScreen, CreateBookingScreen, OwnerHistoryScreen, MedicalRecordDetailScreen
│   │   │   └── admin/     # AdminMainScreen, AdminQueueScreen, AdminActivePatientsScreen, InputMedicalRecordScreen, AdminArchiveScreen
│   │   └── theme/         # Color.kt, Theme.kt, Type.kt (Material 3)
│   ├── util/              # Validators (pure Kotlin) & UiState
│   ├── viewmodel/         # AuthViewModel, OwnerViewModel, AdminViewModel, MedicalRecordDetailViewModel
│   └── MainActivity.kt
└── test/java/com/pawcare/app/
    ├── data/repository/FakeRepositoriesTest.kt
    ├── ui/navigation/NavigationRouteTest.kt
    ├── util/ValidatorsTest.kt
    └── viewmodel/         # AuthViewModelTest, OwnerViewModelTest, AdminViewModelTest, MedicalRecordDetailViewModelTest
```

---

## Screenshots Aplikasi

> Tambahkan screenshot dari Android Emulator ke folder `docs/screenshots/` sebelum pengumpulan akhir:

| Login & Register | Daftar Hewan (Owner) | Buat Booking (Slot Lock) |
|:---:|:---:|:---:|
| `docs/screenshots/01_login.png` | `docs/screenshots/02_pets.png` | `docs/screenshots/03_booking.png` |

| Antrean Masuk (Admin) | Input Rekam Medis | Buku Medis Digital (Owner) |
|:---:|:---:|:---:|
| `docs/screenshots/04_admin_queue.png` | `docs/screenshots/05_medical_record.png` | `docs/screenshots/06_medical_history.png` |
