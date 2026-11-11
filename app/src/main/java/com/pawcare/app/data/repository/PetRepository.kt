package com.pawcare.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.pawcare.app.data.model.AppointmentStatus
import com.pawcare.app.data.model.Pet
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

interface PetRepository {
    fun getPetsForOwner(ownerId: String): Flow<List<Pet>>
    suspend fun getPetById(petId: String): Result<Pet?>
    suspend fun savePet(pet: Pet): Result<Unit>
    suspend fun deletePet(petId: String): Result<Unit>
    suspend fun hasActiveAppointments(petId: String): Boolean
}

class FirebasePetRepository(
    private val firestore: FirebaseFirestore
) : PetRepository {

    override fun getPetsForOwner(ownerId: String): Flow<List<Pet>> = callbackFlow {
        val registration = firestore.collection("pets")
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val pets = snapshot?.documents?.mapNotNull { it.toObject(Pet::class.java) } ?: emptyList()
                // In-memory sort by name to avoid composite index requirements
                trySend(pets.sortedBy { it.name.lowercase() })
            }
        awaitClose { registration.remove() }
    }

    override suspend fun getPetById(petId: String): Result<Pet?> = runCatching {
        val doc = firestore.collection("pets").document(petId).get().await()
        doc.toObject(Pet::class.java)
    }

    override suspend fun savePet(pet: Pet): Result<Unit> = runCatching {
        val id = if (pet.petId.isBlank()) UUID.randomUUID().toString() else pet.petId
        val toSave = pet.copy(
            petId = id,
            createdAt = pet.createdAt ?: Timestamp.now()
        )
        firestore.collection("pets").document(id).set(toSave).await()
    }

    override suspend fun hasActiveAppointments(petId: String): Boolean {
        return try {
            val snapshot = firestore.collection("appointments")
                .whereEqualTo("petId", petId)
                .get()
                .await()
            val activeStatuses = setOf(AppointmentStatus.PENDING.name, AppointmentStatus.CONFIRMED.name)
            snapshot.documents.any { doc ->
                val status = doc.getString("status")
                status in activeStatuses
            }
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun deletePet(petId: String): Result<Unit> = runCatching {
        if (hasActiveAppointments(petId)) {
            throw IllegalStateException("Hewan ini masih memiliki janji temu aktif. Batalkan janji terlebih dahulu sebelum menghapus.")
        }
        firestore.collection("pets").document(petId).delete().await()
    }
}

class FakePetRepository : PetRepository {
    private val petsFlow = MutableStateFlow<Map<String, Pet>>(emptyMap())
    var appointmentRepository: FakeAppointmentRepository? = null

    override fun getPetsForOwner(ownerId: String): Flow<List<Pet>> {
        return petsFlow.map { map ->
            map.values.filter { it.ownerId == ownerId }.sortedBy { it.name.lowercase() }
        }
    }

    override suspend fun getPetById(petId: String): Result<Pet?> {
        return Result.success(petsFlow.value[petId])
    }

    override suspend fun savePet(pet: Pet): Result<Unit> {
        val id = if (pet.petId.isBlank()) "pet_${System.currentTimeMillis()}" else pet.petId
        val updated = pet.copy(petId = id, createdAt = pet.createdAt ?: Timestamp.now())
        val current = petsFlow.value.toMutableMap()
        current[id] = updated
        petsFlow.value = current
        return Result.success(Unit)
    }

    override suspend fun hasActiveAppointments(petId: String): Boolean {
        val aptRepo = appointmentRepository ?: return false
        val activeStatuses = setOf(AppointmentStatus.PENDING.name, AppointmentStatus.CONFIRMED.name)
        return aptRepo.allAppointments().any { it.petId == petId && it.status in activeStatuses }
    }

    override suspend fun deletePet(petId: String): Result<Unit> {
        if (hasActiveAppointments(petId)) {
            return Result.failure(IllegalStateException("Hewan ini masih memiliki janji temu aktif. Batalkan janji terlebih dahulu sebelum menghapus."))
        }
        val current = petsFlow.value.toMutableMap()
        current.remove(petId)
        petsFlow.value = current
        return Result.success(Unit)
    }
}
