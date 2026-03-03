package com.aura.ai.model

import android.content.Context
import com.aura.ai.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class InferenceEngine(private val context: Context) {
    
    private var onnxEnvironment: Any? = null
    private var session: Any? = null
    private val executor = Executors.newSingleThreadExecutor()
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized
    
    private val _currentModelPath = MutableStateFlow("")
    val currentModelPath: StateFlow<String> = _currentModelPath
    
    private val _currentTokenizerPath = MutableStateFlow("")
    val currentTokenizerPath: StateFlow<String> = _currentTokenizerPath
    
    private val _lastInferenceTime = MutableStateFlow(0L)
    val lastInferenceTime: StateFlow<Long> = _lastInferenceTime
    
    suspend fun initialize(modelPath: String, tokenizerPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            _currentModelPath.value = modelPath
            _currentTokenizerPath.value = tokenizerPath
            Thread.sleep(500)
            _isInitialized.value = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun generateResponse(
        prompt: String, 
        context: String = "",
        maxLength: Int = Constants.MAX_SEQUENCE_LENGTH
    ): String = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        try {
            // FIXED: Calculate as Long from the start
            val processingTime = (prompt.length * 10L).coerceIn(100L, 1000L)
            Thread.sleep(processingTime)
            
            _lastInferenceTime.value = System.currentTimeMillis() - startTime
            
            when {
                prompt.contains("hello", ignoreCase = true) || prompt.contains("hi", ignoreCase = true) ->
                    "Hello! How can I help you today?"
                    
                prompt.contains("how are you", ignoreCase = true) ->
                    "I'm functioning optimally! Ready to assist you with any task."
                    
                prompt.contains("what can you do", ignoreCase = true) ->
                    "I can help you with conversations, answer questions, and control your device."
                    
                prompt.contains("help", ignoreCase = true) ->
                    "You can ask me questions or give me commands like 'open Chrome'."
                    
                else -> generateContextualResponse(prompt)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "I encountered an error while processing your request."
        }
    }
    
    private fun generateContextualResponse(prompt: String): String {
        return when {
            containsAny(prompt, Constants.APP_OPEN_COMMANDS) ->
                "I'll help you open an app. Which app would you like to open?"
                
            containsAny(prompt, Constants.SEARCH_COMMANDS) ->
                "I'll search for that. What would you like me to search for?"
                
            containsAny(prompt, Constants.SCROLL_COMMANDS) ->
                "I can scroll for you. Which direction (up/down)?"
                
            else ->
                "I understand your message: '$prompt'."
        }
    }
    
    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it, ignoreCase = true) }
    }
    
    fun shutdown() {
        executor.shutdown()
    }
    
    fun getModelInfo(): Map<String, Any> {
        return mapOf(
            "initialized" to _isInitialized.value,
            "modelPath" to _currentModelPath.value,
            "tokenizerPath" to _currentTokenizerPath.value,
            "lastInferenceTimeMs" to _lastInferenceTime.value,
            "maxSequenceLength" to Constants.MAX_SEQUENCE_LENGTH
        )
    }
}
