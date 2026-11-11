# PawCare — Design Specification

**Date:** 2026-11-11
**Project:** PawCare (Veterinary Clinic Management Android App)
**Course Context:** Final Project Guidelines 2026 — Mobile Programming (5-Week Delivery)

---

## 1. Intent & Why

PawCare is a native Android application for managing veterinary clinic visits (vaccinations, general checkups, consultations, and sterilization) across two distinct roles: **Pet Owner** and **Clinic Admin**.

Pet owners often struggle to track their pets' vaccination schedules, checkup history, and appointment confirmations without calling the clinic repeatedly. Clinic staff struggle with manual booking queues and scattered paper medical records. PawCare connects both roles in real time using Firebase, providing a clean self-service booking & digital medical booklet for owners and a structured queue & examination workflow for clinic admins.

---

## 2. Goals, Non-Goals, and Anti-Goals

### Goals
- **Core MVP Workflow (5 Core Features):**
  1. Email/password authentication with role-based routing (`owner` self-registration; `admin` assigned in Firestore).
  2. Pet profile CRUD for Pet Owners using built-in species avatar icons.
  3. Clinic appointment booking with double-booking prevention per date and time slot.
  4. Admin queue management (filter incoming bookings, approve or reject with reason).
  5. Digital medical & vaccination records written by Admin upon completing an appointment and visible immediately to the Pet Owner.
- **Course Rubric Alignment (Final Project Guidelines 2026):**
  - **UI/UX (25%):** Material 3 design, Bottom Navigation (3 tabs per role) + Stack Navigation (>= 5 interconnected screens excluding Splash & Login), responsive states.
  - **Functionality (25%):** Complete end-to-end flows for both roles, inline form validation, snackbar/dialog feedback, loading/empty/error states.
  - **Code Quality & State Management (20%):** Kotlin + Jetpack Compose + MVVM (`ViewModel`, `StateFlow`, `sealed interface UiState<out T>`), clean package separation (`ui/`, `viewmodel/`, `data/model/`, `data/repository/`, `util/`).
  - **Database Integration (15%):** Firebase Auth + Cloud Firestore with real-time snapshot listeners and offline persistence enabled.
  - **Repository & Delivery Discipline:** 10–15+ incremental meaningful commits (`feat:`, `fix:`, etc.), zero committed secrets, `README.md` with 3–5 screenshots and setup guide, APK build, 2–4 min demo video, Week 5 live demo.

### Non-Goals (Out of Scope for 5-Week MVP)
- iOS, Compose Multiplatform, or Flutter builds (Android native `.apk` only, verified on Android Emulator).
- Image upload / Firebase Storage (species-based built-in avatars only).
- Payments, billing, invoices, or receipts.
- In-app chat between Pet Owner and veterinarian.
- Admin CRUD screens for clinic services or operating hours (fixed predefined catalog & hourly slots).
- Local push / WorkManager / AlarmManager reminders (defer unless all core features and tests complete early).
- Multi-clinic or branch switching (single clinic scope).

### Anti-Goals
- Allowing client registration or client writes to assign or escalate to the `admin` role.
- Committing `google-services.json` or private API keys directly into the public GitHub repository.
- Unhandled exceptions or app crashes when offline, on empty Firestore collections, or on invalid form input during demo.
- Monolithic code dumps without clear separation of UI, state, and data layers.

---

## 3. Constraints & Tech Stack

