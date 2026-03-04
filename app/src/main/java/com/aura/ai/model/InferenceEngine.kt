package com.aura.ai.model

import android.content.Context
import android.util.Log
import com.aura.ai.utils.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class InferenceEngine(private val context: Context) {
    
    private val TAG = "InferenceEngine"
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized
    
    private val _currentModel = MutableStateFlow("")
    val currentModel: StateFlow<String> = _currentModel
    
    private val _lastInferenceTime = MutableStateFlow(0L)
    val lastInferenceTime: StateFlow<Long> = _lastInferenceTime
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        val modelName = FileHelper.getModelName(context)
        _currentModel.value = modelName
        _isInitialized.value = true
        Log.d(TAG, "✅ Model initialized: $modelName")
        true
    }
    
    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        Log.d(TAG, "Generating response for: $prompt")
        
        // SIMPLE TEST RESPONSE
        val response = when {
            prompt.contains("hello", ignoreCase = true) -> "Hello! I'm ${_currentModel.value}. How can I help?"
            prompt.contains("how are you", ignoreCase = true) -> "I'm working great!"
            prompt.contains("what model", ignoreCase = true) -> "I'm using ${_currentModel.value}"
            else -> "You said: '$prompt'"
        }
        
        _lastInferenceTime.value = System.currentTimeMillis() - startTime
        response
    }
    
    fun shutdown() { }
}
