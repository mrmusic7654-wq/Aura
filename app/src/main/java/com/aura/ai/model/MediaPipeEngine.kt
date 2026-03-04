package com.aura.ai.model

import android.content.Context
import android.util.Log
import com.aura.ai.utils.FileHelper
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class MediaPipeEngine(private val context: Context) {
    
    private val TAG = "MediaPipeEngine"
    
    private var llmInference: LlmInference? = null
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized
    
    private val _currentModel = MutableStateFlow("")
    val currentModel: StateFlow<String> = _currentModel
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val modelFile = FileHelper.getModelFile(context)
            if (modelFile == null) {
                Log.e(TAG, "No model file found")
                return@withContext false
            }
            
            _currentModel.value = modelFile.nameWithoutExtension
            
            // MediaPipe handles everything internally!
            llmInference = LlmInference.createFromOptions(
                context,
                LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(512)
                    .setTemperature(0.8f)
                    .setTopK(40)
                    .build()
            )
            
            _isInitialized.value = true
            Log.d(TAG, "✅ MediaPipe LLM ready")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize", e)
            false
        }
    }
    
    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        if (!_isInitialized.value || llmInference == null) {
            return@withContext "Model not ready"
        }
        
        try {
            // MediaPipe handles tokenization and generation!
            llmInference?.generateResponse(prompt) ?: "No response"
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed", e)
            "Error: ${e.message}"
        }
    }
    
    fun shutdown() {
        llmInference?.close()
    }
}
