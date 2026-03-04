package com.aura.ai.model

import android.content.Context
import android.util.Log
import com.aura.ai.utils.Constants
import com.aura.ai.utils.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.SessionOptions
import java.util.concurrent.Executors

class InferenceEngine(private val context: Context) {
    
    private val TAG = "InferenceEngine"
    
    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized
    
    private val _currentModel = MutableStateFlow("")
    val currentModel: StateFlow<String> = _currentModel
    
    private val _lastInferenceTime = MutableStateFlow(0L)
    val lastInferenceTime: StateFlow<Long> = _lastInferenceTime
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val modelFile = FileHelper.getModelFile(context) ?: return@withContext false
            val modelName = FileHelper.getModelName(context)
            
            Log.d(TAG, "Loading model: ${modelFile.absolutePath}")
            
            // Initialize ONNX Runtime
            ortEnvironment = OrtEnvironment.getEnvironment()
            
            val sessionOptions = SessionOptions().apply {
                setIntraOpNumThreads(4)
                setOptimizationLevel(SessionOptions.OptLevel.ALL_OPTIMIZATIONS)
                addXnnpack("") // Enable XNNPACK for ARM CPUs
            }
            
            ortSession = ortEnvironment?.createSession(modelFile.absolutePath, sessionOptions)
            
            _currentModel.value = modelName
            _isInitialized.value = true
            
            Log.d(TAG, "✅ Model initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize model", e)
            false
        }
    }
    
    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        try {
            // Check if model is initialized
            if (!_isInitialized.value) {
                return@withContext "Model not initialized yet. Please wait."
            }
            
            if (ortSession == null) {
                return@withContext "Model session not available."
            }
            
            Log.d(TAG, "Generating response for: $prompt")
            
            // TODO: Actual inference with the model
            // This is where you would:
            // 1. Tokenize the input
            // 2. Run inference
            // 3. Decode the output
            
            // FOR NOW - return a simple response to test
            val response = when {
                prompt.contains("hello", ignoreCase = true) -> 
                    "Hello! I'm running on ${_currentModel.value}. How can I help?"
                    
                prompt.contains("how are you", ignoreCase = true) -> 
                    "I'm working great! Ready to assist you."
                    
                prompt.contains("what model", ignoreCase = true) -> 
                    "I'm using ${_currentModel.value} which is auto-detected from your models folder."
                    
                prompt.contains("what can you do", ignoreCase = true) -> 
                    "I can chat with you, answer questions, and control your device - all offline!"
                    
                prompt.contains("help", ignoreCase = true) -> 
                    "Try commands like 'open Chrome', 'search for cats', or just chat with me!"
                    
                else -> "I received: '$prompt'. (Running on ${_currentModel.value})"
            }
            
            _lastInferenceTime.value = System.currentTimeMillis() - startTime
            Log.d(TAG, "Response generated in ${_lastInferenceTime.value}ms")
            
            return@withContext response
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating response", e)
            return@withContext "Error: ${e.message}"
        }
    }
    
    fun shutdown() {
        try {
            ortSession?.close()
            // Don't close OrtEnvironment as it's a singleton
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down", e)
        }
    }
}
