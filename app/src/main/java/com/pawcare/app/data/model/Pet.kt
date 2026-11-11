package com.pawcare.app.data.model

import com.google.firebase.Timestamp

data class Pet(
    val petId: String = "",
    val ownerId: String = "",
    val name: String = "",
    val species: String = PetSpecies.CAT.name,
    val breed: String = "",
    val ageMonths: Int = 1,
    val weightKg: Double = 1.0,
    val createdAt: Timestamp? = null
) {
    val speciesEnum: PetSpecies
        get() = PetSpecies.fromValue(species)

    val ageLabel: String
        get() = if (ageMonths >= 12) {
            val years = ageMonths / 12
            val remMonths = ageMonths % 12
            if (remMonths > 0) "$years thn $remMonths bln" else "$years tahun"
        } else {
            "$ageMonths bulan"
        }
}
