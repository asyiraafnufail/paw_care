package com.pawcare.app.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.pawcare.app.data.repository.AppointmentRepository
import com.pawcare.app.data.repository.AuthRepository
import com.pawcare.app.data.repository.FirebaseAppointmentRepository
import com.pawcare.app.data.repository.FirebaseAuthRepository
import com.pawcare.app.data.repository.FirebaseMedicalRecordRepository
import com.pawcare.app.data.repository.FirebasePetRepository
import com.pawcare.app.data.repository.MedicalRecordRepository
import com.pawcare.app.data.repository.PetRepository

class AppContainer {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
        }
    }

    val authRepository: AuthRepository by lazy {
        FirebaseAuthRepository(auth, firestore)
    }

    val petRepository: PetRepository by lazy {
        FirebasePetRepository(firestore)
    }

    val appointmentRepository: AppointmentRepository by lazy {
        FirebaseAppointmentRepository(firestore)
    }

    val medicalRecordRepository: MedicalRecordRepository by lazy {
        FirebaseMedicalRecordRepository(firestore)
    }
}
