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
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.NodeInfo
import java.nio.LongBuffer
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
    
    // Simple tokenizer placeholder - you'll need to replace with actual tokenizer
    private fun tokenize(text: String): LongArray {
        // TODO: Replace with actual tokenizer
        // This is a SIMPLE placeholder - just converts chars to longs
        return text.map { it.code.toLong() }.toLongArray()
    }
    
    // Simple detokenizer placeholder
    private fun detokenize(tokens: LongArray): String {
        // TODO: Replace with actual detokenizer
        // This is a SIMPLE placeholder - just converts longs back to chars
        return tokens.map { it.toInt().toChar() }.joinToString("")
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
            
            // Initialize ONNX Runtime environment
            ortEnvironment = OrtEnvironment.getEnvironment()
            
            // Create session options
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
                
                try {
                    addNnapi()
                    Log.d(TAG, "NNAPI enabled")
                } catch (e: Exception) {
                    Log.d(TAG, "NNAPI not available")
                }
                
                setMemoryPatternOptimization(true)
                setCPUArenaAllocator(true)
            }
            
            // Create the session
            ortSession = ortEnvironment?.createSession(modelFile.absolutePath, sessionOptions)
            
            if (ortSession == null) {
                Log.e(TAG, "Failed to create session")
                return@withContext false
            }
            
            // Log model info
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
            if (!_isInitialized.value) {
                return@withContext "⚠️ Model not initialized yet."
            }
            
            if (ortSession == null || ortEnvironment == null) {
                return@withContext "⚠️ Model session not available."
            }
            
            Log.d(TAG, "Generating response for: $prompt")
            
            // Step 1: Tokenize the input
            val inputTokens = tokenize(prompt)
            Log.d(TAG, "Tokenized ${inputTokens.size} tokens")
            
            // Step 2: Create input tensor
            val inputShape = longArrayOf(1, inputTokens.size.toLong())
            val inputBuffer = LongBuffer.wrap(inputTokens)
            
            val inputTensor = OnnxTensor.createTensor(
                ortEnvironment,
                inputBuffer,
                inputShape
            )
            
            // Step 3: Get input/output names
            val inputName = ortSession?.inputInfo?.keys?.firstOrNull() ?: return@withContext "No input found"
            val outputName = ortSession?.outputInfo?.keys?.firstOrNull() ?: return@withContext "No output found"
            
            // Step 4: Run inference
            val inputs = mapOf(inputName to inputTensor)
            val results = ortSession?.run(inputs)
            
            // Step 5: Get output tensor
            val outputTensor = results?.get(outputName)?.value as? OnnxTensor
            val outputData = outputTensor?.longBuffer
            
            // Step 6: Convert output to text
            val response = if (outputData != null) {
                val outputTokens = LongArray(outputData.remaining())
                outputData.get(outputTokens)
                detokenize(outputTokens)
            } else {
                "⚠️ No output generated"
            }
            
            // Clean up
            inputTensor.close()
            results?.close()
            
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
