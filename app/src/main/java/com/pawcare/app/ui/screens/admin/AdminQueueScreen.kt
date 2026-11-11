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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawcare.app.data.model.Appointment
import com.pawcare.app.ui.components.EmptyStateView
import com.pawcare.app.ui.components.LoadingView
import com.pawcare.app.ui.components.PetAvatar
import com.pawcare.app.ui.components.StatusBadge
import com.pawcare.app.viewmodel.AdminViewModel

@Composable
fun AdminQueueScreen(
    viewModel: AdminViewModel
) {
    val pendingList by viewModel.filteredPendingAppointments.collectAsStateWithLifecycle()
    val availableDates by viewModel.availableQueueDates.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingData.collectAsStateWithLifecycle()
    val selectedDateFilter by viewModel.selectedDateFilter.collectAsStateWithLifecycle()

    var appointmentToReject by remember { mutableStateOf<Appointment?>(null) }
    var rejectionReasonInput by remember { mutableStateOf("") }
    var rejectionError by remember { mutableStateOf<String?>(null) }

    if (appointmentToReject != null) {
        val target = appointmentToReject!!
        AlertDialog(
            onDismissRequest = { appointmentToReject = null },
            title = { Text("Tolak Janji Temu") },
            text = {
                Column {
                    Text("Berikan alasan penolakan untuk booking anabul ${target.petName} (${target.date} ${target.timeSlot}). Alasan ini akan tampil pada pemilik hewan.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rejectionReasonInput,
                        onValueChange = {
                            rejectionReasonInput = it
                            rejectionError = null
                        },
                        label = { Text("Alasan Penolakan *") },
                        isError = rejectionError != null,
                        supportingText = rejectionError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectionReasonInput.trim().isEmpty()) {
                            rejectionError = "Alasan pembatalan/penolakan wajib diisi"
                            return@Button
                        }
                        viewModel.rejectAppointment(
                            appointmentId = target.appointmentId,
                            slotKey = target.slotKey,
                            reason = rejectionReasonInput
                        )
                        appointmentToReject = null
                        rejectionReasonInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Tolak & Kosongkan Slot")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    appointmentToReject = null
                    rejectionReasonInput = ""
                }) {
                    Text("Batal")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (availableDates.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedDateFilter == null,
                        onClick = { viewModel.setDateFilter(null) },
                        label = { Text("Semua Tanggal") }
                    )
                }
                items(availableDates) { date ->
                    FilterChip(
                        selected = selectedDateFilter == date,
                        onClick = { viewModel.setDateFilter(date) },
                        label = { Text(date) }
                    )
                }
            }
        }

        if (isLoading && pendingList.isEmpty()) {
            LoadingView()
        } else if (pendingList.isEmpty()) {
            EmptyStateView(
                title = "Tidak Ada Antrean Baru",
                message = "Semua permohonan booking janji temu telah ditinjau atau belum ada pemesanan baru.",
                icon = Icons.Default.FormatListBulleted
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
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

@Composable
private fun AdminQueueCard(
    appointment: Appointment,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PetAvatar(species = appointment.petSpeciesEnum, size = 44.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${appointment.petName} (${appointment.serviceTypeEnum.displayName})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pemilik: ${appointment.ownerEmail}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "🗓️ ${appointment.date} pukul ${appointment.timeSlot} WIB",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                StatusBadge(status = appointment.statusEnum)
            }

            if (appointment.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Keluhan: ${appointment.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tolak")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onApprove) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Setujui")
                }
            }
        }
    }
}
