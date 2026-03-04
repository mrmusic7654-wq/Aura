package com.aura.ai.model

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.util.Log
import java.nio.LongBuffer
import java.util.concurrent.Executors

class ModelInference(
    private val ortEnvironment: OrtEnvironment,
    private val ortSession: OrtSession,
    private val tokenizer: Tokenizer
) {
    
    private val TAG = "ModelInference"
    private val executor = Executors.newSingleThreadExecutor()
    
    // Model configuration
    private val maxLength = 512
    private val temperature = 0.7f
    private val topK = 50
    private val topP = 0.9f
    private val eosTokenId = 2L // Common EOS token ID
    
    // Cache input/output names
    private val inputNames: List<String>
    private val outputNames: List<String>
    
    init {
        inputNames = ortSession.inputInfo.keys.toList()
        outputNames = ortSession.outputInfo.keys.toList()
        
        Log.d(TAG, "Model inputs: $inputNames")
        Log.d(TAG, "Model outputs: $outputNames")
    }
    
    suspend fun generate(
        prompt: String,
        maxNewTokens: Int = 128,
        doSample: Boolean = true
    ): String {
        return try {
            // Encode prompt to tokens
            val inputTokens = tokenizer.encode(prompt)
            Log.d(TAG, "Input tokens: ${inputTokens.size}")
            
            // Generate tokens autoregressively
            val allTokens = inputTokens.toMutableList()
            
            for (i in 0 until maxNewTokens) {
                // Prepare input for current step - FIXED: Convert List<Int> to LongArray properly
                val currentInput = allTokens.takeLast(maxLength).map { it.toLong() }.toLongArray()
                
                // Run inference
                val nextToken = runInferenceStep(currentInput, doSample)
                
                // FIXED: Compare Long with Long properly
                if (nextToken == eosTokenId) break
                
                allTokens.add(nextToken.toInt())
            }
            
            // Decode tokens back to text
            tokenizer.decode(allTokens)
            
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed", e)
            "Error: ${e.message}"
        }
    }
    
    private fun runInferenceStep(inputTokens: LongArray, doSample: Boolean): Long {
        // Create input tensor
        val inputShape = longArrayOf(1, inputTokens.size.toLong())
        val inputBuffer = LongBuffer.wrap(inputTokens)
        
        val inputTensor = OnnxTensor.createTensor(
            ortEnvironment,
            inputBuffer,
            inputShape
        )
        
        // Prepare inputs map
        val inputs = mapOf(inputNames[0] to inputTensor)
        
        // Run inference
        val results = ortSession.run(inputs)
        
        // Get logits (assume first output)
        val logitsTensor = results[outputNames[0]].get() as OnnxTensor
        val logitsBuffer = logitsTensor.floatBuffer
        
        // Convert to array
        val logits = FloatArray(logitsBuffer.remaining())
        logitsBuffer.get(logits)
        
        // Get next token
        val nextToken = if (doSample) {
            sampleTopK(logits, topK)
        } else {
            argMax(logits)
        }
        
        // Cleanup
        inputTensor.close()
        results.close()
        
        return nextToken
    }
    
    private fun argMax(logits: FloatArray): Long {
        var maxIdx = 0
        var maxVal = logits[0]
        
        for (i in 1 until logits.size) {
            if (logits[i] > maxVal) {
                maxVal = logits[i]
                maxIdx = i
            }
        }
        
        return maxIdx.toLong()
    }
    
    private fun sampleTopK(logits: FloatArray, k: Int): Long {
        // Get top k indices and values
        val indices = logits.indices.toList()
        val sortedIndices = indices.sortedByDescending { logits[it] }
        
        val topKIndices = sortedIndices.take(k)
        val topKLogits = topKIndices.map { logits[it] }
        
        // Apply softmax
        val maxLogit = topKLogits.maxOrNull() ?: 0f
        val expLogits = topKLogits.map { Math.exp((it - maxLogit).toDouble()).toFloat() }
        val sumExp = expLogits.sum()
        val probs = expLogits.map { it / sumExp }
        
        // Sample from distribution
        val random = Math.random()
        var cumulative = 0.0
        
        for (i in probs.indices) {
            cumulative += probs[i]
            if (random < cumulative) {
                return topKIndices[i].toLong()
            }
        }
        
        return topKIndices.last().toLong()
    }
    
    fun shutdown() {
        executor.shutdown()
    }
}
