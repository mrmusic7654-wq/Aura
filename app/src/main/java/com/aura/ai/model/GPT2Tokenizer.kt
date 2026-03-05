package com.aura.ai.model

import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class GPT2Tokenizer(private val inputStream: InputStream) {
    
    private val vocab: MutableMap<String, Int> = HashMap()
    private val reverseVocab: MutableMap<Int, String> = HashMap()
    private val bpeRanks: MutableMap<Pair<String, String>, Int> = HashMap()
    private val cache: MutableMap<String, String> = HashMap()
    private var mergesStream: InputStream? = null
    
    fun setMergesStream(mergesStream: InputStream) {
        this.mergesStream = mergesStream
        loadBpeRanks()
    }

    init {
        loadVocab()
    }

    private fun loadVocab() {
        try {
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val keys = jsonObject.keys()
            
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject.getInt(key)
                vocab[key] = value
                reverseVocab[value] = key
            }
            Log.d("GPT2Tokenizer", "Loaded ${vocab.size} tokens")
        } catch (e: Exception) {
            Log.e("GPT2Tokenizer", "Error loading vocab", e)
        }
    }

    private fun loadBpeRanks() {
        try {
            mergesStream?.let { stream ->
                val reader = BufferedReader(InputStreamReader(stream))
                var line: String?
                var index = 0
                
                // Skip first line if it contains "version"
                if (reader.readLine()?.startsWith("#version") == true) {
                    // Skip version line
                } else {
                    reader.close()
                    return
                }
                
                while (reader.readLine().also { line = it } != null) {
                    val parts = line!!.split(" ")
                    if (parts.size == 2) {
                        bpeRanks[Pair(parts[0], parts[1])] = index
                        index++
                    }
                }
                reader.close()
                Log.d("GPT2Tokenizer", "Loaded ${bpeRanks.size} BPE merges")
            }
        } catch (e: Exception) {
            Log.e("GPT2Tokenizer", "Error loading BPE ranks", e)
        }
    }

    fun encode(text: String): IntArray {
        val tokens = mutableListOf<Int>()
        
        // Simple tokenization by spaces for demo
        val words = text.lowercase().split(" ")
        
        for (word in words) {
            val token = vocab[word]
            if (token != null) {
                tokens.add(token)
            } else {
                // Handle unknown words with BPE
                val subwords = bpe(word)
                for (subword in subwords) {
                    val subwordToken = vocab[subword]
                    if (subwordToken != null) {
                        tokens.add(subwordToken)
                    } else {
                        // Use unknown token if available
                        tokens.add(vocab["<unk>"] ?: 0)
                    }
                }
            }
        }
        
        return tokens.toIntArray()
    }

    private fun bpe(token: String): List<String> {
        // Simplified BPE implementation
        // In production, use full BPE algorithm
        return listOf(token)
    }

    fun decode(tokens: IntArray): String {
        val result = StringBuilder()
        for (token in tokens) {
            val word = reverseVocab[token]
            if (word != null) {
                result.append(word)
            }
        }
        return result.toString()
    }
}
