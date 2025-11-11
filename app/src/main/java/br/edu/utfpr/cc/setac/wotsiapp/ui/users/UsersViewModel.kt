package br.edu.utfpr.cc.setac.wotsiapp.ui.users

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.utfpr.cc.setac.wotsiapp.data.model.User
import br.edu.utfpr.cc.setac.wotsiapp.data.repository.AuthRepository
import br.edu.utfpr.cc.setac.wotsiapp.data.repository.ChatRepository
import br.edu.utfpr.cc.setac.wotsiapp.data.repository.UserRepository
import kotlinx.coroutines.launch

class UsersViewModel(
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _conversationCreated = MutableLiveData<String>()
    val conversationCreated: LiveData<String> = _conversationCreated

    fun loadUsers() {
        viewModelScope.launch {
            _loading.value = true
            val result = userRepository.getAllUsers()
            _loading.value = false

            result.fold(
                onSuccess = { userList ->
                    // Filter out current user
                    val currentUserId = authRepository.currentUser?.uid
                    _users.value = userList.filter { it.uid != currentUserId }
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to load users"
                }
            )
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            loadUsers()
            return
        }

        viewModelScope.launch {
            _loading.value = true
            val result = userRepository.searchUsers(query)
            _loading.value = false

            result.fold(
                onSuccess = { userList ->
                    // Filter out current user
                    val currentUserId = authRepository.currentUser?.uid
                    _users.value = userList.filter { it.uid != currentUserId }
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to search users"
                }
            )
        }
    }

    fun startConversation(otherUser: User) {
        val currentUser = authRepository.currentUser ?: return

        viewModelScope.launch {
            _loading.value = true
            val result = chatRepository.createConversation(
                currentUserId = currentUser.uid,
                currentUserName = currentUser.displayName ?: currentUser.email ?: "Unknown",
                otherUserId = otherUser.uid,
                otherUserName = otherUser.displayName,
                otherUserPhotoUrl = otherUser.photoUrl
            )
            _loading.value = false

            result.fold(
                onSuccess = { conversationId ->
                    _conversationCreated.value = conversationId
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to create conversation"
                }
            )
        }
    }
}

