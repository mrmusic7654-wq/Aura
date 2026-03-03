package com.aura.ai.model

import android.content.Context
import android.util.Log
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
        
        Log.d("ModelManager", "Scanning for model in: ${modelsDir.absolutePath}")
        
        val modelFile = File(modelsDir, Constants.MODEL_FILENAME)
        val tokenizerFile = File(tokenizerDir, Constants.TOKENIZER_FILENAME)
        
        return@withContext if (modelFile.exists() && tokenizerFile.exists()) {
            _modelPath.value = modelFile.absolutePath
            _tokenizerPath.value = tokenizerFile.absolutePath
            _modelSize.value = modelFile.length()
            _isModelLoaded.value = true
            Log.d("ModelManager", "Model found! Size: ${modelFile.length() / (1024*1024)} MB")
            true
        } else {
            Log.d("ModelManager", "Model not found")
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
