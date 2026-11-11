package com.pawcare.app.data.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val email: String = "",
    val role: String = Role.OWNER.value,
    val createdAt: Timestamp? = null
) {
    val roleEnum: Role?
        get() = Role.fromValue(role)
}
