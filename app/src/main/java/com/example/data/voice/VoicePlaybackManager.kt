package com.example.data.voice

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

private const val TAG = "VoicePlaybackManager"

data class SpeechItem(
    val messageId: String,
    val text: String,
    val isYui: Boolean
)

class VoicePlaybackManager(context: Context) {

    private data class SpeechChunk(
        val utteranceId: String,
        val messageId: String,
        val text: String,
        val isYui: Boolean
    )

    private val lock = Any()
    private val utteranceCounter = AtomicLong(0L)

    private var deviceTts: TextToSpeech? = null
    private var isInitialized = false
    private var isSpanishAvailable = false

    private val queue = ArrayDeque<SpeechChunk>()
    private var currentChunk: SpeechChunk? = null
    private var pendingItems: List<SpeechItem>? = null

    private val _speakingMessageId = MutableStateFlow<String?>(null)
    val speakingMessageId: StateFlow<String?> = _speakingMessageId.asStateFlow()

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    init {
        deviceTts = TextToSpeech(context.applicationContext) { status ->
            handleTtsInit(status)
        }
    }

    private fun handleTtsInit(status: Int) {
        val pendingToStart: List<SpeechItem>?
        synchronized(lock) {
            isInitialized = true
            val tts = deviceTts
            if (status == TextToSpeech.SUCCESS && tts != null) {
                tts.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                isSpanishAvailable = configureSpanishLanguage(tts)
                _isTtsReady.value = isSpanishAvailable
                if (isSpanishAvailable) {
                    tts.setOnUtteranceProgressListener(createProgressListener())
                } else {
                    Log.w(TAG, "Spanish TTS language data is missing or unsupported on this device.")
                }
            } else {
                isSpanishAvailable = false
                _isTtsReady.value = false
                Log.w(TAG, "Android TextToSpeech initialization failed with status=$status")
            }

            pendingToStart = if (isSpanishAvailable) {
                pendingItems.also { pendingItems = null }
            } else {
                pendingItems = null
                null
            }
        }

        if (!pendingToStart.isNullOrEmpty()) {
            speakSequence(pendingToStart)
        }
    }

    private fun configureSpanishLanguage(tts: TextToSpeech): Boolean {
        val preferredLocales = listOf(
            Locale.forLanguageTag("es-ES"),
            Locale.forLanguageTag("es-MX"),
            Locale.forLanguageTag("es-US"),
            Locale.forLanguageTag("es-419"),
            Locale.forLanguageTag("es")
        )

        for (locale in preferredLocales) {
            val result = tts.setLanguage(locale)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.d(TAG, "Configured Android TTS in Spanish using locale: ${locale.toLanguageTag()}")
                return true
            }
        }

        val spanishVoice = runCatching {
            tts.voices
                ?.filter { it.locale?.language.equals("es", ignoreCase = true) && !it.isNetworkConnectionRequired }
                ?.minByOrNull { it.latency }
                ?: tts.voices?.firstOrNull { it.locale?.language.equals("es", ignoreCase = true) }
        }.getOrNull()

        if (spanishVoice != null) {
            val voiceResult = tts.setVoice(spanishVoice)
            if (voiceResult == TextToSpeech.SUCCESS) {
                Log.d(TAG, "Configured Android TTS using Spanish voice: ${spanishVoice.name}")
                return true
            }
        }

