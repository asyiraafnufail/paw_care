package com.pawcare.app.viewmodel

import com.pawcare.app.data.model.Appointment
import com.pawcare.app.data.model.AppointmentStatus
import com.pawcare.app.data.model.PetSpecies
import com.pawcare.app.data.model.ServiceType
import com.pawcare.app.data.repository.FakeAppointmentRepository
import com.pawcare.app.data.repository.FakeMedicalRecordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var appointmentRepo: FakeAppointmentRepository
    private lateinit var medicalRecordRepo: FakeMedicalRecordRepository
    private lateinit var viewModel: AdminViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        appointmentRepo = FakeAppointmentRepository()
        medicalRecordRepo = FakeMedicalRecordRepository(appointmentRepo)
        viewModel = AdminViewModel(
            appointmentRepository = appointmentRepo,
            medicalRecordRepository = medicalRecordRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testApproveBooking_TransitionsToConfirmed() = runTest {
        val apt = Appointment(
            appointmentId = "apt-1",
            ownerId = "owner-1",
            ownerEmail = "owner1@mail.com",
            petId = "pet-1",
            petName = "Luna",
            petSpecies = PetSpecies.CAT.name,
            serviceType = ServiceType.CHECKUP.name,
            date = "2026-11-20",
            timeSlot = "10:00",
            slotKey = "2026-11-20_10:00",
            status = AppointmentStatus.PENDING.name
        )
        appointmentRepo.saveAppointmentDirect(apt)

        viewModel.confirmAppointment("apt-1")
        advanceUntilIdle()

        val updated = appointmentRepo.getAppointment("apt-1").getOrNull()
        assertNotNull(updated)
        assertEquals(AppointmentStatus.CONFIRMED.name, updated?.status)
        assertEquals("Janji temu dikonfirmasi", viewModel.actionMessage.value)
    }

    @Test
    fun testRejectBooking_BlankReasonFailsValidation() = runTest {
        val apt = Appointment(
            appointmentId = "apt-1",
            ownerId = "owner-1",
            date = "2026-11-20",
            timeSlot = "10:00",
            slotKey = "2026-11-20_10:00",
            status = AppointmentStatus.PENDING.name
        )
        appointmentRepo.saveAppointmentDirect(apt)

        viewModel.rejectAppointment("apt-1", "2026-11-20_10:00", "   ")
        advanceUntilIdle()

        val notUpdated = appointmentRepo.getAppointment("apt-1").getOrNull()
        assertEquals(AppointmentStatus.PENDING.name, notUpdated?.status)
        assertEquals("Alasan pembatalan/penolakan wajib diisi", viewModel.errorMessage.value)
    }

    @Test
    fun testRejectBooking_ValidReasonCancelsAndFreesSlot() = runTest {
        val apt = Appointment(
            appointmentId = "apt-1",
            ownerId = "owner-1",
            date = "2026-11-20",
            timeSlot = "10:00",
            slotKey = "2026-11-20_10:00",
            status = AppointmentStatus.PENDING.name
        )
        appointmentRepo.createBooking(apt)
        advanceUntilIdle()

        viewModel.rejectAppointment("apt-1", "2026-11-20_10:00", "Dokter sedang ada tindakan darurat")
        advanceUntilIdle()

        val updated = appointmentRepo.getAppointment("apt-1").getOrNull()
        assertEquals(AppointmentStatus.CANCELLED.name, updated?.status)
        assertEquals("Dokter sedang ada tindakan darurat", updated?.rejectionReason)

        val slots = appointmentRepo.getBookedSlots("2026-11-20").first()
        assertFalse(slots.contains("10:00"))
        assertEquals("Janji temu ditolak", viewModel.actionMessage.value)
    }

    @Test
    fun testSubmitMedicalRecord_VaccineValidationRequiresVaccineName() = runTest {
        val apt = Appointment(
            appointmentId = "apt-1",
            ownerId = "owner-1",
            ownerEmail = "owner1@mail.com",
            petId = "pet-1",
            petName = "Luna",
            petSpecies = PetSpecies.CAT.name,
            serviceType = ServiceType.VACCINATION.name,
            date = "2026-11-20",
            timeSlot = "10:00",
            slotKey = "2026-11-20_10:00",
            status = AppointmentStatus.CONFIRMED.name
        )
        appointmentRepo.saveAppointmentDirect(apt)

        var successCalled = false
        viewModel.submitMedicalRecord(
            appointment = apt,
            diagnosis = "Sehat dan siap vaksin",
            treatment = "Injeksi subkutan",
            vaccineName = "", // Empty for vaccination service
            notes = "Istirahat 24 jam",
            nextVisitDate = "2026-12-20",
            onSuccess = { successCalled = true }
        )
        advanceUntilIdle()

        assertFalse(successCalled)
        assertEquals("Nama vaksin wajib diisi untuk layanan vaksinasi", viewModel.errorMessage.value)
    }

    @Test
    fun testSubmitMedicalRecord_CompletesAppointmentAtomically() = runTest {
        val apt = Appointment(
            appointmentId = "apt-1",
            ownerId = "owner-1",
            ownerEmail = "owner1@mail.com",
            petId = "pet-1",
            petName = "Luna",
            petSpecies = PetSpecies.CAT.name,
            serviceType = ServiceType.VACCINATION.name,
            date = "2026-11-20",
            timeSlot = "10:00",
            slotKey = "2026-11-20_10:00",
            status = AppointmentStatus.CONFIRMED.name
        )
        appointmentRepo.saveAppointmentDirect(apt)

        var successCalled = false
        viewModel.submitMedicalRecord(
            appointment = apt,
            diagnosis = "Sehat untuk vaksin",
            treatment = "Pemberian vaksin Rabies dosis 1",
            vaccineName = "Nobivac Rabies",
            notes = "Pantau reaksi alergi 1x24 jam",
            nextVisitDate = "2027-11-20",
            onSuccess = { successCalled = true }
        )
        advanceUntilIdle()

        assertTrue(successCalled)
        assertEquals("Rekam medis berhasil disimpan", viewModel.actionMessage.value)

        // Check record exists
        val recordResult = medicalRecordRepo.getRecordForAppointment("apt-1")
        assertTrue(recordResult.isSuccess)
        val record = recordResult.getOrNull()
        assertNotNull(record)
        assertEquals("Nobivac Rabies", record?.vaccineName)
        assertEquals("Sehat untuk vaksin", record?.diagnosis)

        // Check appointment completed
        val aptUpdated = appointmentRepo.getAppointment("apt-1").getOrNull()
        assertEquals(AppointmentStatus.COMPLETED.name, aptUpdated?.status)
    }

    @Test
    fun testArchiveSearch_FiltersByPetNameOrOwnerEmail() = runTest {
        val apt1 = Appointment(
            appointmentId = "apt-1",
            ownerId = "owner-1",
            ownerEmail = "budi@mail.com",
            petName = "Choco",
            status = AppointmentStatus.COMPLETED.name,
            date = "2026-11-10",
            timeSlot = "09:00"
        )
        val apt2 = Appointment(
            appointmentId = "apt-2",
            ownerId = "owner-2",
            ownerEmail = "siti@mail.com",
            petName = "Snowy",
            status = AppointmentStatus.CANCELLED.name,
            date = "2026-11-12",
            timeSlot = "11:00"
        )
        appointmentRepo.saveAppointmentDirect(apt1)
        appointmentRepo.saveAppointmentDirect(apt2)
        advanceUntilIdle()

        // Search by pet name
        viewModel.setSearchQuery("Choco")
        advanceUntilIdle()
        val filteredByPet = viewModel.filteredArchiveAppointments.first()
        assertEquals(1, filteredByPet.size)
        assertEquals("Choco", filteredByPet[0].petName)

        // Search by email
        viewModel.setSearchQuery("siti@mail.com")
        advanceUntilIdle()
        val filteredByEmail = viewModel.filteredArchiveAppointments.first()
        assertEquals(1, filteredByEmail.size)
        assertEquals("siti@mail.com", filteredByEmail[0].ownerEmail)

        // Empty query shows all
        viewModel.setSearchQuery("")
        advanceUntilIdle()
        val all = viewModel.filteredArchiveAppointments.first()
        assertEquals(2, all.size)
    }

    @Test
    fun testQueueDateFilter_KeepsAvailableDatesWhenFiltered() = runTest {
        val apt1 = Appointment(
            appointmentId = "apt-1",
            petName = "Luna",
            date = "2026-11-20",
            status = AppointmentStatus.PENDING.name
        )
        val apt2 = Appointment(
            appointmentId = "apt-2",
            petName = "Rocky",
            date = "2026-11-21",
            status = AppointmentStatus.PENDING.name
        )
        appointmentRepo.saveAppointmentDirect(apt1)
        appointmentRepo.saveAppointmentDirect(apt2)
        advanceUntilIdle()

        assertEquals(listOf("2026-11-20", "2026-11-21"), viewModel.availableQueueDates.value)

        viewModel.setDateFilter("2026-11-20")
        advanceUntilIdle()

        assertEquals(1, viewModel.filteredPendingAppointments.value.size)
        assertEquals(listOf("2026-11-20", "2026-11-21"), viewModel.availableQueueDates.value)
    }
}
