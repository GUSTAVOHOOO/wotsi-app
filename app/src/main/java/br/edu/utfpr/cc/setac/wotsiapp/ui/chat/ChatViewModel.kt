package br.edu.utfpr.cc.setac.wotsiapp.ui.chat

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.utfpr.cc.setac.wotsiapp.data.model.Message
import br.edu.utfpr.cc.setac.wotsiapp.data.model.User
import br.edu.utfpr.cc.setac.wotsiapp.data.repository.AuthRepository
import br.edu.utfpr.cc.setac.wotsiapp.data.repository.ChatRepository
import br.edu.utfpr.cc.setac.wotsiapp.data.repository.UserRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val analytics: FirebaseAnalytics,
    private val crashlytics: FirebaseCrashlytics
) : ViewModel() {

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _sendMessageSuccess = MutableLiveData<Boolean>()
    val sendMessageSuccess: LiveData<Boolean> = _sendMessageSuccess

    private val _otherUser = MutableLiveData<User>()
    val otherUser: LiveData<User> = _otherUser

    private var conversationId: String = ""
    private var otherUserId: String = ""

    fun setConversationId(id: String, otherParticipantId: String) {
        conversationId = id
        otherUserId = otherParticipantId
        loadMessages()
        markMessagesAsRead()
        loadOtherUserInfo()

        // Log analytics event
        analytics.logEvent("conversation_opened", null)
    }

    private fun loadOtherUserInfo() {
        viewModelScope.launch {
            userRepository.observeUser(otherUserId)
                .catch { exception ->
                    crashlytics.recordException(exception)
                }
                .collect { user ->
                    user?.let { _otherUser.value = it }
                }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            _loading.value = true
            chatRepository.getMessages(conversationId)
                .catch { exception ->
                    _loading.value = false
                    _error.value = exception.message ?: "Failed to load messages"

                    // Log non-fatal error to Crashlytics
                    crashlytics.recordException(exception)
                }
                .collect { messageList ->
                    _loading.value = false
                    _messages.value = messageList
                }
        }
    }

    fun sendTextMessage(content: String) {
        if (content.isBlank()) return

        val user = authRepository.currentUser ?: return

        viewModelScope.launch {
            val result = chatRepository.sendTextMessage(
                conversationId = conversationId,
                senderId = user.uid,
                senderName = user.displayName ?: user.email ?: "Unknown",
                content = content
            )

            result.fold(
                onSuccess = {
                    _sendMessageSuccess.value = true

                    // Log analytics event
                    analytics.logEvent("message_sent", android.os.Bundle().apply {
                        putString("message_type", "text")
                    })
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to send message"

                    // Log error to Crashlytics
                    crashlytics.log("Failed to send text message")
                    crashlytics.recordException(exception)
                }
            )
        }
    }

    fun sendImageMessage(imageUri: Uri) {
        val user = authRepository.currentUser ?: return

        viewModelScope.launch {
            _loading.value = true
            val result = chatRepository.sendImageMessage(
                conversationId = conversationId,
                senderId = user.uid,
                senderName = user.displayName ?: user.email ?: "Unknown",
                imageUri = imageUri
            )
            _loading.value = false

            result.fold(
                onSuccess = {
                    _sendMessageSuccess.value = true

                    // Log analytics event
                    analytics.logEvent("message_sent", android.os.Bundle().apply {
                        putString("message_type", "image")
                    })
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to send image"

                    // Log error to Crashlytics
                    crashlytics.log("Failed to send image message")
                    crashlytics.recordException(exception)
                }
            )
        }
    }

    private fun markMessagesAsRead() {
        val userId = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            chatRepository.markMessagesAsRead(conversationId, userId)
        }
    }

    fun isMyMessage(message: Message): Boolean {
        return message.senderId == authRepository.currentUser?.uid
    }
}

