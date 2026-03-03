package com.aura.ai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aura.ai.AuraApplication
import com.aura.ai.utils.FileHelper
import com.aura.ai.utils.Constants
import kotlinx.coroutines.launch

@Composable
fun ModelSetupScreen(
    onModelLoaded: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as AuraApplication
    val scope = rememberCoroutineScope()
    
    var isScanning by remember { mutableStateOf(false) }
    var isModelReady by remember { mutableStateOf(FileHelper.isModelReady(context)) }
    var statusMessage by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        // Check if model already exists
        isModelReady = FileHelper.isModelReady(context)
        if (isModelReady) {
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
            "Welcome to Aura AI",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Please place your Qwen model files in:",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = FileHelper.getModelsDirectory(context).absolutePath,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Model file: ${Constants.MODEL_FILENAME}",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Text(
            text = "Tokenizer file: ${Constants.TOKENIZER_FILENAME}",
            style = MaterialTheme.typography.bodyMedium
        )
        
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
                    statusMessage = "Scanning for model files..."
                    
                    val modelReady = FileHelper.isModelReady(context)
                    if (modelReady) {
                        statusMessage = "Model found! Loading..."
                        onModelLoaded()
                    } else {
                        statusMessage = "Model files not found. Please place them in the directory above."
                    }
                    
                    isScanning = false
                }
            },
            enabled = !isScanning
        ) {
            Text(if (isScanning) "Scanning..." else "Scan for Model")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Show continue button if model is ready
        if (isModelReady) {
            Button(
                onClick = onModelLoaded,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Continue to Chat")
            }
        }
    }
}
