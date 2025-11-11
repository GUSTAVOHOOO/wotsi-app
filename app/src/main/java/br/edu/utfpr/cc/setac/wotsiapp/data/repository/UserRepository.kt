package br.edu.utfpr.cc.setac.wotsiapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import br.edu.utfpr.cc.setac.wotsiapp.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val firestore: FirebaseFirestore
) {

    suspend fun getUser(userId: String): Result<User> {
        return try {
            val document = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            val user = document.toObject(User::class.java)
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val snapshot = firestore.collection("users")
                .get()
                .await()

            val users = snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)
            }

            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            val snapshot = firestore.collection("users")
                .get()
                .await()

            val users = snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)
            }.filter { user ->
                user.displayName.contains(query, ignoreCase = true) ||
                user.email.contains(query, ignoreCase = true)
            }

            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeUser(userId: String): Flow<User?> = callbackFlow {
        val listener = firestore.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val user = snapshot?.toObject(User::class.java)
                trySend(user)
            }

        awaitClose { listener.remove() }
    }

    suspend fun updateUserProfile(userId: String, displayName: String, photoUrl: String?): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "displayName" to displayName
            )

            if (photoUrl != null) {
                updates["photoUrl"] = photoUrl
            }

            firestore.collection("users")
                .document(userId)
                .update(updates)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

