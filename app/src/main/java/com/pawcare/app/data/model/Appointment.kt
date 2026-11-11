package com.pawcare.app.data.model

import com.google.firebase.Timestamp

data class Appointment(
    val appointmentId: String = "",
    val ownerId: String = "",
    val ownerEmail: String = "",
    val petId: String = "",
    val petName: String = "",
    val petSpecies: String = PetSpecies.CAT.name,
    val serviceType: String = ServiceType.CHECKUP.name,
    val date: String = "", // Format: "YYYY-MM-DD"
    val timeSlot: String = "", // e.g. "09:00"
    val slotKey: String = "", // "${date}_${timeSlot}"
    val notes: String = "",
    val status: String = AppointmentStatus.PENDING.name,
    val rejectionReason: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val statusEnum: AppointmentStatus
        get() = AppointmentStatus.fromValue(status)

    val serviceTypeEnum: ServiceType
        get() = ServiceType.fromValue(serviceType)

    val petSpeciesEnum: PetSpecies
        get() = PetSpecies.fromValue(petSpecies)

    val canBeCancelledByOwner: Boolean
        get() = statusEnum == AppointmentStatus.PENDING
}
