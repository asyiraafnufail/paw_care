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
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawcare.app.data.model.Appointment
import com.pawcare.app.data.model.AppointmentStatus
import com.pawcare.app.data.model.MedicalRecord
import com.pawcare.app.ui.components.AppointmentScheduleBanner
import com.pawcare.app.ui.components.EmptyStateView
import com.pawcare.app.ui.components.OwnerInfoRow
import com.pawcare.app.ui.components.PawCardShape
import com.pawcare.app.ui.components.PawSearchBar
import com.pawcare.app.ui.components.PetAvatar
import com.pawcare.app.ui.components.ResponsiveScreenBox
import com.pawcare.app.ui.components.StatusBadge
import com.pawcare.app.viewmodel.OwnerViewModel

@Composable
fun OwnerHistoryScreen(
    viewModel: OwnerViewModel,
    onRecordClick: (String) -> Unit
) {
    val selectedTab by viewModel.selectedHistoryTab.collectAsStateWithLifecycle()
    val allRecords by viewModel.medicalRecords.collectAsStateWithLifecycle()
    val filteredRecords by viewModel.filteredMedicalRecords.collectAsStateWithLifecycle()
    val allHistoryAppointments by viewModel.historyAppointments.collectAsStateWithLifecycle()
    val filteredHistoryAppointments by viewModel.filteredHistoryAppointments.collectAsStateWithLifecycle()
    val searchQuery by viewModel.historySearchQuery.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { viewModel.setSelectedHistoryTab(0) },
                text = {
                    Text(
                        text = "Buku Medis (${allRecords.size})",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { viewModel.setSelectedHistoryTab(1) },
                text = {
                    Text(
                        text = "Riwayat Janji (${allHistoryAppointments.size})",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }

        val hasDataInActiveTab = if (selectedTab == 0) allRecords.isNotEmpty() else allHistoryAppointments.isNotEmpty()
        if (hasDataInActiveTab) {
            ResponsiveScreenBox(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                PawSearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.setHistorySearchQuery(it) },
                    placeholder = if (selectedTab == 0) {
                        "Cari hewan, diagnosa, atau vaksin..."
                    } else {
                        "Cari hewan, layanan, atau tanggal..."
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> MedicalRecordsTab(
                hasAnyData = allRecords.isNotEmpty(),
                records = filteredRecords,
                fallbackOwnerEmail = viewModel.ownerEmail,
                searchQuery = searchQuery,
                onRecordClick = onRecordClick
            )
            1 -> AppointmentsHistoryTab(
                hasAnyData = allHistoryAppointments.isNotEmpty(),
                appointments = filteredHistoryAppointments,
                fallbackOwnerEmail = viewModel.ownerEmail,
                searchQuery = searchQuery,
                onRecordClick = onRecordClick
            )
        }
    }
}

@Composable
private fun MedicalRecordsTab(
    hasAnyData: Boolean,
    records: List<MedicalRecord>,
    fallbackOwnerEmail: String,
    searchQuery: String,
    onRecordClick: (String) -> Unit
) {
    if (!hasAnyData) {
        EmptyStateView(
            title = "Belum Ada Catatan Medis",
            message = "Catatan medis, diagnosa dokter, dan riwayat vaksin anabul akan muncul di sini setelah pemeriksaan selesai.",
            icon = Icons.Default.Description
        )
    } else if (records.isEmpty()) {
        EmptyStateView(
            title = "Catatan Tidak Ditemukan",
            message = "Tidak ada buku medis yang cocok dengan \"$searchQuery\".",
            icon = Icons.Default.SearchOff
        )
    } else {
        ResponsiveScreenBox {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(records, key = { it.appointmentId }) { record ->
                    MedicalRecordCard(
                        record = record,
                        fallbackOwnerEmail = fallbackOwnerEmail,
                        onClick = { onRecordClick(record.appointmentId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MedicalRecordCard(
    record: MedicalRecord,
    fallbackOwnerEmail: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = PawCardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${record.petName} • ${record.serviceTypeEnum.displayName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                OwnerInfoRow(ownerEmail = record.ownerEmail.ifBlank { fallbackOwnerEmail })
                Text(
                    text = "Tanggal Periksa: ${record.visitDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Diagnosa: ${record.diagnosis}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!record.vaccineName.isNullOrBlank()) {
                    Text(
                        text = "Vaksin: ${record.vaccineName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = "Detail")
        }
    }
}

@Composable
private fun AppointmentsHistoryTab(
    hasAnyData: Boolean,
    appointments: List<Appointment>,
    fallbackOwnerEmail: String,
    searchQuery: String,
    onRecordClick: (String) -> Unit
) {
    if (!hasAnyData) {
        EmptyStateView(
            title = "Belum Ada Riwayat Selesai",
            message = "Janji temu yang telah selesai atau dibatalkan akan diarsipkan di sini.",
            icon = Icons.Default.History
        )
    } else if (appointments.isEmpty()) {
        EmptyStateView(
            title = "Riwayat Tidak Ditemukan",
            message = "Tidak ada riwayat janji yang cocok dengan \"$searchQuery\".",
            icon = Icons.Default.SearchOff
        )
    } else {
        ResponsiveScreenBox {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(appointments, key = { it.appointmentId }) { apt ->
                    val isCompleted = apt.statusEnum == AppointmentStatus.COMPLETED
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isCompleted) {
                                onRecordClick(apt.appointmentId)
                            },
                        shape = PawCardShape,
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PetAvatar(species = apt.petSpeciesEnum, size = 40.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "${apt.petName} (${apt.petSpeciesEnum.displayName})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = apt.serviceTypeEnum.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                StatusBadge(status = apt.statusEnum)
                            }
                            OwnerInfoRow(ownerEmail = apt.ownerEmail.ifBlank { fallbackOwnerEmail })
                            AppointmentScheduleBanner(
                                date = apt.date,
                                timeSlot = apt.timeSlot
                            )
                            if (!apt.rejectionReason.isNullOrBlank()) {
                                Text(
                                    text = "Alasan Batal: ${apt.rejectionReason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else if (isCompleted) {
                                Text(
                                    text = "Ketuk untuk melihat rekam medis →",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
