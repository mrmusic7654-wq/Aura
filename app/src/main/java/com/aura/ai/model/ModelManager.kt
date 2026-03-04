package com.aura.ai.model

import android.content.Context
import android.util.Log
import com.aura.ai.utils.FileHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class ModelManager(private val context: Context) {
    
    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded
    
    private val _modelPath = MutableStateFlow<String?>(null)
    val modelPath: StateFlow<String?> = _modelPath
    
    private val _modelName = MutableStateFlow("")
    val modelName: StateFlow<String> = _modelName
    
    fun scanForModel(): Boolean {
        val modelsDir = FileHelper.getModelsDirectory(context)
        
        Log.d("ModelManager", "Scanning for models in: ${modelsDir.absolutePath}")
        
        if (!modelsDir.exists()) {
            Log.d("ModelManager", "Models directory doesn't exist")
            return false
        }
        
        // Look for .tflite files
        val tfliteFiles = modelsDir.listFiles { file -> 
            file.extension.equals("tflite", ignoreCase = true) 
        }
        
        val modelFile = tfliteFiles?.firstOrNull()
        
        return if (modelFile != null && modelFile.exists()) {
            _modelPath.value = modelFile.absolutePath
            _modelName.value = modelFile.nameWithoutExtension
            _isModelLoaded.value = true
            Log.d("ModelManager", "✅ Found TFLite model: ${modelFile.name}")
            true
        } else {
            _isModelLoaded.value = false
            Log.d("ModelManager", "❌ No TFLite model found")
            false
        }
    }
    
    fun getModelInfo(): Map<String, Any> {
        return mapOf(
            "isLoaded" to _isModelLoaded.value,
            "modelName" to _modelName.value,
            "modelPath" to (_modelPath.value ?: "Not found"),
            "modelSizeMB" to (File(_modelPath.value ?: "").length() / (1024 * 1024))
        )
    }
}
