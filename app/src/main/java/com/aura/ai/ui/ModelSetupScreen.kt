package com.aura.ai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aura.ai.utils.FileHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ModelSetupScreen(
    onModelLoaded: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var isScanning by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    val modelPath = remember { FileHelper.getExternalDisplayPath(context) }
    
    LaunchedEffect(Unit) {
        if (FileHelper.isModelReady(context)) {
            onModelLoaded()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Aura AI Setup",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Place your GGUF model file in:",
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = modelPath,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "Required:",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Any .gguf model file")
                Text("• No tokenizer needed - GGUF includes it")
                Text("• FunctionGemma 270M recommended")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
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
                    statusMessage = "Scanning for GGUF files..."
                    delay(1000)
                    
                    if (FileHelper.isModelReady(context)) {
                        statusMessage = "✅ GGUF model found! Loading..."
                        delay(500)
                        onModelLoaded()
                    } else {
                        statusMessage = "❌ No .gguf file found. Place a GGUF model in the folder."
                    }
                    
                    isScanning = false
                }
            },
            enabled = !isScanning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isScanning) "Scanning..." else "Scan for GGUF Models")
        }
    }
}
