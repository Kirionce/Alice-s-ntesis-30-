package com.example.data.firebase

import androidx.annotation.Keep
import com.example.data.ai.GeminiModelOption
import com.example.data.local.ChatChannel
import com.example.data.local.MessageSpeaker
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class CloudUserPreferences(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Administrador Kirionce",
    val affectionateMode: Boolean = true,
    val autoVoiceReply: Boolean = true,
    val selectedModel: String = GeminiModelOption.DEFAULT.modelId,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val channelId: String = ChatChannel.ALICE.id,
    val speaker: String = MessageSpeaker.USER.id,
    val content: String = "",
    val errorFlag: Boolean = false,
    val sequenceOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun displayTimestampMillis(): Long {
        return createdAt?.toDate()?.time ?: sequenceOrder.takeIf { it > 0L } ?: System.currentTimeMillis()
    }
}
