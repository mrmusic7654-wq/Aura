package com.aura.ai.utils

import android.content.Context
import android.util.Log
import java.io.File

object FileHelper {
    private const val TAG = "FileHelper"
    
    fun getAuraDirectory(context: Context): File {
        return File(context.getExternalFilesDir(null), "AuraAI")
    }
    
    fun getModelsDirectory(context: Context): File {
        return File(getAuraDirectory(context), "models")
    }
    
    fun createAuraDirectory(context: Context): Boolean {
        return try {
            val auraDir = getAuraDirectory(context)
            val modelsDir = getModelsDirectory(context)
            
            if (!auraDir.exists()) auraDir.mkdirs()
            if (!modelsDir.exists()) modelsDir.mkdirs()
            
            Log.d(TAG, "Directory created: ${modelsDir.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating directories", e)
            false
        }
    }
    
    fun isModelReady(context: Context): Boolean {
        val modelsDir = getModelsDirectory(context)
        
        Log.d(TAG, "Checking: ${modelsDir.absolutePath}")
        
        if (!modelsDir.exists()) {
            Log.e(TAG, "Directory missing")
            return false
        }
        
        val files = modelsDir.listFiles()
        Log.d(TAG, "Files found: ${files?.size ?: 0}")
        
        // FORCE DETECTION - Look for ANY .tflite and ANY .json file
        var hasModel = false
        var hasVocab = false
        
        files?.forEach { file ->
            Log.d(TAG, "  Found: ${file.name}")
            if (file.name.endsWith(".tflite", ignoreCase = true)) {
                hasModel = true
                Log.d(TAG, "  ✅ Model file recognized")
            }
            if (file.name.endsWith(".json", ignoreCase = true)) {
                hasVocab = true
                Log.d(TAG, "  ✅ Vocab file recognized")
            }
        }
        
        // If we have at least one of each type, consider it ready
        val ready = hasModel && hasVocab
        Log.d(TAG, "Model ready: $ready")
        return ready
    }
    
    fun getModelFile(context: Context): File? {
        val modelsDir = getModelsDirectory(context)
        return modelsDir.listFiles()?.firstOrNull { 
            it.name.endsWith(".tflite", ignoreCase = true)
        }
    }
    
    fun getTokenizerFile(context: Context): File? {
        val modelsDir = getModelsDirectory(context)
        return modelsDir.listFiles()?.firstOrNull { 
            it.name.endsWith(".json", ignoreCase = true)
        }
    }
    
    fun getModelName(context: Context): String {
        return getModelFile(context)?.nameWithoutExtension ?: "Unknown"
    }
    
    fun getExternalDisplayPath(context: Context): String {
        return "/storage/emulated/0/Android/data/${context.packageName}/files/AuraAI/models"
    }
}
