package com.pawcare.app.util

import com.pawcare.app.data.model.ServiceType
import java.time.LocalDate
import java.time.LocalDateTime

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

data class PetValidationResult(
    val nameError: String? = null,
    val breedError: String? = null,
    val ageError: String? = null,
    val weightError: String? = null,
    val ageMonths: Int = 0,
    val weightKg: Double = 0.0
) {
    val isValid: Boolean
        get() = nameError == null && breedError == null && ageError == null && weightError == null
}

data class BookingValidationResult(
    val petError: String? = null,
    val serviceError: String? = null,
    val dateError: String? = null,
    val slotError: String? = null
) {
    val isValid: Boolean
        get() = petError == null && serviceError == null && dateError == null && slotError == null
}

data class MedicalRecordValidationResult(
    val diagnosisError: String? = null,
    val treatmentError: String? = null,
    val vaccineError: String? = null,
    val nextVisitDateError: String? = null
) {
    val isValid: Boolean
        get() = diagnosisError == null && treatmentError == null && vaccineError == null && nextVisitDateError == null
}

object Validators {

    val CLINIC_TIME_SLOTS = listOf(
        "09:00", "10:00", "11:00", "12:00",
        "13:00", "14:00", "15:00", "16:00"
    )

    private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

    fun validateEmail(email: String): ValidationResult {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> ValidationResult(false, "Email tidak boleh kosong")
            !EMAIL_REGEX.matches(trimmed) -> ValidationResult(false, "Format email tidak valid")
            else -> ValidationResult(true)
        }
    }

    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isEmpty() -> ValidationResult(false, "Password tidak boleh kosong")
            password.length < 6 -> ValidationResult(false, "Password minimal 6 karakter")
            else -> ValidationResult(true)
        }
    }

    fun validateConfirmPassword(password: String, confirm: String): ValidationResult {
        return when {
            confirm.isEmpty() -> ValidationResult(false, "Konfirmasi password tidak boleh kosong")
            password != confirm -> ValidationResult(false, "Konfirmasi password tidak cocok")
            else -> ValidationResult(true)
        }
    }

    fun validatePet(
        name: String,
        breed: String,
        ageMonthsText: String,
        weightKgText: String
    ): PetValidationResult {
        val trimmedName = name.trim()
        val trimmedBreed = breed.trim()
        val parsedAge = ageMonthsText.trim().toIntOrNull()
        val parsedWeight = weightKgText.trim().replace(',', '.').toDoubleOrNull()

        val nameError = when {
            trimmedName.isEmpty() -> "Nama hewan tidak boleh kosong"
            trimmedName.length > 50 -> "Nama hewan maksimal 50 karakter"
            else -> null
        }

        val breedError = when {
            trimmedBreed.isEmpty() -> "Ras hewan tidak boleh kosong"
            trimmedBreed.length > 50 -> "Ras hewan maksimal 50 karakter"
            else -> null
        }

        val ageError = if (parsedAge == null || parsedAge !in 1..360) {
            "Umur harus antara 1 sampai 360 bulan"
        } else {
            null
        }

        val weightError = if (parsedWeight == null || parsedWeight < 0.1 || parsedWeight > 150.0) {
            "Berat harus antara 0.1 sampai 150.0 kg"
        } else {
            null
        }

        return PetValidationResult(
            nameError = nameError,
            breedError = breedError,
            ageError = ageError,
            weightError = weightError,
            ageMonths = parsedAge ?: 0,
            weightKg = parsedWeight ?: 0.0
        )
    }

    fun parseSlotHour(timeSlot: String): Int? =
        timeSlot.substringBefore(":").toIntOrNull()

    fun isSlotPastForDate(
        date: LocalDate,
        timeSlot: String,
        now: LocalDateTime = LocalDateTime.now()
    ): Boolean {
        val today = now.toLocalDate()
        if (date.isBefore(today)) return true
        if (date.isAfter(today)) return false
        val slotHour = parseSlotHour(timeSlot) ?: return true
        return slotHour <= now.hour
    }

    fun validateBooking(
        petId: String,
        serviceType: ServiceType?,
        date: LocalDate?,
        timeSlot: String?,
        now: LocalDateTime = LocalDateTime.now()
    ): BookingValidationResult {
        val petError = if (petId.isBlank()) "Pilih hewan peliharaan" else null
        val serviceError = if (serviceType == null) "Pilih jenis layanan" else null
        val today = now.toLocalDate()

        val dateError = when {
            date == null -> "Pilih tanggal janji temu"
            date.isBefore(today) -> "Tanggal janji temu tidak boleh sebelum hari ini"
            else -> null
        }

        val slotError = when {
            timeSlot.isNullOrBlank() -> "Pilih slot waktu"
            timeSlot !in CLINIC_TIME_SLOTS -> "Slot waktu tidak valid"
            date != null && !date.isBefore(today) && isSlotPastForDate(date, timeSlot, now) ->
                "Slot waktu sudah terlewat untuk hari ini"
            else -> null
        }

        return BookingValidationResult(
            petError = petError,
            serviceError = serviceError,
            dateError = dateError,
            slotError = slotError
        )
    }

    fun validateMedicalRecord(
        diagnosis: String,
        treatment: String,
        serviceType: ServiceType,
        vaccineName: String?,
        visitDate: LocalDate,
        nextVisitDate: LocalDate?
    ): MedicalRecordValidationResult {
        val diagnosisError = if (diagnosis.trim().isEmpty()) {
            "Diagnosa tidak boleh kosong"
        } else {
            null
        }

        val treatmentError = if (treatment.trim().isEmpty()) {
            "Tindakan tidak boleh kosong"
        } else {
            null
        }

        val vaccineError = if (serviceType == ServiceType.VACCINATION && vaccineName.isNullOrBlank()) {
            "Nama vaksin wajib diisi untuk layanan vaksinasi"
        } else {
            null
        }

        val nextVisitDateError = if (nextVisitDate != null && nextVisitDate.isBefore(visitDate)) {
            "Jadwal kontrol berikutnya tidak boleh sebelum tanggal periksa"
        } else {
            null
        }

        return MedicalRecordValidationResult(
            diagnosisError = diagnosisError,
            treatmentError = treatmentError,
            vaccineError = vaccineError,
            nextVisitDateError = nextVisitDateError
        )
    }

    fun validateRejectionReason(reason: String): ValidationResult {
        return if (reason.trim().isEmpty()) {
            ValidationResult(false, "Alasan pembatalan/penolakan wajib diisi")
        } else {
            ValidationResult(true)
        }
    }
}
