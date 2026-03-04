package com.aura.ai.model

import android.content.Context
import android.util.Log
import com.aura.ai.utils.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ModelDownloader(private val context: Context) {
    
    private val TAG = "ModelDownloader"
    
    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress
    
    private val _downloadStatus = MutableStateFlow("")
    val downloadStatus: StateFlow<String> = _downloadStatus
    
    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading
    
    // Default model URLs - Change these to your actual model URLs
    private val MODEL_URL = "https://huggingface.co/onnx-community/MobileLLM-600M/resolve/main/model_q4f16.onnx"
    private val TOKENIZER_URL = "https://huggingface.co/onnx-community/MobileLLM-600M/resolve/main/tokenizer.json"
    
    suspend fun downloadDefaultModel(): Boolean = withContext(Dispatchers.IO) {
        _isDownloading.value = true
        _downloadStatus.value = "Starting download..."
        
        try {
            // Create directories if needed
            FileHelper.createAuraDirectory(context)
            
            val modelsDir = FileHelper.getModelsDirectory(context)
            val tokenizerDir = FileHelper.getTokenizerDirectory(context)
            
            val modelFile = File(modelsDir, "model_q4f16.onnx")
            val tokenizerFile = File(tokenizerDir, "tokenizer.json")
            
            // Download model
            _downloadStatus.value = "Downloading model (414 MB)..."
            val modelSuccess = downloadFile(MODEL_URL, modelFile)
            
            if (!modelSuccess) {
                _downloadStatus.value = "Failed to download model"
                _isDownloading.value = false
                return@withContext false
            }
            
            // Download tokenizer
            _downloadStatus.value = "Downloading tokenizer..."
            val tokenizerSuccess = downloadFile(TOKENIZER_URL, tokenizerFile)
            
            if (!tokenizerSuccess) {
                _downloadStatus.value = "Failed to download tokenizer"
                _isDownloading.value = false
                return@withContext false
            }
            
            _downloadStatus.value = "Download complete!"
            _isDownloading.value = false
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            _downloadStatus.value = "Error: ${e.message}"
            _isDownloading.value = false
            false
        }
    }
    
    private suspend fun downloadFile(urlString: String, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()
            
            val fileLength = connection.contentLength
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(outputFile)
            
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                
                if (fileLength > 0) {
                    val progress = (totalBytesRead * 100 / fileLength)
                    _downloadProgress.value = progress
                }
            }
            
            outputStream.close()
            inputStream.close()
            connection.disconnect()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
            false
        }
    }
    
    fun cancelDownload() {
        _isDownloading.value = false
    }
}
