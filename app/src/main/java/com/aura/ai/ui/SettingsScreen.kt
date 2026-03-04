package com.aura.ai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aura.ai.AuraApplication
import com.aura.ai.automation.AuraAccessibilityService
import com.aura.ai.utils.FileHelper
import kotlinx.coroutines.launch
import android.content.Context
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
    
    var isAccessibilityEnabled by remember { 
        mutableStateOf(checkAccessibilityServiceEnabled(context)) 
    }
    
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
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Model Information",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            "Model: ${app.modelManager.modelName.value}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Status: ${if (modelInfo["isLoaded"] == true) "✅ Loaded" else "❌ Not Loaded"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Size: ${modelInfo["modelSizeMB"]} MB",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Path: ${modelInfo["modelPath"]}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = onNavigateToModelSetup,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Change Model")
                        }
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Storage, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Storage",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // ⬇️ THIS LINE IS NOW CORRECT ⬇️
                        // The function getFreeSpace exists in the FileHelper above
                        val freeSpaceBytes = FileHelper.getFreeSpace(context)
                        val freeSpaceMB = freeSpaceBytes / (1024 * 1024)
                        
                        Text(
                            "Free Space: $freeSpaceMB MB",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Text(
                            "Models Directory: ${FileHelper.getModelsDirectory(context).absolutePath}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Automation Settings",
                            style = MaterialTheme.typography.titleLarge
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Accessibility Service")
                            Switch(
                                checked = isAccessibilityEnabled,
                                onCheckedChange = {
                                    if (!isAccessibilityEnabled) {
                                        context.startActivity(
                                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                        )
                                    }
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            "Enable accessibility service to allow Aura to control your device",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Danger Zone",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    // Clear all conversations
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
    }
}

private fun checkAccessibilityServiceEnabled(context: Context): Boolean {
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
    return enabledServices?.contains(context.packageName + "/" + AuraAccessibilityService::class.java.name) == true
}