        return false
    }

    private fun createProgressListener(): UtteranceProgressListener {
        return object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                synchronized(lock) {
                    if (currentChunk?.utteranceId == utteranceId) {
                        _speakingMessageId.value = currentChunk?.messageId
                    }
                }
            }

            override fun onDone(utteranceId: String?) {
                advanceQueue(utteranceId)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                advanceQueue(utteranceId)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.w(TAG, "TTS utterance error code=$errorCode for id=$utteranceId")
                advanceQueue(utteranceId)
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                synchronized(lock) {
                    if (currentChunk?.utteranceId == utteranceId) {
                        currentChunk = null
                        if (queue.isEmpty()) {
                            _speakingMessageId.value = null
                        }
                    }
                }
            }
        }
    }

    fun speak(messageId: String, text: String, isYui: Boolean): Boolean {
        return speakSequence(listOf(SpeechItem(messageId = messageId, text = text, isYui = isYui)))
    }

    fun speakSequence(items: List<SpeechItem>): Boolean {
        if (items.isEmpty()) return false

        synchronized(lock) {
            val tts = deviceTts ?: return false

            if (!isInitialized) {
                pendingItems = items
                return true
            }

            if (!isSpanishAvailable) {
                return false
            }

            queue.clear()
            currentChunk = null
            runCatching { tts.stop() }

            val maxChunkLength = (TextToSpeech.getMaxSpeechInputLength() - 100).coerceAtLeast(500)
            items.forEach { item ->
                val cleaned = sanitizeForSpeech(item.text)
                if (cleaned.isNotEmpty()) {
                    splitIntoChunks(cleaned, maxChunkLength).forEach { part ->
                        val id = "tts_${utteranceCounter.incrementAndGet()}_${item.messageId}"
                        queue.addLast(
                            SpeechChunk(
                                utteranceId = id,
                                messageId = item.messageId,
                                text = part,
                                isYui = item.isYui
                            )
                        )
                    }
                }
            }

            if (queue.isEmpty()) {
                _speakingMessageId.value = null
                return false
            }

            return playNextLocked(tts)
        }
    }

    private fun advanceQueue(finishedUtteranceId: String?) {
        synchronized(lock) {
            if (finishedUtteranceId != null && currentChunk?.utteranceId != finishedUtteranceId) {
                return
            }
            val tts = deviceTts
            if (tts == null || queue.isEmpty()) {
                currentChunk = null
                _speakingMessageId.value = null
                return
            }
            playNextLocked(tts)
        }
    }

    private fun playNextLocked(tts: TextToSpeech): Boolean {
        val next = queue.removeFirstOrNull()
        if (next == null) {
            currentChunk = null
            _speakingMessageId.value = null
            return false
        }

        currentChunk = next
        _speakingMessageId.value = next.messageId

        tts.setPitch(if (next.isYui) 1.18f else 1.02f)
        tts.setSpeechRate(if (next.isYui) 1.04f else 1.0f)

        val status = tts.speak(next.text, TextToSpeech.QUEUE_FLUSH, null, next.utteranceId)
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "TTS speak() returned error status=$status")
            currentChunk = null
            queue.clear()
            _speakingMessageId.value = null
            return false
        }
        return true
    }

    private fun sanitizeForSpeech(raw: String): String {
        return raw
            .replace(Regex("""```[\s\S]*?```"""), " ")
            .replace(Regex("""\[([^\]]+)\]\([^)]+\)"""), "$1")
            .replace(Regex("""[*_`#~>]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun splitIntoChunks(text: String, maxLength: Int): List<String> {
        if (text.length <= maxLength) return listOf(text)

        val chunks = mutableListOf<String>()
        var remaining = text
        while (remaining.length > maxLength) {
            val window = remaining.substring(0, maxLength)
            val splitAt = listOf(
                window.lastIndexOf(". "),
                window.lastIndexOf("! "),
                window.lastIndexOf("? "),
                window.lastIndexOf('\n'),
                window.lastIndexOf(' ')
            ).maxOrNull()?.takeIf { it > maxLength / 3 } ?: maxLength

            val cutIndex = if (splitAt < maxLength && (window[splitAt] == '.' || window[splitAt] == '!' || window[splitAt] == '?')) {
                splitAt + 1
            } else {
                splitAt
            }
            chunks.add(remaining.substring(0, cutIndex).trim())
            remaining = remaining.substring(cutIndex).trim()
        }
        if (remaining.isNotEmpty()) {
            chunks.add(remaining)
        }
        return chunks
    }

    fun stop() {
        synchronized(lock) {
            pendingItems = null
            queue.clear()
            currentChunk = null
            runCatching { deviceTts?.stop() }
            _speakingMessageId.value = null
        }
    }

    fun release() {
        synchronized(lock) {
            pendingItems = null
            queue.clear()
            currentChunk = null
            runCatching { deviceTts?.stop() }
            runCatching { deviceTts?.shutdown() }
            deviceTts = null
            isInitialized = false
            isSpanishAvailable = false
            _isTtsReady.value = false
            _speakingMessageId.value = null
        }
    }
}
