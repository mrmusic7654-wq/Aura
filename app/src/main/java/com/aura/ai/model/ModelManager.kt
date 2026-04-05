package com.aura.ai.model

import android.content.Context
import android.util.Log
import com.aura.ai.utils.FileHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ModelManager(private val context: Context) {
    
    private val TAG = "ModelManager"
    
    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded
    
    private val _modelPath = MutableStateFlow<String?>(null)
    val modelPath: StateFlow<String?> = _modelPath
    
    private val _modelName = MutableStateFlow("")
    val modelName: StateFlow<String> = _modelName
    
    fun scanForModel(): Boolean {
        val modelsDir = FileHelper.getModelsDirectory(context)
        
        Log.d(TAG, "Scanning for GGUF models in: ${modelsDir.absolutePath}")
        
        if (!modelsDir.exists()) {
            Log.e(TAG, "Models directory missing")
            modelsDir.mkdirs()
            return false
        }
        
        val ggufFiles = modelsDir.listFiles { file -> 
            file.extension.equals("gguf", ignoreCase = true) 
        }
        
        val modelFile = ggufFiles?.firstOrNull()
        
        return if (modelFile != null && modelFile.exists()) {
            _modelPath.value = modelFile.absolutePath
            _modelName.value = modelFile.nameWithoutExtension
            _isModelLoaded.value = true
            Log.d(TAG, "✅ Found GGUF model: ${modelFile.name}")
            true
        } else {
            _isModelLoaded.value = false
            Log.e(TAG, "❌ No GGUF model found")
            false
        }
    }
    
    fun getModelInfo(): Map<String, Any> {
        return mapOf(
            "isLoaded" to _isModelLoaded.value,
            "modelName" to _modelName.value,
            "modelPath" to (_modelPath.value ?: "Not found")
        )
    }
}
