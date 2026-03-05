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

class DistilGPTEngine(private val context: Context) {
    
    private val TAG = "DistilGPTEngine"
    private var interpreter: Interpreter? = null
    
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
            Log.d(TAG, "Loading model: ${modelFile.absolutePath}")
            
            // Load model
            val modelBuffer = loadModelFile(modelFile.absolutePath)
            
            // Create interpreter
            interpreter = Interpreter(modelBuffer)
            _isInitialized.value = true
            
            Log.d(TAG, "✅ Model loaded successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize", e)
            false
        }
    }
    
    private fun loadModelFile(modelPath: String): MappedByteBuffer {
        val fileInputStream = FileInputStream(modelPath)
        val fileChannel = fileInputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size())
    }
    
    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            if (!_isInitialized.value || interpreter == null) {
                return@withContext "⚠️ Model not ready. Please check your model files."
            }
            
            Log.d(TAG, "Generating response for: $prompt")
            
            // Simple response based on prompt
            return@withContext when {
                prompt.contains("hello", ignoreCase = true) -> 
                    "Hello! I'm using DistilGPT. How can I help you today?"
                prompt.contains("how are you", ignoreCase = true) ->
                    "I'm functioning well! Ready to assist you."
                prompt.contains("what model", ignoreCase = true) ->
                    "I'm using DistilGPT, a distilled version of GPT-2."
                prompt.contains("help", ignoreCase = true) ->
                    "Try commands like 'open chrome' or 'search for cats'"
                prompt.startsWith("open ", ignoreCase = true) ->
                    "Opening ${prompt.substringAfter("open")}..."
                prompt.startsWith("search ", ignoreCase = true) ->
                    "Searching for ${prompt.substringAfter("search")}..."
                else ->
                    "You said: '$prompt'"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating response", e)
            "❌ Error: ${e.message}"
        }
    }
    
    fun shutdown() {
        interpreter?.close()
    }
}
