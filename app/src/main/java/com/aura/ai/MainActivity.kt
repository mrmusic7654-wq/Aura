package com.aura.ai

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aura.ai.ui.*
import com.aura.ai.utils.FileHelper
import com.aura.ai.utils.PermissionHelper
import com.aura.ai.utils.showToast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private val permissionHelper = PermissionHelper(this)
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            showToast("All permissions granted")
            checkStoragePermission()
        } else {
            showToast("Some permissions were denied")
        }
    }
    
    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        lifecycleScope.launch {
            delay(1000)
            checkModelAndProceed()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            FileHelper.createAuraDirectory(this@MainActivity)
            checkPermissions()
        }
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuraApp()
                }
            }
        }
    }
    
    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                requestManageStorage()
            } else {
                permissionHelper.requestPermissions(requestPermissionLauncher)
            }
        } else {
            permissionHelper.requestPermissions(requestPermissionLauncher)
        }
    }
    
    private fun requestManageStorage() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        intent.data = Uri.parse("package:$packageName")
        manageStorageLauncher.launch(intent)
    }
    
    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                requestManageStorage()
            }
        }
    }
    
    private suspend fun checkModelAndProceed() {
        val isReady = FileHelper.isModelReady(this@MainActivity)
        if (isReady) {
            showToast("✅ Model loaded successfully!")
        } else {
            showToast("⚠️ Models not found. Please download or add model files.")
        }
    }
}

@Composable
fun AuraApp() {
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf("splash") }
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        delay(2000)
        val isModelReady = FileHelper.isModelReady(context)
        startDestination = when {
            isModelReady -> "chat"
            else -> "model_setup"
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("splash") {
            SplashScreen(
                onTimeout = {
                    if (FileHelper.isModelReady(context)) {
                        navController.navigate("chat") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        navController.navigate("model_setup") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            )
        }
        
        composable("model_setup") {
            ModelSetupScreen(
                onScanPressed = {
                    navController.navigate("chat") {
                        popUpTo("model_setup") { inclusive = true }
                    }
                },
                onDownloadPressed = {
                    navController.navigate("download")
                }
            )
        }
        
        composable("download") {
            DownloadScreen(
                onDownloadComplete = {
                    navController.navigate("chat") {
                        popUpTo("download") { inclusive = true }
                    }
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("chat") {
            ChatScreen(
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToConversations = {
                    navController.navigate("conversations")
                }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                onBackPressed = {
                    navController.popBackStack()
                },
                onNavigateToModelSetup = {
                    navController.navigate("model_setup") {
                        popUpTo("chat") { inclusive = false }
                    }
                }
            )
        }
        
        composable("conversations") {
            ConversationsScreen(
                onConversationSelected = { conversationId ->
                    navController.popBackStack()
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
    }
}
