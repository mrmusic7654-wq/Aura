package com.aura.ai.model

import android.content.Context
import android.util.Log
import com.aura.ai.utils.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.SessionOptions
import ai.onnxruntime.OnnxTensor
import java.nio.LongBuffer
import java.util.*

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
    
    // Simple tokenizer (replace with actual tokenizer later)
    private fun simpleTokenize(text: String): LongArray {
        // Very simple tokenization - just convert chars to IDs
        // THIS IS A PLACEHOLDER - replace with actual tokenizer
        val tokens = mutableListOf<Long>()
        tokens.add(1) // BOS token
        text.lowercase().forEach { char ->
            when (char) {
                in 'a'..'z' -> tokens.add((char.code - 'a'.code + 10).toLong())
                ' ' -> tokens.add(37L)
                else -> tokens.add(0L)
            }
        }
        tokens.add(2) // EOS token
        return tokens.toLongArray()
    }
    
    // Simple detokenizer
    private fun simpleDetokenize(tokens: LongArray): String {
        val result = StringBuilder()
        tokens.forEach { token ->
            when (token) {
                1L -> {} // BOS
                2L -> {} // EOS
                37L -> result.append(' ')
                in 10..35 -> result.append(('a'.code + (token - 10)).toChar())
                else -> result.append('?')
            }
        }
        return result.toString()
    }
    
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
            
            // Initialize ONNX Runtime
            ortEnvironment = OrtEnvironment.getEnvironment()
            
            val sessionOptions = SessionOptions().apply {
                setIntraOpNumThreads(4)
                setInterOpNumThreads(4)
                setOptimizationLevel(SessionOptions.OptLevel.values()[2])
                
                try {
                    addXnnpack(emptyMap())
                    Log.d(TAG, "XNNPACK enabled")
                } catch (e: Exception) {
                    Log.d(TAG, "XNNPACK not available")
                }
            }
            
            ortSession = ortEnvironment?.createSession(modelFile.absolutePath, sessionOptions)
            
            if (ortSession == null) {
                Log.e(TAG, "Failed to create session")
                return@withContext false
            }
            
            Log.d(TAG, "Model inputs: ${ortSession?.inputInfo?.keys}")
            Log.d(TAG, "Model outputs: ${ortSession?.outputInfo?.keys}")
            
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
            if (!_isInitialized.value || ortSession == null || ortEnvironment == null) {
                return@withContext "⚠️ Model not ready. Please wait."
            }
            
            Log.d(TAG, "Generating response for: $prompt")
            
            // Step 1: Tokenize input
            val inputTokens = simpleTokenize(prompt)
            Log.d(TAG, "Input tokens: ${inputTokens.size}")
            
            // Step 2: Create input tensor
            val inputShape = longArrayOf(1, inputTokens.size.toLong())
            val inputBuffer = LongBuffer.wrap(inputTokens)
            val inputTensor = OnnxTensor.createTensor(ortEnvironment!!, inputBuffer, inputShape)
            
            // Step 3: Run inference
            val inputName = ortSession?.inputInfo?.keys?.firstOrNull() ?: return@withContext "Error: No input"
            val inputs = mapOf(inputName to inputTensor)
            val results = ortSession?.run(inputs)
            
            // Step 4: Get output
            val outputName = ortSession?.outputInfo?.keys?.firstOrNull() ?: return@withContext "Error: No output"
            val outputTensor = results?.get(outputName)?.get() as? OnnxTensor
            
            // Step 5: Process output (simplified - just return first few tokens)
            val response = if (outputTensor != null) {
                val outputBuffer = outputTensor.longBuffer
                val outputTokens = LongArray(outputBuffer.remaining())
                outputBuffer.get(outputTokens)
                simpleDetokenize(outputTokens.take(20).toLongArray())
            } else {
                "I received: '$prompt'"
            }
            
            // Cleanup
            inputTensor.close()
            results?.close()
            
            _lastInferenceTime.value = System.currentTimeMillis() - startTime
            Log.d(TAG, "Response generated in ${_lastInferenceTime.value}ms")
            
            response
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating response", e)
            "❌ Error: ${e.message}"
        }
    }
    
    fun shutdown() {
        try {
            ortSession?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error during shutdown", e)
        }
    }
}
