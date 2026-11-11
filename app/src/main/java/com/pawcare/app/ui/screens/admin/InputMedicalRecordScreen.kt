package com.pawcare.app.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawcare.app.data.model.ServiceType
import com.pawcare.app.ui.components.EmptyStateView
import com.pawcare.app.ui.components.ErrorStateView
import com.pawcare.app.ui.components.PawButtonShape
import com.pawcare.app.ui.components.PawCardShape
import com.pawcare.app.ui.components.PawFieldShape
import com.pawcare.app.ui.components.PawTextField
import com.pawcare.app.ui.components.ResponsiveScreenBox
import com.pawcare.app.ui.components.SkeletonDetailScreen
import com.pawcare.app.util.UiState
import com.pawcare.app.viewmodel.AdminViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputMedicalRecordScreen(
    appointmentId: String,
    viewModel: AdminViewModel,
    onNavigateBack: () -> Unit
) {
    val appointmentState by viewModel.selectedAppointment.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var diagnosis by remember { mutableStateOf("") }
    var treatment by remember { mutableStateOf("") }
    var vaccineName by remember { mutableStateOf("") }
    var doctorNotes by remember { mutableStateOf("") }
    var nextVisitDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(appointmentId) {
        viewModel.loadAppointment(appointmentId)
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    val loadedApt = (appointmentState as? UiState.Success)?.data

    if (showDatePicker) {
        val initialMillis = (nextVisitDate ?: LocalDate.now().plusMonths(1))
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            shape = PawCardShape,
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val chosen = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                            val visitDate = loadedApt?.let { runCatching { LocalDate.parse(it.date) }.getOrNull() } ?: LocalDate.now()
                            if (chosen.isBefore(visitDate)) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Jadwal kontrol berikutnya tidak boleh sebelum tanggal periksa")
                                }
                            } else {
                                nextVisitDate = chosen
                            }
                        }
                        showDatePicker = false
                    },
                    shape = PawButtonShape
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }, shape = PawButtonShape) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Input Rekam Medis", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = appointmentState) {
            is UiState.Loading -> SkeletonDetailScreen(modifier = Modifier.padding(padding))
            is UiState.Error -> ErrorStateView(
                message = state.message,
                onRetry = { viewModel.loadAppointment(appointmentId) },
                modifier = Modifier.padding(padding)
            )
            is UiState.Success -> {
                val apt = state.data
                if (apt == null) {
                    EmptyStateView(
                        title = "Data Janji Tidak Ditemukan",
                        message = "Data pasien tidak dapat dimuat atau telah dihapus.",
                        icon = Icons.Default.ErrorOutline,
                        modifier = Modifier.padding(padding)
                    )
                    return@Scaffold
                }

                val isVaccination = apt.serviceTypeEnum == ServiceType.VACCINATION

                ResponsiveScreenBox(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .imePadding()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = PawCardShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Pasien: ${apt.petName} (${apt.petSpeciesEnum.displayName})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Layanan: ${apt.serviceTypeEnum.displayName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Pemilik: ${apt.ownerEmail.ifBlank { "-" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Jadwal: ${apt.date} • Pukul ${apt.timeSlot} WIB",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                if (apt.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Keluhan Awal: ${apt.notes}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        PawTextField(
                            value = diagnosis,
                            onValueChange = { diagnosis = it },
                            label = "Diagnosa Dokter *",
                            singleLine = false,
                            minLines = 2,
                            maxLines = 4
                        )

                        PawTextField(
                            value = treatment,
                            onValueChange = { treatment = it },
                            label = "Tindakan & Terapi *",
                            singleLine = false,
                            minLines = 2,
                            maxLines = 4
                        )

                        if (isVaccination) {
                            PawTextField(
                                value = vaccineName,
                                onValueChange = { vaccineName = it },
                                label = "Nama / Merk Vaksin *",
                                placeholder = "Contoh: Nobivac Rabies / Felocell 3",
                                singleLine = true
                            )
                        } else {
                            PawTextField(
                                value = vaccineName,
                                onValueChange = { vaccineName = it },
                                label = "Nama Vaksin (Opsional)",
                                singleLine = true
                            )
                        }

                        PawTextField(
                            value = doctorNotes,
                            onValueChange = { doctorNotes = it },
                            label = "Catatan / Resep Obat (Opsional)",
                            singleLine = false,
                            minLines = 2,
                            maxLines = 4
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(PawFieldShape)
                                .clickable { showDatePicker = true },
                            shape = PawFieldShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Jadwal Kontrol Berikutnya (Opsional)",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(
                                        text = nextVisitDate?.toString() ?: "Ketuk untuk pilih tanggal",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (nextVisitDate != null) {
                                    IconButton(onClick = { nextVisitDate = null }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Hapus tanggal")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                viewModel.submitMedicalRecord(
                                    appointment = apt,
                                    diagnosis = diagnosis,
                                    treatment = treatment,
                                    vaccineName = vaccineName,
                                    notes = doctorNotes,
                                    nextVisitDate = nextVisitDate?.toString(),
                                    onSuccess = onNavigateBack
                                )
                            },
                            enabled = !isSaving,
                            shape = PawButtonShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Simpan & Selesaikan Pemeriksaan", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
