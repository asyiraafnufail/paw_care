package com.pawcare.app.ui.screens.owner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.pawcare.app.data.model.Pet
import com.pawcare.app.data.model.ServiceType
import com.pawcare.app.ui.components.PawButtonShape
import com.pawcare.app.ui.components.PawCardShape
import com.pawcare.app.ui.components.PawChipShape
import com.pawcare.app.ui.components.PawFieldShape
import com.pawcare.app.ui.components.PawTextField
import com.pawcare.app.ui.components.PetAvatar
import com.pawcare.app.ui.components.ResponsiveScreenBox
import com.pawcare.app.util.Validators
import com.pawcare.app.viewmodel.OwnerViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateBookingScreen(
    viewModel: OwnerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddPet: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val pets by viewModel.pets.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedBookingDate.collectAsStateWithLifecycle()
    val bookedSlots by viewModel.bookedSlotsForSelectedDate.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var selectedPet by remember { mutableStateOf<Pet?>(null) }
    var selectedService by remember { mutableStateOf(ServiceType.VACCINATION) }
    var selectedSlot by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(pets) {
        if (selectedPet == null && pets.isNotEmpty()) {
            selectedPet = pets.first()
        } else if (selectedPet != null) {
            selectedPet = pets.find { it.petId == selectedPet?.petId } ?: pets.firstOrNull()
        }
    }

    LaunchedEffect(bookedSlots) {
        if (selectedSlot in bookedSlots) {
            selectedSlot = null
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            shape = PawCardShape,
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val chosen = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                            if (chosen.isBefore(LocalDate.now())) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Tanggal tidak boleh sebelum hari ini")
                                }
                            } else {
                                viewModel.selectBookingDate(chosen)
                                selectedSlot = null
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
                title = { Text("Buat Booking", fontWeight = FontWeight.Bold) },
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pilih Hewan Peliharaan *",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (pets.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = PawCardShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Anda belum memiliki anabul terdaftar.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                OutlinedButton(
                                    onClick = onNavigateToAddPet,
                                    shape = PawButtonShape
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Daftarkan Hewan Sekarang")
                                }
                            }
                        }
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pets.forEach { pet ->
                                FilterChip(
                                    selected = selectedPet?.petId == pet.petId,
                                    onClick = { selectedPet = pet },
                                    shape = PawChipShape,
                                    leadingIcon = { PetAvatar(species = pet.speciesEnum, size = 24.dp) },
                                    label = {
                                        Text(
                                            text = "${pet.name} (${pet.speciesEnum.displayName})",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Jenis Layanan *",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ServiceType.entries.forEach { svc ->
                            FilterChip(
                                selected = selectedService == svc,
                                onClick = { selectedService = svc },
                                shape = PawChipShape,
                                label = { Text(svc.displayName) }
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pilih Tanggal Kunjungan *",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
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
                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pilih Jam Kunjungan *",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Validators.CLINIC_TIME_SLOTS.forEach { slot ->
                            val isPast = Validators.isSlotPastForDate(selectedDate, slot, LocalDateTime.now())
                            val isBooked = bookedSlots.contains(slot)
                            val isEnabled = !isPast && !isBooked

                            FilterChip(
                                selected = selectedSlot == slot,
                                onClick = { if (isEnabled) selectedSlot = slot },
                                enabled = isEnabled,
                                shape = PawChipShape,
                                label = {
                                    Text(
                                        text = when {
                                            isBooked -> "$slot (Penuh)"
                                            isPast -> "$slot (Lewat)"
                                            else -> slot
                                        },
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }
                }

                PawTextField(
                    value = notes,
                    onValueChange = { if (it.length <= 300) notes = it },
                    label = "Keluhan / Catatan Tambahan (Opsional)",
                    supportingText = "${notes.length}/300",
                    singleLine = false,
                    minLines = 3,
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.createBooking(
                            pet = selectedPet,
                            serviceType = selectedService,
                            date = selectedDate,
                            timeSlot = selectedSlot,
                            notes = notes,
                            onSuccess = onNavigateBack
                        )
                    },
                    enabled = !isSubmitting && selectedPet != null && selectedSlot != null,
                    shape = PawButtonShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Konfirmasi Booking", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
