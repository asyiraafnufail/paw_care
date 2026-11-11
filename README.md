# PawCare — Manajemen Klinik Kesehatan Hewan

Aplikasi Android untuk manajemen kunjungan klinik kesehatan hewan (vaksinasi, periksa umum, konsultasi, dan sterilisasi) dengan dua role terpisah: **Pet Owner** dan **Clinic Admin**.

## Tech Stack & State Management

- **Language & UI:** Kotlin, Jetpack Compose, Material Design 3
- **Architecture:** MVVM + Single-Activity + Compose Navigation
- **State Management:** `ViewModel` + `StateFlow` (`collectAsStateWithLifecycle`) + `sealed interface UiState<out T>`
- **Backend & Storage:** Firebase Authentication (Email/Password) & Cloud Firestore (real-time + offline persistence)

## Fitur Utama

1. **Autentikasi & Routing Role:** Registrasi mandiri untuk Pet Owner; role Admin ditentukan di Firestore.
2. **Manajemen Data Pet (Owner):** Tambah, ubah, dan hapus hewan peliharaan dengan avatar spesies bawaan.
3. **Booking Klinik Real-time (Owner):** Pilih hewan, layanan, tanggal (>= hari ini), dan slot jam tersedia (anti double-booking).
4. **Manajemen Antrean (Admin):** Filter antrean masuk, setujui atau tolak dengan alasan wajib.
5. **Rekam Medis & Buku Vaksin Digital (Admin -> Owner):** Input diagnosa, tindakan, nama vaksin, dan jadwal kontrol yang langsung sinkron ke riwayat hewan milik Owner.

## Struktur Folder

```text
app/src/main/java/com/pawcare/app/
├── data/
│   ├── model/         # Data class & enum (User, Pet, Appointment, MedicalRecord)
│   └── repository/    # Abstraksi dan implementasi Firebase Auth & Firestore
├── di/                # AppContainer dependency manual
├── ui/
│   ├── components/    # Komponen Compose reusable (PetAvatar, StatusBadge, Dialogs)
│   ├── navigation/    # NavHost & definisi route
│   ├── screens/       # Layar Auth, Pet Owner (3 tab), dan Admin (3 tab)
│   └── theme/         # Material 3 color scheme & typography
├── util/              # Validators & UiState
├── viewmodel/         # ViewModel tiap alur fitur
└── MainActivity.kt
```

## Cara Menjalankan Project

1. Clone repository ini lalu buka di **Android Studio**.
2. Buat project di [Firebase Console](https://console.firebase.google.com), aktifkan **Authentication (Email/Password)** dan **Cloud Firestore**.
3. Daftarkan Android App dengan package name `com.pawcare.app`.
4. Download `google-services.json` dari Firebase Console dan taruh di `app/google-services.json` (lihat format contoh pada `app/google-services.json.example`).
5. Deploy aturan keamanan di `firestore.rules` ke Firestore Rules.
6. Untuk membuat akun Admin: daftar lewat aplikasi atau buat user di Firebase Authentication, lalu ubah field `role` pada dokumen `users/{uid}` menjadi `"admin"` di Firestore Console.
7. Jalankan di **Android Emulator** atau device Android (`minSdk 26`).

## Menjalankan Unit Test

```bash
./gradlew test
```

## Screenshots

_(Tambahkan 3–5 screenshot aplikasi setelah verifikasi di emulator sebelum pengumpulan akhir)_
