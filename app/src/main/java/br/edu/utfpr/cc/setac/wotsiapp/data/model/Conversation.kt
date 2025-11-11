package br.edu.utfpr.cc.setac.wotsiapp.data.model

import com.google.firebase.Timestamp

data class Conversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantsInfo: Map<String, ParticipantInfo> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageType: MessageType = MessageType.TEXT,
    val lastMessageSenderId: String = "",
    val lastMessageTimestamp: Timestamp = Timestamp.now(),
    val createdAt: Timestamp = Timestamp.now(),
    val unreadCount: Map<String, Int> = emptyMap()
) {
    constructor() : this("", emptyList(), emptyMap(), "", MessageType.TEXT, "", Timestamp.now(), Timestamp.now(), emptyMap())

    data class ParticipantInfo(
        val name: String = "",
        val photoUrl: String? = null
    ) {
        constructor() : this("", null)
    }
}

