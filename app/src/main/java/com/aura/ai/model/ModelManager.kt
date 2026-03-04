package com.aura.ai.model

import android.content.Context
import android.util.Log
import com.aura.ai.utils.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class ModelManager(private val context: Context) {
    
    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded
    
    private val _modelPath = MutableStateFlow<String?>(null)
    val modelPath: StateFlow<String?> = _modelPath
    
    private val _tokenizerPath = MutableStateFlow<String?>(null)
    val tokenizerPath: StateFlow<String?> = _tokenizerPath
    
    private val _modelName = MutableStateFlow("")
    val modelName: StateFlow<String> = _modelName
    
    private val _modelSize = MutableStateFlow(0L)
    val modelSize: StateFlow<Long> = _modelSize
    
    suspend fun scanForModel(): Boolean = withContext(Dispatchers.IO) {
        val modelFiles = FileHelper.detectModelFiles(context)
        
        if (modelFiles != null) {
            _modelPath.value = modelFiles.modelFile.absolutePath
            _tokenizerPath.value = modelFiles.tokenizerFile?.absolutePath
            _modelName.value = modelFiles.modelName
            _modelSize.value = modelFiles.modelFile.length()
            _isModelLoaded.value = true
            
            Log.d("ModelManager", "✅ Loaded model: ${modelFiles.modelName}")
            Log.d("ModelManager", "   Size: ${modelFiles.modelFile.length() / (1024*1024)} MB")
            Log.d("ModelManager", "   Tokenizer: ${modelFiles.tokenizerFile?.name ?: "None"}")
            true
        } else {
            _isModelLoaded.value = false
            Log.d("ModelManager", "❌ No model found")
            false
        }
    }
    
    fun getModelInfo(): Map<String, Any> {
        return mapOf(
            "isLoaded" to _isModelLoaded.value,
            "modelName" to _modelName.value,
            "modelPath" to (_modelPath.value ?: "Not found"),
            "modelSize" to _modelSize.value,
            "modelSizeMB" to (_modelSize.value / (1024 * 1024))
        )
    }
}
