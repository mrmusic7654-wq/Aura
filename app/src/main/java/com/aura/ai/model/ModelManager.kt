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
    
    private var gpt2Engine: GPT2Engine? = null
    
    fun scanForModel(): Boolean {
        val modelsDir = FileHelper.getModelsDirectory(context)
        
        Log.d("ModelManager", "Scanning: ${modelsDir.absolutePath}")
        
        if (!modelsDir.exists()) {
            Log.e("ModelManager", "Directory missing")
            return false
        }
        
        val files = modelsDir.listFiles() ?: return false
        
        var modelFile: java.io.File? = null
        var vocabFile: java.io.File? = null
        var mergesFile: java.io.File? = null
        
        files.forEach { file ->
            when {
                file.name.endsWith(".tflite") -> {
                    modelFile = file
                    Log.d("ModelManager", "Found model: ${file.name}")
                }
                file.name.equals("vocab.json", ignoreCase = true) -> {
                    vocabFile = file
                    Log.d("ModelManager", "Found vocab: ${file.name}")
                }
                file.name.equals("merges.txt", ignoreCase = true) -> {
                    mergesFile = file
                    Log.d("ModelManager", "Found merges: ${file.name}")
                }
            }
        }
        
        return if (modelFile != null && vocabFile != null) {
            _modelPath.value = modelFile!!.absolutePath
            _modelName.value = modelFile!!.nameWithoutExtension
            
            // Initialize GPT2 engine
            gpt2Engine = GPT2Engine(context)
            val initialized = gpt2Engine!!.initialize(modelFile!!, vocabFile!!, mergesFile)
            
            _isModelLoaded.value = initialized
            Log.d("ModelManager", "✅ Model loaded: $initialized")
            initialized
        } else {
            _isModelLoaded.value = false
            Log.e("ModelManager", "❌ Missing files. Need .tflite and vocab.json")
            false
        }
    }
    
    fun generateText(prompt: String): String {
        return gpt2Engine?.generate(prompt) ?: "Model not initialized"
    }
    
    fun getModelInfo(): Map<String, Any> {
        return mapOf(
            "isLoaded" to _isModelLoaded.value,
            "modelName" to _modelName.value,
            "modelPath" to (_modelPath.value ?: "Not found")
        )
    }
    
    fun close() {
        gpt2Engine?.close()
    }
}