| Dimension | Decision |
|---|---|
| Platform | Native Android (`applicationId = "com.pawcare.app"`, `minSdk = 26`, `targetSdk = 35` — `[our call]` so `java.time` works natively without desugaring; tested on Android Emulator on macOS, packaged as `.apk`) |
| UI Language | Indonesian labels & messages (`[our call]`) matching user context |
| Language & UI | Kotlin + Jetpack Compose + Material Design 3 |
| Architecture | Single-Activity (`ComponentActivity`) + Jetpack Navigation Compose + MVVM |
| State Management | `StateFlow` exposed from `ViewModel`, consumed via `collectAsStateWithLifecycle()`, modeled with `sealed interface UiState<out T>` (`Loading`, `Success`, `Error`) |
| Backend & Persistence | Firebase Authentication (Email & Password) + Cloud Firestore (offline persistence enabled) |
| Media/Avatars | Built-in vector/drawable avatars mapped by pet species (no Firebase Storage) |
| Secret Hygiene | `google-services.json` listed in `.gitignore`; provide `app/google-services.json.example` + setup steps in `README.md` |
| Security Rules | `firestore.rules` stored in repo enforcing role immutability, owner isolation, and admin-only medical record writes |

---

## 4. Architecture & Data Model

### 4.1 Package Structure
- `ui/theme/` — Material 3 color scheme, typography, theme.
- `ui/navigation/` — Route definitions & `PawCareNavHost` (Auth graph, Owner graph, Admin graph).
- `ui/components/` — Reusable UI components (status badges, pet avatar mapper, loading/empty/error states, confirmation dialogs).
- `ui/screens/auth/` — Splash/AuthGate, Login, Register.
- `ui/screens/owner/` — Owner shell (3-tab BottomBar) + `PetsScreen`, `AddEditPetScreen`, `OwnerBookingsScreen`, `CreateBookingScreen`, `OwnerHistoryScreen`, `MedicalRecordDetailScreen`.
- `ui/screens/admin/` — Admin shell (3-tab BottomBar) + `AdminQueueScreen`, `AdminActivePatientsScreen`, `InputMedicalRecordScreen`, `AdminArchiveScreen`.
- `viewmodel/` — `AuthViewModel`, `PetViewModel`, `OwnerBookingViewModel`, `AdminViewModel`, `MedicalRecordViewModel` (or cohesive per-flow ViewModels).
- `data/model/` — Kotlin data classes & enums (`User`, `Pet`, `PetSpecies`, `ServiceType`, `Appointment`, `AppointmentStatus`, `BookedSlot`, `MedicalRecord`).
- `data/repository/` — Interfaces & Firebase implementations (`AuthRepository`, `PetRepository`, `AppointmentRepository`, `MedicalRecordRepository`).
- `di/` — Lightweight manual dependency container (`AppContainer`) + ViewModel factory helpers (no unnecessary DI framework overhead unless preferred).
- `util/` — Input validators, date/slot helpers, `UiState` wrapper.

### 4.2 Cloud Firestore Schema

1. **`users/{uid}`**
   - `uid`: `String` (matches Firebase Auth UID)
   - `email`: `String`
   - `role`: `String` (`"owner"` | `"admin"`)
   - `createdAt`: `Timestamp`
   - *Rule:* Register screen creates `users/{uid}` strictly with `role = "owner"`. Admin accounts are created in Firebase Console Auth and promoted by setting `role = "admin"` directly in Firestore Console. Security rules block any client write that sets `role` to anything other than `"owner"` on create or modifies `role` on update.

2. **`pets/{petId}`**
   - `petId`: `String`
   - `ownerId`: `String` (`users.uid`)
   - `name`: `String` (required)
   - `species`: `String` (`"CAT"` | `"DOG"` | `"RABBIT"` | `"BIRD"` | `"OTHER"`)
   - `breed`: `String` (required)
   - `ageMonths`: `Int` (> 0)
   - `weightKg`: `Double` (> 0.0)
   - `createdAt`: `Timestamp`

3. **`appointments/{appointmentId}`**
   - `appointmentId`: `String`
   - `ownerId`: `String`
   - `ownerEmail`: `String`
   - `petId`: `String`
   - `petName`: `String` (denormalized snapshot for fast list rendering)
   - `petSpecies`: `String` (denormalized snapshot for avatar icon)
   - `serviceType`: `String` (`"VACCINATION"` | `"CHECKUP"` | `"CONSULTATION"` | `"STERILIZATION"`)
   - `date`: `String` (`"YYYY-MM-DD"`)
   - `timeSlot`: `String` (fixed hourly slots, e.g. `"09:00"`, `"10:00"`, ..., `"16:00"`)
   - `slotKey`: `String` (`"${date}_${timeSlot}"`)
   - `notes`: `String` (optional complaint / notes)
   - `status`: `String` (`"PENDING"` | `"CONFIRMED"` | `"COMPLETED"` | `"CANCELLED"`)
   - `rejectionReason`: `String?` (required when Admin rejects)
   - `createdAt`: `Timestamp`
   - `updatedAt`: `Timestamp`

