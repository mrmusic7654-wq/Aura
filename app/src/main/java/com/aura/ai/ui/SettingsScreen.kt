package com.aura.ai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Folder  // Correct import for the folder icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aura.ai.AuraApplication
import com.aura.ai.utils.FileHelper
import kotlinx.coroutines.launch
import android.content.Intent
import android.provider.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackPressed: () -> Unit,
    onNavigateToModelSetup: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as AuraApplication
    val scope = rememberCoroutineScope()
    
    var modelInfo by remember { mutableStateOf(app.modelManager.getModelInfo()) }
    
    LaunchedEffect(Unit) {
        modelInfo = app.modelManager.getModelInfo()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Model Information", style = MaterialTheme.typography.titleLarge)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("Model: ${app.modelManager.modelName.value}")
                        Text("Status: ${if (modelInfo["isLoaded"] == true) "✅ Loaded" else "❌ Not Loaded"}")
                        Text("Path: ${modelInfo["modelPath"]}")
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = onNavigateToModelSetup,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Model Setup")
                        }
                    }
                }
            }
            
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Use the correctly imported Folder icon
                            Icon(Icons.Default.Folder, contentDescription = "Storage Icon")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Storage", style = MaterialTheme.typography.titleLarge)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("Models Directory: ${FileHelper.getModelsDirectory(context).absolutePath}")
                    }
                }
            }
            
            item {
                Button(
                    onClick = {
                        scope.launch {
                            // Clear all conversations - implement if needed
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All Chat History")
                }
            }
        }
    }
}
