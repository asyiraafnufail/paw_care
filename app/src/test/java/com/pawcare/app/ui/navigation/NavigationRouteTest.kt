package com.pawcare.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationRouteTest {

    @Test
    fun testAllBaseRoutesAreUnique() {
        val baseRoutes = listOf(
            Screen.Splash.route,
            Screen.Login.route,
            Screen.Register.route,
            Screen.OwnerRoot.route,
            Screen.AdminRoot.route,
            Screen.AddEditPet.routePattern,
            Screen.CreateBooking.route,
            Screen.MedicalRecordDetail.routePattern,
            Screen.InputMedicalRecord.routePattern
        )
        assertEquals(baseRoutes.size, baseRoutes.distinct().size)
    }

    @Test
    fun testArgumentRouteBuilders() {
        assertEquals("add_edit_pet?petId=new", Screen.AddEditPet.createRoute(null))
        assertEquals("add_edit_pet?petId=pet-42", Screen.AddEditPet.createRoute("pet-42"))
        assertEquals("medical_record_detail/apt-99", Screen.MedicalRecordDetail.createRoute("apt-99"))
        assertEquals("input_medical_record/apt-99", Screen.InputMedicalRecord.createRoute("apt-99"))
        assertTrue(Screen.OwnerRoot.route.isNotBlank())
    }
}
