package com.pawcare.app.data.model

import com.google.firebase.Timestamp

data class MedicalRecord(
    val appointmentId: String = "",
    val petId: String = "",
    val petName: String = "",
    val ownerId: String = "",
    val ownerEmail: String = "",
    val serviceType: String = ServiceType.CHECKUP.name,
    val diagnosis: String = "",
    val treatment: String = "",
    val vaccineName: String? = null,
    val doctorNotes: String = "",
    val visitDate: String = "", // Format: "YYYY-MM-DD"
    val nextVisitDate: String? = null, // Format: "YYYY-MM-DD"
    val createdAt: Timestamp? = null
) {
    val serviceTypeEnum: ServiceType
        get() = ServiceType.fromValue(serviceType)
}
