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
        
        if (!modelsDir.exists()) return null
        
        val onnxFiles = modelsDir.listFiles { file -> 
            file.extension.equals("onnx", ignoreCase = true) 
        }
        
        if (onnxFiles.isNullOrEmpty()) return null
        
        val modelFile = onnxFiles[0]
        
        var tokenizerFile: File? = null
        if (tokenizerDir.exists()) {
            val jsonFiles = tokenizerDir.listFiles { file -> 
                file.extension.equals("json", ignoreCase = true) 
            }
            if (!jsonFiles.isNullOrEmpty()) {
                tokenizerFile = jsonFiles[0]
            }
        }
        
        val modelName = modelFile.nameWithoutExtension
        return ModelFiles(modelFile, tokenizerFile, modelName)
    }
    
    fun isModelReady(context: Context): Boolean {
        return detectModelFiles(context) != null
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
    
    // ⬇️ THIS IS THE FUNCTION THAT WAS MISSING ⬇️
    fun getFreeSpace(context: Context): Long {
        return getAuraDirectory(context).freeSpace
    }
}
