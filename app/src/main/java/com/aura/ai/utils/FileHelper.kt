package com.aura.ai.utils

import android.content.Context
import android.util.Log
import java.io.File

object FileHelper {
    private const val TAG = "FileHelper-DEBUG"
    
    data class ModelFiles(
        val modelFile: File,
        val tokenizerFile: File?,
        val modelName: String
    )
    
    fun getAuraDirectory(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), Constants.AURA_DIR)
        Log.d(TAG, "Aura directory: ${dir.absolutePath}")
        return dir
    }
    
    fun getModelsDirectory(context: Context): File {
        val dir = File(getAuraDirectory(context), Constants.MODELS_DIR)
        Log.d(TAG, "Models directory: ${dir.absolutePath}")
        return dir
    }
    
    fun getTokenizerDirectory(context: Context): File {
        val dir = File(getAuraDirectory(context), Constants.TOKENIZER_DIR)
        Log.d(TAG, "Tokenizer directory: ${dir.absolutePath}")
        return dir
    }
    
    fun createAuraDirectory(context: Context): Boolean {
        return try {
            val auraDir = getAuraDirectory(context)
            val modelsDir = getModelsDirectory(context)
            val tokenizerDir = getTokenizerDirectory(context)
            
            Log.d(TAG, "Creating directories...")
            
            if (!auraDir.exists()) {
                val created = auraDir.mkdirs()
                Log.d(TAG, "Created aura dir: $created")
            }
            if (!modelsDir.exists()) {
                val created = modelsDir.mkdirs()
                Log.d(TAG, "Created models dir: $created")
            }
            if (!tokenizerDir.exists()) {
                val created = tokenizerDir.mkdirs()
                Log.d(TAG, "Created tokenizer dir: $created")
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating directories", e)
            false
        }
    }
    
    fun detectModelFiles(context: Context): ModelFiles? {
        val modelsDir = getModelsDirectory(context)
        val tokenizerDir = getTokenizerDirectory(context)
        
        Log.d(TAG, "=== MODEL DETECTION ===")
        Log.d(TAG, "Models dir exists: ${modelsDir.exists()}")
        
        if (!modelsDir.exists()) {
            Log.e(TAG, "❌ Models directory does not exist!")
            createAuraDirectory(context)
            return null
        }
        
        // List ALL files in models directory
        Log.d(TAG, "Files in models directory:")
        val allFiles = modelsDir.listFiles()
        if (allFiles == null || allFiles.isEmpty()) {
            Log.e(TAG, "❌ Models directory is EMPTY!")
        } else {
            allFiles.forEach { file ->
                Log.d(TAG, "  📄 ${file.name} (${file.length()} bytes) - canRead: ${file.canRead()}")
            }
        }
        
        // Find ANY .onnx file
        val onnxFiles = modelsDir.listFiles { file -> 
            file.extension.equals("onnx", ignoreCase = true) 
        }
        
        if (onnxFiles.isNullOrEmpty()) {
            Log.e(TAG, "❌ No .onnx files found!")
            return null
        }
        
        val modelFile = onnxFiles[0]
        Log.d(TAG, "✅ Found model: ${modelFile.name} (${modelFile.length() / (1024*1024)} MB)")
        
        // Check tokenizer directory
        Log.d(TAG, "Tokenizer dir exists: ${tokenizerDir.exists()}")
        var tokenizerFile: File? = null
        
        if (tokenizerDir.exists()) {
            Log.d(TAG, "Files in tokenizer directory:")
            val tokenizerFiles = tokenizerDir.listFiles()
            if (tokenizerFiles != null) {
                tokenizerFiles.forEach { file ->
                    Log.d(TAG, "  📄 ${file.name} (${file.length()} bytes)")
                    if (file.extension.equals("json", ignoreCase = true)) {
                        tokenizerFile = file
                    }
                }
            }
        }
        
        val modelName = modelFile.nameWithoutExtension
        Log.d(TAG, "=== DETECTION COMPLETE ===")
        Log.d(TAG, "Model ready: ${modelFile.exists() && modelFile.length() > 0}")
        Log.d(TAG, "Tokenizer ready: ${tokenizerFile?.exists() == true}")
        
        return ModelFiles(modelFile, tokenizerFile, modelName)
    }
    
    fun isModelReady(context: Context): Boolean {
        val files = detectModelFiles(context)
        return files != null && files.modelFile.exists() && files.modelFile.length() > 0
    }
    
    fun getModelFile(context: Context): File? {
        return detectModelFiles(context)?.modelFile
    }
    
    fun getTokenizerFile(context: Context): File? {
        return detectModelFiles(context)?.tokenizerFile
    }
    
    fun getModelName(context: Context): String {
        return detectModelFiles(context)?.modelName ?: "Unknown Model"
    }
    
    fun getExternalDisplayPath(context: Context): String {
        return "/storage/emulated/0/Android/data/${context.packageName}/files/${Constants.AURA_DIR}"
    }
}
