package br.edu.utfpr.cc.setac.wotsiapp.ui.profile

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.utfpr.cc.setac.wotsiapp.data.model.User
import br.edu.utfpr.cc.setac.wotsiapp.data.repository.AuthRepository
import br.edu.utfpr.cc.setac.wotsiapp.data.repository.UserRepository
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileEditViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val storage: FirebaseStorage
) : ViewModel() {

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _profileUpdated = MutableLiveData<Boolean>()
    val profileUpdated: LiveData<Boolean> = _profileUpdated

    private val _imageUrl = MutableLiveData<String?>()
    val imageUrl: LiveData<String?> = _imageUrl

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            _loading.value = true
            val userId = authRepository.currentUser?.uid
            if (userId != null) {
                userRepository.getUser(userId).fold(
                    onSuccess = { user ->
                        _user.value = user
                        _imageUrl.value = user.photoUrl
                        _loading.value = false
                    },
                    onFailure = { e ->
                        _error.value = e.message ?: "Failed to load user"
                        _loading.value = false
                    }
                )
            } else {
                _error.value = "User not logged in"
                _loading.value = false
            }
        }
    }

    fun uploadProfileImage(uri: Uri) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val userId = authRepository.currentUser?.uid
                    ?: throw Exception("User not logged in")

                // Create a reference to the profile image
                val storageRef = storage.reference
                    .child("profile_images")
                    .child("$userId.jpg")

                // Upload the file
                storageRef.putFile(uri).await()

                // Get the download URL
                val downloadUrl = storageRef.downloadUrl.await().toString()

                _imageUrl.value = downloadUrl
                _loading.value = false
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to upload image"
                _loading.value = false
            }
        }
    }

    fun updateProfile(displayName: String) {
        if (displayName.isBlank()) {
            _error.value = "Name cannot be empty"
            return
        }

        viewModelScope.launch {
            _loading.value = true
            try {
                val userId = authRepository.currentUser?.uid
                    ?: throw Exception("User not logged in")

                val photoUrl = _imageUrl.value

                // Update Firebase Auth profile
                authRepository.updateProfile(displayName, photoUrl).fold(
                    onSuccess = {
                        // Update Firestore user document
                        userRepository.updateUserProfile(userId, displayName, photoUrl).fold(
                            onSuccess = {
                                _profileUpdated.value = true
                                _loading.value = false
                            },
                            onFailure = { e ->
                                _error.value = e.message ?: "Failed to update profile"
                                _loading.value = false
                            }
                        )
                    },
                    onFailure = { e ->
                        _error.value = e.message ?: "Failed to update profile"
                        _loading.value = false
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update profile"
                _loading.value = false
            }
        }
    }
}

