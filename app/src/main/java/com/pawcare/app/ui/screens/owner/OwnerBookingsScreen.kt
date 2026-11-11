package com.pawcare.app.ui.screens.owner

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.pawcare.app.ui.components.PawCareConfirmDialog
import com.pawcare.app.ui.components.PetAvatar
import com.pawcare.app.ui.components.StatusBadge
import com.pawcare.app.viewmodel.OwnerViewModel

@Composable
fun OwnerBookingsScreen(
    viewModel: OwnerViewModel,
    onCreateBookingClick: () -> Unit
) {
    val activeBookings by viewModel.activeAppointments.collectAsStateWithLifecycle()
    var appointmentToCancel by remember { mutableStateOf<Appointment?>(null) }

    if (appointmentToCancel != null) {
        val target = appointmentToCancel!!
        PawCareConfirmDialog(
            title = "Batalkan Janji Temu?",
            message = "Apakah Anda yakin ingin membatalkan booking ${target.serviceTypeEnum.displayName} untuk ${target.petName} pada ${target.date} pukul ${target.timeSlot}?",
            confirmText = "Batalkan Janji",
            isDestructive = true,
            onConfirm = {
                viewModel.cancelBooking(target.appointmentId, target.slotKey)
                appointmentToCancel = null
            },
            onDismiss = { appointmentToCancel = null }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateBookingClick) {
                Icon(Icons.Default.Add, contentDescription = "Booking Baru")
            }
        }
    ) { padding ->
        if (activeBookings.isEmpty()) {
            EmptyStateView(
                title = "Tidak Ada Janji Temu Aktif",
                message = "Anda belum memiliki booking periksa atau vaksinasi yang sedang berjalan.",
                icon = Icons.Default.CalendarMonth,
                actionLabel = "+ Buat Booking Baru",
                onAction = onCreateBookingClick,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activeBookings, key = { it.appointmentId }) { apt ->
                    BookingCard(
                        appointment = apt,
                        onCancelClick = { appointmentToCancel = apt }
                    )
                }
            }
        }
    }
}

@Composable
fun BookingCard(
    appointment: Appointment,
    onCancelClick: () -> Unit
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
                        text = "${appointment.petName} • ${appointment.serviceTypeEnum.displayName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "🗓️ ${appointment.date} pukul ${appointment.timeSlot} WIB",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
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

            if (appointment.canBeCancelledByOwner) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onCancelClick,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Batalkan Janji")
                }
            }
        }
    }
}
