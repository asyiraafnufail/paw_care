# Panduan Lengkap Setup, Build, Instalasi & Penggunaan PawCare
### *Centralized Master Guide: Veterinary Clinic Management Native Android App*

Dokumen ini adalah **pusat panduan tunggal (*single source of truth*)** untuk instalasi lingkungan, konfigurasi database, kompilasi aplikasi, pemasangan APK, pengujian otomatis, serta pengoperasian dua peran pengguna (**Pet Owner** & **Clinic Admin**) pada aplikasi PawCare.

> **Navigasi Cepat:** [Kembali ke README](../README.md) | [Proposal Proyek (Minggu 1)](PROPOSAL.md) | [Spesifikasi Desain](superpowers/specs/2026-11-11-pawcare-design.md) | [Aturan Keamanan Database](../firestore.rules)

---

## Daftar Isi Terpusat

1. [Spesifikasi Teknis & Persyaratan Sistem](#1-spesifikasi-teknis--persyaratan-sistem)
2. [Panduan Setup Android Studio & JDK 17](#2-panduan-setup-android-studio--jdk-17)
3. [Panduan Konfigurasi Firebase Authentication & Cloud Firestore](#3-panduan-konfigurasi-firebase-authentication--cloud-firestore)
4. [Panduan Menyiapkan & Menjalankan Android Emulator](#4-panduan-menyiapkan--menjalankan-android-emulator)
5. [Panduan Kompilasi & Build APK (Debug & Release)](#5-panduan-kompilasi--build-apk-debug--release)
6. [Panduan Instalasi APK ke Emulator & Smartphone Fisik (3 Metode)](#6-panduan-instalasi-apk-ke-emulator--smartphone-fisik-3-metode)
7. [Panduan Menjalankan Automated Unit Tests (100% JVM)](#7-panduan-menjalankan-automated-unit-tests-100-jvm)
8. [Panduan Pengoperasian Aplikasi (User Manual Pet Owner & Clinic Admin)](#8-panduan-pengoperasian-aplikasi-user-manual-pet-owner--clinic-admin)
9. [Arsitektur Data & Aturan Keamanan Database (`firestore.rules`)](#9-arsitektur-data--aturan-keamanan-database-firestorerules)
10. [Matriks Troubleshooting & Solusi Error Lengkap](#10-matriks-troubleshooting--solusi-error-lengkap)

---

## 1. Spesifikasi Teknis & Persyaratan Sistem

| Parameter | Nilai / Spesifikasi | Catatan |
|---|---|---|
| **Nama Aplikasi** | **PawCare** (`com.pawcare.app`) | Didefinisikan di `app/build.gradle.kts` |
| **Bahasa Pemrograman** | **Kotlin `2.0.21`** | Menggunakan plugin Kotlin Compose Compiler bawaan |
| **UI Framework** | **Jetpack Compose BOM `2024.10.01`** | Material Design 3, Edge-to-Edge |
| **Arsitektur** | **Clean Architecture MVVM** | Single-Activity, Unidirectional Data Flow, `StateFlow` |
| **Dependency Injection** | **Manual `AppContainer`** | Ringan, instan, tanpa overhead annotation processor |
| **Backend & Cloud DB** | **Firebase Auth & Cloud Firestore (`33.6.0`)** | Snapshot listener real-time, `PersistentCacheSettings` offline |
| **SDK Levels** | `compileSdk = 35`, `targetSdk = 35`, `minSdk = 26` | Mendukung Android 8.0 Oreo hingga Android 15 VanillaIceCream |
| **Java / JVM Target** | **Java 17 (`JVM_17`)** | Kompatibel dengan AGP 8.7.3 & Android Studio terbaru |
| **Android Studio** | **Ladybug (2024.2.1)** atau **Meerkat (2024.3+)** | macOS (Apple Silicon/Intel), Windows 64-bit, atau Linux |

---

## 2. Panduan Setup Android Studio & JDK 17

Ikuti langkah-langkah berikut untuk membuka dan menyinkronkan proyek PawCare di komputer Anda:

### Langkah 2.1: Clone atau Buka Folder Proyek
1. Buka aplikasi **Android Studio**.
2. Pada layar pembuka (*Welcome Dialog*), klik tombol **Open**.
3. Navigasikan ke direktori tempat repositori berada: `.../paw_care`.
4. Klik **Open** / **OK**.

### Langkah 2.2: Pastikan Menggunakan Gradle JDK 17
Aplikasi ini dikompilasi dengan target bytecode Java 17. Android Studio harus diarahkan ke JDK 17:
1. Buka jendela pengaturan:
   - **macOS:** Klik menu `Android Studio` -> `Settings...` (atau `Preferences...` / shortcut `Cmd + ,`).
   - **Windows / Linux:** Klik menu `File` -> `Settings...` (shortcut `Ctrl + Alt + S`).
2. Masuk ke menu:
   `Build, Execution, Deployment` -> `Build Tools` -> `Gradle`.
3. Pada dropdown **Gradle JDK**, pilih salah satu opsi JDK 17:
   - **Embedded JDK version 17.0.x** (disarankan, bawaan JetBrains Runtime / `jbr-17`).
   - Atau pilih **Download JDK...** -> Vendor: `Eclipse Temurin` / `Amazon Corretto` -> Version: `17` -> klik **Download**.
4. Klik **Apply**, lalu klik **OK**.

### Langkah 2.3: Konfigurasi File SDK `local.properties`
Android Studio umumnya membuat file `local.properties` secara otomatis. Namun, jika Anda baru melakukan kloning Git atau ingin menjalankan build via terminal, pastikan file `local.properties` ada di root folder `paw_care/`:

- **macOS (Default):**
  ```properties
  sdk.dir=/Users/<nama_user_anda>/Library/Android/sdk
  ```
- **Windows (Default):**
  ```properties
  sdk.dir=C\:\\Users\\<nama_user_anda>\\AppData\\Local\\Android\\Sdk
  ```
- **Linux (Default):**
  ```properties
  sdk.dir=/home/<nama_user_anda>/Android/Sdk
  ```

### Langkah 2.4: Lakukan Gradle Sync
1. Klik tombol **Sync Project with Gradle Files** (ikon gajah dengan panah biru di pojok kanan atas toolbar).
2. Tunggu proses resolusi dependensi dan indexing hingga jendela *Build* mencetak:
   `BUILD SUCCESSFUL in ...s`.

> **Graceful Fallback:** Modul `app/build.gradle.kts` telah dilengkapi logika proteksi: jika file `app/google-services.json` belum diletakkan, Gradle Sync dan **seluruh unit test tetap berjalan sukses** tanpa error. Namun, untuk menjalankan aplikasi di perangkat/emulator, ikuti langkah setup Firebase di bawah.

---

## 3. Panduan Konfigurasi Firebase Authentication & Cloud Firestore

PawCare menggunakan Firebase untuk autentikasi user dan penyimpanan data real-time klinik.

### Langkah 3.1: Buat Proyek di Firebase Console
1. Akses [Firebase Console](https://console.firebase.google.com/) dan login dengan Google Account.
2. Klik **Add project** (Tambah proyek).
3. Beri nama proyek, contoh: `PawCare Clinic`.
4. Google Analytics bersifat opsional (dapat di-uncheck untuk kemudahan), klik **Create project**.

### Langkah 3.2: Daftarkan Aplikasi Android
1. Di halaman Project Overview, klik ikon **Android** untuk menambahkan aplikasi.
2. Masukkan data konfigurasi aplikasi:
   - **Android package name (Wajib Persis):** `com.pawcare.app`
   - **App nickname:** `PawCare`
   - **Debug signing certificate SHA-1:** Kosongkan saja.
3. Klik tombol **Register app**.

### Langkah 3.3: Unduh & Tempatkan `google-services.json`
1. Unduh file `google-services.json` yang diberikan di Langkah 2 Firebase Console.
2. Letakkan file tersebut di dalam folder modul `app/`:
   ```text
   paw_care/app/google-services.json
   ```
   > **Peringatan Penting:** Letakkan di dalam folder `app/`, **BUKAN** di luar / root proyek.
   > 
   > **Keamanan Kredensial:** File `google-services.json` telah dimasukkan ke `.gitignore` (`*google-services.json`) agar kredensial API Key tidak pernah terunggah ke repositori publik Git. Contoh format file tersedia di `app/google-services.json.example`.

### Langkah 3.4: Aktifkan Firebase Authentication
1. Di panel navigasi kiri Firebase Console, buka **Build** -> **Authentication**.
2. Klik **Get started**.
3. Di tab **Sign-in method**, klik penyedia **Email/Password**.
4. Aktifkan toggle **Email/Password** (opsi *Email link (passwordless sign-in)* biarkan nonaktif).
5. Klik **Save**.

### Langkah 3.5: Buat Cloud Firestore Database
1. Di panel kiri, buka **Build** -> **Firestore Database**.
2. Klik tombol **Create database**.
3. Konfigurasi:
   - **Database ID:** `(default)`
   - **Location:** Pilih lokasi terdekat (contoh: `asia-southeast2` Jakarta atau `asia-southeast1` Singapura).
   - **Security rules:** Pilih **Start in production mode**.
4. Klik **Create** dan tunggu database terinisialisasi.

### Langkah 3.6: Terapkan Aturan Keamanan Database (`firestore.rules`)
Aturan keamanan resmi PawCare melindungi transaksi slot jam dan mencegah eskalasi hak akses:
1. Masuk ke tab **Rules** pada menu Firestore Database di Firebase Console.
2. Hapus aturan bawaan, salin seluruh kode dari file [`firestore.rules`](../firestore.rules) di root repositori:

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
3. Klik tombol **Publish**.

### Langkah 3.7: Pembuatan Akun Demo (Owner vs Admin)

#### Akun Pet Owner
- Dapat langsung mendaftar via aplikasi pada layar **Daftar Sekarang** (email & password min 6 karakter). Akun otomatis terdaftar dengan role `"owner"`.

#### Akun Clinic Admin (Dokter / Petugas)
Karena registrasi admin sengaja diblokir di sisi klien demi keamanan:
1. Daftarkan akun baru di aplikasi PawCare (misalnya: `admin@pawcare.com` dengan password rahasia) atau buat via menu **Authentication -> Add user** di Firebase Console.
2. Buka **Firestore Database** -> tab **Data** -> buka koleksi `users`.
3. Temukan dokumen pengguna tersebut berdasarkan UID-nya.
4. Ubah nilai field `role` dari `"owner"` menjadi `"admin"`.
5. Klik **Save**.
6. Pada aplikasi PawCare, logout lalu login kembali menggunakan akun tersebut: aplikasi otomatis mengarahkan ke dashboard **Clinic Admin**!

---

## 4. Panduan Menyiapkan & Menjalankan Android Emulator

### Langkah 4.1: Buat Virtual Device (AVD)
1. Di Android Studio, buka menu `Tools` -> `Device Manager` (atau ikon HP di kanan atas toolbar).
2. Klik tombol **Create Virtual Device** (`+`).
3. Pilih kategori **Phone** -> pilih profil **Pixel 7** atau **Pixel 8** (layar 1080 x 2400 piksel). Klik **Next**.
4. Pada tab **Recommended**, pilih System Image:
   - **API 34 ("UpsideDownCake")** atau **API 35 ("VanillaIceCream")**.
   - Pastikan tipe image adalah **Google APIs** / **Google Play x86_64**.
5. Klik **Next** -> beri nama AVD (misal: `PawCare_Pixel8_API34`) -> klik **Finish**.

### Langkah 4.2: Jalankan Aplikasi
1. Klik tombol **Play (▶)** di Device Manager untuk menghidupkan emulator.
2. Tunggu emulator selesai *booting* ke layar home Android.
3. Di toolbar Android Studio:
   - Pilih target module: `app`.
   - Pilih target device: nama emulator yang sedang berjalan.
4. Klik tombol hijau **Run 'app'** (atau tekan `Shift + F10`).
5. Aplikasi akan dikompilasi, dipasang, dan langsung membuka tampilan awal PawCare.

---

## 5. Panduan Kompilasi & Build APK (Debug & Release)

### 5.1. Build Debug APK via Terminal (Direkomendasikan)
Buka terminal di root direktori proyek `paw_care/`:

- **macOS / Linux:**
  ```bash
  ./gradlew assembleDebug
  ```
- **Windows (CMD / PowerShell):**
  ```cmd
  gradlew.bat assembleDebug
  ```

Output file APK yang dihasilkan:
```text
app/build/outputs/apk/debug/app-debug.apk
```

> **Keunggulan `app-debug.apk`:** Sudah ditandatangani otomatis oleh Android Studio dengan debug certificate bawaan, sehingga dapat langsung diinstal ke emulator dan seluruh smartphone Android fisik tanpa setup keystore rilis.

### 5.2. Build Debug APK via Android Studio GUI
1. Klik menu bar Android Studio: `Build` -> `Build Bundle(s) / APK(s)` -> `Build APK(s)`.
2. Tunggu notifikasi muncul di sudut kanan bawah:
   *"APK(s) generated successfully for 1 module"*.
3. Klik teks biru **locate** pada notifikasi tersebut untuk langsung membuka lokasi file APK di File Manager / Finder.

### 5.3. Build Release APK (Produksi)
- **Via Terminal (Unsigned):**
  ```bash
  ./gradlew assembleRelease
  ```
  File output: `app/build/outputs/apk/release/app-release-unsigned.apk`
- **Via GUI Android Studio (Signed Keystore):**
  1. Klik menu `Build` -> `Generate Signed Bundle / APK...`.
  2. Pilih **APK** -> klik **Next**.
  3. Masukkan Keystore file (`.jks`), password keystore, dan key alias.
  4. Pilih Build Variant: **release**.
  5. Klik **Finish**.

---

## 6. Panduan Instalasi APK ke Emulator & Smartphone Fisik (3 Metode)

### Metode 6.1: Drag-and-Drop ke Android Emulator (Paling Cepat & Simpel)
1. Jalankan Android Emulator hingga berada pada layar utama (*Home Screen*).
2. Buka Finder (macOS) atau File Explorer (Windows).
3. Cari file `app-debug.apk` di folder `app/build/outputs/apk/debug/`.
4. Tarik (*drag*) file **`app-debug.apk`** dan lepaskan (*drop*) tepat di atas jendela Android Emulator.
5. Emulator akan menampilkan status *"Installing APK..."* selama beberapa detik.
6. Ikon **PawCare** hijau akan langsung muncul di menu aplikasi emulator. Klik untuk membuka!

---

### Metode 6.2: Menggunakan ADB (Android Debug Bridge CLI)
1. Hubungkan HP fisik via kabel data USB atau nyalakan emulator.
2. Di terminal, periksa apakah perangkat terdeteksi:
   ```bash
   adb devices
   ```
3. Pasang APK menggunakan flag `-r` (mengizinkan overwrite data aplikasi yang sudah ada):
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
4. Output terminal akan mencetak:
   ```text
   Performing Streamed Install
   Success
   ```
5. Untuk meluncurkan aplikasi langsung dari terminal:
   ```bash
   adb shell am start -n com.pawcare.app/.MainActivity
   ```

---

### Metode 6.3: Instalasi di Smartphone Android Fisik (HP Nyata)

#### Cara A: Via Kabel USB & Developer Options
1. **Buka Opsi Pengembang:** Buka `Pengaturan (Settings)` di ponsel Android -> `Tentang Ponsel (About Phone)` -> ketuk **Nomor Bentukan (Build Number)** sebanyak 7 kali hingga muncul pesan pengembang aktif.
2. **Aktifkan USB Debugging:** Buka `Pengaturan` -> `Sistem` -> `Opsi Pengembang (Developer Options)` -> aktifkan toggle **Debugging USB (USB Debugging)**.
3. Hubungkan HP ke komputer dengan kabel USB, pilih **Izinkan Debugging USB dari komputer ini**.
4. Di terminal komputer, jalankan `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

#### Cara B: Kirim File APK Mandiri (Tanpa Komputer / ADB)
1. Kirimkan file `app-debug.apk` ke HP Anda (bisa lewat Google Drive, WhatsApp dokumen, Telegram, atau kabel USB biasa).
2. Buka aplikasi **File Manager** / **Files by Google** di HP Anda.
3. Cari file `app-debug.apk` yang telah diunduh, lalu ketuk file tersebut.
4. Jika muncul peringatan keamanan *"Aplikasi dari sumber tidak dikenal"*, pilih **Pengaturan (Settings)** dan aktifkan toggle **Izinkan dari sumber ini (Allow from this source)**.
5. Ketuk **Pasang (Install)** -> tunggu proses selesai -> ketuk **Buka (Open)**.

---

## 7. Panduan Menjalankan Automated Unit Tests (100% JVM)

PawCare dilengkapi suite pengujian otomatis berbasis JUnit 4 dan Kotlin Coroutines Test. Seluruh logika repository, alur transaksi atomik, validasi form, dan state management ViewModel diuji secara murni di JVM (**tanpa emulator, tanpa perangkat, dan tanpa internet**).

### 7.1. Jalankan Unit Test via Terminal
```bash
./gradlew testDebugUnitTest --continue
```
*(Windows: `gradlew.bat testDebugUnitTest --continue`)*

### 7.2. Jalankan Unit Test via Android Studio GUI
1. Pada panel *Project* sebelah kiri, buka folder:
   `app/src/test/java/com/pawcare/app/`.
2. Klik kanan pada folder `com.pawcare.app` -> pilih **Run 'Tests in 'pawcare.app''**.
3. Hasil eksekusi akan ditampilkan dengan centang hijau pada jendela *Run* di bagian bawah.

### 7.3. Rincian 7 Test Suite yang Diuji

| Nama File Test Suite | Layer Arsitektur | Perilaku yang Diverifikasi |
|---|---|---|
| `ValidatorsTest.kt` | `util/` | Format email regex, password minimum 6 karakter, form validasi data anabul (nama, ras, umur bulan, berat kg), pemblokiran booking tanggal lampau & slot lampau hari ini, alasan penolakan wajib, dan nama vaksin wajib pada layanan vaksinasi. |
| `FakeRepositoriesTest.kt` | `data/repository/` | Penguncian slot jam unik atomik `booked_slots/{date}_{timeSlot}`, pembatalan booking yang mengosongkan kembali slot, larangan hapus anabul jika masih memiliki janji temu aktif, dan batch penulisan rekam medis. |
| `NavigationRouteTest.kt` | `ui/navigation/` | Integritas rute Compose Navigation (`Screen.kt`), parsing argumen dinamis ID rute, dan kepatuhan tab bottom bar Owner dan Admin. |
| `AuthViewModelTest.kt` | `viewmodel/` | Alur registrasi otomatis role `"owner"`, alur login berhasil/gagal, validasi error state form, routing otomatis role admin/owner, dan logout. |
| `OwnerViewModelTest.kt` | `viewmodel/` | CRUD anabul, pencarian anabul live, pembuatan booking dengan validasi slot, pemblokiran jam lewat, pembatalan booking, dan tab backstack navigation. |
| `AdminViewModelTest.kt` | `viewmodel/` | Review antrean booking masuk, filter per tanggal kunjungan, persetujuan janji temu, penolakan booking dengan alasan wajib yang membebaskan slot, input rekam medis atomik, dan pencarian arsip pasien. |
| `MedicalRecordDetailViewModelTest.kt` | `viewmodel/` | Pengambilan dan pemetaan data rekam medis terperinci berdasarkan `appointmentId` untuk buku vaksin/medis digital. |

### 7.4. Melihat Laporan Visual HTML Test
Gradle secara otomatis menghasilkan laporan HTML lengkap. Buka file berikut di peramban web:
```text
paw_care/app/build/reports/tests/testDebugUnitTest/index.html
```

---

## 8. Panduan Pengoperasian Aplikasi (User Manual Pet Owner & Clinic Admin)

### 8.1. Alur Pengguna Pet Owner (Pemilik Hewan)

```text
[Buka Aplikasi] -> [Register / Login Akun Owner] -> [Dashboard Owner (3 Tab)]
       │
       ├── Tab 1: Hewan Peliharaan
       │     ├── Tambah Anabul: Nama, Spesies (Kucing/Anjing/Kelinci/Burung/Lainnya), Ras, Umur, Berat
       │     ├── Edit Profil Anabul
       │     └── Hapus Anabul (Otomatis ditolak jika anabul punya janji aktif!)
       │
       ├── Tab 2: Janji Temu Aktif
       │     ├── FAB "Buat Janji Temu"
       │     │     ├── Pilih Anabul
       │     │     ├── Pilih Layanan (Vaksinasi / Periksa Umum / Konsultasi / Sterilisasi)
       │     │     ├── Pilih Tanggal (Dilarang tanggal lampau)
       │     │     ├── Pilih Slot Jam Klinik (09:00 - 16:00, slot penuh otomatis terkunci)
       │     │     └── Simpan Booking (Slot dikunci atomik)
       │     └── Batalkan Janji Temu (Selama status masih PENDING; slot jam langsung bebas)
       │
       └── Tab 3: Riwayat & Buku Medis
             ├── Sub-tab "Buku Medis": Kartu rekam medis, diagnosa dokter, merk vaksin, dan jadwal kontrol
             └── Sub-tab "Riwayat Janji": Daftar janji temu COMPLETED dan CANCELLED (beserta alasan batal)
```

### 8.2. Alur Pengguna Clinic Admin (Dokter / Petugas Klinik)

```text
[Buka Aplikasi] -> [Login Akun Admin] -> [Dashboard Admin (3 Tab)]
       │
       ├── Tab 1: Antrean Masuk (Queue)
       │     ├── Filter antrean berdasarkan tanggal kunjungan
       │     ├── Setujui Janji Temu (Status berubah menjadi CONFIRMED)
       │     └── Tolak Janji Temu (Wajib isi alasan di dialog; slot jam otomatis dibebaskan kembali)
       │
       ├── Tab 2: Pasien Aktif (Hari Ini)
       │     ├── Daftar pasien CONFIRMED yang siap diperiksa
       │     └── Tombol "Periksa / Input Rekam Medis"
       │           ├── Input Diagnosa Dokter (Wajib)
       │           ├── Input Tindakan Terapi (Wajib)
       │           ├── Input Nama / Merk Vaksin (Wajib jika layanan = Vaksinasi)
       │           ├── Catatan Resep & Anjuran Dokter (Opsional)
       │           ├── Tanggal Kontrol Lanjutan (Opsional)
       │           └── Simpan (Atomik: Rekam medis tersimpan & status booking jadi COMPLETED)
       │
       └── Tab 3: Arsip Kunjungan (Archive)
             ├── Pencarian instan berdasarkan nama anabul atau email pemilik
             └── Filter status janji temu (Semua / Selesai / Dibatalkan)
```

---

## 9. Arsitektur Data & Aturan Keamanan Database (`firestore.rules`)

### 9.1. Koleksi Cloud Firestore

1. **`users/{uid}`**: Data akun pengguna (`uid`, `email`, `role`, `createdAt`). Role `"owner"` dibuat secara mandiri; role `"admin"` dilindungi dan hanya dapat diberikan melalui Firebase Console.
2. **`pets/{petId}`**: Profil anabul (`petId`, `ownerId`, `name`, `species`, `breed`, `ageMonths`, `weightKg`, `createdAt`).
3. **`appointments/{appointmentId}`**: Janji temu (`appointmentId`, `ownerId`, `ownerEmail`, `petId`, `petName`, `petSpecies`, `serviceType`, `date`, `timeSlot`, `slotKey`, `notes`, `status`, `rejectionReason`, `createdAt`, `updatedAt`).
4. **`booked_slots/{date_slot}`**: Pengunci slot atomik (`appointmentId`, `ownerId`, `date`, `timeSlot`). Menjamin dua pengguna tidak dapat memilih slot jam yang sama.
5. **`medical_records/{appointmentId}`**: Dokumen rekam medis 1:1 terhadap janji temu (`appointmentId`, `petId`, `petName`, `ownerId`, `ownerEmail`, `serviceType`, `diagnosis`, `treatment`, `vaccineName`, `doctorNotes`, `visitDate`, `nextVisitDate`, `createdAt`).

### 9.2. Disiplin Query Tanpa Composite Index Crash
Seluruh query Firestore pada aplikasi PawCare dirancang secara disiplin hanya menggunakan filter kesetaraan tunggal (`whereEqualTo("ownerId", ...)` atau `whereEqualTo("status", ...)`). Pengurutan (*sorting*) berdasarkan tanggal/jam dan pencarian (*filtering*) nama/email dilakukan secara reaktif di memori Kotlin (`StateFlow.combine`). Pendekatan ini menjamin aplikasi **bebas dari crash `FAILED_PRECONDITION (missing composite index)`** saat dinilai atau dijalankan di lingkungan baru!

---

## 10. Matriks Troubleshooting & Solusi Error Lengkap

| Gejala Masalah / Pesan Error | Penyebab Utama | Solusi Penanganan |
|---|---|---|
| **`compileJava failed` / `Unsupported class file major version`** | Gradle JDK di Android Studio masih menggunakan Java 8, 11, atau 21. | Buka `Settings` -> `Build, Execution, Deployment` -> `Build Tools` -> `Gradle` -> ubah **Gradle JDK** ke **JDK 17** (`jbr-17` atau Temurin 17). |
| **Peringatan `SDK XML versions up to 3 but an SDK XML file of version 4 was encountered`** | Format metadata baru pada Android SDK Platform-Tools saat dibaca AGP. | Peringatan ini aman diabaikan. Untuk menghilangkannya, perbarui **Android SDK Platform-Tools** dan **Build-Tools** via SDK Manager. |
| **`Execution failed for task ':app:processDebugGoogleServices'` (File hilang)** | File konfigurasi Firebase belum ditaruh di direktori modul Android. | Unduh `google-services.json` dari Firebase Console dan simpan di `paw_care/app/google-services.json`. *(Unit test JVM tetap dapat jalan tanpa file ini).* |
| **Firestore `PERMISSION_DENIED: Missing or insufficient permissions`** | Rules pada Firestore Database di Firebase Console belum diperbarui atau ditimpa aturan bawaan yang memblokir akses. | Buka Firebase Console -> Firestore Database -> tab **Rules** -> tempelkan seluruh isi file `firestore.rules` -> klik **Publish**. |
| **Akun Admin masuk ke tampilan Pet Owner** | Dokumen user di koleksi `users` di Firestore masih memiliki field `role: "owner"`. | Buka Firestore Console -> koleksi `users` -> temukan UID admin -> ubah field `role` menjadi `"admin"`. Lakukan logout lalu login kembali di aplikasi. |
| **Emulator sangat lambat / lag parah** | Hardware acceleration (GLES) belum aktif pada emulator. | Buka Device Manager -> Edit AVD (ikon pensil) -> Show Advanced Settings -> Graphics: ubah ke **Hardware - GLES 2.0**. |
| **Teks form tertutup oleh keyboard virtual** | Keyboard menutupi bagian bawah layar pada ponsel berlayar pendek. | Seluruh form PawCare telah dibekali `.imePadding()` dan `windowSoftInputMode="adjustResize"`, cukup gulir (*scroll*) form ke atas. |
| **Anabul tidak bisa dihapus** | Anabul masih memiliki janji temu dengan status `PENDING` atau `CONFIRMED`. | Ini adalah proteksi data integritas PawCare. Batalkan janji temu anabul terlebih dahulu di tab **Janji Temu**, barulah anabul dapat dihapus. |

---

*Dokumen ini merupakan panduan terpusat resmi untuk proyek akhir aplikasi PawCare. Seluruh hak cipta konfigurasi dan kode dilindungi.*
