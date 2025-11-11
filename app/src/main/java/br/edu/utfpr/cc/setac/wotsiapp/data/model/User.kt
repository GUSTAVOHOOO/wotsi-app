package br.edu.utfpr.cc.setac.wotsiapp.data.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val lastSeen: Timestamp = Timestamp.now(),
    val isOnline: Boolean = false
) {
    // Firestore requires a no-arg constructor
    constructor() : this("", "", "", null, Timestamp.now(), Timestamp.now(), false)
}

