package com.pawcare.app.ui.screens.owner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
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
import com.pawcare.app.data.model.Pet
import com.pawcare.app.ui.components.EmptyStateView
import com.pawcare.app.ui.components.LoadingView
import com.pawcare.app.ui.components.OwnerInfoRow
import com.pawcare.app.ui.components.PawCardShape
import com.pawcare.app.ui.components.PawSearchBar
import com.pawcare.app.ui.components.PetAvatar
import com.pawcare.app.ui.components.ResponsiveScreenBox
import com.pawcare.app.viewmodel.OwnerViewModel

@Composable
fun PetsScreen(
    viewModel: OwnerViewModel,
    onAddPetClick: () -> Unit,
    onEditPetClick: (String) -> Unit
) {
    val allPets by viewModel.pets.collectAsStateWithLifecycle()
    val filteredPets by viewModel.filteredPets.collectAsStateWithLifecycle()
    val searchQuery by viewModel.petSearchQuery.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingData.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading && allPets.isEmpty()) {
            LoadingView()
        } else if (allPets.isEmpty()) {
            EmptyStateView(
                title = "Belum Ada Hewan Peliharaan",
                message = "Daftarkan anabul Anda untuk memudahkan booking vaksinasi dan pemeriksaan kesehatan.",
                icon = Icons.Default.Pets,
                actionLabel = "+ Tambah Hewan Pertama",
                onAction = onAddPetClick
            )
        } else {
            ResponsiveScreenBox {
                Column(modifier = Modifier.fillMaxSize()) {
                    PawSearchBar(
                        query = searchQuery,
                        onQueryChange = { viewModel.setPetSearchQuery(it) },
                        placeholder = "Cari nama hewan, jenis, atau ras...",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )

                    if (filteredPets.isEmpty()) {
                        EmptyStateView(
                            title = "Hewan Tidak Ditemukan",
                            message = "Tidak ada hewan peliharaan yang cocok dengan \"$searchQuery\".",
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
                            items(filteredPets, key = { it.petId }) { pet ->
                                PetCard(
                                    pet = pet,
                                    ownerEmail = viewModel.ownerEmail,
                                    onClick = { onEditPetClick(pet.petId) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (allPets.isNotEmpty()) {
            FloatingActionButton(
                onClick = onAddPetClick,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Pet")
            }
        }
    }
}

@Composable
fun PetCard(
    pet: Pet,
    ownerEmail: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = PawCardShape,
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
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = pet.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                OwnerInfoRow(ownerEmail = ownerEmail)
                Text(
                    text = "${pet.speciesEnum.displayName} • ${pet.breed}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Umur: ${pet.ageLabel} • Berat: ${pet.weightKg} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
        }
    }
}
