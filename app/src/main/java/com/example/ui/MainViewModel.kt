package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.FirebaseGeminiService
import com.example.data.ai.GeminiModelOption
import com.example.data.firebase.CloudChatMessage
import com.example.data.firebase.FirestoreChatRepository
import com.example.data.local.ChatChannel
import com.example.data.local.MessageSpeaker
import com.example.data.preferences.AppPreferences
import com.example.data.voice.SpeechItem
import com.example.data.voice.VoicePlaybackManager
import com.example.domain.PersonaPrompts
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "MainViewModel"

sealed interface MessagesUiState {
    data object Loading : MessagesUiState
    data class Success(val messages: List<CloudChatMessage>) : MessagesUiState
    data class Error(val message: String) : MessagesUiState
}

data class UiBanner(
    val message: String,
    val isError: Boolean = false
)

data class MainUiState(
    val selectedChannel: ChatChannel = ChatChannel.ALICE,
    val selectedModel: GeminiModelOption = GeminiModelOption.DEFAULT,
    val affectionateMode: Boolean = true,
    val autoVoiceReply: Boolean = true,
    val isSending: Boolean = false,
    val streamingPreviewText: String = "",
    val banner: UiBanner? = null,
    val userEmail: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val firestoreRepository: FirestoreChatRepository,
    private val geminiService: FirebaseGeminiService,
    private val preferences: AppPreferences,
    private val voicePlayer: VoicePlaybackManager,
    private val currentUserId: String,
    userEmail: String?
) : ViewModel() {

    private val _selectedChannel = MutableStateFlow(ChatChannel.ALICE)

    private val _uiState = MutableStateFlow(
        MainUiState(
            selectedChannel = ChatChannel.ALICE,
            selectedModel = preferences.getSelectedModel(),
            affectionateMode = preferences.isAffectionateModeEnabled(),
            autoVoiceReply = preferences.isAutoVoiceEnabled(),
            userEmail = userEmail
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val speakingMessageId: StateFlow<String?> = voicePlayer.speakingMessageId

    val messagesState: StateFlow<MessagesUiState> = _selectedChannel
        .flatMapLatest { channel ->
            firestoreRepository.observeMessages(currentUserId, channel)
                .map<List<CloudChatMessage>, MessagesUiState> { MessagesUiState.Success(it) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing Firestore messages", rootError)
                    emit(MessagesUiState.Error(rootError.message ?: "No se pudo sincronizar el historial de chat."))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = MessagesUiState.Loading
        )

    init {
        initializeAndObserveCloudPreferences()
    }

    private fun initializeAndObserveCloudPreferences() {
        viewModelScope.launch {
            val initResult = firestoreRepository.ensurePreferencesInitialized(
                userId = currentUserId,
                defaultAffectionate = preferences.isAffectionateModeEnabled(),
                defaultAutoVoice = preferences.isAutoVoiceEnabled(),
                defaultModel = preferences.getSelectedModel()
            )
            initResult.onFailure { err ->
                Log.w(TAG, "Could not initialize cloud preferences", err)
            }

            firestoreRepository.observePreferences(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing cloud preferences", firestoreError ?: error)
                }
                .collect { cloudPrefs ->
                    if (cloudPrefs != null) {
                        val model = GeminiModelOption.fromModelId(cloudPrefs.selectedModel)
                        preferences.setAffectionateModeEnabled(cloudPrefs.affectionateMode)
                        preferences.setAutoVoiceEnabled(cloudPrefs.autoVoiceReply)
                        preferences.setSelectedModel(model)
                        _uiState.update {
                            it.copy(
                                affectionateMode = cloudPrefs.affectionateMode,
                                autoVoiceReply = cloudPrefs.autoVoiceReply,
                                selectedModel = model
                            )
                        }
                    }
                }
        }
    }

    fun selectChannel(channel: ChatChannel) {
        _selectedChannel.value = channel
        _uiState.update { it.copy(selectedChannel = channel) }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(banner = null) }
    }

    fun setAffectionateMode(enabled: Boolean) {
        preferences.setAffectionateModeEnabled(enabled)
        _uiState.update { it.copy(affectionateMode = enabled) }
        viewModelScope.launch {
            firestoreRepository.updatePreferences(currentUserId, affectionateMode = enabled)
                .onFailure { err ->
                    _uiState.update { it.copy(banner = UiBanner(err.localizedMessage ?: "Error al sincronizar preferencia.", isError = true)) }
                }
        }
    }

    fun setAutoVoiceReply(enabled: Boolean) {
        preferences.setAutoVoiceEnabled(enabled)
        if (!enabled) {
            voicePlayer.stop()
        }
        _uiState.update {
            it.copy(
                autoVoiceReply = enabled,
                banner = UiBanner(
                    if (enabled) "Lectura automática en español activada."
                    else "Lectura automática en voz alta desactivada."
                )
            )
        }
        viewModelScope.launch {
            firestoreRepository.updatePreferences(currentUserId, autoVoiceReply = enabled)
        }
    }

    fun selectGeminiModel(model: GeminiModelOption) {
        preferences.setSelectedModel(model)
        _uiState.update {
            it.copy(
                selectedModel = model,
                banner = UiBanner("Modelo actualizado a ${model.displayName}.")
            )
        }
        viewModelScope.launch {
            firestoreRepository.updatePreferences(currentUserId, selectedModel = model)
        }
    }

    fun sendMessage(rawText: String, encourageDialogueBetweenThem: Boolean = false) {
        val text = rawText.trim()
        if (text.isEmpty() || _uiState.value.isSending) return

        val state = _uiState.value
        val channel = state.selectedChannel

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSending = true,
                    streamingPreviewText = "",
                    banner = null
                )
            }

            val historyBeforeSend = firestoreRepository
                .getRecentMessages(currentUserId, channel, limit = 16)
                .getOrDefault(emptyList())

            val saveUserResult = firestoreRepository.addMessage(
                userId = currentUserId,
                channel = channel,
                speaker = MessageSpeaker.USER,
                content = text,
                sequenceOrder = System.currentTimeMillis()
            )
            saveUserResult.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSending = false,
                        banner = UiBanner(err.localizedMessage ?: "No se pudo guardar el mensaje en Firestore.", isError = true)
                    )
                }
                return@launch
            }

            val systemPrompt = PersonaPrompts.buildSystemPrompt(
                channel = channel,
                affectionateMode = state.affectionateMode,
                encourageDialogueBetweenThem = encourageDialogueBetweenThem
            )

            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    geminiService.streamReply(
                        modelOption = state.selectedModel,
                        systemPrompt = systemPrompt,
                        history = historyBeforeSend,
                        currentUserMessage = text
                    ).collect { chunk ->
                        accumulated.append(chunk)
                        _uiState.update { it.copy(streamingPreviewText = accumulated.toString()) }
                    }
                }

                val finalReply = accumulated.toString().trim()
                if (finalReply.isNotEmpty()) {
                    val savedReplies = saveAssistantReply(channel, finalReply)
                    _uiState.update { it.copy(isSending = false, streamingPreviewText = "") }

                    if (_uiState.value.autoVoiceReply && savedReplies.isNotEmpty()) {
                        speakMessages(savedReplies)
                    }
                } else {
                    recordErrorMessage(channel, "No se recibió respuesta del modelo. Intenta de nuevo, Administrador Kirionce.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini streaming timed out", e)
                recordErrorMessage(channel, "La respuesta tardó más de 60 segundos. Intenta nuevamente, Administrador Kirionce.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI generation failed", e)
                val errMsg = e.localizedMessage?.take(180)
                    ?: "No se pudo completar la respuesta de IA. Verifica tu conexión e inténtalo de nuevo."
                recordErrorMessage(channel, errMsg)
            }
        }
    }

    private suspend fun recordErrorMessage(channel: ChatChannel, message: String) {
        firestoreRepository.addMessage(
            userId = currentUserId,
            channel = channel,
            speaker = MessageSpeaker.SYSTEM,
            content = message,
            errorFlag = true,
            sequenceOrder = System.currentTimeMillis()
        )
        _uiState.update {
            it.copy(
                isSending = false,
                streamingPreviewText = "",
                banner = UiBanner(message, isError = true)
            )
        }
    }

    private suspend fun saveAssistantReply(
        channel: ChatChannel,
        rawText: String
    ): List<CloudChatMessage> {
        val baseSeq = System.currentTimeMillis()
        return when (channel) {
            ChatChannel.ALICE -> {
                val id = firestoreRepository.addMessage(
                    userId = currentUserId,
                    channel = channel,
                    speaker = MessageSpeaker.ALICE,
                    content = rawText,
                    sequenceOrder = baseSeq
                ).getOrDefault("local_${baseSeq}")
                listOf(
                    CloudChatMessage(
                        id = id,
                        userId = currentUserId,
                        channelId = channel.id,
                        speaker = MessageSpeaker.ALICE.id,
                        content = rawText,
                        sequenceOrder = baseSeq
                    )
                )
            }

            ChatChannel.YUI -> {
                val id = firestoreRepository.addMessage(
                    userId = currentUserId,
                    channel = channel,
                    speaker = MessageSpeaker.YUI,
                    content = rawText,
                    sequenceOrder = baseSeq
                ).getOrDefault("local_${baseSeq}")
                listOf(
                    CloudChatMessage(
                        id = id,
                        userId = currentUserId,
                        channelId = channel.id,
                        speaker = MessageSpeaker.YUI.id,
                        content = rawText,
                        sequenceOrder = baseSeq
                    )
                )
            }

            ChatChannel.GROUP -> {
                val turns = PersonaPrompts.parseGroupResponse(rawText)
                turns.mapIndexed { idx, turn ->
                    val seq = baseSeq + idx
                    val id = firestoreRepository.addMessage(
                        userId = currentUserId,
                        channel = channel,
                        speaker = turn.speaker,
                        content = turn.content,
                        sequenceOrder = seq
                    ).getOrDefault("local_$seq")
                    CloudChatMessage(
                        id = id,
                        userId = currentUserId,
                        channelId = channel.id,
                        speaker = turn.speaker.id,
                        content = turn.content,
                        sequenceOrder = seq
                    )
                }
            }
        }
    }

    fun toggleSpeakMessage(message: CloudChatMessage) {
        if (speakingMessageId.value == message.id) {
            voicePlayer.stop()
        } else {
            val started = speakMessage(message)
            if (!started) {
                _uiState.update {
                    it.copy(
                        banner = UiBanner(
                            "No se pudo iniciar la voz en español. Verifica el motor TextToSpeech en los ajustes de Android.",
                            isError = true
                        )
                    )
                }
            }
        }
    }

    private fun speakMessage(message: CloudChatMessage): Boolean {
        val isYui = message.speaker == MessageSpeaker.YUI.id
        return voicePlayer.speak(
            messageId = message.id,
            text = message.content,
            isYui = isYui
        )
    }

    private fun speakMessages(messages: List<CloudChatMessage>) {
        val items = messages.map { msg ->
            SpeechItem(
                messageId = msg.id,
                text = msg.content,
                isYui = msg.speaker == MessageSpeaker.YUI.id
            )
        }
        voicePlayer.speakSequence(items)
    }

    fun clearCurrentChannel() {
        val channel = _uiState.value.selectedChannel
        voicePlayer.stop()
        viewModelScope.launch {
            firestoreRepository.clearChannel(currentUserId, channel)
                .onSuccess {
                    _uiState.update { it.copy(banner = UiBanner("Conversación de ${channel.displayName} limpiada en la nube.")) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(banner = UiBanner(err.localizedMessage ?: "Error al limpiar conversación.", isError = true)) }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voicePlayer.release()
    }
}
