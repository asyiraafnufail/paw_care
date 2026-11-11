package com.pawcare.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pawcare.app.data.model.MedicalRecord
import com.pawcare.app.data.repository.MedicalRecordRepository
import com.pawcare.app.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MedicalRecordDetailViewModel(
    private val medicalRecordRepository: MedicalRecordRepository
) : ViewModel() {

    private val _recordState = MutableStateFlow<UiState<MedicalRecord?>>(UiState.Loading)
    val recordState: StateFlow<UiState<MedicalRecord?>> = _recordState.asStateFlow()

    fun loadRecord(appointmentId: String) {
        viewModelScope.launch {
            _recordState.value = UiState.Loading
            medicalRecordRepository.getRecordForAppointment(appointmentId)
                .onSuccess { _recordState.value = UiState.Success(it) }
                .onFailure { _recordState.value = UiState.Error(it.message ?: "Gagal memuat catatan medis") }
        }
    }
}
