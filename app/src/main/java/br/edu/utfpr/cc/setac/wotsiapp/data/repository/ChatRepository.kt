package br.edu.utfpr.cc.setac.wotsiapp.data.repository

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import br.edu.utfpr.cc.setac.wotsiapp.data.model.Conversation
import br.edu.utfpr.cc.setac.wotsiapp.data.model.Message
import br.edu.utfpr.cc.setac.wotsiapp.data.model.MessageType
import br.edu.utfpr.cc.setac.wotsiapp.data.model.MessageStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {

    fun getConversations(userId: String): Flow<List<Conversation>> = callbackFlow {
        val listener = firestore.collection("conversations")
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val conversations = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Conversation::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(conversations)
            }

        awaitClose { listener.remove() }
    }

    fun getMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    suspend fun sendTextMessage(
        conversationId: String,
        senderId: String,
        senderName: String,
        content: String
    ): Result<Message> {
        return try {
            val messageId = UUID.randomUUID().toString()
            val message = Message(
                id = messageId,
                conversationId = conversationId,
                senderId = senderId,
                senderName = senderName,
                content = content,
                type = MessageType.TEXT,
                timestamp = Timestamp.now(),
                status = MessageStatus.SENT
            )

            // Add message to subcollection
            firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId)
                .set(message)
                .await()

            // Update conversation last message
            updateConversationLastMessage(conversationId, senderId, content, MessageType.TEXT)

            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendImageMessage(
        conversationId: String,
        senderId: String,
        senderName: String,
        imageUri: Uri
    ): Result<Message> {
        return try {
            // Upload image to Firebase Storage
            val imageUrl = uploadImage(conversationId, imageUri)

            val messageId = UUID.randomUUID().toString()
            val message = Message(
                id = messageId,
                conversationId = conversationId,
                senderId = senderId,
                senderName = senderName,
                content = "Image",
                type = MessageType.IMAGE,
                imageUrl = imageUrl,
                timestamp = Timestamp.now(),
                status = MessageStatus.SENT
            )

            // Add message to subcollection
            firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId)
                .set(message)
                .await()

            // Update conversation last message
            updateConversationLastMessage(conversationId, senderId, "📷 Image", MessageType.IMAGE)

            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createConversation(
        currentUserId: String,
        currentUserName: String,
        otherUserId: String,
        otherUserName: String,
        otherUserPhotoUrl: String?
    ): Result<String> {
        return try {
            // Check if conversation already exists
            val existingConversation = firestore.collection("conversations")
                .whereArrayContains("participants", currentUserId)
                .get()
                .await()
                .documents
                .firstOrNull { doc ->
                    val participants = doc.get("participants") as? List<*>
                    participants?.contains(otherUserId) == true
                }

            if (existingConversation != null) {
                return Result.success(existingConversation.id)
            }

            // Create new conversation
            val conversationId = UUID.randomUUID().toString()
            val conversation = Conversation(
                id = conversationId,
                participants = listOf(currentUserId, otherUserId),
                participantsInfo = mapOf(
                    currentUserId to Conversation.ParticipantInfo(currentUserName, null),
                    otherUserId to Conversation.ParticipantInfo(otherUserName, otherUserPhotoUrl)
                ),
                createdAt = Timestamp.now(),
                lastMessageTimestamp = Timestamp.now()
            )

            firestore.collection("conversations")
                .document(conversationId)
                .set(conversation)
                .await()

            Result.success(conversationId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateConversationLastMessage(
        conversationId: String,
        senderId: String,
        content: String,
        type: MessageType
    ) {
        firestore.collection("conversations")
            .document(conversationId)
            .update(
                mapOf(
                    "lastMessage" to content,
                    "lastMessageType" to type.name,
                    "lastMessageSenderId" to senderId,
                    "lastMessageTimestamp" to Timestamp.now()
                )
            )
            .await()
    }

    private suspend fun uploadImage(conversationId: String, imageUri: Uri): String {
        val imageId = UUID.randomUUID().toString()
        val imageRef = storage.reference
            .child("chat_images/$conversationId/$imageId.jpg")

        imageRef.putFile(imageUri).await()
        return imageRef.downloadUrl.await().toString()
    }

    suspend fun markMessagesAsRead(conversationId: String, userId: String) {
        try {
            firestore.collection("conversations")
                .document(conversationId)
                .update("unreadCount.$userId", 0)
                .await()
        } catch (e: Exception) {
            // Ignore errors
        }
    }
}

