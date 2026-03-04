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
    
    // ✅ TFLite MODEL URLs (pre-converted)
    private val MODEL_URL = "https://huggingface.co/Xenova/mobilellm-125m-quantized/resolve/main/model_quantized.tflite"
    private val TOKENIZER_URL = "https://huggingface.co/Xenova/mobilellm-125m-quantized/raw/main/tokenizer.json"
    
    // Model file names
    private val MODEL_FILENAME = "model.tflite"
    private val TOKENIZER_FILENAME = "tokenizer.json"
    
    suspend fun downloadDefaultModel(): Boolean = withContext(Dispatchers.IO) {
        _isDownloading.value = true
        _downloadStatus.value = "Starting download..."
        _downloadProgress.value = 0
        
        try {
            FileHelper.createAuraDirectory(context)
            
            val modelsDir = FileHelper.getModelsDirectory(context)
            val tokenizerDir = FileHelper.getTokenizerDirectory(context)
            
            val modelFile = File(modelsDir, MODEL_FILENAME)
            val tokenizerFile = File(tokenizerDir, TOKENIZER_FILENAME)
            
            // Download model
            _downloadStatus.value = "Downloading model (125 MB)..."
            val modelSuccess = downloadFile(MODEL_URL, modelFile)
            if (!modelSuccess) throw Exception("Model download failed")
            
            // Download tokenizer
            _downloadStatus.value = "Downloading tokenizer..."
            val tokenizerSuccess = downloadFile(TOKENIZER_URL, tokenizerFile)
            if (!tokenizerSuccess) throw Exception("Tokenizer download failed")
            
            _downloadStatus.value = "✅ Download complete! Model ready."
            _isDownloading.value = false
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            _downloadStatus.value = "❌ Error: ${e.message}"
            _isDownloading.value = false
            false
        }
    }
    
    private suspend fun downloadFile(urlString: String, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Downloading from: $urlString")
            
            if (outputFile.exists()) outputFile.delete()
            
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.connect()
            
            if (connection.responseCode != 200) {
                Log.e(TAG, "HTTP Error: ${connection.responseCode}")
                return@withContext false
            }
            
            val fileLength = connection.contentLength
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(outputFile)
            
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                
                if (fileLength > 0) {
                    val progress = ((totalBytesRead * 100) / fileLength).toInt()
                    _downloadProgress.value = progress
                }
            }
            
            outputStream.close()
            inputStream.close()
            connection.disconnect()
            
            Log.d(TAG, "✅ Downloaded: ${outputFile.name} (${outputFile.length()} bytes)")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
            false
        }
    }
    
    fun cancelDownload() {
        _isDownloading.value = false
        _downloadStatus.value = "Download cancelled"
    }
}
