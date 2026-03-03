package com.aura.ai.model

import android.content.Context
import com.aura.ai.utils.FileHelper
import com.aura.ai.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

class ModelManager(private val context: Context) {
    
    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded
    
    private val _modelPath = MutableStateFlow<String?>(null)
    val modelPath: StateFlow<String?> = _modelPath
    
    private val _tokenizerPath = MutableStateFlow<String?>(null)
    val tokenizerPath: StateFlow<String?> = _tokenizerPath
    
    private val _modelSize = MutableStateFlow(0L)
    val modelSize: StateFlow<Long> = _modelSize
    
    suspend fun scanForModel(): Boolean = withContext(Dispatchers.IO) {
        val modelsDir = FileHelper.getModelsDirectory(context)
        val tokenizerDir = FileHelper.getTokenizerDirectory(context)
        
        val modelFile = File(modelsDir, Constants.MODEL_FILENAME)
        val tokenizerFile = File(tokenizerDir, Constants.TOKENIZER_FILENAME)
        
        return@withContext if (modelFile.exists() && tokenizerFile.exists()) {
            _modelPath.value = modelFile.absolutePath
            _tokenizerPath.value = tokenizerFile.absolutePath
            _modelSize.value = modelFile.length()
            _isModelLoaded.value = true
            true
        } else {
            false
        }
    }
    
    suspend fun loadModel(modelFile: File, tokenizerFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            // Validate files
            if (!modelFile.exists() || !tokenizerFile.exists()) {
                return@withContext false
            }
            
            // Copy to Aura directory
            val modelsDir = FileHelper.getModelsDirectory(context)
            val tokenizerDir = FileHelper.getTokenizerDirectory(context)
            
            val destModel = File(modelsDir, Constants.MODEL_FILENAME)
            val destTokenizer = File(tokenizerDir, Constants.TOKENIZER_FILENAME)
            
            modelFile.copyTo(destModel, overwrite = true)
            tokenizerFile.copyTo(destTokenizer, overwrite = true)
            
            _modelPath.value = destModel.absolutePath
            _tokenizerPath.value = destTokenizer.absolutePath
            _modelSize.value = destModel.length()
            _isModelLoaded.value = true
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun getModelInfo(): Map<String, Any> {
        return mapOf(
            "isLoaded" to _isModelLoaded.value,
            "modelPath" to (_modelPath.value ?: "Not found"),
            "modelSize" to _modelSize.value,
            "modelSizeMB" to (_modelSize.value / (1024 * 1024))
        )
    }
}
