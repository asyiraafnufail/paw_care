package com.pawcare.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.pawcare.app.data.model.Role
import com.pawcare.app.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun getCurrentUser(): User?
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signUp(email: String, password: String): Result<User>
    suspend fun signOut()
}

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override val currentUser: Flow<User?> = callbackFlow {
        var snapshotRegistration: ListenerRegistration? = null
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            snapshotRegistration?.remove()
            snapshotRegistration = null
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                trySend(null)
            } else {
                snapshotRegistration = firestore.collection("users").document(firebaseUser.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(null)
                            return@addSnapshotListener
                        }
                        val user = snapshot?.toObject(User::class.java)
                        trySend(user)
                    }
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose {
            snapshotRegistration?.remove()
            auth.removeAuthStateListener(listener)
        }
    }

    override suspend fun getCurrentUser(): User? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            doc.toObject(User::class.java)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun signIn(email: String, password: String): Result<User> = runCatching {
        val authResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
        val uid = authResult.user?.uid ?: throw IllegalStateException("User ID tidak ditemukan")
        val doc = firestore.collection("users").document(uid).get().await()
        val user = doc.toObject(User::class.java)
            ?: throw IllegalStateException("Profil user tidak ditemukan di database")
        user
    }

    override suspend fun signUp(email: String, password: String): Result<User> = runCatching {
        val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val uid = authResult.user?.uid ?: throw IllegalStateException("Gagal membuat user")
        val newUser = User(
            uid = uid,
            email = email.trim(),
            role = Role.OWNER.value,
            createdAt = Timestamp.now()
        )
        firestore.collection("users").document(uid).set(newUser).await()
        newUser
    }

    override suspend fun signOut() {
        auth.signOut()
    }
}

class FakeAuthRepository(
    initialUser: User? = null
) : AuthRepository {
    private val _currentUserFlow = MutableStateFlow<User?>(initialUser)
    override val currentUser: Flow<User?> = _currentUserFlow.asStateFlow()

    private val usersDb = mutableMapOf<String, Pair<User, String>>()

    init {
        if (initialUser != null) {
            usersDb[initialUser.email.lowercase()] = Pair(initialUser, "password")
        }
    }

    fun setMockUser(user: User?) {
        _currentUserFlow.value = user
    }

    override suspend fun getCurrentUser(): User? = _currentUserFlow.value

    override suspend fun signIn(email: String, password: String): Result<User> {
        val normalized = email.trim().lowercase()
        val entry = usersDb[normalized]
            ?: return Result.failure(IllegalArgumentException("Email atau password salah"))
        if (entry.second != password) {
            return Result.failure(IllegalArgumentException("Email atau password salah"))
        }
        _currentUserFlow.value = entry.first
        return Result.success(entry.first)
    }

    override suspend fun signUp(email: String, password: String): Result<User> {
        val normalized = email.trim().lowercase()
        if (usersDb.containsKey(normalized)) {
            return Result.failure(IllegalArgumentException("Email sudah terdaftar"))
        }
        val newUser = User(
            uid = "user_${System.currentTimeMillis()}",
            email = email.trim(),
            role = Role.OWNER.value,
            createdAt = Timestamp.now()
        )
        usersDb[normalized] = Pair(newUser, password)
        _currentUserFlow.value = newUser
        return Result.success(newUser)
    }

    override suspend fun signOut() {
        _currentUserFlow.value = null
    }
}
