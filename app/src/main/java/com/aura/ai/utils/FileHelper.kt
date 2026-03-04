package com.aura.ai.utils

import android.content.Context
import android.util.Log
import java.io.File

object FileHelper {
    private const val TAG = "FileHelper"
    
    data class ModelFiles(
        val modelFile: File,
        val tokenizerFile: File?,
        val modelName: String
    )
    
    fun getAuraDirectory(context: Context): File {
        return File(context.getExternalFilesDir(null), Constants.AURA_DIR)
    }
    
    fun getModelsDirectory(context: Context): File {
        return File(getAuraDirectory(context), Constants.MODELS_DIR)
    }
    
    fun getTokenizerDirectory(context: Context): File {
        return File(getAuraDirectory(context), Constants.TOKENIZER_DIR)
    }
    
    fun getExportsDirectory(context: Context): File {
        return File(getAuraDirectory(context), Constants.EXPORT_DIR)
    }
    
    fun createAuraDirectory(context: Context): Boolean {
        return try {
            val auraDir = getAuraDirectory(context)
            val modelsDir = getModelsDirectory(context)
            val tokenizerDir = getTokenizerDirectory(context)
            val exportsDir = getExportsDirectory(context)
            
            Log.d(TAG, "Creating directories at: ${auraDir.absolutePath}")
            
            if (!auraDir.exists()) auraDir.mkdirs()
            if (!modelsDir.exists()) modelsDir.mkdirs()
            if (!tokenizerDir.exists()) tokenizerDir.mkdirs()
            if (!exportsDir.exists()) exportsDir.mkdirs()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating directories", e)
            false
        }
    }
    
    fun detectModelFiles(context: Context): ModelFiles? {
        val modelsDir = getModelsDirectory(context)
        val tokenizerDir = getTokenizerDirectory(context)
        
        Log.d(TAG, "Scanning for models in: ${modelsDir.absolutePath}")
        
        if (!modelsDir.exists()) {
            Log.d(TAG, "Models directory doesn't exist")
            return null
        }
        
        // Find any .onnx file
        val onnxFiles = modelsDir.listFiles { file -> 
            file.extension.equals("onnx", ignoreCase = true) 
        }
        
        if (onnxFiles.isNullOrEmpty()) {
            Log.d(TAG, "No ONNX files found")
            return null
        }
        
        // Use the first ONNX file found
        val modelFile = onnxFiles[0]
        Log.d(TAG, "Found model: ${modelFile.name} (${modelFile.length() / (1024*1024)} MB)")
        
        // Look for tokenizer files
        var tokenizerFile: File? = null
        
        if (tokenizerDir.exists()) {
            // Common tokenizer file names
            val tokenizerCandidates = listOf(
                "tokenizer.json",
                "tokenizer.model",
                "vocab.json",
                "merges.txt",
                "tokenizer_config.json"
            )
            
            // First try common names
            for (name in tokenizerCandidates) {
                val file = File(tokenizerDir, name)
                if (file.exists()) {
                    tokenizerFile = file
                    Log.d(TAG, "Found tokenizer: ${file.name}")
                    break
                }
            }
            
            // If not found, take any json or model file
            if (tokenizerFile == null) {
                val jsonFiles = tokenizerDir.listFiles { file -> 
                    file.extension.equals("json", ignoreCase = true) ||
                    file.extension.equals("model", ignoreCase = true)
                }
                if (!jsonFiles.isNullOrEmpty()) {
                    tokenizerFile = jsonFiles[0]
                    Log.d(TAG, "Found tokenizer: ${tokenizerFile?.name}")
                }
            }
        }
        
        val modelName = modelFile.nameWithoutExtension
        return ModelFiles(modelFile, tokenizerFile, modelName)
    }
    
    fun isModelReady(context: Context): Boolean {
        val modelFiles = detectModelFiles(context)
        val ready = modelFiles != null && modelFiles.modelFile.exists() && modelFiles.modelFile.length() > 0
        Log.d(TAG, "Model ready: $ready")
        return ready
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
    
    fun getModelSize(context: Context): Long {
        return getModelFile(context)?.length() ?: 0
    }
    
    fun getExternalDisplayPath(context: Context): String {
        return "/storage/emulated/0/Android/data/${context.packageName}/files/${Constants.AURA_DIR}"
    }
    
    fun getFreeSpace(context: Context): Long {
        return getAuraDirectory(context).freeSpace
    }
    
    fun listModelFiles(context: Context): List<File> {
        val modelsDir = getModelsDirectory(context)
        return if (modelsDir.exists()) {
            modelsDir.listFiles()?.filter { it.isFile } ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    fun listTokenizerFiles(context: Context): List<File> {
        val tokenizerDir = getTokenizerDirectory(context)
        return if (tokenizerDir.exists()) {
            tokenizerDir.listFiles()?.filter { it.isFile } ?: emptyList()
        } else {
            emptyList()
        }
    }
}
