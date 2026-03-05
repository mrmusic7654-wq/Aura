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
        return File(getAuraDirectory(context), "models")  // Both files go here
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
        
        if (!modelsDir.exists()) return false
        
        val files = modelsDir.listFiles() ?: return false
        
        var hasModel = false
        var hasTokenizer = false
        
        files.forEach { file ->
            when {
                file.extension.equals("tflite", ignoreCase = true) -> hasModel = true
                file.extension.equals("json", ignoreCase = true) -> hasTokenizer = true
            }
        }
        
        return hasModel && hasTokenizer
    }
    
    fun getModelFile(context: Context): File? {
        val modelsDir = getModelsDirectory(context)
        return modelsDir.listFiles()?.firstOrNull { 
            it.extension.equals("tflite", ignoreCase = true) 
        }
    }
    
    fun getTokenizerFile(context: Context): File? {
        val modelsDir = getModelsDirectory(context)
        return modelsDir.listFiles()?.firstOrNull { 
            it.extension.equals("json", ignoreCase = true) 
        }
    }
    
    fun getModelName(context: Context): String {
        return getModelFile(context)?.nameWithoutExtension ?: "Unknown"
    }
    
    fun getExternalDisplayPath(context: Context): String {
        return "/storage/emulated/0/Android/data/${context.packageName}/files/AuraAI/models"
    }
}
