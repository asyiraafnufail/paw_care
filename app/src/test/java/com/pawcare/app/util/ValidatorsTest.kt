package com.pawcare.app.util

import com.pawcare.app.data.model.ServiceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ValidatorsTest {

    @Test
    fun testValidateEmail() {
        assertFalse(Validators.validateEmail("").isValid)
        assertEquals("Email tidak boleh kosong", Validators.validateEmail("").errorMessage)

        assertFalse(Validators.validateEmail("invalid-email").isValid)
        assertFalse(Validators.validateEmail("test@").isValid)
        assertFalse(Validators.validateEmail("@domain.com").isValid)

        assertTrue(Validators.validateEmail("owner@pawcare.com").isValid)
        assertTrue(Validators.validateEmail("doctor.sarah+clinic@gmail.com").isValid)
    }

    @Test
    fun testValidatePassword() {
        assertFalse(Validators.validatePassword("").isValid)
        assertEquals("Password tidak boleh kosong", Validators.validatePassword("").errorMessage)

        assertFalse(Validators.validatePassword("12345").isValid)
        assertEquals("Password minimal 6 karakter", Validators.validatePassword("12345").errorMessage)

        assertTrue(Validators.validatePassword("123456").isValid)
        assertTrue(Validators.validatePassword("secure_password!").isValid)
    }

    @Test
    fun testValidateConfirmPassword() {
        assertFalse(Validators.validateConfirmPassword("secret123", "").isValid)
        assertFalse(Validators.validateConfirmPassword("secret123", "secret999").isValid)
        assertEquals("Konfirmasi password tidak cocok", Validators.validateConfirmPassword("secret123", "secret999").errorMessage)

        assertTrue(Validators.validateConfirmPassword("secret123", "secret123").isValid)
    }

    @Test
    fun testValidatePet() {
        // Blank name
        val blankName = Validators.validatePet("", "Persia", "12", "3.5")
        assertFalse(blankName.isValid)
        assertEquals("Nama hewan tidak boleh kosong", blankName.nameError)

        // Blank breed
        val blankBreed = Validators.validatePet("Mochi", "", "12", "3.5")
        assertFalse(blankBreed.isValid)
        assertEquals("Ras hewan tidak boleh kosong", blankBreed.breedError)

        // Invalid age
        val invalidAge = Validators.validatePet("Mochi", "Persia", "0", "3.5")
        assertFalse(invalidAge.isValid)
        assertEquals("Umur harus antara 1 sampai 360 bulan", invalidAge.ageError)

        val nonNumAge = Validators.validatePet("Mochi", "Persia", "dua belas", "3.5")
        assertFalse(nonNumAge.isValid)

        // Invalid weight
        val invalidWeight = Validators.validatePet("Mochi", "Persia", "12", "0.0")
        assertFalse(invalidWeight.isValid)
        assertEquals("Berat harus antara 0.1 sampai 150.0 kg", invalidWeight.weightError)

        // Valid pet
        val validPet = Validators.validatePet("Mochi", "Persia", "24", "4.2")
        assertTrue(validPet.isValid)
        assertEquals(24, validPet.ageMonths)
        assertEquals(4.2, validPet.weightKg, 0.001)
    }

    @Test
    fun testValidateBooking_DateNotBeforeToday() {
        val now = LocalDateTime.of(2026, 11, 12, 11, 0)
        val pastDate = LocalDate.of(2026, 11, 11)

        val result = Validators.validateBooking(
            petId = "pet-1",
            serviceType = ServiceType.VACCINATION,
            date = pastDate,
            timeSlot = "14:00",
            now = now
        )

        assertFalse(result.isValid)
        assertEquals("Tanggal janji temu tidak boleh sebelum hari ini", result.dateError)
    }

    @Test
    fun testValidateBooking_PastHourOnTodayDisabled() {
        val now = LocalDateTime.of(2026, 11, 12, 11, 30) // 11:30 WIB
        val today = LocalDate.of(2026, 11, 12)

        // Slot 10:00 is in the past
        val pastSlotResult = Validators.validateBooking(
            petId = "pet-1",
            serviceType = ServiceType.CHECKUP,
            date = today,
            timeSlot = "10:00",
            now = now
        )
        assertFalse(pastSlotResult.isValid)
        assertEquals("Slot waktu sudah terlewat untuk hari ini", pastSlotResult.slotError)

        // Slot 11:00 is currently active / passed
        val currentSlotResult = Validators.validateBooking(
            petId = "pet-1",
            serviceType = ServiceType.CHECKUP,
            date = today,
            timeSlot = "11:00",
            now = now
        )
        assertFalse(currentSlotResult.isValid)

        // Slot 14:00 today is valid
        val futureSlotToday = Validators.validateBooking(
            petId = "pet-1",
            serviceType = ServiceType.CHECKUP,
            date = today,
            timeSlot = "14:00",
            now = now
        )
        assertTrue(futureSlotToday.isValid)

        // Slot 09:00 tomorrow is valid
        val tomorrow = LocalDate.of(2026, 11, 13)
        val tomorrowBooking = Validators.validateBooking(
            petId = "pet-1",
            serviceType = ServiceType.CHECKUP,
            date = tomorrow,
            timeSlot = "09:00",
            now = now
        )
        assertTrue(tomorrowBooking.isValid)
    }

    @Test
    fun testValidateMedicalRecord_VaccineNameRequiredForVaccination() {
        val visitDate = LocalDate.of(2026, 11, 12)

        // Missing vaccine name for VACCINATION
        val missingVaccine = Validators.validateMedicalRecord(
            diagnosis = "Kucing sehat untuk divaksin",
            treatment = "Pemberian vaksin tricat",
            serviceType = ServiceType.VACCINATION,
            vaccineName = "",
            visitDate = visitDate,
            nextVisitDate = null
        )
        assertFalse(missingVaccine.isValid)
        assertEquals("Nama vaksin wajib diisi untuk layanan vaksinasi", missingVaccine.vaccineError)

        // Valid vaccination record
        val validVaccine = Validators.validateMedicalRecord(
            diagnosis = "Kucing sehat untuk divaksin",
            treatment = "Injeksi SC",
            serviceType = ServiceType.VACCINATION,
            vaccineName = "Tricat Trio Felocell",
            visitDate = visitDate,
            nextVisitDate = LocalDate.of(2026, 12, 12)
        )
        assertTrue(validVaccine.isValid)

        // Checkup does not require vaccine name
        val checkup = Validators.validateMedicalRecord(
            diagnosis = "Flu kucing ringan",
            treatment = "Antibiotik dan vitamin",
            serviceType = ServiceType.CHECKUP,
            vaccineName = null,
            visitDate = visitDate,
            nextVisitDate = null
        )
        assertTrue(checkup.isValid)
    }

    @Test
    fun testValidateMedicalRecord_NextVisitDateNotBeforeVisitDate() {
        val visitDate = LocalDate.of(2026, 11, 12)
        val pastControlDate = LocalDate.of(2026, 11, 10)

        val invalidControl = Validators.validateMedicalRecord(
            diagnosis = "Pemeriksaan telinga",
            treatment = "Pembersihan earmite",
            serviceType = ServiceType.CHECKUP,
            vaccineName = null,
            visitDate = visitDate,
            nextVisitDate = pastControlDate
        )
        assertFalse(invalidControl.isValid)
        assertEquals("Jadwal kontrol berikutnya tidak boleh sebelum tanggal periksa", invalidControl.nextVisitDateError)
    }

    @Test
    fun testValidateRejectionReason() {
        assertFalse(Validators.validateRejectionReason("").isValid)
        assertFalse(Validators.validateRejectionReason("   ").isValid)
        assertEquals("Alasan pembatalan/penolakan wajib diisi", Validators.validateRejectionReason(" ").errorMessage)

        assertTrue(Validators.validateRejectionReason("Dokter sedang operasi darurat pada jam tersebut").isValid)
    }
}
