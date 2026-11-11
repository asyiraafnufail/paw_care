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
import androidx.compose.material.icons.filled.SearchOff
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
import com.pawcare.app.ui.components.OwnerInfoRow
import com.pawcare.app.ui.components.PawButtonShape
import com.pawcare.app.ui.components.PawCardShape
import com.pawcare.app.ui.components.PawCareConfirmDialog
import com.pawcare.app.ui.components.PawSearchBar
import com.pawcare.app.ui.components.PetAvatar
import com.pawcare.app.ui.components.ResponsiveScreenBox
import com.pawcare.app.ui.components.SkeletonCardList
import com.pawcare.app.ui.components.StatusBadge
import com.pawcare.app.viewmodel.OwnerViewModel

@Composable
fun OwnerBookingsScreen(
    viewModel: OwnerViewModel,
    onCreateBookingClick: () -> Unit
) {
    val allActiveBookings by viewModel.activeAppointments.collectAsStateWithLifecycle()
    val filteredBookings by viewModel.filteredActiveAppointments.collectAsStateWithLifecycle()
    val searchQuery by viewModel.bookingSearchQuery.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingAppointments.collectAsStateWithLifecycle()
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
        if (isLoading && allActiveBookings.isEmpty()) {
            ResponsiveScreenBox {
                SkeletonCardList(
                    count = 3,
                    showBanner = true,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        } else if (allActiveBookings.isEmpty()) {
            EmptyStateView(
                title = "Tidak Ada Janji Temu Aktif",
                message = "Anda belum memiliki booking periksa atau vaksinasi yang sedang berjalan.",
                icon = Icons.Default.CalendarMonth,
                actionLabel = "+ Buat Booking Baru",
                onAction = onCreateBookingClick
            )
        } else {
            ResponsiveScreenBox {
                Column(modifier = Modifier.fillMaxSize()) {
                    PawSearchBar(
                        query = searchQuery,
                        onQueryChange = { viewModel.setBookingSearchQuery(it) },
                        placeholder = "Cari nama hewan, layanan, atau tanggal...",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )

                    if (filteredBookings.isEmpty()) {
                        EmptyStateView(
                            title = "Booking Tidak Ditemukan",
                            message = "Tidak ada janji temu yang cocok dengan \"$searchQuery\".",
                            icon = Icons.Default.SearchOff,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = filteredBookings,
                                key = { it.appointmentId },
                                contentType = { "booking_card" }
                            ) { apt ->
                                BookingCard(
                                    appointment = apt,
                                    fallbackOwnerEmail = viewModel.ownerEmail,
                                    onCancelClick = { appointmentToCancel = apt }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (allActiveBookings.isNotEmpty()) {
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
    fallbackOwnerEmail: String = "",
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

            OwnerInfoRow(ownerEmail = appointment.ownerEmail.ifBlank { fallbackOwnerEmail })

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
