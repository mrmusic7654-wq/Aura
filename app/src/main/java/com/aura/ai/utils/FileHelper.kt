package com.aura.ai.utils

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

object FileHelper {
    
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
    
    fun getChatHistoryFile(context: Context): File {
        return File(getAuraDirectory(context), Constants.CHAT_HISTORY_FILE)
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
            e.printStackTrace()
            false
        }
    }
    
    fun isModelReady(context: Context): Boolean {
        val modelFile = File(getModelsDirectory(context), Constants.MODEL_FILENAME)
        val tokenizerFile = File(getTokenizerDirectory(context), Constants.TOKENIZER_FILENAME)
        return modelFile.exists() && tokenizerFile.exists() && modelFile.length() > 0 && tokenizerFile.length() > 0
    }
    
    fun getModelFileSize(context: Context): Long {
        val modelFile = File(getModelsDirectory(context), Constants.MODEL_FILENAME)
        return if (modelFile.exists()) modelFile.length() else 0
    }
    
    fun getTokenizerFileSize(context: Context): Long {
        val tokenizerFile = File(getTokenizerDirectory(context), Constants.TOKENIZER_FILENAME)
        return if (tokenizerFile.exists()) tokenizerFile.length() else 0
    }
    
    fun copyFile(source: File, destination: File): Boolean {
        return try {
            FileInputStream(source).use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun calculateMD5(file: File): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            FileInputStream(file).use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } > 0) {
                    md.update(buffer, 0, read)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
    
    fun getFreeSpace(context: Context): Long {
        return getAuraDirectory(context).freeSpace
    }
    
    fun isSpaceSufficient(context: Context, requiredBytes: Long): Boolean {
        return getFreeSpace(context) > requiredBytes
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
