package com.aura.ai.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

object FileHelper {
    private const val TAG = "FileHelper"
    
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
            
            Log.d(TAG, "Creating directories:")
            Log.d(TAG, "Aura dir: ${auraDir.absolutePath}")
            Log.d(TAG, "Models dir: ${modelsDir.absolutePath}")
            Log.d(TAG, "Tokenizer dir: ${tokenizerDir.absolutePath}")
            
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
    
    fun isModelReady(context: Context): Boolean {
        val modelsDir = getModelsDirectory(context)
        val tokenizerDir = getTokenizerDirectory(context)
        
        Log.d(TAG, "Checking model in: ${modelsDir.absolutePath}")
        Log.d(TAG, "Checking tokenizer in: ${tokenizerDir.absolutePath}")
        
        if (modelsDir.exists()) {
            val modelFiles = modelsDir.listFiles()
            Log.d(TAG, "Files in models dir: ${modelFiles?.joinToString { it.name }}")
        } else {
            Log.d(TAG, "Models directory does not exist!")
            createAuraDirectory(context)
        }
        
        if (tokenizerDir.exists()) {
            val tokenizerFiles = tokenizerDir.listFiles()
            Log.d(TAG, "Files in tokenizer dir: ${tokenizerFiles?.joinToString { it.name }}")
        } else {
            Log.d(TAG, "Tokenizer directory does not exist!")
        }
        
        val modelFile = File(modelsDir, Constants.MODEL_FILENAME)
        val tokenizerFile = File(tokenizerDir, Constants.TOKENIZER_FILENAME)
        
        val modelExists = modelFile.exists()
        val tokenizerExists = tokenizerFile.exists()
        val modelValid = modelExists && modelFile.length() > 0
        val tokenizerValid = tokenizerExists && tokenizerFile.length() > 0
        
        Log.d(TAG, "Model exists: $modelExists, size: ${if(modelExists) modelFile.length() else 0} bytes")
        Log.d(TAG, "Tokenizer exists: $tokenizerExists, size: ${if(tokenizerExists) tokenizerFile.length() else 0} bytes")
        Log.d(TAG, "Model ready: ${modelValid && tokenizerValid}")
        
        return modelValid && tokenizerValid
    }
    
    fun getExternalDisplayPath(context: Context): String {
        return "/storage/emulated/0/Android/data/${context.packageName}/files/${Constants.AURA_DIR}"
    }
    
    fun getModelFileSize(context: Context): Long {
        val modelFile = File(getModelsDirectory(context), Constants.MODEL_FILENAME)
        return if (modelFile.exists()) modelFile.length() else 0
    }
    
    fun getTokenizerFileSize(context: Context): Long {
        val tokenizerFile = File(getTokenizerDirectory(context), Constants.TOKENIZER_FILENAME)
        return if (tokenizerFile.exists()) tokenizerFile.length() else 0
    }
    
    fun getFreeSpace(context: Context): Long {
        return getAuraDirectory(context).freeSpace
    }
}
