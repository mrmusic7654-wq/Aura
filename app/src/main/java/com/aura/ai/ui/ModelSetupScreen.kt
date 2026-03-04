package com.aura.ai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aura.ai.utils.FileHelper
import kotlinx.coroutines.launch

@Composable
fun ModelSetupScreen(
    onScanPressed: () -> Unit,
    onDownloadPressed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var isScanning by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    val externalPath = remember { FileHelper.getExternalDisplayPath(context) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Welcome to Aura AI",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Choose how to get your AI model",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Option 1: Download automatically
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
                    "Option 1: Download Automatically",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "• Model: MobileLLM-600M (414 MB)",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "• Works immediately after download",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "• Requires Wi-Fi connection",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onDownloadPressed,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Download Model (414 MB)")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Option 2: Manual placement
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "Option 2: Add Manually",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Place your model files in:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = externalPath,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "• Any .onnx file in /models/ folder",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "• tokenizer.json in /tokenizer/ folder",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (statusMessage.isNotEmpty()) {
                    Text(
                        text = statusMessage,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                
                Button(
                    onClick = {
                        scope.launch {
                            isScanning = true
                            statusMessage = "Scanning for model files..."
                            
                            val modelReady = FileHelper.isModelReady(context)
                            
                            if (modelReady) {
                                statusMessage = "✅ Model found!"
                                onScanPressed()
                            } else {
                                statusMessage = "❌ No model found. Try downloading or check files."
                            }
                            
                            isScanning = false
                        }
                    },
                    enabled = !isScanning,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isScanning) "Scanning..." else "Scan for Existing Models")
                }
            }
        }
    }
}
