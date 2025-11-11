package br.edu.utfpr.cc.setac.wotsiapp.ui.conversations

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.utfpr.cc.setac.wotsiapp.data.model.Conversation
import br.edu.utfpr.cc.setac.wotsiapp.data.repository.AuthRepository
import br.edu.utfpr.cc.setac.wotsiapp.data.repository.ChatRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ConversationsViewModel(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _conversations = MutableLiveData<List<Conversation>>()
    val conversations: LiveData<List<Conversation>> = _conversations

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    init {
        loadConversations()
    }

    private fun loadConversations() {
        val userId = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            _loading.value = true
            chatRepository.getConversations(userId)
                .catch { exception ->
                    _loading.value = false
                    _error.value = exception.message ?: "Failed to load conversations"
                }
                .collect { conversationList ->
                    _loading.value = false
                    _conversations.value = conversationList
                }
        }
    }

    fun getOtherParticipantName(conversation: Conversation): String {
        val currentUserId = authRepository.currentUser?.uid ?: return ""
        val otherUserId = conversation.participants.firstOrNull { it != currentUserId } ?: return ""
        return conversation.participantsInfo[otherUserId]?.name ?: "Unknown"
    }

    fun getOtherParticipantId(conversation: Conversation): String? {
        val currentUserId = authRepository.currentUser?.uid ?: return null
        return conversation.participants.firstOrNull { it != currentUserId }
    }

    fun getOtherParticipantPhotoUrl(conversation: Conversation): String? {
        val currentUserId = authRepository.currentUser?.uid ?: return null
        val otherUserId = conversation.participants.firstOrNull { it != currentUserId } ?: return null
        return conversation.participantsInfo[otherUserId]?.photoUrl
    }

    fun getUnreadCount(conversation: Conversation): Int {
        val currentUserId = authRepository.currentUser?.uid ?: return 0
        return conversation.unreadCount[currentUserId] ?: 0
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}
