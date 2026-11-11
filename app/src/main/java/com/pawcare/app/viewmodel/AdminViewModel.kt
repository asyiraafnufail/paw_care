package com.pawcare.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pawcare.app.data.model.Appointment
import com.pawcare.app.data.model.AppointmentStatus
import com.pawcare.app.data.model.MedicalRecord
import com.pawcare.app.data.repository.AppointmentRepository
import com.pawcare.app.data.repository.MedicalRecordRepository
import com.pawcare.app.util.UiState
import com.pawcare.app.util.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class AdminViewModel(
    private val appointmentRepository: AppointmentRepository,
    private val medicalRecordRepository: MedicalRecordRepository
) : ViewModel() {

    private val _isLoadingData = MutableStateFlow(true)
    val isLoadingData: StateFlow<Boolean> = _isLoadingData.asStateFlow()

    private val allPendingAppointments: StateFlow<List<Appointment>> =
        appointmentRepository.getPendingAppointments()
            .onEach { _isLoadingData.value = false }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val availableQueueDates: StateFlow<List<String>> = allPendingAppointments
        .map { list -> list.map { it.date }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedDateFilter = MutableStateFlow<String?>(null)
    val selectedDateFilter: StateFlow<String?> = _selectedDateFilter.asStateFlow()

    val filteredPendingAppointments: StateFlow<List<Appointment>> = combine(
        allPendingAppointments,
        _selectedDateFilter
    ) { list, dateFilter ->
        if (dateFilter.isNullOrBlank()) list
        else list.filter { it.date == dateFilter }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val confirmedAppointments: StateFlow<List<Appointment>> =
        appointmentRepository.getConfirmedAppointments()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val allArchiveAppointments: StateFlow<List<Appointment>> =
        appointmentRepository.getArchiveAppointments()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _archiveStatusFilter = MutableStateFlow<AppointmentStatus?>(null)
    val archiveStatusFilter: StateFlow<AppointmentStatus?> = _archiveStatusFilter.asStateFlow()

    val filteredArchiveAppointments: StateFlow<List<Appointment>> = combine(
        allArchiveAppointments,
        _searchQuery,
        _archiveStatusFilter
    ) { list, query, statusFilter ->
        val q = query.trim().lowercase()
        list.filter { apt ->
            val matchesStatus = statusFilter == null || apt.status == statusFilter.name
            val matchesQuery = q.isEmpty() ||
                apt.petName.lowercase().contains(q) ||
                apt.ownerEmail.lowercase().contains(q)
            matchesStatus && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedAppointment = MutableStateFlow<UiState<Appointment?>>(UiState.Loading)
    val selectedAppointment: StateFlow<UiState<Appointment?>> = _selectedAppointment.asStateFlow()

    fun loadAppointment(appointmentId: String) {
        val cached = confirmedAppointments.value.find { it.appointmentId == appointmentId }
        if (cached != null) {
            _selectedAppointment.value = UiState.Success(cached)
            return
        }
        viewModelScope.launch {
            _selectedAppointment.value = UiState.Loading
            appointmentRepository.getAppointment(appointmentId)
                .onSuccess { _selectedAppointment.value = UiState.Success(it) }
                .onFailure { _selectedAppointment.value = UiState.Error(it.message ?: "Gagal memuat data pasien") }
        }
    }

    fun setDateFilter(date: String?) {
        _selectedDateFilter.value = date
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setArchiveStatusFilter(status: AppointmentStatus?) {
        _archiveStatusFilter.value = status
    }

    fun clearMessages() {
        _actionMessage.value = null
        _errorMessage.value = null
    }

    fun confirmAppointment(appointmentId: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            appointmentRepository.confirmAppointment(appointmentId)
                .onSuccess {
                    _isSubmitting.value = false
                    _actionMessage.value = "Janji temu dikonfirmasi"
                }
                .onFailure { err ->
                    _isSubmitting.value = false
                    _errorMessage.value = err.message ?: "Gagal mengkonfirmasi janji temu"
                }
        }
    }

    fun rejectAppointment(appointmentId: String, slotKey: String, reason: String) {
        val validation = Validators.validateRejectionReason(reason)
        if (!validation.isValid) {
            _errorMessage.value = validation.errorMessage
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            appointmentRepository.cancelAppointmentByAdmin(appointmentId, slotKey, reason)
                .onSuccess {
                    _isSubmitting.value = false
                    _actionMessage.value = "Janji temu ditolak"
                }
                .onFailure { err ->
                    _isSubmitting.value = false
                    _errorMessage.value = err.message ?: "Gagal menolak janji temu"
                }
        }
    }

    fun submitMedicalRecord(
        appointment: Appointment,
        diagnosis: String,
        treatment: String,
        vaccineName: String?,
        notes: String,
        nextVisitDate: String?,
        onSuccess: () -> Unit
    ) {
        val visitDate = runCatching { LocalDate.parse(appointment.date) }.getOrElse { LocalDate.now() }
        val parsedNextVisit = if (nextVisitDate.isNullOrBlank()) {
            null
        } else {
            runCatching { LocalDate.parse(nextVisitDate.trim()) }.getOrElse {
                _errorMessage.value = "Format tanggal kontrol berikutnya tidak valid"
                return
            }
        }

        val validation = Validators.validateMedicalRecord(
            diagnosis = diagnosis,
            treatment = treatment,
            serviceType = appointment.serviceTypeEnum,
            vaccineName = vaccineName,
            visitDate = visitDate,
            nextVisitDate = parsedNextVisit
        )

        if (!validation.isValid) {
            _errorMessage.value = listOfNotNull(
                validation.diagnosisError,
                validation.treatmentError,
                validation.vaccineError,
                validation.nextVisitDateError
            ).firstOrNull()
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            val record = MedicalRecord(
                appointmentId = appointment.appointmentId,
                petId = appointment.petId,
                petName = appointment.petName,
                ownerId = appointment.ownerId,
                ownerEmail = appointment.ownerEmail,
                serviceType = appointment.serviceType,
                diagnosis = diagnosis.trim(),
                treatment = treatment.trim(),
                vaccineName = vaccineName?.trim()?.takeIf { it.isNotEmpty() },
                doctorNotes = notes.trim(),
                visitDate = visitDate.toString(),
                nextVisitDate = parsedNextVisit?.toString()
            )
            medicalRecordRepository.submitMedicalRecord(record)
                .onSuccess {
                    _isSubmitting.value = false
                    _actionMessage.value = "Rekam medis berhasil disimpan"
                    onSuccess()
                }
                .onFailure { err ->
                    _isSubmitting.value = false
                    _errorMessage.value = err.message ?: "Gagal menyimpan rekam medis"
                }
        }
    }
}
