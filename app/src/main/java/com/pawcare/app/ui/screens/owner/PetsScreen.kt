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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawcare.app.data.model.Pet
import com.pawcare.app.ui.components.EmptyStateView
import com.pawcare.app.ui.components.LoadingView
import com.pawcare.app.ui.components.PetAvatar
import com.pawcare.app.viewmodel.OwnerViewModel

@Composable
fun PetsScreen(
    viewModel: OwnerViewModel,
    onAddPetClick: () -> Unit,
    onEditPetClick: (String) -> Unit
) {
    val pets by viewModel.pets.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingData.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPetClick) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Pet")
            }
        }
    ) { padding ->
        if (isLoading && pets.isEmpty()) {
            LoadingView(modifier = Modifier.padding(padding))
        } else if (pets.isEmpty()) {
            EmptyStateView(
                title = "Belum Ada Hewan Peliharaan",
                message = "Daftarkan anabul Anda untuk memudahkan booking vaksinasi dan pemeriksaan kesehatan.",
                icon = Icons.Default.Pets,
                actionLabel = "+ Tambah Hewan Pertama",
                onAction = onAddPetClick,
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
                items(pets, key = { it.petId }) { pet ->
                    PetCard(pet = pet, onClick = { onEditPetClick(pet.petId) })
                }
            }
        }
    }
}

@Composable
fun PetCard(
    pet: Pet,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PetAvatar(species = pet.speciesEnum, size = 52.dp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pet.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${pet.speciesEnum.displayName} • ${pet.breed}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Umur: ${pet.ageLabel} • Berat: ${pet.weightKg} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            IconButton(onClick = onClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
        }
    }
}
