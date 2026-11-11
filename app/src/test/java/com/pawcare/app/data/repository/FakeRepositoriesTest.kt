package com.pawcare.app.data.repository

import com.pawcare.app.data.model.Appointment
import com.pawcare.app.data.model.AppointmentStatus
import com.pawcare.app.data.model.MedicalRecord
import com.pawcare.app.data.model.Pet
import com.pawcare.app.data.model.PetSpecies
import com.pawcare.app.data.model.Role
import com.pawcare.app.data.model.ServiceType
import com.pawcare.app.data.model.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeRepositoriesTest {

    private lateinit var authRepo: FakeAuthRepository
    private lateinit var petRepo: FakePetRepository
    private lateinit var appointmentRepo: FakeAppointmentRepository
    private lateinit var medicalRepo: FakeMedicalRecordRepository

    @Before
    fun setUp() {
        authRepo = FakeAuthRepository()
        petRepo = FakePetRepository()
        appointmentRepo = FakeAppointmentRepository()
        medicalRepo = FakeMedicalRecordRepository(appointmentRepo)
        petRepo.appointmentRepository = appointmentRepo
    }

    @Test
    fun testAuth_SignUpForcesOwnerRole() = runTest {
        val result = authRepo.signUp("owner@pawcare.com", "secret123")
        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("owner", user?.role)
        assertEquals(Role.OWNER, user?.roleEnum)
    }

    @Test
    fun testAppointment_SlotLockAndDoubleBookingPrevention() = runTest {
        val apt1 = Appointment(
            appointmentId = "apt-1",
            ownerId = "owner-1",
            ownerEmail = "owner1@mail.com",
            petId = "pet-1",
            petName = "Mochi",
            serviceType = ServiceType.VACCINATION.name,
            date = "2026-11-15",
            timeSlot = "10:00",
            slotKey = "2026-11-15_10:00",
            status = AppointmentStatus.PENDING.name
        )

        // First booking succeeds
        val result1 = appointmentRepo.createBooking(apt1)
        assertTrue(result1.isSuccess)

        val slots = appointmentRepo.getBookedSlots("2026-11-15").first()
        assertTrue(slots.contains("10:00"))

        // Second booking on the same slot fails with conflict
        val apt2 = Appointment(
            appointmentId = "apt-2",
            ownerId = "owner-2",
            ownerEmail = "owner2@mail.com",
            petId = "pet-2",
            petName = "Rocky",
            serviceType = ServiceType.CHECKUP.name,
            date = "2026-11-15",
            timeSlot = "10:00",
            slotKey = "2026-11-15_10:00",
            status = AppointmentStatus.PENDING.name
        )
        val result2 = appointmentRepo.createBooking(apt2)
        assertFalse(result2.isSuccess)
        assertTrue(result2.exceptionOrNull()?.message?.contains("sudah dipesan") == true)
    }

    @Test
    fun testAppointment_CancellationFreesSlot() = runTest {
        val apt = Appointment(
            appointmentId = "apt-1",
            ownerId = "owner-1",
            date = "2026-11-15",
            timeSlot = "10:00",
            slotKey = "2026-11-15_10:00",
            status = AppointmentStatus.PENDING.name
        )
        appointmentRepo.createBooking(apt)

        // Owner cancels
        val cancelResult = appointmentRepo.cancelOwnerBooking("apt-1", "2026-11-15_10:00")
        assertTrue(cancelResult.isSuccess)

        // Slot is freed
        val slots = appointmentRepo.getBookedSlots("2026-11-15").first()
        assertFalse(slots.contains("10:00"))

        // Now another booking can take this slot
        val aptNew = apt.copy(appointmentId = "apt-new", ownerId = "owner-2")
        val rebookResult = appointmentRepo.createBooking(aptNew)
        assertTrue(rebookResult.isSuccess)
    }

    @Test
    fun testMedicalRecord_SubmissionCompletesAppointment() = runTest {
        val apt = Appointment(
            appointmentId = "apt-1",
            ownerId = "owner-1",
            petId = "pet-1",
            serviceType = ServiceType.VACCINATION.name,
            status = AppointmentStatus.CONFIRMED.name
        )
        appointmentRepo.saveAppointmentDirect(apt)

        val record = MedicalRecord(
            appointmentId = "apt-1",
            petId = "pet-1",
            petName = "Mochi",
            ownerId = "owner-1",
            serviceType = ServiceType.VACCINATION.name,
            diagnosis = "Kucing sehat",
            treatment = "Vaksin Felocell",
            vaccineName = "Felocell 4",
            visitDate = "2026-11-15"
        )

        val submitResult = medicalRepo.submitMedicalRecord(record)
        assertTrue(submitResult.isSuccess)

        // Appointment status transitioned to COMPLETED
        val updatedApt = appointmentRepo.getAppointmentById("apt-1")
        assertEquals(AppointmentStatus.COMPLETED.name, updatedApt?.status)

        // Record is retrievable by appointmentId
        val fetchedRecord = medicalRepo.getRecordForAppointment("apt-1").getOrNull()
        assertNotNull(fetchedRecord)
        assertEquals("Felocell 4", fetchedRecord?.vaccineName)
    }

    @Test
    fun testPet_DeletionBlockedWhenActiveAppointmentExists() = runTest {
        val pet = Pet(petId = "pet-1", ownerId = "owner-1", name = "Mochi", species = PetSpecies.CAT.name)
        petRepo.savePet(pet)

        val apt = Appointment(
            appointmentId = "apt-1",
            ownerId = "owner-1",
            petId = "pet-1",
            status = AppointmentStatus.CONFIRMED.name
        )
        appointmentRepo.saveAppointmentDirect(apt)

        assertTrue(petRepo.hasActiveAppointments("pet-1"))
        val deleteResult = petRepo.deletePet("pet-1")
        assertFalse(deleteResult.isSuccess)
        assertTrue(deleteResult.exceptionOrNull()?.message?.contains("janji temu aktif") == true)
    }
}
