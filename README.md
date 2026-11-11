# PawCare — Veterinary Clinic Management Application

Aplikasi Native Android berbasis **Kotlin 2.0 + Jetpack Compose + Material Design 3 + Firebase Authentication & Cloud Firestore** untuk sistem manajemen klinik kesehatan hewan terintegrasi (vaksinasi, pemeriksaan umum, konsultasi, dan sterilisasi).

Aplikasi dirancang dengan arsitektur **Clean MVVM (Model-View-ViewModel)** dengan pemisahan peran yang ketat antara dua pengguna: **Pet Owner** (Pemilik Hewan) dan **Clinic Admin** (Staf/Dokter Hewan), dilengkapi sistem booking *real-time* dengan transaksi atomik pencegah *double-booking* (*slot lock*), rekam medis digital, pencarian instan, dan penanganan *offline persistence*.

Proyek ini dibangun untuk memenuhi seluruh ketentuan penilaian mata kuliah Mobile Programming berdasarkan **FINAL PROJECT GUIDELINES 2026** (UI/UX 25%, Functionality 25%, Code Quality 20%, Database Integration 15%, Delivery Discipline 15%).

---

## Daftar Isi

1. [Tech Stack & Spesifikasi Sistem](#tech-stack--spesifikasi-sistem)
2. [Fitur Utama & Alur Peran Pengguna](#fitur-utama--alur-peran-pengguna)
3. [Struktur Folder & Arsitektur](#struktur-folder--arsitektur)
4. [Prasyarat Lingkungan Pengembangan](#prasyarat-lingkungan-pengembangan)
5. [Panduan Setup di Android Studio](#panduan-setup-di-android-studio)
6. [Panduan Konfigurasi Firebase & Firestore](#panduan-konfigurasi-firebase--firestore)
7. [Panduan Konfigurasi Android Emulator](#panduan-konfigurasi-android-emulator)
8. [Panduan Build APK (Debug & Release)](#panduan-build-apk-debug--release)
9. [Panduan Instalasi APK ke Emulator & Smartphone Fisik](#panduan-instalasi-apk-ke-emulator--smartphone-fisik)
10. [Panduan Menjalankan Automated Unit Tests](#panduan-menjalankan-automated-unit-tests)
11. [Troubleshooting & Solusi Kendala Umum](#troubleshooting--solusi-kendala-umum)
12. [Dokumentasi Tambahan & Proposal](#dokumentasi-tambahan--proposal)

---

## Tech Stack & Spesifikasi Sistem

| Komponen | Spesifikasi / Library | Keterangan |
|---|---|---|
| **Bahasa Pemrograman** | Kotlin `2.0.21` | Modern language features, Kotlin Compose Compiler bawaan |
| **User Interface** | Jetpack Compose BOM `2024.10.01` + Material Design 3 | Declarative UI, Responsive Layout, Dynamic Shimmer Skeleton |
| **Arsitektur** | Clean MVVM + Single-Activity | Unidirectional Data Flow, `StateFlow`, `collectAsStateWithLifecycle` |
| **State Handling** | Reactive `StateFlow` + `sealed interface UiState<T>` | Pengelolaan `Loading`, `Success`, dan `Error` terstandarisasi |
| **Navigasi** | Jetpack Navigation Compose `2.8.4` | Single NavHost, BackHandler tab history backstack |
| **Dependency Injection** | Manual AppContainer (`AppContainer.kt`) | Ringan, cepat, tanpa boilerplate annotation processor |
| **Backend & Autentikasi** | Firebase Auth (Email/Password) | Registrasi mandiri role-locked, sesi persisten |
| **Database Cloud** | Cloud Firestore (`33.6.0`) | Transaksi atomik slot lock, real-time snapshot listeners |
| **Offline Persistence** | Firestore `PersistentCacheSettings` | Data tersimpan di disk lokal saat perangkat luring |
| **Target SDK** | `minSdk = 26` (Android 8.0 Oreo), `targetSdk = 35`, `compileSdk = 35` (Android 15) | Mendukung 95%+ perangkat Android aktif |
| **Java / JVM Target** | Java 17 (`sourceCompatibility = VERSION_17`, `jvmTarget = JVM_17`) | Standar Android Gradle Plugin 8.7.3 |

---

## Fitur Utama & Alur Peran Pengguna

### 1. Autentikasi & Routing Berbasis Peran (*Role Gate*)
- **Pet Owner Self-Registration:** Pendaftaran akun mandiri melalui aplikasi (email & password valid min 6 karakter). Field `role` otomatis dikunci sebagai `"owner"` di aplikasi dan divalidasi oleh `firestore.rules`.
- **Clinic Admin Protection:** Akun admin tidak dapat dibuat sembarangan melalui formulir registrasi publik. Otorisasi admin diberikan langsung pada Cloud Firestore Console dengan mengubah field `role: "admin"`.
- **Splash Screen Otomatis:** Saat membuka aplikasi, `SplashScreen` memeriksa token autentikasi aktif dan membaca profil Firestore untuk secara instan mengarahkan user ke dashboard Pet Owner atau Clinic Admin tanpa login berulang.

### 2. Alur Pengguna Pet Owner (Pemilik Hewan)
- **Manajemen Profil Hewan (Pet CRUD):**
  - Menambah, mengedit, dan menghapus profil anabul (nama, ras, jenis spesies dengan ikon/avatar visual, umur bulan, dan berat badan kg).
  - *Data Integrity Guard:* Hewan peliharaan yang masih memiliki janji temu aktif (`PENDING` atau `CONFIRMED`) dilarang dihapus untuk menjaga keutuhan riwayat medis.
- **Reservasi Janji Temu Real-time (Atomic Slot Lock):**
  - Memilih anabul, jenis layanan (Vaksinasi, Pemeriksaan Umum, Konsultasi, Sterilisasi), tanggal kunjungan (otomatis memblokir tanggal lampau), dan slot jam klinik (`09:00` s.d. `16:00`).
  - *Anti Double-Booking:* Transaksi atomik pada koleksi `booked_slots/{YYYY-MM-DD}_{HH:mm}` menjamin slot yang sudah dipilih tidak bisa direbut oleh pemilik hewan lain di detik yang sama.
- **Manajemen Booking Aktif & Pembatalan:**
  - Memantau status janji temu (`PENDING` menunggu konfirmasi admin, `CONFIRMED` siap datang).
  - Pemilik hewan dapat membatalkan booking mandiri selama status masih `PENDING`; transaksi pembatalan secara otomatis menghapus kunci slot sehingga slot jam langsung tersedia kembali untuk pemilik hewan lain.
- **Buku Rekam Medis & Riwayat Kunjungan Digital:**
  - Tab **Buku Medis:** Menampilkan kartu riwayat rekam medis, diagnosa dokter, tindakan terapi, merk vaksin, dan jadwal kontrol berikutnya.
  - Tab **Riwayat Janji:** Arsip lengkap seluruh janji temu selesai maupun dibatalkan (disertai alasan pembatalan).

### 3. Alur Pengguna Clinic Admin (Dokter / Staf Klinik)
- **Pusat Antrean Kunjungan Masuk (*Queue Management*):**
  - Memfilter permohonan booking masuk berdasarkan tanggal kunjungan.
  - Menyetujui janji temu (`CONFIRMED`).
  - Menolak janji temu (`CANCELLED`) dengan **alasan penolakan wajib** melalui dialog konfirmasi; penolakan otomatis menghapus kunci slot agar jam tersebut dapat dipesan ulang.
- **Pasien Aktif Hari Ini & Input Rekam Medis:**
  - Memantau daftar pasien yang telah tiba dan menunggu penanganan.
  - Formulir input rekam medis komprehensif: diagnosa, tindakan, nama vaksin (wajib diisi apabila layanan adalah Vaksinasi), catatan resep dokter, dan tanggal kontrol lanjutan.
  - Transaksi *batch write* atomik: menyimpan dokumen rekam medis ke `medical_records` dan mengubah status janji temu menjadi `COMPLETED` dalam satu kesatuan operasi.
- **Pencarian & Arsip Riwayat Pasien:**
  - Filter dan cari arsip riwayat berdasarkan nama hewan atau email pemilik hewan.

### 4. Detail UX & Polish
- **Pencarian Instan:** Fitur `PawSearchBar` terintegrasi pada layar hewan, antrean booking, riwayat rekam medis, dan arsip admin.
- **Skeleton Shimmer Loading:** Animasi placeholder saat data awal diambil dari Firestore untuk pengalaman visual yang mulus.
- **Tab Backstack:** Integrasi `BackHandler` memastikan tombol Back perangkat kembali ke tab sebelumnya yang dibuka sebelum menutup aplikasi.
- **Desain Responsif:** Komponen `ResponsiveScreenBox` membatasi lebar maksimum konten (`600.dp`) pada layar tablet dan orientasi landscape agar tampilan tetap proporsional dan tidak meregang.
- **Keyboard IME Padding:** Menjamin input form tetap terlihat saat keyboard virtual muncul (`imePadding` & `windowSoftInputMode="adjustResize"`).

---

## Struktur Folder & Arsitektur

```text
paw_care/
├── app/
│   ├── build.gradle.kts           # Konfigurasi dependensi modul Android, SDK 35, JVM 17
│   ├── google-services.json       # File konfigurasi Firebase (gitignored demi keamanan)
│   ├── google-services.json.example # Contoh struktur file konfigurasi Firebase
│   ├── proguard-rules.pro         # Aturan obfuscation & keep rules model Firestore
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml # Entry point, permission INTERNET, adjustResize
│       │   ├── java/com/pawcare/app/
│       │   │   ├── MainActivity.kt # Single Activity dengan Edge-to-Edge & PawCareNavHost
│       │   │   ├── data/
│       │   │   │   ├── model/      # Data class (Appointment, BookedSlot, Enums, MedicalRecord, Pet, User)
│       │   │   │   └── repository/ # Repository Interfaces, Firebase Repos, & Fake Repositories
│       │   │   ├── di/
│       │   │   │   └── AppContainer.kt # Dependency Injection Container manual
│       │   │   ├── ui/
│       │   │   │   ├── components/ # Komponen UI reusable (CommonComponents.kt, PetAvatar.kt)
│       │   │   │   ├── navigation/ # NavHost, Screen routes, & OwnerTab/AdminTab
│       │   │   │   ├── screens/
│       │   │   │   │   ├── auth/   # SplashScreen, LoginScreen, RegisterScreen
│       │   │   │   │   ├── owner/  # OwnerMainScreen, PetsScreen, AddEditPetScreen, OwnerBookingsScreen,
│       │   │   │   │   │           # CreateBookingScreen, OwnerHistoryScreen, MedicalRecordDetailScreen
│       │   │   │   │   └── admin/  # AdminMainScreen, AdminQueueScreen, AdminActivePatientsScreen,
│       │   │   │   │               # InputMedicalRecordScreen, AdminArchiveScreen
│       │   │   │   └── theme/      # Color.kt (Forest Green), Theme.kt, Type.kt
│       │   │   ├── util/
│       │   │   │   ├── UiState.kt  # Sealed interface state (Loading, Success, Error)
│       │   │   │   └── Validators.kt # Validasi pure-Kotlin bebas dependensi framework
│       │   │   └── viewmodel/      # AuthViewModel, OwnerViewModel, AdminViewModel, MedicalRecordDetailViewModel
│       │   └── res/                # XML Resources, Drawables, App Icon, Themes
│       └── test/java/com/pawcare/app/ # 7 Unit Test Suites (100% JVM-based)
│           ├── data/repository/FakeRepositoriesTest.kt
│           ├── ui/navigation/NavigationRouteTest.kt
│           ├── util/ValidatorsTest.kt
│           └── viewmodel/
│               ├── AdminViewModelTest.kt
│               ├── AuthViewModelTest.kt
│               ├── MedicalRecordDetailViewModelTest.kt
│               └── OwnerViewModelTest.kt
├── docs/
│   ├── PROPOSAL.md                # Proposal Proyek Akhir Minggu 1
│   └── superpowers/               # Dokumen spesifikasi dan rencana implementasi
├── firestore.rules                # Aturan keamanan Cloud Firestore berbasis peran
├── gradle/libs.versions.toml      # Version Catalog dependensi Gradle
├── build.gradle.kts               # Root build configuration
├── settings.gradle.kts            # Module include & repository configuration
└── README.md                      # Dokumentasi utama proyek
```

---

## Prasyarat Lingkungan Pengembangan

Sebelum menjalankan atau melakukan build proyek ini, pastikan komputer Anda telah terpasang:

1. **Android Studio:**
   - Versi **Android Studio Ladybug (2024.2.1)** atau **Android Studio Meerkat (2024.3+)** / versi yang lebih baru.
   - Tersedia untuk macOS (Apple Silicon / Intel), Windows 10/11 64-bit, dan Linux.
2. **Java Development Kit (JDK):**
   - **JDK 17 (Java 17)** — wajib.
   - Disarankan menggunakan **Embedded JDK 17 (JetBrains Runtime / `jbr-17`)** yang sudah disertakan langsung di dalam instalasi Android Studio.
3. **Android SDK Platform:**
   - Android SDK Platform **API 35 (VanillaIceCream)**.
   - Android SDK Build-Tools **35.0.0**.
   - Android SDK Platform-Tools terbaru.
4. **Perangkat Pengujian (Pilih salah satu):**
   - **Android Emulator:** Pixel 7 atau Pixel 8 dengan System Image **API 34 (UpsideDownCake)** atau **API 35 (VanillaIceCream)** (Google APIs / Google Play x86_64).
   - **Smartphone Android Fisik:** Perangkat dengan Android 8.0 Oreo (API 26) ke atas yang telah mengaktifkan **Developer Options** & **USB Debugging**.
5. **Akun Google Firebase (Gratis):**
   - Untuk mengaktifkan Firebase Authentication & Cloud Firestore.

---

## Panduan Setup di Android Studio

Ikuti langkah-langkah berikut untuk membuka dan menyinkronkan proyek di Android Studio:

### Langkah 1: Buka Proyek di Android Studio
1. Buka aplikasi **Android Studio**.
2. Pada layar selamat datang (*Welcome Screen*), pilih **Open** (atau jika sudah membuka proyek lain, pilih menu `File` -> `Open...`).
3. Cari dan pilih direktori root `paw_care`.
4. Klik **OK / Open**. Tunggu hingga Android Studio selesai membaca struktur direktori.

### Langkah 2: Konfigurasi Gradle JDK 17
Proyek ini mengompilasi bytecode dengan target JVM 17. Pastikan Android Studio diarahkan ke JDK 17:
1. Buka jendela pengaturan:
   - **macOS:** Klik menu `Android Studio` -> `Settings...` (atau `Preferences...`).
   - **Windows / Linux:** Klik menu `File` -> `Settings...`.
2. Pada panel navigasi sebelah kiri, buka:
   `Build, Execution, Deployment` -> `Build Tools` -> `Gradle`.
3. Pada bagian **Gradle Projects**, periksa dropdown **Gradle JDK**:
   - Pastikan terpilih versi **JDK 17** (misal: `Embedded JDK version 17.0.x`, `jbr-17`, atau `corretto-17`).
   - Jika belum ada, klik dropdown -> pilih **Download JDK...** -> pilih Version `17` (Vendor: Amazon Corretto atau Eclipse Temurin) -> klik **Download**.
4. Klik **Apply**, lalu klik **OK**.

### Langkah 3: Konfigurasi Android SDK (`local.properties`)
Android Studio secara otomatis mendeteksi lokasi Android SDK dan membuat file `local.properties`. Jika file ini belum terbentuk secara otomatis, buat file bernama `local.properties` di root direktori proyek dengan isi sesuai sistem operasi Anda:

- **macOS:**
  ```properties
  sdk.dir=/Users/<username_anda>/Library/Android/sdk
  ```
- **Windows:**
  ```properties
  sdk.dir=C\:\\Users\\<username_anda>\\AppData\\Local\\Android\\Sdk
  ```
- **Linux:**
  ```properties
  sdk.dir=/home/<username_anda>/Android/Sdk
  ```

### Langkah 4: Sinkronisasi Gradle (*Sync Project*)
1. Klik tombol **Sync Project with Gradle Files** (ikon gajah dengan panah biru di toolbar kanan atas, atau pilih menu `File` -> `Sync Project with Gradle Files`).
2. Tunggu proses *indexing* dan pengunduhan dependensi selesai.
3. Pastikan jendela *Build* di bagian bawah menampilkan status:
   `BUILD SUCCESSFUL in ...s`.

> **Catatan Tanpa Firebase:** Jika Anda belum memasang `google-services.json`, Gradle Sync akan tetap berhasil karena `app/build.gradle.kts` telah dilengkapi proteksi *fallback*. Seluruh unit test dapat langsung dijalankan tanpa Firebase. Untuk menjalankan aplikasi di emulator atau perangkat nyata, lanjutkan ke langkah konfigurasi Firebase di bawah ini.

---

## Panduan Konfigurasi Firebase & Firestore

PawCare memanfaatkan layanan Firebase Authentication dan Cloud Firestore. Ikuti panduan pembuatan proyek Firebase berikut:

### 1. Buat Proyek Firebase Baru
1. Kunjungi [Firebase Console](https://console.firebase.google.com/) dan masuk menggunakan akun Google Anda.
2. Klik tombol **Add project** (atau *Create a project*).
3. Masukkan nama proyek, misalnya: `PawCare Clinic`.
4. (Opsional) Google Analytics dapat dinonaktifkan atau diaktifkan sesuai preferensi, lalu klik **Create project**.
5. Tunggu proses inisialisasi hingga selesai, lalu klik **Continue**.

### 2. Daftarkan Aplikasi Android ke Proyek Firebase
1. Pada halaman beranda proyek Firebase, klik ikon **Android** untuk menambahkan aplikasi.
2. Masukkan data konfigurasi aplikasi:
   - **Android package name (Wajib sama):** `com.pawcare.app`
   - **App nickname:** `PawCare`
   - **Debug signing certificate SHA-1:** Kosongkan (tidak diwajibkan untuk login email & password).
3. Klik tombol **Register app**.

### 3. Unduh & Pasang `google-services.json`
1. Unduh file konfigurasi `google-services.json` yang disediakan pada Langkah 2 di Firebase Console.
2. Salin dan letakkan file tersebut ke dalam direktori modul `app/`:
   ```text
   paw_care/app/google-services.json
   ```
   > ⚠️ **Penting:** Pastikan file diletakkan di dalam folder `app/`, **bukan** di root direktori `paw_care/`!
   > 
   > 🔒 **Kebersihan Rahasia:** File `app/google-services.json` secara otomatis diabaikan oleh Git melalui `.gitignore` (`*google-services.json`). Jangan pernah menghapus aturan ini agar kredensial API key Firebase Anda tidak bocor ke repositori publik. Contoh struktur file dapat dilihat di `app/google-services.json.example`.

### 4. Aktifkan Firebase Authentication
1. Pada menu sebelah kiri Firebase Console, klik **Build** -> **Authentication**.
2. Klik tombol **Get started**.
3. Pada tab **Sign-in method**, pilih penyedia **Email/Password**.
4. Aktifkan toggle **Email/Password** (opsi *Email link / passwordless* biarkan nonaktif).
5. Klik **Save**.

### 5. Aktifkan Cloud Firestore Database
1. Pada menu sebelah kiri Firebase Console, klik **Build** -> **Firestore Database**.
2. Klik tombol **Create database**.
3. Pengaturan database:
   - **Database ID:** `(default)`
   - **Location:** Pilih lokasi data center terdekat (misalnya: `asia-southeast2` Jakarta atau `asia-southeast1` Singapura).
   - **Secure rules:** Pilih **Start in production mode** (aturan bawaan akan segera ditimpa pada langkah berikutnya).
4. Klik **Create** dan tunggu proses provisioning database selesai.

### 6. Pasang Aturan Keamanan Database (`firestore.rules`)
Aturan keamanan bawaan Firestore akan memblokir seluruh operasi tulis dan baca. Anda wajib memasang aturan keamanan PawCare:
1. Buka tab **Rules** pada menu Firestore Database di Firebase Console.
2. Hapus seluruh kode aturan bawaan yang ada.
3. Buka file [`firestore.rules`](firestore.rules) dari root repositori PawCare, salin seluruh kodenya, dan tempelkan ke editor Rules di Firebase Console:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function isAuthenticated() {
      return request.auth != null;
    }

    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }

    function getUserRole() {
      return get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role;
    }

    function isAdmin() {
      return isAuthenticated() && getUserRole() == "admin";
    }

    match /users/{userId} {
      allow read: if isOwner(userId) || isAdmin();
      allow create: if isOwner(userId) && request.resource.data.role == "owner";
      allow update: if isOwner(userId) && request.resource.data.role == resource.data.role;
      allow delete: if false;
    }

    match /pets/{petId} {
      allow read: if isAuthenticated() && (resource.data.ownerId == request.auth.uid || isAdmin());
      allow create: if isAuthenticated() && request.resource.data.ownerId == request.auth.uid;
      allow update: if isAuthenticated() && resource.data.ownerId == request.auth.uid && request.resource.data.ownerId == request.auth.uid;
      allow delete: if isAuthenticated() && resource.data.ownerId == request.auth.uid;
    }

    match /booked_slots/{slotKey} {
      allow read: if isAuthenticated();
      allow create: if isAuthenticated() && request.resource.data.ownerId == request.auth.uid;
      allow delete: if isAuthenticated() && (resource.data.ownerId == request.auth.uid || isAdmin());
      allow update: if false;
    }

    match /appointments/{appointmentId} {
      allow read: if isAuthenticated() && (resource.data.ownerId == request.auth.uid || isAdmin());
      allow create: if isAuthenticated() &&
        request.resource.data.ownerId == request.auth.uid &&
        request.resource.data.status == "PENDING";
      allow update: if (
        isAuthenticated() &&
        resource.data.ownerId == request.auth.uid &&
        resource.data.status == "PENDING" &&
        request.resource.data.status == "CANCELLED"
      ) || isAdmin();
      allow delete: if false;
    }

    match /medical_records/{recordId} {
      allow read: if isAuthenticated() && (resource.data.ownerId == request.auth.uid || isAdmin());
      allow write: if isAdmin();
    }
  }
}
```
4. Klik tombol **Publish**. Aturan ini aktif seketika di seluruh dunia.

### 7. Setup Akun Demo (Pet Owner & Clinic Admin)

#### A. Membuat Akun Pet Owner
1. Jalankan aplikasi PawCare.
2. Di halaman login, klik **"Belum punya akun? Daftar"**.
3. Masukkan email (misal: `owner@example.com`) dan password (min 6 karakter).
4. Klik **Daftar Sekarang**. Akun otomatis tersimpan dengan `role: "owner"` dan Anda langsung masuk ke dashboard Pet Owner.

#### B. Menyiapkan Akun Clinic Admin (Dokter / Staf)
Untuk membuat akun Admin Klinik, lakukan promosi peran di Firestore Console:
1. Daftarkan akun baru melalui halaman registrasi aplikasi PawCare (misalnya: `admin@pawcare.com` dengan password rahasia).
2. Buka [Firebase Console](https://console.firebase.google.com/) -> buka menu **Firestore Database** -> pilih tab **Data**.
3. Klik koleksi `users`. Cari dokumen yang memiliki field `email` bernilai `admin@pawcare.com`.
4. Klik dua kali pada nilai field `role` (yang awalnya bernilai `"owner"`), ubah menjadi `"admin"`.
5. Klik **Save**.
6. Kembali ke aplikasi PawCare: lakukan Logout lalu Login kembali menggunakan akun tersebut. Aplikasi akan otomatis mendeteksi role admin dan membuka dashboard **Clinic Admin**!

---

## Panduan Konfigurasi Android Emulator

Untuk menguji dan menjalankan aplikasi di Android Studio Emulator:

### 1. Buat Android Virtual Device (AVD)
1. Di Android Studio, buka menu `Tools` -> `Device Manager` (atau klik ikon ponsel pada panel kanan atas).
2. Klik tombol **Create Virtual Device** (ikon `+`).
3. Pada kategori **Phone**, pilih profil hardware yang direkomendasikan: **Pixel 7** atau **Pixel 8** (layar 1080 x 2400 piksel). Klik **Next**.
4. Pada tab **Recommended**, unduh dan pilih System Image:
   - **API 34 ("UpsideDownCake")** atau **API 35 ("VanillaIceCream")**.
   - Pilih image dengan ABI **x86_64** yang menyertakan **Google APIs** atau **Google Play**.
5. Klik **Next**.
6. Beri nama AVD, misalnya: `PawCare_Pixel8_API34`.
7. Pastikan orientasi awal terpilih **Portrait**.
8. Klik **Finish**.

### 2. Jalankan Aplikasi di Emulator
1. Pada jendela Device Manager, klik tombol **Play (▶)** di samping nama AVD untuk menyalakan emulator.
2. Tunggu hingga emulator selesai melakukan *booting* ke layar beranda Android.
3. Di toolbar utama Android Studio bagian atas:
   - Pastikan dropdown konfigurasi memilih `app`.
   - Pastikan target device memilih emulator yang sedang aktif.
4. Klik tombol hijau **Run 'app'** (atau tekan shortcut `Shift + F10`).
5. Android Studio akan mengompilasi proyek, menginstal APK ke emulator, dan meluncurkan `com.pawcare.app.MainActivity`.

---

## Panduan Build APK (Debug & Release)

Anda dapat membuat file instalasi APK mandiri (*standalone APK*) melalui dua cara: antarmuka grafis Android Studio atau perintah terminal Gradle Wrapper.

### 1. Build Debug APK via Terminal (Paling Direkomendasikan)
Buka terminal pada root direktori proyek `paw_care/`:

- **macOS / Linux:**
  ```bash
  ./gradlew assembleDebug
  ```
- **Windows (Command Prompt / PowerShell):**
  ```cmd
  gradlew.bat assembleDebug
  ```

Setelah kompilasi selesai, file APK siap pakai akan berada di lokasi:
```text
app/build/outputs/apk/debug/app-debug.apk
```
> **Keunggulan Debug APK:** `app-debug.apk` sudah ditandatangani secara otomatis (*auto-signed*) menggunakan kunci debug standar Android Studio, sehingga dapat langsung diinstal dan dijalankan di emulator maupun smartphone Android fisik tanpa memerlukan sertifikat rilis.

### 2. Build Debug APK via Menu GUI Android Studio
1. Buka menu bar Android Studio: `Build` -> `Build Bundle(s) / APK(s)` -> pilih **Build APK(s)**.
2. Tunggu proses kompilasi Gradle selesai di latar belakang.
3. Notifikasi pop-up akan muncul di pojok kanan bawah:
   *"APK(s) generated successfully for 1 module"*.
4. Klik tautan bertuliskan **locate** pada notifikasi tersebut untuk langsung membuka folder output file `app-debug.apk` di Finder (macOS) atau File Explorer (Windows).

### 3. Build Release APK (Produksi)
Untuk menghasilkan APK release yang teroptimasi dengan R8 code shrinking dan resource shrinking:

- **Terminal:**
  ```bash
  ./gradlew assembleRelease
  ```
  File output APK:
  ```text
  app/build/outputs/apk/release/app-release-unsigned.apk
  ```
- **Generate Signed Release APK (Siap Upload / Pasang):**
  1. Klik menu `Build` -> `Generate Signed Bundle / APK...`.
  2. Pilih opsi **APK** -> klik **Next**.
  3. Pilih atau buat Keystore baru (`.jks`), isi password keystore dan key alias.
  4. Pilih Build Variant: **release**.
  5. Centang tanda tangan **V1 (Jar Signature)** dan **V2 (Full APK Signature)** jika diminta.
  6. Klik **Finish**. APK hasil rilis siap didistribusikan.

---

## Panduan Instalasi APK ke Emulator & Smartphone Fisik

Tersedia tiga metode mudah untuk memasang file `app-debug.apk` ke perangkat pengujian:

### Metode 1: Drag-and-Drop ke Android Emulator (Paling Cepat & Praktis)
Metode ini adalah cara tercepat tanpa mengetikkan perintah apapun di terminal:
1. Pastikan Android Emulator di Android Studio sudah menyala dan berada pada layar utama.
2. Buka **Finder** (macOS) atau **File Explorer** (Windows).
3. Arahkan ke folder:
   `paw_care/app/build/outputs/apk/debug/`
4. Klik dan tahan (*drag*) file **`app-debug.apk`**, lalu lepaskan (*drop*) tepat di atas layar jendela emulator.
5. Emulator akan menampilkan status *"Installing APK..."* di bagian tengah layar selama 2–5 detik.
6. Setelah selesai, ikon aplikasi **PawCare** (berlogo paw hijau) akan langsung muncul di app drawer emulator. Klik ikon tersebut untuk membuka aplikasi!

---

### Metode 2: Instalasi Menggunakan Android Debug Bridge (ADB via Terminal)
Metode ini cocok untuk pengembang yang menyukai alur kerja berbasis Command Line Interface:
1. Buka terminal di komputer Anda.
2. Pastikan emulator menyala atau HP fisik terhubung via kabel USB.
3. Jalankan perintah untuk memeriksa apakah perangkat terdeteksi:
   ```bash
   adb devices
   ```
   *Contoh output jika emulator terdeteksi:*
   ```text
   List of devices attached
   emulator-5554    device
   ```
4. Pasang APK menggunakan flag `-r` (reinstall / perbarui data aplikasi yang ada):
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
5. Tunggu hingga terminal mencetak output konfirmasi:
   ```text
   Performing Streamed Install
   Success
   ```
6. (Opsional) Buka aplikasi langsung dari terminal:
   ```bash
   adb shell am start -n com.pawcare.app/.MainActivity
   ```

---

### Metode 3: Instalasi di Smartphone Android Fisik (HP Nyata)

#### Cara A: Instalasi Menggunakan Kabel USB & Komputer
1. **Aktifkan Opsi Pengembang (Developer Options):**
   - Buka aplikasi **Pengaturan (Settings)** di ponsel Android Anda.
   - Buka menu **Tentang Ponsel (About Phone)** -> cari item **Nomor Bentukan (Build Number)**.
   - Ketuk (tap) pada **Build Number** sebanyak 7 kali berturut-turut hingga muncul pesan *"Anda kini seorang pengembang!"* (*You are now a developer!*).
2. **Aktifkan USB Debugging:**
   - Kembali ke menu Pengaturan -> buka **Sistem (System)** -> pilih **Opsi Pengembang (Developer Options)**.
   - Gulir ke bawah, aktifkan toggle **USB Debugging (Debugging USB)** -> klik OK.
3. **Hubungkan ke Komputer:**
   - Sambungkan HP ke komputer menggunakan kabel data USB.
   - Saat pop-up otorisasi *"Allow USB debugging?"* muncul di layar HP, centang *"Always allow from this computer"* lalu pilih **Allow**.
4. **Instal APK:**
   - Di terminal komputer, jalankan:
     ```bash
     adb install -r app/build/outputs/apk/debug/app-debug.apk
     ```
   - Aplikasi PawCare akan langsung terpasang di HP fisik Anda.

#### Cara B: Transfer File Langsung ke Ponsel (Tanpa Komputer / ADB)
1. Kirimkan file `app-debug.apk` ke HP fisik Anda (melalui Google Drive, kabel USB MTP, email, WhatsApp Document, atau Telegram).
2. Di ponsel Android Anda, buka aplikasi **File Manager** (atau *Files by Google*).
3. Cari file `app-debug.apk` yang telah diunduh, lalu ketuk pada file tersebut.
4. Jika muncul peringatan keamanan *"For your security, your phone is not allowed to install unknown apps from this source"*:
   - Ketuk tombol **Settings / Pengaturan**.
   - Aktifkan izin toggle **"Allow from this source"** (Izinkan dari sumber ini).
   - Tekan tombol Back.
5. Ketuk **Install / Pasang**.
6. Tunggu hingga proses instalasi selesai, lalu ketuk **Open / Buka** untuk menjalankan PawCare!

---

## Panduan Menjalankan Automated Unit Tests

Seluruh logika bisnis, validasi form, transaksi atomik, role lock, dan alur ViewModel diuji menggunakan **Automated JVM Unit Tests**. Pengujian ini **100% mandiri** (menggunakan implementasi *Fake Repositories* berbasis Kotlin coroutines) sehingga dapat dijalankan tanpa emulator, tanpa perangkat fisik, dan tanpa koneksi internet.

### 1. Jalankan Seluruh Unit Test via Terminal
Jalankan perintah berikut pada terminal:

- **macOS / Linux:**
  ```bash
  ./gradlew testDebugUnitTest --continue
  ```
- **Windows:**
  ```cmd
  gradlew.bat testDebugUnitTest --continue
  ```

### 2. Jalankan Unit Test Melalui Android Studio GUI
1. Pada panel *Project* di sebelah kiri Android Studio, buka folder:
   `app` -> `src` -> `test` -> `java` -> `com.pawcare.app`.
2. Klik kanan pada folder `com.pawcare.app` atau sub-folder yang diinginkan.
3. Pilih menu **Run 'Tests in 'pawcare.app''** (ikon dua panah hijau).
4. Hasil pengujian akan ditampilkan pada panel *Run* di bagian bawah secara terperinci.

### 3. Ringkasan 7 Unit Test Suites yang Diuji

| Test Suite File | Modul / Layer | Cakupan Pengujian |
|---|---|---|
| `ValidatorsTest.kt` | `util/` | Validasi email regex, batas minimal karakter password, validasi lengkap form anabul (nama, ras, batas umur bulan, batas berat badan kg), pemblokiran slot lampau & tanggal hari ini, validasi alasan penolakan booking, serta kewajiban pengisian vaksin pada layanan vaksinasi. |
| `FakeRepositoriesTest.kt` | `data/repository/` | Uji integrasi data palsu: pencegahan *double-booking* melalui penguncian slot atomik `booked_slots`, pembebasan slot saat booking dibatalkan/ditolak, proteksi larangan hapus pet yang memiliki janji aktif, dan batch atomik penulisan rekam medis. |
| `NavigationRouteTest.kt` | `ui/navigation/` | Verifikasi integritas rute navigasi Compose (`Screen.kt`), parsing argumen dinamis (`appointmentId`, `petId`), dan validasi string rute untuk tab Owner dan Admin. |
| `AuthViewModelTest.kt` | `viewmodel/` | Alur registrasi mandiri role `"owner"`, alur login dengan penanganan error, validasi form kredensial, pemetaan otomatis peran `"admin"` dan `"owner"`, serta fungsi logout. |
| `OwnerViewModelTest.kt` | `viewmodel/` | Manajemen data hewan peliharaan, pencarian live pet, pembuatan reservasi dengan slot lock, penolakan slot lampau, pembatalan booking mandiri, dan integrasi backstack tab navigasi. |
| `AdminViewModelTest.kt` | `viewmodel/` | Peninjauan antrean masuk, filter per tanggal kunjungan, persetujuan janji temu, penolakan booking dengan alasan wajib, input rekam medis atomik, validasi vaksin wajib, dan filter pencarian arsip pasien. |
| `MedicalRecordDetailViewModelTest.kt` | `viewmodel/` | Pengambilan data rekam medis terperinci berdasarkan `appointmentId`, pemetaan diagnosa, dan penanganan state error/sukses. |

### 4. Melihat Laporan HTML Hasil Pengujian
Setelah perintah test dijalankan, Gradle secara otomatis menghasilkan laporan HTML visual yang sangat rapi. Buka file berikut di browser Anda:
```text
paw_care/app/build/reports/tests/testDebugUnitTest/index.html
```

---

## Troubleshooting & Solusi Kendala Umum

### 1. Error: `compileJava failed` / `Unsupported class file major version` / JDK Version Mismatch
- **Penyebab:** Android Studio atau Gradle mengarah ke versi Java lama (JDK 8 / 11) atau JDK 21 yang belum kompatibel penuh dengan konfigurasi AGP proyek.
- **Solusi:**
  1. Buka `Settings` -> `Build, Execution, Deployment` -> `Build Tools` -> `Gradle`.
  2. Pastikan dropdown **Gradle JDK** diarahkan ke **JDK 17** (pilih `Embedded JDK` atau unduh JDK 17).
  3. Lakukan `File` -> `Sync Project with Gradle Files`.

---

### 2. Peringatan: `SDK processing. This version only understands SDK XML versions up to 3 but an SDK XML file of version 4 was encountered`
- **Penyebab:** Peringatan non-fatal dari Android Gradle Plugin ketika membaca format file XML metadata komponen Android SDK versi terbaru.
- **Solusi:** Peringatan ini **aman diabaikan** dan tidak menggagalkan proses build maupun eksekusi aplikasi. Untuk menghilangkan peringatan ini, buka `Tools` -> `SDK Manager` di Android Studio, lalu perbarui **Android SDK Platform-Tools** dan **Android SDK Build-Tools** ke versi paling mutakhir.

---

### 3. Error Build: `File google-services.json is missing` saat `./gradlew assembleDebug`
- **Penyebab:** Anda mencoba membangun APK Android sebelum mengunduh file kredensial dari Firebase Console.
- **Solusi:**
  1. Buat proyek di [Firebase Console](https://console.firebase.google.com).
  2. Unduh `google-services.json` dan simpan tepat di dalam folder:
     `paw_care/app/google-services.json`
  3. Jalankan kembali `./gradlew assembleDebug`.
  *(Catatan: Jika Anda hanya ingin menjalankan unit test, unit test JVM tetap dapat dijalankan tanpa file ini melalui `./gradlew testDebugUnitTest`).*

---

### 4. Error Firestore: `PERMISSION_DENIED: Missing or insufficient permissions`
- **Penyebab:** Anda belum memublikasikan aturan keamanan [`firestore.rules`](firestore.rules) ke Firebase Console, atau akun pengguna mencoba mengakses data milik pengguna lain.
- **Solusi:**
  1. Buka [Firebase Console](https://console.firebase.google.com/) -> **Firestore Database** -> tab **Rules**.
  2. Salin seluruh isi file [`firestore.rules`](firestore.rules) dari repositori PawCare.
  3. Tempelkan ke editor aturan lalu klik tombol **Publish**.
  4. Pastikan pengguna telah login sebelum melakukan operasi tulis/baca database.

---

### 5. Akun Admin Masuk ke Dashboard Pet Owner
- **Penyebab:** Dokumen user di Cloud Firestore masih memiliki field `role: "owner"`.
- **Solusi:**
  1. Buka Firebase Console -> **Firestore Database** -> tab **Data**.
  2. Buka koleksi `users`, cari dokumen yang sesuai dengan UID akun admin tersebut.
  3. Ubah field `role` menjadi `"admin"`.
  4. Pada aplikasi PawCare, lakukan Logout lalu Login kembali dengan akun tersebut.

---

### 6. Android Emulator Berjalan Lambat atau Macet
- **Penyebab:** Fitur akselerasi perangkat keras (*hardware acceleration / Hypervisor*) belum aktif di sistem operasi Anda.
- **Solusi:**
  1. Buka `Tools` -> `Device Manager` di Android Studio.
  2. Edit AVD yang digunakan (ikon pensil) -> klik **Show Advanced Settings**.
  3. Pada bagian **Emulated Performance**, ubah opsi **Graphics** dari `Software` menjadi **Hardware - GLES 2.0**.
  4. Pastikan alokasi RAM emulator minimal `2048 MB` (2 GB).

---

### 7. Teks Input Tertutup Keyboard Virtual pada Layar Kecil
- **Penyebab:** Keyboard virtual Android menutupi bagian bawah layar saat pengguna mengetik.
- **Solusi:** PawCare telah dikonfigurasi dengan `android:windowSoftInputMode="adjustResize"` pada `AndroidManifest.xml` serta modifier Compose `.imePadding()` dan `.verticalScroll()` pada seluruh layar formulir (`LoginScreen`, `RegisterScreen`, `AddEditPetScreen`, `CreateBookingScreen`, `InputMedicalRecordScreen`), sehingga layar otomatis bergeser ke atas saat keyboard terbuka.

---

## Dokumentasi Tambahan & Proposal

- **Proposal Proyek Akhir (Minggu 1):** [`docs/PROPOSAL.md`](docs/PROPOSAL.md) — Dokumen lengkap latar belakang, tujuan, persona peran, rincian jadwal 5 minggu, dan rubrik mata kuliah.
- **Dokumen Desain Sistem & Arsitektur:** [`docs/superpowers/specs/2026-11-11-pawcare-design.md`](docs/superpowers/specs/2026-11-11-pawcare-design.md) — Spesifikasi teknis arsitektur Clean MVVM, format dokumen database Firestore, kontrak slot booking unik, dan mitigasi query index.
- **Rencana Implementasi Teknis:** [`docs/superpowers/plans/2026-11-11-pawcare.md`](docs/superpowers/plans/2026-11-11-pawcare.md) — Rincian tahapan implementasi TDD dari pembuatan model, repository palsu, validasi pure-Kotlin, hingga antarmuka Compose.
- **Aturan Keamanan Database:** [`firestore.rules`](firestore.rules) — Kode aturan keamanan Cloud Firestore untuk kontrol akses berbasis peran (*Role-Based Access Control*).

---

## Screenshots Placeholder Aplikasi

> Letakkan tangkapan layar aplikasi (*screenshots*) berukuran proporsional yang diambil dari Android Emulator ke dalam folder `docs/screenshots/` sebelum pengumpulan tugas akhir:

| 1. Login & Autentikasi | 2. Daftar Hewan (Pet Owner) | 3. Form Booking & Slot Jam |
|:---:|:---:|:---:|
| `docs/screenshots/01_login.png` | `docs/screenshots/02_pets.png` | `docs/screenshots/03_booking.png` |

| 4. Antrean Booking (Admin) | 5. Input Rekam Medis & Vaksin | 6. Buku Medis Digital (Owner) |
|:---:|:---:|:---:|
| `docs/screenshots/04_admin_queue.png` | `docs/screenshots/05_medical_record.png` | `docs/screenshots/06_medical_history.png` |

---

*PawCare — Dikembangkan dengan standar Clean MVVM, Kotlin 2.0, Jetpack Compose, dan Cloud Firestore sesuai ketentuan Final Project Guidelines 2026.*
