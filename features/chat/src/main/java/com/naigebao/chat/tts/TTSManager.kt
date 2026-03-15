package com.naigebao.chat.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * TTS Manager for text-to-speech functionality
 * Provides play, pause, stop, and voice selection capabilities
 */
class TTSManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentText = MutableStateFlow<String?>(null)
    val currentText: StateFlow<String?> = _currentText.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<VoiceInfo>>(emptyList())
    val availableVoices: StateFlow<List<VoiceInfo>> = _availableVoices.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _pitch = MutableStateFlow(1.0f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private var currentMessageId: String? = null

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            loadAvailableVoices()
            setupUtteranceListener()
        }
    }

    private fun loadAvailableVoices() {
        tts?.let { engine ->
            val voices = engine.voices
                .filter { it.locale != null && it.locale.language.isNotEmpty() }
                .map { voice ->
                    VoiceInfo(
                        name = voice.name,
                        language = voice.locale.toString(),
                        isQualityVoice = voice.quality >= Voice.QUALITY_HIGH
                    )
                }
            _availableVoices.value = voices
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _playbackState.value = PlaybackState.PLAYING
            }

            override fun onDone(utteranceId: String?) {
                _playbackState.value = PlaybackState.IDLE
                _currentText.value = null
                currentMessageId = null
            }

            override fun onError(utteranceId: String?) {
                _playbackState.value = PlaybackState.IDLE
                _currentText.value = null
                currentMessageId = null
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?, errorCode: Int) {
                _playbackState.value = PlaybackState.IDLE
                _currentText.value = null
                currentMessageId = null
            }
        })
    }

    /**
     * Play the given text
     * @param text Text to speak
     * @param messageId Optional message ID for tracking
     */
    fun play(text: String, messageId: String? = null) {
        if (!isInitialized) return

        // Stop any current playback
        stop()

        currentMessageId = messageId
        _currentText.value = text
        _playbackState.value = PlaybackState.PLAYING

        tts?.setSpeechRate(_speechRate.value)
        tts?.setPitch(_pitch.value)

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, messageId ?: "utterance_${System.currentTimeMillis()}")
    }

    /**
     * Pause the current playback
     */
    fun pause() {
        if (!isInitialized || _playbackState.value != PlaybackState.PLAYING) return
        // Note: Android TTS doesn't support true pause, we stop and store position
        // For simplicity, we'll just stop
        stop()
        _playbackState.value = PlaybackState.PAUSED
    }

    /**
     * Resume playback from where it was paused
     */
    fun resume() {
        if (!isInitialized || _playbackState.value != PlaybackState.PAUSED) return
        _currentText.value?.let { text ->
            play(text, currentMessageId)
        }
    }

    /**
     * Stop the current playback
     */
    fun stop() {
        if (!isInitialized) return
        tts?.stop()
        _playbackState.value = PlaybackState.IDLE
        _currentText.value = null
        currentMessageId = null
    }

    /**
     * Set speech rate (0.5 - 2.0, default 1.0)
     */
    fun setSpeechRate(rate: Float) {
        val clampedRate = rate.coerceIn(0.5f, 2.0f)
        _speechRate.value = clampedRate
        tts?.setSpeechRate(clampedRate)
    }

    /**
     * Set pitch (0.5 - 2.0, default 1.0)
     */
    fun setPitch(pitch: Float) {
        val clampedPitch = pitch.coerceIn(0.5f, 2.0f)
        _pitch.value = clampedPitch
        tts?.setPitch(clampedPitch)
    }

    /**
     * Check if currently playing
     */
    fun isPlaying(): Boolean = _playbackState.value == PlaybackState.PLAYING

    /**
     * Check if playback is paused
     */
    fun isPaused(): Boolean = _playbackState.value == PlaybackState.PAUSED

    /**
     * Clean up resources
     */
    fun release() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    enum class PlaybackState {
        IDLE,
        PLAYING,
        PAUSED
    }

    data class VoiceInfo(
        val name: String,
        val language: String,
        val isQualityVoice: Boolean = false
    )
}
