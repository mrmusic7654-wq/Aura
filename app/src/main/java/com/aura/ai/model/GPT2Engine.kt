package com.aura.ai.model

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class GPT2Engine(private val context: Context) {
    
    private val TAG = "GPT2Engine"
    private var interpreter: Interpreter? = null
    private var tokenizer: GPT2Tokenizer? = null
    
    private var _isInitialized = false
    val isInitialized: Boolean get() = _isInitialized
    
    fun initialize(modelFile: java.io.File, vocabFile: java.io.File, mergesFile: java.io.File? = null): Boolean {
        return try {
            Log.d(TAG, "Loading model: ${modelFile.absolutePath}")
            
            // Load TFLite model
            val modelBuffer = loadModelFile(modelFile.absolutePath)
            interpreter = Interpreter(modelBuffer)
            
            // Load tokenizer
            val vocabStream = FileInputStream(vocabFile)
            tokenizer = GPT2Tokenizer(vocabStream)
            
            // Load merges if available
            mergesFile?.let {
                val mergesStream = FileInputStream(it)
                tokenizer?.setMergesStream(mergesStream)
            }
            
            _isInitialized = true
            Log.d(TAG, "✅ Model and tokenizer loaded successfully")
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
    
    fun generate(prompt: String, maxLength: Int = 30): String {
        // Check if interpreter exists and is not closed
        if (!_isInitialized || interpreter == null) {
            return "Model not ready"
        }
        
        try {
            Log.d(TAG, "Generating for: $prompt")
            
            // Tokenize input
            val inputIds = tokenizer!!.encode(prompt)
            if (inputIds.isEmpty()) {
                return "No tokens generated"
            }
            
            // Prepare input tensor [1, sequence_length]
            val inputArray = Array(1) { FloatArray(inputIds.size) }
            for (i in inputIds.indices) {
                inputArray[0][i] = inputIds[i].toFloat()
            }
            
            // Prepare output tensor (vocab size 50257 for GPT-2)
            val outputArray = Array(1) { FloatArray(50257) }
            
            // Run inference
            interpreter!!.run(inputArray, outputArray)
            
            // Get most likely next token
            val nextTokenId = argMax(outputArray[0])
            
            // Decode
            val result = tokenizer!!.decode(intArrayOf(nextTokenId))
            Log.d(TAG, "Generated: $result")
            return result
            
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed", e)
            return "Error: ${e.message}"
        }
    }
    
    private fun argMax(array: FloatArray): Int {
        var maxIdx = 0
        var maxVal = array[0]
        for (i in 1 until array.size) {
            if (array[i] > maxVal) {
                maxVal = array[i]
                maxIdx = i
            }
        }
        return maxIdx
    }
    
    // REMOVED close() method - keep interpreter alive!
    // Only close when app is destroyed
    fun shutdown() {
        // Only called when ViewModel clears
        interpreter?.close()
        interpreter = null
    }
}
