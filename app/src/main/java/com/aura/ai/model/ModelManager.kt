package com.aura.ai.model

import android.content.Context
import android.util.Log
import com.aura.ai.utils.FileHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ModelManager(private val context: Context) {
    
    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded
    
    private val _modelPath = MutableStateFlow<String?>(null)
    val modelPath: StateFlow<String?> = _modelPath
    
    private val _modelName = MutableStateFlow("")
    val modelName: StateFlow<String> = _modelName
    
    fun scanForModel(): Boolean {
        val modelsDir = FileHelper.getModelsDirectory(context)
        
        Log.d("ModelManager", "Scanning: ${modelsDir.absolutePath}")
        
        if (!modelsDir.exists()) {
            Log.e("ModelManager", "Directory missing")
            return false
        }
        
        val files = modelsDir.listFiles() ?: return false
        
        var modelFile: java.io.File? = null
        var tokenizerFile: java.io.File? = null
        
        files.forEach { file ->
            when {
                file.extension.equals("tflite", ignoreCase = true) -> {
                    modelFile = file
                    Log.d("ModelManager", "Found model: ${file.name}")
                }
                file.extension.equals("json", ignoreCase = true) -> {
                    tokenizerFile = file
                    Log.d("ModelManager", "Found tokenizer: ${file.name}")
                }
            }
        }
        
        return if (modelFile != null && tokenizerFile != null) {
            _modelPath.value = modelFile!!.absolutePath
            _modelName.value = modelFile!!.nameWithoutExtension
            _isModelLoaded.value = true
            Log.d("ModelManager", "✅ Both files found in same folder")
            true
        } else {
            _isModelLoaded.value = false
            Log.e("ModelManager", "❌ Missing files. Need .tflite and .json in /models/")
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
