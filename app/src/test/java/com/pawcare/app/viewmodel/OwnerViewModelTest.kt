package com.pawcare.app.viewmodel

import com.pawcare.app.data.model.Appointment
import com.pawcare.app.data.model.AppointmentStatus
import com.pawcare.app.data.model.Pet
import com.pawcare.app.data.model.PetSpecies
import com.pawcare.app.data.model.ServiceType
import com.pawcare.app.data.model.User
import com.pawcare.app.data.repository.FakeAppointmentRepository
import com.pawcare.app.data.repository.FakeAuthRepository
import com.pawcare.app.data.repository.FakeMedicalRecordRepository
import com.pawcare.app.data.repository.FakePetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class OwnerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var authRepo: FakeAuthRepository
    private lateinit var petRepo: FakePetRepository
    private lateinit var appointmentRepo: FakeAppointmentRepository
    private lateinit var medicalRepo: FakeMedicalRecordRepository
    private lateinit var viewModel: OwnerViewModel

    private val currentUser = User(uid = "owner-1", email = "owner1@mail.com", role = "owner")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepo = FakeAuthRepository(currentUser)
        petRepo = FakePetRepository()
        appointmentRepo = FakeAppointmentRepository()
        medicalRepo = FakeMedicalRecordRepository(appointmentRepo)
        petRepo.appointmentRepository = appointmentRepo

        viewModel = OwnerViewModel(
            ownerId = currentUser.uid,
            ownerEmail = currentUser.email,
            petRepository = petRepo,
            appointmentRepository = appointmentRepo,
            medicalRecordRepository = medicalRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDeletePet_BlockedWhenActiveAppointmentExists() = runTest {
        petRepo.savePet(Pet(petId = "pet-1", ownerId = currentUser.uid, name = "Mochi", species = PetSpecies.CAT.name))
        appointmentRepo.saveAppointmentDirect(
            Appointment(
                appointmentId = "apt-1",
                ownerId = currentUser.uid,
                petId = "pet-1",
                status = AppointmentStatus.PENDING.name
            )
        )

        var deleteSuccess = false
        viewModel.deletePet("pet-1") { deleteSuccess = true }

        assertFalse(deleteSuccess)
        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value?.contains("janji temu aktif") == true)
    }

    @Test
    fun testCreateBooking_RejectsPastDate() = runTest {
        var booked = false
        val pastDate = LocalDate.now().minusDays(1)

        viewModel.createBooking(
            pet = Pet(petId = "p1", name = "Mochi"),
            serviceType = ServiceType.CHECKUP,
            date = pastDate,
            timeSlot = "10:00",
            notes = "batuk",
            onSuccess = { booked = true }
        )

        assertFalse(booked)
        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value?.contains("tidak boleh sebelum hari ini") == true)
    }

    @Test
    fun testCreateBooking_SuccessAndSlotReservation() = runTest {
        var booked = false
        val tomorrow = LocalDate.now().plusDays(1)

        viewModel.createBooking(
            pet = Pet(petId = "p1", name = "Mochi", species = PetSpecies.CAT.name),
            serviceType = ServiceType.VACCINATION,
            date = tomorrow,
            timeSlot = "10:00",
            notes = "vaksin rabies",
            onSuccess = { booked = true }
        )

        assertTrue(booked)
        val activeList = viewModel.activeAppointments.value
        assertEquals(1, activeList.size)
        assertEquals("Mochi", activeList.first().petName)
    }

    @Test
    fun testCancelBooking_CancelsPendingAndFreesSlot() = runTest {
        val tomorrow = LocalDate.now().plusDays(1)
        val apt = Appointment(
            appointmentId = "apt-cancel",
            ownerId = currentUser.uid,
            date = tomorrow.toString(),
            timeSlot = "14:00",
            slotKey = "${tomorrow}_14:00",
            status = AppointmentStatus.PENDING.name
        )
        appointmentRepo.createBooking(apt)

        viewModel.cancelBooking("apt-cancel", "${tomorrow}_14:00")
        val activeAfter = viewModel.activeAppointments.value
        assertTrue(activeAfter.none { it.appointmentId == "apt-cancel" })
    }
}
