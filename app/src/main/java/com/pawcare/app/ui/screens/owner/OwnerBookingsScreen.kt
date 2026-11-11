package com.pawcare.app.ui.screens.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.pawcare.app.ui.components.PawButtonShape
import com.pawcare.app.ui.components.PawCardShape
import com.pawcare.app.ui.components.PawCareConfirmDialog
import com.pawcare.app.ui.components.PetAvatar
import com.pawcare.app.ui.components.ResponsiveScreenBox
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

    Box(modifier = Modifier.fillMaxSize()) {
        if (activeBookings.isEmpty()) {
            EmptyStateView(
                title = "Tidak Ada Janji Temu Aktif",
                message = "Anda belum memiliki booking periksa atau vaksinasi yang sedang berjalan.",
                icon = Icons.Default.CalendarMonth,
                actionLabel = "+ Buat Booking Baru",
                onAction = onCreateBookingClick
            )
        } else {
            ResponsiveScreenBox {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
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

        if (activeBookings.isNotEmpty()) {
            FloatingActionButton(
                onClick = onCreateBookingClick,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Booking Baru")
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
        shape = PawCardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Baris atas: Avatar + Nama Hewan & Layanan di kiri, Badge Status di kanan
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
                        text = appointment.petName,
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

            // Baris khusus jadwal kunjungan: Full-width agar jam & tanggal TIDAK tertutup/terpotong
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

            if (appointment.canBeCancelledByOwner) {
                OutlinedButton(
                    onClick = onCancelClick,
                    shape = PawButtonShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Batalkan Janji", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
