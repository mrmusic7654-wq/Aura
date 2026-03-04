package com.aura.ai.model

import android.content.Context
import android.util.Log
import com.aura.ai.utils.Constants
import com.aura.ai.utils.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class InferenceEngine(private val context: Context) {
    
    private val TAG = "InferenceEngine"
    
    private var modelFactory: ModelFactory? = null
    private var modelInference: ModelInference? = null
    private var tokenizer: Tokenizer? = null
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized
    
    private val _currentModel = MutableStateFlow("")
    val currentModel: StateFlow<String> = _currentModel
    
    private val _lastInferenceTime = MutableStateFlow(0L)
    val lastInferenceTime: StateFlow<Long> = _lastInferenceTime
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val modelName = FileHelper.getModelName(context)
            
            Log.d(TAG, "Initializing model: $modelName")
            
            // Create factory
            modelFactory = ModelFactory(context)
            
            // Load tokenizer first
            tokenizer = modelFactory?.createTokenizer()
            if (tokenizer == null) {
                Log.e(TAG, "Failed to load tokenizer")
                return@withContext false
            }
            
            // Load model
            val (env, session) = modelFactory?.createModel() ?: Pair(null, null)
            if (env == null || session == null) {
                Log.e(TAG, "Failed to load model")
                return@withContext false
            }
            
            // Create inference engine
            modelInference = ModelInference(env, session, tokenizer!!)
            
            _currentModel.value = modelName
            _isInitialized.value = true
            
            Log.d(TAG, "✅ Model fully initialized: $modelName")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize", e)
            false
        }
    }
    
    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        try {
            if (!_isInitialized.value) {
                return@withContext "⚠️ Model not initialized yet."
            }
            
            if (modelInference == null) {
                return@withContext "⚠️ Model inference not available."
            }
            
            Log.d(TAG, "Generating response for: $prompt")
            
            // Generate response using the model
            val response = modelInference!!.generate(prompt)
            
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
            "lastInferenceTimeMs" to _lastInferenceTime.value,
            "vocabSize" to (tokenizer?.getVocabSize() ?: 0)
        )
    }
    
    fun shutdown() {
        try {
            modelInference?.shutdown()
            Log.d(TAG, "Inference engine shutdown complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error during shutdown", e)
        }
    }
}
