package com.pawcare.app.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.pawcare.app.ui.components.AppointmentScheduleBanner
import com.pawcare.app.ui.components.EmptyStateView
import com.pawcare.app.ui.components.OwnerInfoRow
import com.pawcare.app.ui.components.PawCardShape
import com.pawcare.app.ui.components.PawChipShape
import com.pawcare.app.ui.components.PawTextField
import com.pawcare.app.ui.components.PetAvatar
import com.pawcare.app.ui.components.ResponsiveScreenBox
import com.pawcare.app.ui.components.StatusBadge
import com.pawcare.app.viewmodel.AdminViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminArchiveScreen(
    viewModel: AdminViewModel,
    onDetailClick: (String) -> Unit
) {
    val archiveList by viewModel.filteredArchiveAppointments.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.archiveStatusFilter.collectAsStateWithLifecycle()

    ResponsiveScreenBox {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                PawTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    label = "Cari Arsip",
                    placeholder = "Nama anabul atau email pemilik...",
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Bersihkan")
                            }
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = statusFilter == null,
                        onClick = { viewModel.setArchiveStatusFilter(null) },
                        shape = PawChipShape,
                        label = { Text("Semua") }
                    )
                    FilterChip(
                        selected = statusFilter == AppointmentStatus.COMPLETED,
                        onClick = { viewModel.setArchiveStatusFilter(AppointmentStatus.COMPLETED) },
                        shape = PawChipShape,
                        label = { Text("Selesai") }
                    )
                    FilterChip(
                        selected = statusFilter == AppointmentStatus.CANCELLED,
                        onClick = { viewModel.setArchiveStatusFilter(AppointmentStatus.CANCELLED) },
                        shape = PawChipShape,
                        label = { Text("Ditolak / Batal") }
                    )
                }
            }

            if (archiveList.isEmpty()) {
                EmptyStateView(
                    title = "Arsip Kosong",
                    message = if (searchQuery.isNotBlank()) "Tidak ditemukan hasil untuk \"$searchQuery\"." else "Belum ada riwayat janji temu yang selesai atau dibatalkan.",
                    icon = Icons.Default.Archive,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(archiveList, key = { it.appointmentId }) { apt ->
                        ArchiveCard(
                            appointment = apt,
                            onClick = {
                                if (apt.statusEnum == AppointmentStatus.COMPLETED) {
                                    onDetailClick(apt.appointmentId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveCard(
    appointment: Appointment,
    onClick: () -> Unit
) {
    val isCompleted = appointment.statusEnum == AppointmentStatus.COMPLETED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isCompleted, onClick = onClick),
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
                PetAvatar(species = appointment.petSpeciesEnum, size = 40.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${appointment.petName} (${appointment.petSpeciesEnum.displayName})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = appointment.serviceTypeEnum.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(status = appointment.statusEnum)
            }

            OwnerInfoRow(ownerEmail = appointment.ownerEmail)

            AppointmentScheduleBanner(
                date = appointment.date,
                timeSlot = appointment.timeSlot
            )

            if (isCompleted) {
                Text(
                    text = "Ketuk untuk melihat rekam medis lengkap →",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            } else if (!appointment.rejectionReason.isNullOrBlank()) {
                Text(
                    text = "Alasan: ${appointment.rejectionReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
