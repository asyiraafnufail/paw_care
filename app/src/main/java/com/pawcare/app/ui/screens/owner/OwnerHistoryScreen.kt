package com.pawcare.app.ui.screens.owner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawcare.app.data.model.Appointment
import com.pawcare.app.data.model.MedicalRecord
import com.pawcare.app.ui.components.EmptyStateView
import com.pawcare.app.ui.components.PetAvatar
import com.pawcare.app.ui.components.StatusBadge
import com.pawcare.app.viewmodel.OwnerViewModel

@Composable
fun OwnerHistoryScreen(
    viewModel: OwnerViewModel,
    onRecordClick: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val medicalRecords by viewModel.medicalRecords.collectAsStateWithLifecycle()
    val historyAppointments by viewModel.historyAppointments.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Buku Medis (${medicalRecords.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Riwayat Janji (${historyAppointments.size})") }
            )
        }

        when (selectedTab) {
            0 -> MedicalRecordsTab(records = medicalRecords, onRecordClick = onRecordClick)
            1 -> AppointmentsHistoryTab(appointments = historyAppointments, onRecordClick = onRecordClick)
        }
    }
}

@Composable
private fun MedicalRecordsTab(
    records: List<MedicalRecord>,
    onRecordClick: (String) -> Unit
) {
    if (records.isEmpty()) {
        EmptyStateView(
            title = "Belum Ada Catatan Medis",
            message = "Catatan medis, diagnosa dokter, dan riwayat vaksin anabul akan muncul di sini setelah pemeriksaan selesai.",
            icon = Icons.Default.Description
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(records, key = { it.appointmentId }) { record ->
                MedicalRecordCard(record = record, onClick = { onRecordClick(record.appointmentId) })
            }
        }
    }
}

@Composable
private fun MedicalRecordCard(
    record: MedicalRecord,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${record.petName} • ${record.serviceTypeEnum.displayName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Tanggal Periksa: ${record.visitDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Diagnosa: ${record.diagnosis}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
                if (!record.vaccineName.isNullOrBlank()) {
                    Text(
                        text = "Vaksin: ${record.vaccineName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Detail")
        }
    }
}

@Composable
private fun AppointmentsHistoryTab(
    appointments: List<Appointment>,
    onRecordClick: (String) -> Unit
) {
    if (appointments.isEmpty()) {
        EmptyStateView(
            title = "Belum Ada Riwayat Selesai",
            message = "Janji temu yang telah selesai atau dibatalkan akan diarsipkan di sini.",
            icon = Icons.Default.History
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(appointments, key = { it.appointmentId }) { apt ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = apt.statusEnum == com.pawcare.app.data.model.AppointmentStatus.COMPLETED) {
                            onRecordClick(apt.appointmentId)
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PetAvatar(species = apt.petSpeciesEnum, size = 40.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${apt.petName} • ${apt.serviceTypeEnum.displayName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${apt.date} pukul ${apt.timeSlot}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            if (!apt.rejectionReason.isNullOrBlank()) {
                                Text(
                                    text = "Alasan Batal: ${apt.rejectionReason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        StatusBadge(status = apt.statusEnum)
                    }
                }
            }
        }
    }
}
