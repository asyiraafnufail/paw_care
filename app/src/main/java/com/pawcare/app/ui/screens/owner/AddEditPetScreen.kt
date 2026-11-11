package com.pawcare.app.ui.screens.owner

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawcare.app.data.model.PetSpecies
import com.pawcare.app.ui.components.ErrorStateView
import com.pawcare.app.ui.components.LoadingView
import com.pawcare.app.ui.components.PawButtonShape
import com.pawcare.app.ui.components.PawCareConfirmDialog
import com.pawcare.app.ui.components.PawChipShape
import com.pawcare.app.ui.components.PawTextField
import com.pawcare.app.ui.components.ResponsiveScreenBox
import com.pawcare.app.util.UiState
import com.pawcare.app.viewmodel.OwnerViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditPetScreen(
    petId: String?,
    viewModel: OwnerViewModel,
    onNavigateBack: () -> Unit
) {
    val isEditing = petId != null
    var name by remember { mutableStateOf("") }
    var selectedSpecies by remember { mutableStateOf(PetSpecies.CAT) }
    var breed by remember { mutableStateOf("") }
    var ageMonthsText by remember { mutableStateOf("") }
    var weightKgText by remember { mutableStateOf("") }
    var initializedForId by remember { mutableStateOf<String?>(null) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    val editingPetState by viewModel.editingPet.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(petId) {
        viewModel.loadPetForEdit(petId)
    }

    LaunchedEffect(editingPetState) {
        val state = editingPetState
        if (state is UiState.Success && state.data != null && initializedForId != state.data.petId) {
            val pet = state.data
            name = pet.name
            selectedSpecies = pet.speciesEnum
            breed = pet.breed
            ageMonthsText = pet.ageMonths.toString()
            weightKgText = pet.weightKg.toString()
            initializedForId = pet.petId
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    if (showDeleteDialog && petId != null) {
        PawCareConfirmDialog(
            title = "Hapus Hewan Peliharaan",
            message = "Apakah Anda yakin ingin menghapus data ${name}? Riwayat medis yang sudah selesai akan tetap tersimpan.",
            confirmText = "Hapus",
            isDestructive = true,
            onConfirm = {
                showDeleteDialog = false
                viewModel.deletePet(petId, onSuccess = onNavigateBack)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Data Hewan" else "Tambah Hewan",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Hapus",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = editingPetState) {
            is UiState.Loading -> LoadingView(modifier = Modifier.padding(padding))
            is UiState.Error -> ErrorStateView(
                message = state.message,
                onRetry = { viewModel.loadPetForEdit(petId) },
                modifier = Modifier.padding(padding)
            )
            is UiState.Success -> ResponsiveScreenBox(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PawTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Nama Hewan *",
                        singleLine = true
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Jenis Hewan *",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PetSpecies.entries.forEach { species ->
                                FilterChip(
                                    selected = selectedSpecies == species,
                                    onClick = { selectedSpecies = species },
                                    shape = PawChipShape,
                                    label = { Text(species.displayName) }
                                )
                            }
                        }
                    }

                    PawTextField(
                        value = breed,
                        onValueChange = { breed = it },
                        label = "Ras / Jenis (misal: Persia, Golden) *",
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PawTextField(
                            value = ageMonthsText,
                            onValueChange = { ageMonthsText = it },
                            label = "Umur (Bulan) *",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        PawTextField(
                            value = weightKgText,
                            onValueChange = { weightKgText = it },
                            label = "Berat (Kg) *",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.savePet(
                                petId = petId,
                                name = name,
                                species = selectedSpecies,
                                breed = breed,
                                ageMonthsText = ageMonthsText,
                                weightKgText = weightKgText,
                                onSuccess = onNavigateBack
                            )
                        },
                        enabled = !isSaving,
                        shape = PawButtonShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(
                                text = if (isEditing) "Simpan Perubahan" else "Tambah Hewan",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (isEditing) {
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            shape = PawButtonShape,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Hapus Hewan Ini", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
