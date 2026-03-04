package com.aura.ai.model

import android.content.Context
import android.util.Log
import com.aura.ai.utils.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TensorFlowLiteEngine(private val context: Context) {
    
    private val TAG = "TFLiteEngine"
    
    private var interpreter: Interpreter? = null
    
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
            
            _currentModel.value = modelFile.nameWithoutExtension
            Log.d(TAG, "Loading TFLite model: ${modelFile.absolutePath}")
            Log.d(TAG, "Model size: ${modelFile.length() / (1024 * 1024)} MB")
            
            // Load model
            val modelBuffer = loadModelFile(modelFile.absolutePath)
            
            // Simple CPU-only configuration
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            
            interpreter = Interpreter(modelBuffer, options)
            _isInitialized.value = true
            
            Log.d(TAG, "✅ TFLite model loaded successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TFLite", e)
            false
        }
    }
    
    private fun loadModelFile(modelPath: String): MappedByteBuffer {
        val fileInputStream = FileInputStream(modelPath)
        val fileChannel = fileInputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size())
    }
    
    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        try {
            if (!_isInitialized.value || interpreter == null) {
                return@withContext "⚠️ Model not ready yet. Please wait."
            }
            
            Log.d(TAG, "Generating response for: $prompt")
            
            // Simple placeholder response - will be replaced with actual inference
            val response = when {
                prompt.contains("hello", ignoreCase = true) -> 
                    "Hello! I'm running on ${_currentModel.value}. How can I help?"
                prompt.contains("how are you", ignoreCase = true) ->
                    "I'm working great! Ready to assist."
                else ->
                    "You said: '$prompt' (using ${_currentModel.value})"
            }
            
            _lastInferenceTime.value = System.currentTimeMillis() - startTime
            Log.d(TAG, "Response generated in ${_lastInferenceTime.value}ms")
            
            response
            
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed", e)
            "❌ Error: ${e.message}"
        }
    }
    
    fun shutdown() {
        try {
            interpreter?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error during shutdown", e)
        }
    }
}