4. **`booked_slots/{slotKey}`** (Atomic Double-Booking Lock)
   - Document ID: `"{YYYY-MM-DD}_{HH:mm}"`
   - `appointmentId`: `String`
   - `ownerId`: `String`
   - `date`: `String`
   - `timeSlot`: `String`
   - *Rule:* Authenticated users can read `booked_slots` (by `date`) to know which slots are already taken without exposing any sensitive owner/pet details. Creating a booking runs a Firestore transaction that verifies `booked_slots/{slotKey}` does not exist before writing both `booked_slots/{slotKey}` and `appointments/{appointmentId}`. Cancelling a `PENDING` appointment (by Owner) or rejecting one (by Admin) runs a Firestore transaction/batch that updates `appointments.status = "CANCELLED"` and deletes `booked_slots/{slotKey}` so the slot reopens.

5. **`medical_records/{appointmentId}`** `[our call: recordId == appointmentId for 1:1 integrity & instant O(1) lookup]`
   - `appointmentId`: `String` (same as document ID)
   - `petId`: `String`
   - `petName`: `String`
   - `ownerId`: `String`
   - `ownerEmail`: `String`
   - `serviceType`: `String`
   - `diagnosis`: `String` (required)
   - `treatment`: `String` (required)
   - `vaccineName`: `String?` (required if `serviceType == "VACCINATION"`, optional otherwise)
   - `doctorNotes`: `String` (optional)
   - `visitDate`: `String` (`"YYYY-MM-DD"`)
   - `nextVisitDate`: `String?` (`"YYYY-MM-DD"`, optional)
   - `createdAt`: `Timestamp`
   - *Atomicity:* Submitting a medical record runs a Firestore batch/transaction that creates `medical_records/{appointmentId}` AND sets `appointments/{appointmentId}.status = "COMPLETED"` together.

### 4.3 Firestore Query Discipline & Index Resilience `[our call]`
To guarantee the app never crashes during live presentation or offline with `FAILED_PRECONDITION (missing composite index)`:
- Repositories execute only single-field equality queries on Firestore (e.g., `whereEqualTo("ownerId", uid)` or `whereEqualTo("status", "PENDING")`).
- Multi-field filtering and sorting (by date, time, or pet) are performed in Kotlin (`StateFlow` / Repository transform). This completely eliminates custom composite index dependencies in Firebase Console.

### 4.4 Security Rules Summary (`firestore.rules`)
- `users/{uid}`: Read own doc (or Admin reads any); create allowed only if `request.auth.uid == uid` and `request.resource.data.role == "owner"`; update blocked for `role` changes.
- `pets/{petId}`: Read/write allowed only when `ownerId == request.auth.uid` (or Admin read).
- `booked_slots/{slotKey}`: Read allowed for any authenticated user; create allowed for authenticated owner with matching `ownerId`; delete allowed by Admin, or by slot's `ownerId` when pre-transaction `appointments/{appointmentId}` status is `"PENDING"` (`get(...)`).
- `appointments/{appointmentId}`: Read allowed for `ownerId == request.auth.uid` or Admin. Create allowed for authenticated owner (`status == "PENDING"`, `ownerId == request.auth.uid`). Update allowed for Owner only to change `PENDING -> CANCELLED` on own appointment, or Admin for clinic transitions (`PENDING -> CONFIRMED/CANCELLED`, `CONFIRMED -> COMPLETED/CANCELLED`).
- `medical_records/{appointmentId}`: Read allowed for `ownerId == request.auth.uid` or Admin. Write allowed only for Admin (with `appointmentId` matching document ID).

