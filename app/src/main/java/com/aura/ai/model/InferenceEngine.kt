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
                // Set thread count for mobile CPU (4 is optimal for most phones)
                setIntraOpNumThreads(4)
                setInterOpNumThreads(4)
                
                // Enable all graph optimizations
                setOptimizationLevel(SessionOptions.OptLevel.ALL)
                
                // Enable XNNPACK for ARM CPU acceleration
                // Pass empty map for default options
                addXnnpack(emptyMap())
                
                // Optional: Enable other providers if available
                try {
                    // Try to enable NNAPI for hardware acceleration
                    addNnapi()
                } catch (e: Exception) {
                    Log.d(TAG, "NNAPI not available, using CPU only")
                }
                
                // Set memory pattern optimization
                setMemoryPatternOptimization(true)
                
                // Enable CPU memory arena
                setCPUArenaAllocator(true)
            }
            
            // Create the session
            ortSession = ortEnvironment?.createSession(modelFile.absolutePath, sessionOptions)
            
            if (ortSession == null) {
                Log.e(TAG, "Failed to create session")
                return@withContext false
            }
            
            // Get model info
            val inputInfo = ortSession?.inputInfo
            val outputInfo = ortSession?.outputInfo
            
            Log.d(TAG, "Model loaded successfully:")
            Log.d(TAG, "  Inputs: ${inputInfo?.size}")
            Log.d(TAG, "  Outputs: ${outputInfo?.size}")
            
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
            // Check if model is initialized
            if (!_isInitialized.value) {
                return@withContext "⚠️ Model not initialized yet. Please wait or check your model files."
            }
            
            if (ortSession == null) {
                return@withContext "⚠️ Model session not available."
            }
            
            Log.d(TAG, "Generating response for: $prompt")
            
            // TODO: Implement actual inference with tokenization
            // This is a placeholder until you implement the full pipeline
            
            // For now, return a contextual response based on the prompt
            val response = when {
                prompt.contains("hello", ignoreCase = true) || prompt.contains("hi", ignoreCase = true) ->
                    "Hello! I'm running on ${_currentModel.value}. How can I help you today?"
                    
                prompt.contains("how are you", ignoreCase = true) ->
                    "I'm functioning optimally! Ready to assist you with any task."
                    
                prompt.contains("what model", ignoreCase = true) ->
                    "I'm using ${_currentModel.value}, which was auto-detected from your models folder."
                    
                prompt.contains("what can you do", ignoreCase = true) ->
                    "I can chat with you, answer questions, and control your device - all offline!"
                    
                prompt.contains("help", ignoreCase = true) ->
                    "Try commands like 'open Chrome', 'search for cats', or just chat with me!"
                    
                prompt.contains("open", ignoreCase = true) ->
                    "I'll help you open an app. Which app would you like to open?"
                    
                prompt.contains("search", ignoreCase = true) ->
                    "I'll search for that. What would you like me to search for?"
                    
                prompt.contains("scroll", ignoreCase = true) ->
                    "I can scroll for you. Which direction (up/down)?"
                    
                else -> {
                    // Generate a simple response
                    "I received: '$prompt'. (Running on ${_currentModel.value})"
                }
            }
            
            _lastInferenceTime.value = System.currentTimeMillis() - startTime
            Log.d(TAG, "Response generated in ${_lastInferenceTime.value}ms")
            
            return@withContext response
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating response", e)
            return@withContext "❌ Error: ${e.message}"
        }
    }
    
    // Get model metadata
    fun getModelInfo(): Map<String, Any> {
        return mapOf(
            "initialized" to _isInitialized.value,
            "modelName" to _currentModel.value,
            "lastInferenceTimeMs" to _lastInferenceTime.value,
            "maxSequenceLength" to Constants.MAX_SEQUENCE_LENGTH,
            "sessionActive" to (ortSession != null)
        )
    }
    
    // Get input/output info
    fun getModelMetadata(): Map<String, Any> {
        val metadata = mutableMapOf<String, Any>()
        
        try {
            ortSession?.let { session ->
                metadata["inputCount"] = session.inputInfo.size
                metadata["outputCount"] = session.outputInfo.size
                metadata["inputNames"] = session.inputInfo.keys.joinToString()
                metadata["outputNames"] = session.outputInfo.keys.joinToString()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting metadata", e)
        }
        
        return metadata
    }
    
    // Clean up resources
    fun shutdown() {
        try {
            ortSession?.close()
            // Don't close OrtEnvironment as it's a singleton
            executor.shutdown()
            Log.d(TAG, "Inference engine shutdown complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error during shutdown", e)
        }
    }
}
