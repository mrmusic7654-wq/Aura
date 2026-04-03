package com.aura.ai.model

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.Aatricks.llmedge.LLMEdge
import com.Aatricks.llmedge.ModelSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class FunctionGemmaEngine(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope
) {
    
    private val TAG = "FunctionGemma"
    private val edge = LLMEdge.create(context, lifecycleScope)
    
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady
    
    suspend fun loadModel(modelFileName: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Loading model: $modelFileName")
            val modelFile = edge.models.prefetch(
                ModelSpec.huggingFace(
                    repoId = "unsloth/functiongemma-270m-it-GGUF",
                    filename = modelFileName
                )
            )
            _isReady.value = true
            Log.d(TAG, "✅ Model loaded at: ${modelFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model", e)
            false
        }
    }
    
    suspend fun executeCommand(userInput: String): String = withContext(Dispatchers.IO) {
        if (!_isReady.value) {
            return@withContext "Model not ready"
        }
        return@withContext try {
            edge.text.generate(prompt = userInput)
        } catch (e: Exception) {
            Log.e(TAG, "Execution failed", e)
            "Error: ${e.message}"
        }
    }
    
    fun shutdown() {
        _isReady.value = false
    }
}
