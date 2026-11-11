package com.pawcare.app.data.model

data class BookedSlot(
    val appointmentId: String = "",
    val ownerId: String = "",
    val date: String = "",
    val timeSlot: String = ""
) {
    val slotKey: String
        get() = "${date}_${timeSlot}"
}
