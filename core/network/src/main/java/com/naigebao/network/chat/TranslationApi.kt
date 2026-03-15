package com.naigebao.network.chat

/**
 * Translation API interface for message translation
 */
interface TranslationApi {
    /**
     * Translate text to target language
     * @param text The text to translate
     * @param targetLanguage Target language code (e.g., "en", "zh", "ja")
     * @return Translated text
     */
    suspend fun translate(text: String, targetLanguage: String): String
}