---

## 5. User Flows & Screen Specifications

### 5.1 Auth Flow
1. **Splash / AuthGate Screen:** Checks `FirebaseAuth.currentUser`. If present, fetches `users/{uid}.role` and routes to Owner or Admin shell; if absent, routes to Login. Shows loading indicator and retry button on transient network error (if user doc cannot be verified).
2. **Login Screen:** Email + Password fields, inline validation (valid email format, non-empty password >= 6 chars), Login button with loading state, error banner/snackbar on invalid credentials, navigation link to Register.
3. **Register Screen:** Email + Password + Confirm Password only. Validates email format, password >= 6 chars, and password match. Creates Firebase Auth user + `users/{uid}` (`role = "owner"`) and automatically enters Owner shell without email verification gating `[our call]`.

### 5.2 Pet Owner Flow (Option A: 3-Tab Bottom Navigation)
TopAppBar displays `"PawCare"` + Logout icon button (with confirmation dialog).

1. **Tab 1 — `PetsScreen` (Pet Saya):**
   - Lists owner's pets with built-in species avatar, name, species/breed, age, weight.
   - Empty state with prompt to add first pet.
   - Tap pet card -> opens `AddEditPetScreen` (edit/delete pet) OR quick-access button to view that pet's medical history (filtered in History or dedicated sub-screen).
   - FAB `+ Tambah Pet` -> `AddEditPetScreen` (Name, Species dropdown/chips, Breed, Age in months, Weight in kg, inline validation, delete confirmation when editing). Deleting a pet with active (`PENDING`/`CONFIRMED`) appointments is blocked with a prompt to cancel the active appointment first `[our call]`; historical medical records remain intact.
2. **Tab 2 — `OwnerBookingsScreen` (Booking):**
   - Displays active bookings (`PENDING`, `CONFIRMED`) in real time with status badge (`Menunggu Konfirmasi` / `Disetujui`).
   - Bookings with status `PENDING` show a `Batalkan` button (confirmation dialog -> cancels booking & frees slot).
   - FAB / CTA `+ Booking Baru` -> `CreateBookingScreen`:
     - Select Pet (if user has 0 pets, show prompt/button redirecting to Add Pet).
     - Select Service (`Vaksinasi`, `Periksa Umum`, `Konsultasi`, `Steril`).
     - Select Date (DatePicker, today or future only).
     - Select Time Slot (chips `09:00`–`16:00`; already-booked slots queried from `booked_slots` for selected date, plus past hours if selected date is today `[our call]`, are disabled).
     - Optional complaint/notes field.
     - Submit runs atomic booking transaction; shows success snackbar or conflict message if slot was just taken.
