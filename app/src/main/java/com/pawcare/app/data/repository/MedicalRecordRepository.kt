package com.pawcare.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.pawcare.app.data.model.AppointmentStatus
import com.pawcare.app.data.model.MedicalRecord
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

interface MedicalRecordRepository {
    fun getRecordsForOwner(ownerId: String): Flow<List<MedicalRecord>>
    fun getAllRecords(): Flow<List<MedicalRecord>>
    suspend fun getRecordForAppointment(appointmentId: String): Result<MedicalRecord?>
    suspend fun submitMedicalRecord(record: MedicalRecord): Result<Unit>
}

class FirebaseMedicalRecordRepository(
    private val firestore: FirebaseFirestore
) : MedicalRecordRepository {

    override fun getRecordsForOwner(ownerId: String): Flow<List<MedicalRecord>> = callbackFlow {
        val reg = firestore.collection("medical_records")
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val records = snapshot?.documents?.mapNotNull { it.toObject(MedicalRecord::class.java) }
                    ?.sortedByDescending { it.visitDate }
                    ?: emptyList()
                trySend(records)
            }
        awaitClose { reg.remove() }
    }

    override fun getAllRecords(): Flow<List<MedicalRecord>> = callbackFlow {
        val reg = firestore.collection("medical_records")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val records = snapshot?.documents?.mapNotNull { it.toObject(MedicalRecord::class.java) }
                    ?.sortedByDescending { it.visitDate }
                    ?: emptyList()
                trySend(records)
            }
        awaitClose { reg.remove() }
    }

    override suspend fun getRecordForAppointment(appointmentId: String): Result<MedicalRecord?> = runCatching {
        val doc = firestore.collection("medical_records").document(appointmentId).get().await()
        doc.toObject(MedicalRecord::class.java)
    }

    override suspend fun submitMedicalRecord(record: MedicalRecord): Result<Unit> = runCatching {
        val toSave = record.copy(createdAt = Timestamp.now())
        val batch = firestore.batch()
        val recordRef = firestore.collection("medical_records").document(record.appointmentId)
        val aptRef = firestore.collection("appointments").document(record.appointmentId)

        batch.set(recordRef, toSave)
        batch.update(aptRef, mapOf(
            "status" to AppointmentStatus.COMPLETED.name,
            "updatedAt" to Timestamp.now()
        ))
        batch.commit().await()
    }
}

class FakeMedicalRecordRepository(
    private val appointmentRepo: FakeAppointmentRepository? = null
) : MedicalRecordRepository {

    private val recordsFlow = MutableStateFlow<Map<String, MedicalRecord>>(emptyMap())

    override fun getRecordsForOwner(ownerId: String): Flow<List<MedicalRecord>> =
        recordsFlow.map { map ->
            map.values.filter { it.ownerId == ownerId }.sortedByDescending { it.visitDate }
        }

    override fun getAllRecords(): Flow<List<MedicalRecord>> =
        recordsFlow.map { map ->
            map.values.sortedByDescending { it.visitDate }
        }

    override suspend fun getRecordForAppointment(appointmentId: String): Result<MedicalRecord?> {
        return Result.success(recordsFlow.value[appointmentId])
    }

    override suspend fun submitMedicalRecord(record: MedicalRecord): Result<Unit> {
        val toSave = record.copy(createdAt = Timestamp.now())
        val current = recordsFlow.value.toMutableMap()
        current[record.appointmentId] = toSave
        recordsFlow.value = current

        appointmentRepo?.let { repo ->
            val apt = repo.getAppointmentById(record.appointmentId)
            if (apt != null) {
                repo.saveAppointmentDirect(apt.copy(status = AppointmentStatus.COMPLETED.name))
            }
        }
        return Result.success(Unit)
    }
}
