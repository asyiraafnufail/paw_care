package com.pawcare.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pawcare.app.data.model.Appointment
import com.pawcare.app.data.model.AppointmentStatus
import com.pawcare.app.data.model.MedicalRecord
import com.pawcare.app.data.model.Pet
import com.pawcare.app.data.model.PetSpecies
import com.pawcare.app.data.model.ServiceType
import com.pawcare.app.data.repository.AppointmentRepository
import com.pawcare.app.data.repository.MedicalRecordRepository
import com.pawcare.app.data.repository.PetRepository
import com.pawcare.app.util.UiState
import com.pawcare.app.util.Validators
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class OwnerViewModel(
    private val ownerId: String,
    private val ownerEmail: String,
    private val petRepository: PetRepository,
    private val appointmentRepository: AppointmentRepository,
    private val medicalRecordRepository: MedicalRecordRepository
) : ViewModel() {

    private val _isLoadingData = MutableStateFlow(true)
    val isLoadingData: StateFlow<Boolean> = _isLoadingData.asStateFlow()

    val pets: StateFlow<List<Pet>> = petRepository.getPetsForOwner(ownerId)
        .onEach { _isLoadingData.value = false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val allOwnerAppointments: StateFlow<List<Appointment>> =
        appointmentRepository.getOwnerAppointments(ownerId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeAppointments: StateFlow<List<Appointment>> = allOwnerAppointments
        .map { list ->
            list.filter {
                it.status == AppointmentStatus.PENDING.name || it.status == AppointmentStatus.CONFIRMED.name
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val historyAppointments: StateFlow<List<Appointment>> = allOwnerAppointments
        .map { list ->
            list.filter {
                it.status == AppointmentStatus.COMPLETED.name || it.status == AppointmentStatus.CANCELLED.name
            }.sortedByDescending { it.date }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val medicalRecords: StateFlow<List<MedicalRecord>> = medicalRecordRepository.getRecordsForOwner(ownerId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedBookingDate = MutableStateFlow(LocalDate.now())
    val selectedBookingDate: StateFlow<LocalDate> = _selectedBookingDate.asStateFlow()

    val bookedSlotsForSelectedDate: StateFlow<Set<String>> = _selectedBookingDate
        .flatMapLatest { date -> appointmentRepository.getBookedSlots(date.toString()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _editingPet = MutableStateFlow<UiState<Pet?>>(UiState.Success(null))
    val editingPet: StateFlow<UiState<Pet?>> = _editingPet.asStateFlow()

    fun loadPetForEdit(petId: String?) {
        if (petId == null) {
            _editingPet.value = UiState.Success(null)
            return
        }
        val cached = pets.value.find { it.petId == petId }
        if (cached != null) {
            _editingPet.value = UiState.Success(cached)
            return
        }
        viewModelScope.launch {
            _editingPet.value = UiState.Loading
            petRepository.getPetById(petId)
                .onSuccess { _editingPet.value = UiState.Success(it) }
                .onFailure { _editingPet.value = UiState.Error(it.message ?: "Gagal memuat data hewan") }
        }
    }

    fun selectBookingDate(date: LocalDate) {
        _selectedBookingDate.value = date
    }

    fun clearMessages() {
        _actionMessage.value = null
        _errorMessage.value = null
    }

    fun savePet(
        petId: String?,
        name: String,
        species: PetSpecies,
        breed: String,
        ageMonthsText: String,
        weightKgText: String,
        onSuccess: () -> Unit
    ) {
        val validation = Validators.validatePet(name, breed, ageMonthsText, weightKgText)
        if (!validation.isValid) {
            _errorMessage.value = listOfNotNull(
                validation.nameError,
                validation.breedError,
                validation.ageError,
                validation.weightError
            ).firstOrNull()
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            val pet = Pet(
                petId = petId.orEmpty(),
                ownerId = ownerId,
                name = name.trim(),
                species = species.name,
                breed = breed.trim(),
                ageMonths = validation.ageMonths,
                weightKg = validation.weightKg
            )
            petRepository.savePet(pet)
                .onSuccess {
                    _isSubmitting.value = false
                    _actionMessage.value = if (petId == null) "Hewan berhasil ditambahkan" else "Data hewan diperbarui"
                    onSuccess()
                }
                .onFailure { err ->
                    _isSubmitting.value = false
                    _errorMessage.value = err.message ?: "Gagal menyimpan data hewan"
                }
        }
    }

    fun deletePet(petId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSubmitting.value = true
            petRepository.deletePet(petId)
                .onSuccess {
                    _isSubmitting.value = false
                    _actionMessage.value = "Data hewan dihapus"
                    onSuccess()
                }
                .onFailure { err ->
                    _isSubmitting.value = false
                    _errorMessage.value = err.message ?: "Gagal menghapus hewan"
                }
        }
    }

    fun createBooking(
        pet: Pet?,
        serviceType: ServiceType?,
        date: LocalDate?,
        timeSlot: String?,
        notes: String,
        onSuccess: () -> Unit
    ) {
        val validation = Validators.validateBooking(
            petId = pet?.petId.orEmpty(),
            serviceType = serviceType,
            date = date,
            timeSlot = timeSlot,
            now = LocalDateTime.now()
        )

        if (!validation.isValid) {
            _errorMessage.value = listOfNotNull(
                validation.petError,
                validation.serviceError,
                validation.dateError,
                validation.slotError
            ).firstOrNull()
            return
        }

        val validPet = pet!!
        val validService = serviceType!!
        val validDate = date!!
        val validSlot = timeSlot!!

        viewModelScope.launch {
            _isSubmitting.value = true
            val appointment = Appointment(
                ownerId = ownerId,
                ownerEmail = ownerEmail,
                petId = validPet.petId,
                petName = validPet.name,
                petSpecies = validPet.species,
                serviceType = validService.name,
                date = validDate.toString(),
                timeSlot = validSlot,
                slotKey = "${validDate}_${validSlot}",
                notes = notes.trim().take(300)
            )
            appointmentRepository.createBooking(appointment)
                .onSuccess {
                    _isSubmitting.value = false
                    _actionMessage.value = "Booking berhasil dibuat"
                    onSuccess()
                }
                .onFailure { err ->
                    _isSubmitting.value = false
                    _errorMessage.value = err.message ?: "Gagal membuat janji temu"
                }
        }
    }

    fun cancelBooking(appointmentId: String, slotKey: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            appointmentRepository.cancelOwnerBooking(appointmentId, slotKey)
                .onSuccess {
                    _isSubmitting.value = false
                    _actionMessage.value = "Janji temu dibatalkan"
                }
                .onFailure { err ->
                    _isSubmitting.value = false
                    _errorMessage.value = err.message ?: "Gagal membatalkan janji temu"
                }
        }
    }
}
