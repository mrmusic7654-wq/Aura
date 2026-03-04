package com.aura.ai.model

import android.content.Context
import android.util.Log
import com.aura.ai.utils.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.SessionOptions

class ModelFactory(private val context: Context) {
    
    private val TAG = "ModelFactory"
    
    suspend fun createModel(): Pair<OrtEnvironment?, OrtSession?>? = withContext(Dispatchers.IO) {
        return@withContext try {
            val modelFile = FileHelper.getModelFile(context) ?: return@withContext null
            
            Log.d(TAG, "Creating model from: ${modelFile.absolutePath}")
            
            // Create environment
            val env = OrtEnvironment.getEnvironment()
            
            // Create session options
            val options = SessionOptions().apply {
                setIntraOpNumThreads(4)
                setInterOpNumThreads(4)
                setOptimizationLevel(SessionOptions.OptLevel.values()[2])
                
                try {
                    addXnnpack(emptyMap())
                } catch (e: Exception) {
                    // Ignore
                }
                
                try {
                    addNnapi()
                } catch (e: Exception) {
                    // Ignore
                }
            }
            
            // Create session
            val session = env.createSession(modelFile.absolutePath, options)
            
            Log.d(TAG, "✅ Model created successfully")
            Pair(env, session)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create model", e)
            null
        }
    }
    
    suspend fun createTokenizer(): Tokenizer? = withContext(Dispatchers.IO) {
        val tokenizer = Tokenizer(context)
        return@withContext if (tokenizer.initialize()) tokenizer else null
    }
}
