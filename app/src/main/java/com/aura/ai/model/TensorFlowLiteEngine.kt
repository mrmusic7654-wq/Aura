package com.aura.ai.model

import android.content.Context
import android.util.Log
import com.aura.ai.utils.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TensorFlowLiteEngine(private val context: Context) {
    
    private val TAG = "TFLiteEngine"
    
    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized
    
    private val _currentModel = MutableStateFlow("")
    val currentModel: StateFlow<String> = _currentModel
    
    private val _lastInferenceTime = MutableStateFlow(0L)
    val lastInferenceTime: StateFlow<Long> = _lastInferenceTime
    
    // Simple tokenizer for now - will be replaced with actual tokenizer
    private val tokenizer = BasicTokenizer()
    
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
            
            // Configure options with GPU support
            val options = Interpreter.Options().apply {
                // Use GPU if available
                try {
                    gpuDelegate = GpuDelegate(GpuDelegate.Options())
                    addDelegate(gpuDelegate)
                    Log.d(TAG, "GPU delegate enabled")
                } catch (e: Exception) {
                    Log.d(TAG, "GPU delegate not available, using CPU")
                }
                
                // Use multiple threads
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
            
            // Tokenize input
            val inputTokens = tokenizer.encode(prompt)
            
            // Prepare input tensor
            val inputArray = arrayOf(inputTokens)
            val outputArray = Array(1) { FloatArray(tokenizer.vocabSize) }
            
            // Run inference
            interpreter?.run(inputArray, outputArray)
            
            // Get response (simplified - take top token)
            val responseTokens = getTopTokens(outputArray[0], 20)
            val response = tokenizer.decode(responseTokens)
            
            _lastInferenceTime.value = System.currentTimeMillis() - startTime
            Log.d(TAG, "Response generated in ${_lastInferenceTime.value}ms")
            
            response
            
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed", e)
            "❌ Error: ${e.message}"
        }
    }
    
    private fun getTopTokens(output: FloatArray, count: Int): List<Int> {
        return output.indices
            .sortedByDescending { output[it] }
            .take(count)
    }
    
    fun shutdown() {
        try {
            interpreter?.close()
            gpuDelegate?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error during shutdown", e)
        }
    }
}

// Simple tokenizer - replace with actual tokenizer.json
class BasicTokenizer {
    val vocabSize = 50000
    
    fun encode(text: String): FloatArray {
        // Simplified - convert each character to a float
        return text.map { it.code.toFloat() }.toFloatArray()
    }
    
    fun decode(tokens: List<Int>): String {
        // Simplified - convert each token back to a character
        return tokens.map { it.toChar() }.joinToString("")
    }
}
