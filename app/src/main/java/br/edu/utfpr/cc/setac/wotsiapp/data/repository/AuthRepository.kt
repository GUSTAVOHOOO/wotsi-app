package br.edu.utfpr.cc.setac.wotsiapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import br.edu.utfpr.cc.setac.wotsiapp.data.model.User
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isUserLoggedIn: Boolean
        get() = currentUser != null

    suspend fun signUp(email: String, password: String, displayName: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User creation failed")

            // Update profile
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            user.updateProfile(profileUpdates).await()

            // Send email verification
            user.sendEmailVerification().await()

            // Create user document in Firestore
            val userDoc = User(
                uid = user.uid,
                email = email,
                displayName = displayName,
                createdAt = Timestamp.now(),
                lastSeen = Timestamp.now(),
                isOnline = true
            )
            firestore.collection("users").document(user.uid).set(userDoc).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Sign in failed")

            // Update user online status
            updateUserOnlineStatus(user.uid, true)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        currentUser?.let { user ->
            updateUserOnlineStatus(user.uid, false)
        }
        auth.signOut()
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resendVerificationEmail(): Result<Unit> {
        return try {
            currentUser?.sendEmailVerification()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reloadUser() {
        currentUser?.reload()?.await()
    }

    private suspend fun updateUserOnlineStatus(uid: String, isOnline: Boolean) {
        try {
            firestore.collection("users").document(uid)
                .update(
                    mapOf(
                        "isOnline" to isOnline,
                        "lastSeen" to Timestamp.now()
                    )
                ).await()
        } catch (e: Exception) {
            // Ignore errors when updating online status
        }
    }

    suspend fun updateProfile(displayName: String, photoUrl: String?): Result<Unit> {
        return try {
            val user = currentUser ?: throw Exception("User not logged in")

            // Update Firebase Auth profile
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)

            if (photoUrl != null) {
                profileUpdates.setPhotoUri(android.net.Uri.parse(photoUrl))
            }

            user.updateProfile(profileUpdates.build()).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

