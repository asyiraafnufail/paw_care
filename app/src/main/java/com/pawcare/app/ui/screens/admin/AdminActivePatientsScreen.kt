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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.pawcare.app.ui.components.AppointmentScheduleBanner
import com.pawcare.app.ui.components.EmptyStateView
import com.pawcare.app.ui.components.OwnerInfoRow
import com.pawcare.app.ui.components.PawButtonShape
import com.pawcare.app.ui.components.PawCardShape
import com.pawcare.app.ui.components.PawSearchBar
import com.pawcare.app.ui.components.PetAvatar
import com.pawcare.app.ui.components.ResponsiveScreenBox
import com.pawcare.app.ui.components.StatusBadge
import com.pawcare.app.viewmodel.AdminViewModel

@Composable
fun AdminActivePatientsScreen(
    viewModel: AdminViewModel,
    onInputRecordClick: (String) -> Unit
) {
    val allConfirmed by viewModel.confirmedAppointments.collectAsStateWithLifecycle()
    val filteredConfirmed by viewModel.filteredActiveAppointments.collectAsStateWithLifecycle()
    val searchQuery by viewModel.activePatientSearchQuery.collectAsStateWithLifecycle()

    if (allConfirmed.isEmpty()) {
        EmptyStateView(
            title = "Belum Ada Pasien Aktif",
            message = "Janji temu yang telah disetujui akan muncul di sini menunggu pemeriksaan dokter hewan.",
            icon = Icons.Default.LocalHospital
        )
    } else {
        ResponsiveScreenBox {
            Column(modifier = Modifier.fillMaxSize()) {
                PawSearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.setActivePatientSearchQuery(it) },
                    placeholder = "Cari pasien aktif, layanan, atau email pemilik...",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )

                if (filteredConfirmed.isEmpty()) {
                    EmptyStateView(
                        title = "Pasien Tidak Ditemukan",
                        message = "Tidak ada pasien aktif yang cocok dengan \"$searchQuery\".",
                        icon = Icons.Default.SearchOff,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredConfirmed, key = { it.appointmentId }) { apt ->
                            ActivePatientCard(
                                appointment = apt,
                                onInputRecordClick = { onInputRecordClick(apt.appointmentId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivePatientCard(
    appointment: Appointment,
    onInputRecordClick: () -> Unit
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

            Button(
                onClick = onInputRecordClick,
                shape = PawButtonShape,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.EditNote, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Input Rekam Medis", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
