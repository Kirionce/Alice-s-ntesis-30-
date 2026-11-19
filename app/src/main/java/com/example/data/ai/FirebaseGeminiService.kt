package com.example.data.ai

import com.example.data.firebase.CloudChatMessage
import com.example.data.local.MessageSpeaker
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

enum class GeminiModelOption(
    val modelId: String,
    val displayName: String,
    val description: String
) {
    FLASH_3_8(
        modelId = "gemini-3.8-flash",
        displayName = "Gemini 3.8 Flash",
        description = "Recomendado · Rápido, versátil y equilibrado"
    ),
    FLASH_LITE_3_5(
        modelId = "gemini-3.5-flash-lite",
        displayName = "Gemini 3.5 Flash-Lite",
        description = "Ultra rápido · Respuestas ágiles y ligeras"
    ),
    ARGON_4(
        modelId = "gemini-4-argon",
        displayName = "Gemini 4 Argon",
        description = "Razonamiento avanzado · Análisis profundo y código"
    );

    companion object {
        val DEFAULT = FLASH_3_8

        fun fromModelId(id: String?): GeminiModelOption =
            entries.find { it.modelId == id } ?: DEFAULT
    }
}

class FirebaseGeminiService {

    private val aiBackend by lazy { Firebase.ai }

    fun streamReply(
        modelOption: GeminiModelOption,
        systemPrompt: String,
        history: List<CloudChatMessage>,
        currentUserMessage: String
    ): Flow<String> {
        val generativeModel = aiBackend.generativeModel(
            modelName = modelOption.modelId,
            generationConfig = generationConfig {
                temperature = 0.8f
                maxOutputTokens = 1024
            },
            systemInstruction = content {
                text(systemPrompt)
            }
        )

        val chatHistory = history
            .filter { !it.errorFlag && it.speaker != MessageSpeaker.SYSTEM.id }
            .map { msg ->
                val role = if (msg.speaker == MessageSpeaker.USER.id) "user" else "model"
                val text = when (msg.speaker) {
                    MessageSpeaker.ALICE.id -> "Alice: ${msg.content}"
                    MessageSpeaker.YUI.id -> "Yui: ${msg.content}"
                    else -> msg.content
                }
                content(role) { text(text) }
            }

        val chat = generativeModel.startChat(history = chatHistory)
        return chat.sendMessageStream(currentUserMessage).mapNotNull { chunk ->
            chunk.text
        }
    }
}
