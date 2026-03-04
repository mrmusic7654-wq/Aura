package com.aura.ai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aura.ai.model.ModelDownloader
import kotlinx.coroutines.launch

@Composable
fun DownloadScreen(
    onDownloadComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloader = remember { ModelDownloader(context) }
    
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }
    var downloadStatus by remember { mutableStateOf("") }
    
    // Collect states from downloader
    LaunchedEffect(downloader) {
        downloader.isDownloading.collect { isDownloading = it }
        downloader.downloadProgress.collect { downloadProgress = it }
        downloader.downloadStatus.collect { downloadStatus = it }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Download AI Model",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Aura AI needs to download the MobileLLM model to work.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "✅ FIXED: 125M Model (88 MB)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Model: MobileLLM-125M Q4F16",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Size: 88 MB",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Download once, use forever offline",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (isDownloading) {
            Text(
                downloadStatus,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = downloadProgress / 100f,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "$downloadProgress%",
                style = MaterialTheme.typography.bodySmall
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = { downloader.cancelDownload() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Cancel Download")
            }
        } else {
            Button(
                onClick = {
                    scope.launch {
                        val success = downloader.downloadDefaultModel()
                        if (success) {
                            onDownloadComplete()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Download (88 MB)")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Smaller model = faster download and better performance!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