3. **Tab 3 — `OwnerHistoryScreen` (Riwayat & Medis):**
   - Sub-filter / segmented control between **Buku Vaksin & Medis** (`medical_records` for owner's pets, optional filter by pet) and **Riwayat Janji** (`COMPLETED` / `CANCELLED` appointments showing `rejectionReason` if rejected).
   - Tap any medical record card -> `MedicalRecordDetailScreen` showing full clinic report (pet info, visit date, service, diagnosis, treatment, vaccine name, doctor notes, next visit date).

### 5.3 Clinic Admin Flow (Option A: 3-Tab Bottom Navigation)
TopAppBar displays `"PawCare Admin"` + Logout icon button (with confirmation dialog).

1. **Tab 1 — `AdminQueueScreen` (Antrean Masuk):**
   - Lists `PENDING` appointments sorted by date/timeSlot ascending.
   - Date filter chips (`Semua`, `Hari Ini`, `Pilih Tanggal`).
   - Card displays pet name & species icon, service type, date & time slot, owner email, complaint notes.
   - Actions:
     - **Setujui** -> sets status to `CONFIRMED`.
     - **Tolak** -> opens dialog requiring non-empty `rejectionReason`, updates status to `CANCELLED` and releases `booked_slots/{slotKey}`.
2. **Tab 2 — `AdminActivePatientsScreen` (Pasien Aktif):**
   - Lists `CONFIRMED` appointments waiting for / undergoing examination (with optional `Hari Ini` / `Semua` filter).
   - Action button **Selesai & Input Rekam Medis** -> `InputMedicalRecordScreen`:
     - Pre-filled read-only summary (pet name, owner email, service, visit date).
     - Form fields: Diagnosis (required), Tindakan/Treatment (required), Nama Vaksin (required when `serviceType == VACCINATION`, optional otherwise), Catatan Dokter (optional), Jadwal Kontrol Berikutnya (optional DatePicker >= visitDate).
     - Save runs atomic batch/transaction creating `medical_records/{recordId}` and updating `appointments/{appointmentId}` to `COMPLETED`.
     - Also allow secondary **Batalkan Janji** action (with reason + frees slot) in case of patient no-show.
3. **Tab 3 — `AdminArchiveScreen` (Arsip & Riwayat):**
   - Lists all `COMPLETED` and `CANCELLED` records/appointments.
   - Search input (matches `petName` or `ownerEmail`) + status filter (`Semua`, `Selesai`, `Dibatalkan`).
   - Tap completed item -> opens `MedicalRecordDetailScreen`.

### 5.4 Input Validation Rules (UI + ViewModel + Repository)
All forms validate inline and prevent submission while invalid, backed by shared pure-Kotlin functions in `util/Validators.kt` (unit-tested) plus `firestore.rules` enforcement:
- **Login & Register:**
  - `email`: non-empty, valid email format.
  - `password`: non-empty, minimum 6 characters.
  - `confirmPassword` (Register): must match `password` exactly.
- **Add/Edit Pet:**
  - `name`: non-empty after trim (max 50 chars).
  - `breed`: non-empty after trim (max 50 chars).
  - `ageMonths`: valid positive integer (`1..360`).
  - `weightKg`: valid positive decimal (`> 0.0` and `<= 150.0`).
- **Create Booking:**
  - `petId` & `serviceType`: required.
  - `date`: required and **must not be before today** (`date >= LocalDate.now()`), enforced both in Material 3 `DatePicker` (`SelectableDates`) and on submit.
  - `timeSlot`: required, must not be in `booked_slots` for `date`, and if `date == LocalDate.now()`, **must be strictly after the current hour** (`slotHour > LocalTime.now().hour`).
  - `notes`: optional, trimmed (max 300 chars).
- **Admin Reject / Cancel Reason Dialog:**
  - `rejectionReason`: required, non-empty after trim.
- **Admin Input Medical Record:**
  - `diagnosis`: required, non-empty after trim.
  - `treatment`: required, non-empty after trim.
  - `vaccineName`: **required when `serviceType == "VACCINATION"`**, optional otherwise.
  - `nextVisitDate`: optional; when provided, **must not be before `visitDate` or today** (`nextVisitDate >= visitDate`).

---

## 6. Left to the Builder

- Choose exact Material 3 color palette, typography, spacing, and drawable/vector assets for species avatars.
- Choose exact Gradle & library BOM versions compatible with local Android Studio / JDK setup (Kotlin, Compose BOM, Navigation Compose, Firebase BOM, Coroutines, JUnit/MockK).
- Define fixed clinic hours/time slots (default `09:00`–`16:00` hourly) and Indonesian display labels for `PetSpecies` and `ServiceType`.
- Implement unit tests covering form validation, state transitions, and booking/slot conflict repository logic.
- Follow the 5-week course delivery schedule:
  - Week 1: Proposal doc + repo initialization + Firebase project setup + Auth & Role Routing.
  - Week 2: Pet CRUD + Owner Booking + atomic `booked_slots` lock.
  - Week 3: Admin Queue + Medical Record input + Owner History/Detail.
  - Week 4: Error/empty/offline polish, unit tests, `README.md` (with 3–5 screenshots), APK build.
  - Week 5: 2–4 min video demo & live presentation.
