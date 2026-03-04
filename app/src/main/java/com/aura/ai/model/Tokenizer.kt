package com.aura.ai.model

import android.content.Context
import android.util.Log
import com.aura.ai.utils.FileHelper
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Tokenizer(private val context: Context) {
    
    private val TAG = "Tokenizer"
    
    private var vocab: Map<String, Int> = emptyMap()
    private var reverseVocab: Map<Int, String> = emptyMap()
    private var merges: List<Pair<String, String>> = emptyList()
    private var isInitialized = false
    
    // BPE specific
    private var bpeRanks: Map<Pair<String, String>, Int> = emptyMap()
    private var cache: MutableMap<String, String> = mutableMapOf()
    
    suspend fun initialize(): Boolean {
        return try {
            val tokenizerFile = FileHelper.getTokenizerFile(context)
            if (tokenizerFile == null) {
                Log.e(TAG, "No tokenizer file found")
                return false
            }
            
            Log.d(TAG, "Loading tokenizer from: ${tokenizerFile.absolutePath}")
            
            when (tokenizerFile.extension.lowercase()) {
                "json" -> loadJsonTokenizer(tokenizerFile)
                "model" -> loadSentencePieceTokenizer(tokenizerFile)
                "txt" -> loadVocabTxt(tokenizerFile)
                else -> {
                    Log.e(TAG, "Unknown tokenizer format: ${tokenizerFile.extension}")
                    return false
                }
            }
            
            isInitialized = true
            Log.d(TAG, "✅ Tokenizer loaded: ${vocab.size} tokens")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load tokenizer", e)
            false
        }
    }
    
    private fun loadJsonTokenizer(file: File) {
        val jsonString = file.readText()
        val root = JSONObject(jsonString)
        
        // Try different common JSON formats
        if (root.has("model") && root.getJSONObject("model").has("vocab")) {
            // HuggingFace tokenizer.json format
            val vocabObj = root.getJSONObject("model").getJSONObject("vocab")
            vocab = vocabObj.keys().asSequence().associateWith { key ->
                vocabObj.getInt(key)
            }
            
            if (root.getJSONObject("model").has("merges")) {
                val mergesArray = root.getJSONObject("model").getJSONArray("merges")
                merges = (0 until mergesArray.length()).map { i ->
                    val parts = mergesArray.getString(i).split(" ")
                    Pair(parts[0], parts[1])
                }
            }
        } else if (root.has("vocab")) {
            // Simple vocab.json format
            val vocabObj = root.getJSONObject("vocab")
            vocab = vocabObj.keys().asSequence().associateWith { key ->
                vocabObj.getInt(key)
            }
        } else {
            // Assume the entire JSON is vocab mapping
            vocab = root.keys().asSequence().associateWith { key ->
                root.getInt(key)
            }
        }
        
        // Build reverse vocab
        reverseVocab = vocab.entries.associate { it.value to it.key }
        
        // Build BPE ranks if merges exist
        if (merges.isNotEmpty()) {
            bpeRanks = merges.withIndex().associate { it.value to it.index }
        }
    }
    
    private fun loadSentencePieceTokenizer(file: File) {
        // Simple SentencePiece loader for .model files
        // This is a placeholder - you'd need a proper SentencePiece implementation
        Log.w(TAG, "SentencePiece tokenizer loading not fully implemented")
        
        // Create a simple char-level fallback
        vocab = (' '.toInt()..'~'.toInt()).associate { 
            it.toChar().toString() to it - ' '.toInt()
        }
        reverseVocab = vocab.entries.associate { it.value to it.key }
    }
    
    private fun loadVocabTxt(file: File) {
        // Simple vocab.txt format (one token per line, index = line number)
        val lines = file.readLines()
        vocab = lines.withIndex().associate { it.value to it.index }
        reverseVocab = vocab.entries.associate { it.value to it.key }
    }
    
    fun encode(text: String): List<Int> {
        if (!isInitialized) {
            Log.e(TAG, "Tokenizer not initialized")
            return emptyList()
        }
        
        // Simple BPE encoding if available
        return if (bpeRanks.isNotEmpty()) {
            encodeWithBPE(text)
        } else {
            // Fallback to char-level encoding
            text.map { char ->
                vocab[char.toString()] ?: vocab["<unk>"] ?: 0
            }
        }
    }
    
    private fun encodeWithBPE(text: String): List<Int> {
        // Very simplified BPE implementation
        val words = text.lowercase().split(Regex("\\s+"))
        val outputTokens = mutableListOf<Int>()
        
        for (word in words) {
            if (cache.containsKey(word)) {
                val cached = cache[word] ?: continue
                cached.split(" ").forEach { token ->
                    vocab[token]?.let { outputTokens.add(it) }
                }
                continue
            }
            
            var wordChars = word.toList().map { it.toString() }
            var pairs = getPairs(wordChars)
            
            while (pairs.isNotEmpty()) {
                val minPair = pairs.minByOrNull { pair ->
                    bpeRanks[pair] ?: Int.MAX_VALUE
                } ?: break
                
                if (!bpeRanks.containsKey(minPair)) {
                    break
                }
                
                val first = minPair.first
                val second = minPair.second
                val newWord = mutableListOf<String>()
                var i = 0
                
                while (i < wordChars.size) {
                    val j = wordChars.indexOfFirst { it == first } ?: wordChars.size
                    if (j != -1 && j < wordChars.size - 1 && wordChars[j] == first && wordChars[j + 1] == second) {
                        newWord.add(first + second)
                        i = j + 2
                    } else {
                        newWord.add(wordChars[i])
                        i++
                    }
                }
                
                wordChars = newWord
                if (wordChars.size == 1) {
                    break
                } else {
                    pairs = getPairs(wordChars)
                }
            }
            
            val wordStr = wordChars.joinToString(" ")
            cache[word] = wordStr
            wordChars.forEach { token ->
                vocab[token]?.let { outputTokens.add(it) }
            }
        }
        
        return outputTokens
    }
    
    private fun getPairs(word: List<String>): Set<Pair<String, String>> {
        val pairs = mutableSetOf<Pair<String, String>>()
        for (i in 0 until word.size - 1) {
            pairs.add(Pair(word[i], word[i + 1]))
        }
        return pairs
    }
    
    fun decode(tokens: List<Int>): String {
        if (!isInitialized) {
            Log.e(TAG, "Tokenizer not initialized")
            return ""
        }
        
        return tokens.mapNotNull { reverseVocab[it] }.joinToString("")
    }
    
    fun encodeBatch(texts: List<String>): List<List<Int>> {
        return texts.map { encode(it) }
    }
    
    fun decodeBatch(tokens: List<List<Int>>): List<String> {
        return tokens.map { decode(it) }
    }
    
    fun getVocabSize(): Int {
        return vocab.size
    }
    
    fun isReady(): Boolean = isInitialized
}
