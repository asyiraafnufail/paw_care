package com.pawcare.app.ui.screens.admin

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawcare.app.data.model.Appointment
import com.pawcare.app.ui.components.AppointmentScheduleBanner
import com.pawcare.app.ui.components.EmptyStateView
import com.pawcare.app.ui.components.LoadingView
import com.pawcare.app.ui.components.OwnerInfoRow
import com.pawcare.app.ui.components.PawButtonShape
import com.pawcare.app.ui.components.PawCardShape
import com.pawcare.app.ui.components.PawChipShape
import com.pawcare.app.ui.components.PawSearchBar
import com.pawcare.app.ui.components.PetAvatar
import com.pawcare.app.ui.components.RejectionReasonDialog
import com.pawcare.app.ui.components.ResponsiveScreenBox
import com.pawcare.app.ui.components.StatusBadge
import com.pawcare.app.viewmodel.AdminViewModel

@Composable
fun AdminQueueScreen(
    viewModel: AdminViewModel
) {
    val pendingList by viewModel.filteredPendingAppointments.collectAsStateWithLifecycle()
    val availableDates by viewModel.availableQueueDates.collectAsStateWithLifecycle()
    val searchQuery by viewModel.queueSearchQuery.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingData.collectAsStateWithLifecycle()
    val selectedDateFilter by viewModel.selectedDateFilter.collectAsStateWithLifecycle()

    var appointmentToReject by remember { mutableStateOf<Appointment?>(null) }

    if (appointmentToReject != null) {
        val target = appointmentToReject!!
        RejectionReasonDialog(
            title = "Tolak Janji: ${target.petName}",
            onConfirm = { reason ->
                viewModel.rejectAppointment(
                    appointmentId = target.appointmentId,
                    slotKey = target.slotKey,
                    reason = reason
                )
                appointmentToReject = null
            },
            onDismiss = { appointmentToReject = null }
        )
    }

    ResponsiveScreenBox {
        Column(modifier = Modifier.fillMaxSize()) {
            if (availableDates.isNotEmpty() || searchQuery.isNotBlank()) {
                PawSearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.setQueueSearchQuery(it) },
                    placeholder = "Cari nama anabul atau email pemilik...",
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp)
                )
            }

            if (availableDates.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedDateFilter == null,
                            onClick = { viewModel.setDateFilter(null) },
                            shape = PawChipShape,
                            label = { Text("Semua Tanggal") }
                        )
                    }
                    items(availableDates) { date ->
                        FilterChip(
                            selected = selectedDateFilter == date,
                            onClick = { viewModel.setDateFilter(date) },
                            shape = PawChipShape,
                            label = { Text(date) }
                        )
                    }
                }
            }

            if (isLoading && pendingList.isEmpty() && searchQuery.isBlank()) {
                LoadingView(modifier = Modifier.weight(1f))
            } else if (pendingList.isEmpty()) {
                EmptyStateView(
                    title = if (searchQuery.isNotBlank()) "Antrean Tidak Ditemukan" else "Tidak Ada Antrean Baru",
                    message = if (searchQuery.isNotBlank()) {
                        "Tidak ada antrean yang cocok dengan \"$searchQuery\"."
                    } else {
                        "Semua permohonan booking janji temu telah ditinjau atau belum ada pemesanan baru."
                    },
                    icon = if (searchQuery.isNotBlank()) Icons.Default.SearchOff else Icons.Default.FormatListBulleted,
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
                    items(pendingList, key = { it.appointmentId }) { apt ->
                        AdminQueueCard(
                            appointment = apt,
                            onApprove = { viewModel.confirmAppointment(apt.appointmentId) },
                            onReject = { appointmentToReject = apt }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminQueueCard(
    appointment: Appointment,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = PawCardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                PetAvatar(species = appointment.petSpeciesEnum, size = 44.dp)
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
                        style = MaterialTheme.typography.bodyMedium,
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

            if (appointment.notes.isNotBlank()) {
                Text(
                    text = "Keluhan: ${appointment.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onReject,
                    shape = PawButtonShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tolak", fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = onApprove,
                    shape = PawButtonShape
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Setujui", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
