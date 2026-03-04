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
            
            // Create session options with mobile optimizations
            val sessionOptions = SessionOptions().apply {
                // Set thread count for mobile CPU
                setIntraOpNumThreads(4)
                setInterOpNumThreads(4)
                
                // FIX: Use the actual enum value instead of integer
                // The correct way to set optimization level
                setOptimizationLevel(SessionOptions.OptLevel.ALL_OPTIMIZATIONS)
                
                // FIX: XNNPACK expects options map
                addXnnpack(emptyMap())
                
                // Enable memory optimizations
                setMemoryPatternOptimization(true)
                setCPUArenaAllocator(true)
                
                // Set execution mode
                try {
                    setExecutionMode(SessionOptions.ExecutionMode.PARALLEL)
                } catch (e: Exception) {
                    Log.d(TAG, "Parallel execution not available")
                }
                
                // Try to enable NNAPI if available
                try {
                    addNnapi()
                    Log.d(TAG, "NNAPI enabled")
                } catch (e: Exception) {
                    Log.d(TAG, "NNAPI not available")
                }
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
            
            // TODO: Implement actual inference
            val response = when {
                prompt.contains("hello", ignoreCase = true) || prompt.contains("hi", ignoreCase = true) ->
                    "Hello! I'm running on ${_currentModel.value}. How can I help?"
                    
                prompt.contains("what model", ignoreCase = true) ->
                    "I'm using ${_currentModel.value}"
                    
                else -> "I received: '$prompt'"
            }
            
            _lastInferenceTime.value = System.currentTimeMillis() - startTime
            return@withContext response
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating response", e)
            return@withContext "❌ Error: ${e.message}"
        }
    }
    
    fun shutdown() {
        try {
            ortSession?.close()
            executor.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error during shutdown", e)
        }
    }
}
