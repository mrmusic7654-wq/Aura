package com.aura.ai.model

import android.content.Context
import com.aura.ai.utils.Constants
import com.aura.ai.utils.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class InferenceEngine(private val context: Context) {
    
    private val executor = Executors.newSingleThreadExecutor()
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized
    
    private val _currentModel = MutableStateFlow("")
    val currentModel: StateFlow<String> = _currentModel
    
    private val _lastInferenceTime = MutableStateFlow(0L)
    val lastInferenceTime: StateFlow<Long> = _lastInferenceTime
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelFile = FileHelper.getModelFile(context)
            val modelName = FileHelper.getModelName(context)
            
            if (modelFile == null) {
                return@withContext false
            }
            
            _currentModel.value = modelName
            _isInitialized.value = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        try {
            // Simulate processing time
            val processingTime = (prompt.length * 5L).coerceIn(50L, 500L)
            Thread.sleep(processingTime)
            
            _lastInferenceTime.value = System.currentTimeMillis() - startTime
            
            when {
                prompt.contains("hello", ignoreCase = true) || prompt.contains("hi", ignoreCase = true) ->
                    "Hello! I'm running on ${_currentModel.value}. How can I help you today?"
                    
                prompt.contains("how are you", ignoreCase = true) ->
                    "I'm running smoothly! Ready to assist you."
                    
                prompt.contains("what model", ignoreCase = true) ->
                    "I'm using ${_currentModel.value}, which auto-detected from your models folder!"
                    
                prompt.contains("what can you do", ignoreCase = true) ->
                    "I can chat with you, answer questions, and control your device - all offline!"
                    
                prompt.contains("help", ignoreCase = true) ->
                    "Try commands like 'open Chrome', 'search for cats', or just chat with me!"
                    
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
                "I understand: '$prompt' (using ${_currentModel.value})"
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
            "model" to _currentModel.value,
            "lastInferenceTimeMs" to _lastInferenceTime.value
        )
    }
}
