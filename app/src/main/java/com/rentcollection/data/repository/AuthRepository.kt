package com.rentcollection.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.rentcollection.data.model.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() = auth.signOut()

    suspend fun getCurrentUserProfile(): User? {
        val uid = currentUser?.uid ?: return null
        return try {
            firestore.collection("users").document(uid).get().await()
                .toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * One-time setup: creates owner@rent.com and father@rent.com with password Rent@1234.
     * Safe to call multiple times — already-existing accounts are silently skipped.
     * After calling, both accounts are signed out so the user can log in normally.
     */
    suspend fun seedUsers() {
        val accounts = listOf(
            Triple("owner@rent.com", "Owner", "owner"),
            Triple("father@rent.com", "Father", "viewer")
        )
        for ((email, name, role) in accounts) {
            try {
                val result = auth.createUserWithEmailAndPassword(email, "Rent@1234").await()
                result.user?.let { user ->
                    firestore.collection("users").document(user.uid).set(
                        mapOf("name" to name, "email" to email, "role" to role)
                    ).await()
                }
            } catch (_: Exception) {
                // Account already exists — ignore
            }
            try { auth.signOut() } catch (_: Exception) {}
        }
    }
}
