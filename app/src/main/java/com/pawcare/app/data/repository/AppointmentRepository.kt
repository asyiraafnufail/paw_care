package com.pawcare.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.pawcare.app.data.model.Appointment
import com.pawcare.app.data.model.AppointmentStatus
import com.pawcare.app.data.model.BookedSlot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

interface AppointmentRepository {
    fun getOwnerAppointments(ownerId: String): Flow<List<Appointment>>
    fun getPendingAppointments(): Flow<List<Appointment>>
    fun getConfirmedAppointments(): Flow<List<Appointment>>
    fun getArchiveAppointments(): Flow<List<Appointment>>
    fun getBookedSlots(date: String): Flow<Set<String>>
    suspend fun getAppointment(appointmentId: String): Result<Appointment?>
    suspend fun createBooking(appointment: Appointment): Result<Unit>
    suspend fun cancelOwnerBooking(appointmentId: String, slotKey: String): Result<Unit>
    suspend fun confirmAppointment(appointmentId: String): Result<Unit>
    suspend fun cancelAppointmentByAdmin(appointmentId: String, slotKey: String, reason: String): Result<Unit>
}

class FirebaseAppointmentRepository(
    private val firestore: FirebaseFirestore
) : AppointmentRepository {

    override fun getOwnerAppointments(ownerId: String): Flow<List<Appointment>> = callbackFlow {
        val reg = firestore.collection("appointments")
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(Appointment::class.java) } ?: emptyList()
                // Sort in memory by date and slot
                trySend(list.sortedWith(compareBy({ it.date }, { it.timeSlot })))
            }
        awaitClose { reg.remove() }
    }

    override fun getPendingAppointments(): Flow<List<Appointment>> = callbackFlow {
        val reg = firestore.collection("appointments")
            .whereEqualTo("status", AppointmentStatus.PENDING.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(Appointment::class.java) } ?: emptyList()
                trySend(list.sortedWith(compareBy({ it.date }, { it.timeSlot })))
            }
        awaitClose { reg.remove() }
    }

    override fun getConfirmedAppointments(): Flow<List<Appointment>> = callbackFlow {
        val reg = firestore.collection("appointments")
            .whereEqualTo("status", AppointmentStatus.CONFIRMED.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(Appointment::class.java) } ?: emptyList()
                trySend(list.sortedWith(compareBy({ it.date }, { it.timeSlot })))
            }
        awaitClose { reg.remove() }
    }

    override fun getArchiveAppointments(): Flow<List<Appointment>> = callbackFlow {
        // Single-field whereIn avoids composite index requirements
        val reg = firestore.collection("appointments")
            .whereIn("status", listOf(AppointmentStatus.COMPLETED.name, AppointmentStatus.CANCELLED.name))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val archiveList = snapshot?.documents?.mapNotNull { it.toObject(Appointment::class.java) }
                    ?.sortedWith(compareByDescending<Appointment> { it.date }.thenByDescending { it.timeSlot })
                    ?: emptyList()
                trySend(archiveList)
            }
        awaitClose { reg.remove() }
    }

    override fun getBookedSlots(date: String): Flow<Set<String>> = callbackFlow {
        val reg = firestore.collection("booked_slots")
            .whereEqualTo("date", date)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptySet())
                    return@addSnapshotListener
                }
                val slots = snapshot?.documents?.mapNotNull { it.getString("timeSlot") }?.toSet() ?: emptySet()
                trySend(slots)
            }
        awaitClose { reg.remove() }
    }

    override suspend fun getAppointment(appointmentId: String): Result<Appointment?> = runCatching {
        val doc = firestore.collection("appointments").document(appointmentId).get().await()
        doc.toObject(Appointment::class.java)
    }

    override suspend fun createBooking(appointment: Appointment): Result<Unit> = runCatching {
        val id = if (appointment.appointmentId.isBlank()) UUID.randomUUID().toString() else appointment.appointmentId
        val slotKey = "${appointment.date}_${appointment.timeSlot}"
        val toSave = appointment.copy(
            appointmentId = id,
            slotKey = slotKey,
            status = AppointmentStatus.PENDING.name,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )

        firestore.runTransaction { tx ->
            val slotDocRef = firestore.collection("booked_slots").document(slotKey)
            val slotSnapshot = tx.get(slotDocRef)
            if (slotSnapshot.exists()) {
                throw IllegalStateException("Slot waktu ini sudah dipesan orang lain. Silakan pilih slot lain.")
            }

            val slotData = BookedSlot(
                appointmentId = id,
                ownerId = appointment.ownerId,
                date = appointment.date,
                timeSlot = appointment.timeSlot
            )

            tx.set(slotDocRef, slotData)
            tx.set(firestore.collection("appointments").document(id), toSave)
        }.await()
    }

    override suspend fun cancelOwnerBooking(appointmentId: String, slotKey: String): Result<Unit> = runCatching {
        firestore.runTransaction { tx ->
            val aptRef = firestore.collection("appointments").document(appointmentId)
            val aptSnapshot = tx.get(aptRef)
            val currentStatus = aptSnapshot.getString("status")
            if (currentStatus != AppointmentStatus.PENDING.name) {
                throw IllegalStateException("Hanya janji temu dengan status menunggu konfirmasi yang dapat dibatalkan.")
            }

            tx.update(aptRef, mapOf(
                "status" to AppointmentStatus.CANCELLED.name,
                "rejectionReason" to "Dibatalkan oleh pemilik",
                "updatedAt" to Timestamp.now()
            ))

            if (slotKey.isNotBlank()) {
                tx.delete(firestore.collection("booked_slots").document(slotKey))
            }
        }.await()
    }

    override suspend fun confirmAppointment(appointmentId: String): Result<Unit> = runCatching {
        firestore.collection("appointments").document(appointmentId).update(
            mapOf(
                "status" to AppointmentStatus.CONFIRMED.name,
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    override suspend fun cancelAppointmentByAdmin(
        appointmentId: String,
        slotKey: String,
        reason: String
    ): Result<Unit> = runCatching {
        firestore.runTransaction { tx ->
            val aptRef = firestore.collection("appointments").document(appointmentId)
            tx.update(aptRef, mapOf(
                "status" to AppointmentStatus.CANCELLED.name,
                "rejectionReason" to reason.trim(),
                "updatedAt" to Timestamp.now()
            ))
            if (slotKey.isNotBlank()) {
                tx.delete(firestore.collection("booked_slots").document(slotKey))
            }
        }.await()
    }
}

class FakeAppointmentRepository : AppointmentRepository {
    private val appointmentsFlow = MutableStateFlow<Map<String, Appointment>>(emptyMap())
    private val bookedSlotsFlow = MutableStateFlow<Map<String, BookedSlot>>(emptyMap())

    fun allAppointments(): List<Appointment> = appointmentsFlow.value.values.toList()

    fun getAppointmentById(id: String): Appointment? = appointmentsFlow.value[id]

    fun saveAppointmentDirect(appointment: Appointment) {
        val current = appointmentsFlow.value.toMutableMap()
        current[appointment.appointmentId] = appointment
        appointmentsFlow.value = current
    }

    override fun getOwnerAppointments(ownerId: String): Flow<List<Appointment>> =
        appointmentsFlow.map { map ->
            map.values.filter { it.ownerId == ownerId }
                .sortedWith(compareBy({ it.date }, { it.timeSlot }))
        }

    override fun getPendingAppointments(): Flow<List<Appointment>> =
        appointmentsFlow.map { map ->
            map.values.filter { it.status == AppointmentStatus.PENDING.name }
                .sortedWith(compareBy({ it.date }, { it.timeSlot }))
        }

    override fun getConfirmedAppointments(): Flow<List<Appointment>> =
        appointmentsFlow.map { map ->
            map.values.filter { it.status == AppointmentStatus.CONFIRMED.name }
                .sortedWith(compareBy({ it.date }, { it.timeSlot }))
        }

    override fun getArchiveAppointments(): Flow<List<Appointment>> =
        appointmentsFlow.map { map ->
            map.values.filter {
                it.status == AppointmentStatus.COMPLETED.name || it.status == AppointmentStatus.CANCELLED.name
            }.sortedWith(compareByDescending<Appointment> { it.date }.thenByDescending { it.timeSlot })
        }

    override fun getBookedSlots(date: String): Flow<Set<String>> =
        bookedSlotsFlow.map { map ->
            map.values.filter { it.date == date }.map { it.timeSlot }.toSet()
        }

    override suspend fun getAppointment(appointmentId: String): Result<Appointment?> {
        return Result.success(appointmentsFlow.value[appointmentId])
    }

    override suspend fun createBooking(appointment: Appointment): Result<Unit> {
        val slotKey = "${appointment.date}_${appointment.timeSlot}"
        if (bookedSlotsFlow.value.containsKey(slotKey)) {
            return Result.failure(IllegalStateException("Slot waktu ini sudah dipesan orang lain. Silakan pilih slot lain."))
        }

        val id = if (appointment.appointmentId.isBlank()) "apt_${System.currentTimeMillis()}" else appointment.appointmentId
        val toSave = appointment.copy(
            appointmentId = id,
            slotKey = slotKey,
            status = AppointmentStatus.PENDING.name,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )

        val slots = bookedSlotsFlow.value.toMutableMap()
        slots[slotKey] = BookedSlot(appointmentId = id, ownerId = appointment.ownerId, date = appointment.date, timeSlot = appointment.timeSlot)
        bookedSlotsFlow.value = slots

        val current = appointmentsFlow.value.toMutableMap()
        current[id] = toSave
        appointmentsFlow.value = current

        return Result.success(Unit)
    }

    override suspend fun cancelOwnerBooking(appointmentId: String, slotKey: String): Result<Unit> {
        val current = appointmentsFlow.value[appointmentId]
            ?: return Result.failure(IllegalArgumentException("Janji tidak ditemukan"))
        if (current.status != AppointmentStatus.PENDING.name) {
            return Result.failure(IllegalStateException("Hanya janji temu dengan status menunggu konfirmasi yang dapat dibatalkan."))
        }

        val updated = current.copy(status = AppointmentStatus.CANCELLED.name, rejectionReason = "Dibatalkan oleh pemilik")
        val apts = appointmentsFlow.value.toMutableMap()
        apts[appointmentId] = updated
        appointmentsFlow.value = apts

        val slots = bookedSlotsFlow.value.toMutableMap()
        slots.remove(slotKey)
        bookedSlotsFlow.value = slots

        return Result.success(Unit)
    }

    override suspend fun confirmAppointment(appointmentId: String): Result<Unit> {
        val current = appointmentsFlow.value[appointmentId]
            ?: return Result.failure(IllegalArgumentException("Janji tidak ditemukan"))
        val updated = current.copy(status = AppointmentStatus.CONFIRMED.name)
        val apts = appointmentsFlow.value.toMutableMap()
        apts[appointmentId] = updated
        appointmentsFlow.value = apts
        return Result.success(Unit)
    }

    override suspend fun cancelAppointmentByAdmin(
        appointmentId: String,
        slotKey: String,
        reason: String
    ): Result<Unit> {
        val current = appointmentsFlow.value[appointmentId]
            ?: return Result.failure(IllegalArgumentException("Janji tidak ditemukan"))
        val updated = current.copy(status = AppointmentStatus.CANCELLED.name, rejectionReason = reason.trim())
        val apts = appointmentsFlow.value.toMutableMap()
        apts[appointmentId] = updated
        appointmentsFlow.value = apts

        val slots = bookedSlotsFlow.value.toMutableMap()
        slots.remove(slotKey)
        bookedSlotsFlow.value = slots

        return Result.success(Unit)
    }
}
