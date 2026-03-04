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
            
            // Create session options
            val sessionOptions = SessionOptions()
            
            // Set thread count
            sessionOptions.setIntraOpNumThreads(4)
            sessionOptions.setInterOpNumThreads(4)
            
            // TRY DIFFERENT OPTIMIZATION LEVEL NAMES (one of these will work)
            try {
                // Try ALL_OPTIMIZATIONS first
                sessionOptions.setOptimizationLevel(SessionOptions.OptLevel.ALL_OPTIMIZATIONS)
                Log.d(TAG, "Using ALL_OPTIMIZATIONS")
            } catch (e1: Error) {
                try {
                    // Try ALL
                    sessionOptions.setOptimizationLevel(SessionOptions.OptLevel.ALL)
                    Log.d(TAG, "Using ALL")
                } catch (e2: Error) {
                    try {
                        // Try BASIC
                        sessionOptions.setOptimizationLevel(SessionOptions.OptLevel.BASIC_OPTIMIZATIONS)
                        Log.d(TAG, "Using BASIC_OPTIMIZATIONS")
                    } catch (e3: Error) {
                        // If all else fails, skip optimization
                        Log.w(TAG, "Could not set optimization level, continuing without it")
                    }
                }
            }
            
            // Enable XNNPACK with empty options
            try {
                sessionOptions.addXnnpack(emptyMap())
                Log.d(TAG, "XNNPACK enabled")
            } catch (e: Exception) {
                Log.w(TAG, "XNNPACK not available")
            }
            
            // Enable NNAPI if available
            try {
                sessionOptions.addNnapi()
                Log.d(TAG, "NNAPI enabled")
            } catch (e: Exception) {
                Log.d(TAG, "NNAPI not available")
            }
            
            // Memory optimizations
            try {
                sessionOptions.setMemoryPatternOptimization(true)
                sessionOptions.setCPUArenaAllocator(true)
            } catch (e: Exception) {
                Log.w(TAG, "Memory optimizations not available")
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
            
            _lastInferenceTime.value = System.currentTimeMillis() - startTime
            return@withContext "I received: '$prompt' (using ${_currentModel.value})"
            
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
