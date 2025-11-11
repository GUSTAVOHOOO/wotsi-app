package br.edu.utfpr.cc.setac.wotsiapp.data.model

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val type: MessageType = MessageType.TEXT,
    val imageUrl: String? = null,
    val timestamp: Timestamp = Timestamp.now(),
    val status: MessageStatus = MessageStatus.SENT
) {
    constructor() : this("", "", "", "", "", MessageType.TEXT, null, Timestamp.now(), MessageStatus.SENT)
}

enum class MessageType {
    TEXT,
    IMAGE
}

enum class MessageStatus {
    SENDING,
    SENT,
    ERROR
}

