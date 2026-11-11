package com.pawcare.app.data.model

enum class Role(val value: String) {
    OWNER("owner"),
    ADMIN("admin");

    companion object {
        fun fromValue(value: String?): Role? = when (value?.lowercase()) {
            "admin" -> ADMIN
            "owner" -> OWNER
            else -> null
        }
    }
}

enum class PetSpecies(val displayName: String) {
    CAT("Kucing"),
    DOG("Anjing"),
    RABBIT("Kelinci"),
    BIRD("Burung"),
    OTHER("Lainnya");

    companion object {
        fun fromValue(value: String?): PetSpecies = try {
            if (value != null) valueOf(value.uppercase()) else CAT
        } catch (_: Exception) {
            CAT
        }
    }
}

enum class ServiceType(val displayName: String, val description: String) {
    VACCINATION("Vaksinasi", "Pemberian vaksin inti/tambahan untuk kekebalan tubuh"),
    CHECKUP("Periksa Umum", "Pemeriksaan kesehatan rutin atau gejala sakit"),
    CONSULTATION("Konsultasi", "Konsultasi gizi, perilaku, dan perawatan"),
    STERILIZATION("Sterilisasi", "Tindakan kastrasi / ovariohisterektomi");

    companion object {
        fun fromValue(value: String?): ServiceType = try {
            if (value != null) valueOf(value.uppercase()) else CHECKUP
        } catch (_: Exception) {
            CHECKUP
        }
    }
}

enum class AppointmentStatus(val displayName: String) {
    PENDING("Menunggu Konfirmasi"),
    CONFIRMED("Disetujui"),
    COMPLETED("Selesai"),
    CANCELLED("Dibatalkan");

    companion object {
        fun fromValue(value: String?): AppointmentStatus = try {
            if (value != null) valueOf(value.uppercase()) else PENDING
        } catch (_: Exception) {
            PENDING
        }
    }
}
