package com.aura.ai.utils

import android.content.Context
import android.util.Log
import java.io.File

object FileHelper {
    private const val TAG = "AURA-DEBUG"
    
    fun getAuraDirectory(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "AuraAI")
        Log.d(TAG, "getAuraDirectory: ${dir.absolutePath}")
        return dir
    }
    
    fun getModelsDirectory(context: Context): File {
        val dir = File(getAuraDirectory(context), "models")
        Log.d(TAG, "getModelsDirectory: ${dir.absolutePath}")
        return dir
    }
    
    fun createAuraDirectory(context: Context): Boolean {
        return try {
            val auraDir = getAuraDirectory(context)
            val modelsDir = getModelsDirectory(context)
            
            Log.d(TAG, "Creating directories...")
            Log.d(TAG, "Aura dir: ${auraDir.absolutePath}")
            Log.d(TAG, "Models dir: ${modelsDir.absolutePath}")
            
            if (!auraDir.exists()) {
                val created = auraDir.mkdirs()
                Log.d(TAG, "Created Aura dir: $created")
            }
            if (!modelsDir.exists()) {
                val created = modelsDir.mkdirs()
                Log.d(TAG, "Created Models dir: $created")
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating directories", e)
            false
        }
    }
    
    fun isModelReady(context: Context): Boolean {
        val modelsDir = getModelsDirectory(context)
        
        Log.d(TAG, "=== CHECKING MODEL ===")
        Log.d(TAG, "Models dir: ${modelsDir.absolutePath}")
        Log.d(TAG, "Directory exists: ${modelsDir.exists()}")
        
        if (!modelsDir.exists()) {
            Log.e(TAG, "❌ Directory does NOT exist!")
            createAuraDirectory(context)
            return false
        }
        
        val files = modelsDir.listFiles()
        Log.d(TAG, "Number of files: ${files?.size ?: 0}")
        
        if (files == null || files.isEmpty()) {
            Log.e(TAG, "❌ No files found in directory!")
            return false
        }
        
        files.forEach { file ->
            Log.d(TAG, "  📄 Found: ${file.name} (${file.length()} bytes)")
        }
        
        val ggufFiles = files.filter { 
            it.extension.equals("gguf", ignoreCase = true) 
        }
        
        Log.d(TAG, "GGUF files found: ${ggufFiles.size}")
        
        if (ggufFiles.isNotEmpty()) {
            ggufFiles.forEach { file ->
                Log.d(TAG, "  ✅ GGUF model: ${file.name}")
            }
        }
        
        val hasModel = ggufFiles.isNotEmpty()
        Log.d(TAG, "Model ready: $hasModel")
        Log.d(TAG, "=== END CHECK ===")
        
        return hasModel
    }
    
    fun getModelFile(context: Context): File? {
        val modelsDir = getModelsDirectory(context)
        val ggufFiles = modelsDir.listFiles()?.filter { 
            it.extension.equals("gguf", ignoreCase = true) 
        }
        return ggufFiles?.firstOrNull()
    }
    
    fun getModelName(context: Context): String {
        return getModelFile(context)?.nameWithoutExtension ?: "Unknown"
    }
    
    fun getExternalDisplayPath(context: Context): String {
        return "/storage/emulated/0/Android/data/${context.packageName}/files/AuraAI/models"
    }
}
