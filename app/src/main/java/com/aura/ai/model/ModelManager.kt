package com.aura.ai.model

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.aura.ai.utils.FileHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ModelManager(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope
) {
    
    private val TAG = "ModelManager"
    
    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded
    
    private val _modelName = MutableStateFlow("")
    val modelName: StateFlow<String> = _modelName
    
    private var functionGemmaEngine: FunctionGemmaEngine? = null
    
    fun scanForModel(): Boolean {
        val modelsDir = FileHelper.getModelsDirectory(context)
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
            return false
        }
        
        val ggufFiles = modelsDir.listFiles { file -> 
            file.extension.equals("gguf", ignoreCase = true) 
        }
        
        val modelFile = ggufFiles?.firstOrNull()
        
        return if (modelFile != null) {
            _modelName.value = modelFile.nameWithoutExtension
            Log.d(TAG, "✅ Found: ${modelFile.name}")
            true
        } else {
            false
        }
    }
    
    suspend fun loadModel(modelFileName: String): Boolean {
        functionGemmaEngine = FunctionGemmaEngine(context, lifecycleScope)
        val success = functionGemmaEngine?.loadModel(modelFileName) == true
        _isModelLoaded.value = success
        return success
    }
    
    suspend fun executeCommand(command: String): String {
        return functionGemmaEngine?.executeCommand(command) ?: "Engine not initialized"
    }
    
    fun getModelInfo(): Map<String, Any> {
        return mapOf(
            "isLoaded" to _isModelLoaded.value,
            "modelName" to _modelName.value
        )
    }
    
    fun shutdown() {
        functionGemmaEngine?.shutdown()
        functionGemmaEngine = null
        _isModelLoaded.value = false
    }
}
