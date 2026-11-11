package com.pawcare.app.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")

    data object OwnerGraph : Screen("owner_graph")
    data object OwnerRoot : Screen("owner_root")

    data object AdminGraph : Screen("admin_graph")
    data object AdminRoot : Screen("admin_root")

    data object AddEditPet : Screen("add_edit_pet?petId={petId}") {
        const val routePattern = "add_edit_pet?petId={petId}"
        fun createRoute(petId: String?): String = "add_edit_pet?petId=${petId ?: "new"}"
    }

    data object CreateBooking : Screen("create_booking")

    data object MedicalRecordDetail : Screen("medical_record_detail/{appointmentId}") {
        const val routePattern = "medical_record_detail/{appointmentId}"
        fun createRoute(appointmentId: String): String = "medical_record_detail/$appointmentId"
    }

    data object InputMedicalRecord : Screen("input_medical_record/{appointmentId}") {
        const val routePattern = "input_medical_record/{appointmentId}"
        fun createRoute(appointmentId: String): String = "input_medical_record/$appointmentId"
    }
}

enum class OwnerTab(val title: String) {
    PETS("Pet Saya"),
    BOOKINGS("Booking"),
    HISTORY("Riwayat")
}

enum class AdminTab(val title: String) {
    QUEUE("Antrean"),
    ACTIVE("Pasien Aktif"),
    ARCHIVE("Arsip")
}
