package com.pawcare.app.viewmodel

import com.pawcare.app.data.model.MedicalRecord
import com.pawcare.app.data.model.ServiceType
import com.pawcare.app.data.repository.FakeAppointmentRepository
import com.pawcare.app.data.repository.FakeMedicalRecordRepository
import com.pawcare.app.util.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MedicalRecordDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var appointmentRepo: FakeAppointmentRepository
    private lateinit var medicalRepo: FakeMedicalRecordRepository
    private lateinit var viewModel: MedicalRecordDetailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        appointmentRepo = FakeAppointmentRepository()
        medicalRepo = FakeMedicalRecordRepository(appointmentRepo)
        viewModel = MedicalRecordDetailViewModel(medicalRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadRecord_NotFound_ReturnsNullData() = runTest {
        viewModel.loadRecord("non-existent")
        val state = viewModel.recordState.value
        assertTrue(state is UiState.Success)
        assertNull((state as UiState.Success).data)
    }

    @Test
    fun testLoadRecord_Found_ReturnsRecord() = runTest {
        val record = MedicalRecord(
            appointmentId = "apt-100",
            petId = "pet-1",
            petName = "Oreo",
            ownerId = "owner-1",
            serviceType = ServiceType.CHECKUP.name,
            diagnosis = "Flu ringan",
            treatment = "Pemberian vitamin dan antibiotik",
            visitDate = "2026-11-15"
        )
        medicalRepo.submitMedicalRecord(record)

        viewModel.loadRecord("apt-100")
        val state = viewModel.recordState.value
        assertTrue(state is UiState.Success)
        assertEquals("Flu ringan", (state as UiState.Success).data?.diagnosis)
    }
}
