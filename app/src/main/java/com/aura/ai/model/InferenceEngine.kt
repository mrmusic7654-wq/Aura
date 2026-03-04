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
    private val executor = Executors.newSingleThreadExecutor()
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized
    
    private val _currentModel = MutableStateFlow("")
    val currentModel: StateFlow<String> = _currentModel
    
    private val _lastInferenceTime = MutableStateFlow(0L)
    val lastInferenceTime: StateFlow<Long> = _lastInferenceTime
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val modelFile = FileHelper.getModelFile(context)
            if (modelFile == null) {
                Log.e(TAG, "No model file found")
                return@withContext false
            }
            
            val modelName = FileHelper.getModelName(context)
            
            Log.d(TAG, "Loading model: ${modelFile.absolutePath}")
            Log.d(TAG, "Model size: ${modelFile.length() / (1024 * 1024)} MB")
            
            // Initialize ONNX Runtime environment
            ortEnvironment = OrtEnvironment.getEnvironment()
            
            // Create session options with proper optimizations for v1.17.1
            val sessionOptions = SessionOptions().apply {
                // Set thread count for mobile CPU
                setIntraOpNumThreads(4)
                setInterOpNumThreads(4)
                
                // FOR ONNX RUNTIME 1.17.1 - The correct enum is ALL_OPTIMIZATIONS
                // But we need to access it differently
                setOptimizationLevel(SessionOptions.OptLevel.values()[2]) // 2 = ALL_OPTIMIZATIONS
                
                // Enable XNNPACK for ARM CPU acceleration
                try {
                    addXnnpack(emptyMap())
                    Log.d(TAG, "XNNPACK enabled")
                } catch (e: Exception) {
                    Log.d(TAG, "XNNPACK not available")
                }
                
                // Enable NNAPI for hardware acceleration
                try {
                    addNnapi()
                    Log.d(TAG, "NNAPI enabled")
                } catch (e: Exception) {
                    Log.d(TAG, "NNAPI not available")
                }
                
                // Memory optimizations
                setMemoryPatternOptimization(true)
                setCPUArenaAllocator(true)
            }
            
            // Create the session
            ortSession = ortEnvironment?.createSession(modelFile.absolutePath, sessionOptions)
            
            if (ortSession == null) {
                Log.e(TAG, "Failed to create session")
                return@withContext false
            }
            
            _currentModel.value = modelName
            _isInitialized.value = true
            
            Log.d(TAG, "✅ Model initialized: $modelName")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize model", e)
            false
        }
    }
    
    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        try {
            if (!_isInitialized.value) {
                return@withContext "⚠️ Model not initialized yet."
            }
            
            if (ortSession == null) {
                return@withContext "⚠️ Model session not available."
            }
            
            Log.d(TAG, "Generating response for: $prompt")
            
            // TODO: Implement actual inference with tokenization
            val response = when {
                prompt.contains("hello", ignoreCase = true) || prompt.contains("hi", ignoreCase = true) ->
                    "Hello! I'm running on ${_currentModel.value}. How can I help?"
                    
                prompt.contains("what model", ignoreCase = true) ->
                    "I'm using ${_currentModel.value}"
                    
                else -> "I received: '$prompt'"
            }
            
            _lastInferenceTime.value = System.currentTimeMillis() - startTime
            Log.d(TAG, "Response generated in ${_lastInferenceTime.value}ms")
            
            return@withContext response
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating response", e)
            return@withContext "❌ Error: ${e.message}"
        }
    }
    
    fun getModelInfo(): Map<String, Any> {
        return mapOf(
            "initialized" to _isInitialized.value,
            "modelName" to _currentModel.value,
            "lastInferenceTimeMs" to _lastInferenceTime.value
        )
    }
    
    fun shutdown() {
        try {
            ortSession?.close()
            executor.shutdown()
            Log.d(TAG, "Inference engine shutdown complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error during shutdown", e)
        }
    }
}
