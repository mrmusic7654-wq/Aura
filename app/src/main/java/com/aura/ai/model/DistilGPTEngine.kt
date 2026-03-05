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
import java.nio.charset.StandardCharsets
import org.json.JSONObject

class DistilGPTEngine(private val context: Context) {
    
    private val TAG = "DistilGPTEngine"
    private var interpreter: Interpreter? = null
    private var tokenizer: Map<String, Int>? = null
    private var reverseTokenizer: Map<Int, String>? = null
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized
    
    private val _currentModel = MutableStateFlow("")
    val currentModel: StateFlow<String> = _currentModel
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val modelFile = FileHelper.getModelFile(context)
            val tokenizerFile = FileHelper.getTokenizerFile(context)
            
            if (modelFile == null || tokenizerFile == null) {
                Log.e(TAG, "Missing model or tokenizer files")
                return@withContext false
            }
            
            _currentModel.value = modelFile.nameWithoutExtension
            Log.d(TAG, "Loading model: ${modelFile.absolutePath}")
            Log.d(TAG, "Loading tokenizer: ${tokenizerFile.absolutePath}")
            
            // Load tokenizer
            loadTokenizer(tokenizerFile)
            
            // Load model
            val modelBuffer = loadModelFile(modelFile.absolutePath)
            interpreter = Interpreter(modelBuffer)
            
            _isInitialized.value = true
            Log.d(TAG, "✅ Model and tokenizer loaded successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize", e)
            false
        }
    }
    
    private fun loadTokenizer(tokenizerFile: java.io.File) {
        try {
            val jsonString = tokenizerFile.readText(StandardCharsets.UTF_8)
            val json = JSONObject(jsonString)
            
            tokenizer = mutableMapOf()
            reverseTokenizer = mutableMapOf()
            
            // Handle different tokenizer formats
            if (json.has("model") && json.getJSONObject("model").has("vocab")) {
                // HuggingFace tokenizer.json format
                val vocab = json.getJSONObject("model").getJSONObject("vocab")
                val keys = vocab.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val id = vocab.getInt(key)
                    (tokenizer as MutableMap)[key] = id
                    (reverseTokenizer as MutableMap)[id] = key
                }
            } else if (json.has("vocab")) {
                // Simple vocab.json format
                val vocab = json.getJSONObject("vocab")
                val keys = vocab.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val id = vocab.getInt(key)
                    (tokenizer as MutableMap)[key] = id
                    (reverseTokenizer as MutableMap)[id] = key
                }
            } else {
                // Assume entire JSON is vocab mapping
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val id = json.getInt(key)
                    (tokenizer as MutableMap)[key] = id
                    (reverseTokenizer as MutableMap)[id] = key
                }
            }
            
            Log.d(TAG, "Loaded ${tokenizer?.size} tokens")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load tokenizer", e)
            // Create fallback simple tokenizer
            createFallbackTokenizer()
        }
    }
    
    private fun createFallbackTokenizer() {
        tokenizer = mutableMapOf()
        reverseTokenizer = mutableMapOf()
        
        // Simple character-level fallback
        "abcdefghijklmnopqrstuvwxyz ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.,!?-'\";:()[]{}".forEachIndexed { index, char ->
            (tokenizer as MutableMap)[char.toString()] = index
            (reverseTokenizer as MutableMap)[index] = char.toString()
        }
        // Add special tokens
        (tokenizer as MutableMap)["[UNK]"] = tokenizer!!.size
        (tokenizer as MutableMap)["[CLS]"] = tokenizer!!.size
        (tokenizer as MutableMap)["[SEP]"] = tokenizer!!.size
        (tokenizer as MutableMap)["[MASK]"] = tokenizer!!.size
        
        Log.d(TAG, "Created fallback tokenizer with ${tokenizer?.size} tokens")
    }
    
    private fun loadModelFile(modelPath: String): MappedByteBuffer {
        val fileInputStream = FileInputStream(modelPath)
        val fileChannel = fileInputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size())
    }
    
    private fun encode(text: String): IntArray {
        if (tokenizer == null) return intArrayOf(0)
        
        // Simple encoding - split by spaces and map to tokens
        val words = text.lowercase().split(Regex("\\s+"))
        val tokens = mutableListOf<Int>()
        
        // Add [CLS] token if exists
        tokenizer?.get("[CLS]")?.let { tokens.add(it) }
        
        for (word in words) {
            // Try whole word first
            var tokenId = tokenizer?.get(word)
            if (tokenId == null) {
                // Try character by character for unknown words
                for (char in word) {
                    tokenId = tokenizer?.get(char.toString())
                    if (tokenId != null) {
                        tokens.add(tokenId)
                    } else {
                        // Unknown character
                        tokenizer?.get("[UNK]")?.let { tokens.add(it) }
                    }
                }
            } else {
                tokens.add(tokenId)
            }
        }
        
        return tokens.toIntArray()
    }
    
    private fun decode(tokens: IntArray): String {
        if (reverseTokenizer == null) return ""
        
        val result = StringBuilder()
        for (token in tokens) {
            reverseTokenizer?.get(token)?.let { word ->
                result.append(word)
            }
        }
        return result.toString()
    }
    
    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            if (!_isInitialized.value || interpreter == null) {
                return@withContext "⚠️ Model not ready. Please check your model files."
            }
            
            Log.d(TAG, "Generating response for: $prompt")
            
            // Encode prompt to tokens
            val inputTokens = encode(prompt)
            Log.d(TAG, "Encoded ${inputTokens.size} tokens")
            
            // Prepare input tensor
            val inputShape = longArrayOf(1, inputTokens.size.toLong())
            val inputBuffer = java.nio.IntBuffer.wrap(inputTokens)
            
            // Create input array (adjust based on your model's expected input)
            val inputArray = arrayOf(inputBuffer)
            
            // Prepare output tensor
            val outputShape = longArrayOf(1, inputTokens.size.toLong(), tokenizer?.size ?: 50000)
            val outputBuffer = java.nio.FloatBuffer.allocate(1 * inputTokens.size * (tokenizer?.size ?: 50000))
            val outputArray = arrayOf(outputBuffer)
            
            // Run inference
            interpreter?.runForMultipleInputsOutputs(inputArray, outputArray)
            
            // Get next token prediction (simplified - take highest probability)
            val logits = FloatArray(tokenizer?.size ?: 50000)
            outputBuffer.rewind()
            outputBuffer.get(logits)
            
            // Find highest probability token (excluding special tokens)
            var maxIdx = 0
            var maxVal = logits[0]
            for (i in 1 until logits.size) {
                if (logits[i] > maxVal) {
                    maxVal = logits[i]
                    maxIdx = i
                }
            }
            
            // Generate response (simplified - just return predicted token)
            val responseTokens = intArrayOf(maxIdx)
            val response = decode(responseTokens)
            
            Log.d(TAG, "Generated response: $response")
            return@withContext response
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating response", e)
            "❌ Error: ${e.message}"
        }
    }
    
    fun shutdown() {
        interpreter?.close()
    }
}
