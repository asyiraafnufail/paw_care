package com.pawcare.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pawcare.app.data.model.PetSpecies
import com.pawcare.app.ui.theme.BirdSpeciesColor
import com.pawcare.app.ui.theme.CatSpeciesColor
import com.pawcare.app.ui.theme.DogSpeciesColor
import com.pawcare.app.ui.theme.OtherSpeciesColor
import com.pawcare.app.ui.theme.RabbitSpeciesColor

fun speciesColor(species: PetSpecies): Color = when (species) {
    PetSpecies.CAT -> CatSpeciesColor
    PetSpecies.DOG -> DogSpeciesColor
    PetSpecies.RABBIT -> RabbitSpeciesColor
    PetSpecies.BIRD -> BirdSpeciesColor
    PetSpecies.OTHER -> OtherSpeciesColor
}

fun speciesBadgeText(species: PetSpecies): String = when (species) {
    PetSpecies.CAT -> "CAT"
    PetSpecies.DOG -> "DOG"
    PetSpecies.RABBIT -> "RAB"
    PetSpecies.BIRD -> "BRD"
    PetSpecies.OTHER -> "PET"
}

@Composable
fun PetAvatar(
    species: PetSpecies,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val bgColor = speciesColor(species)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Pets,
            contentDescription = species.displayName,
            tint = bgColor,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}
