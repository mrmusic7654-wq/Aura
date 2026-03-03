package com.aura.ai

import android.os.Bundle
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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private val permissionHelper = PermissionHelper(this)
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            showToast("All permissions granted")
        } else {
            showToast("Some permissions were denied")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            FileHelper.createAuraDirectory(this@MainActivity)
        }
        
        permissionHelper.requestPermissions(requestPermissionLauncher)
        
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
}

@Composable
fun AuraApp() {
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf("splash") }
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        val isModelReady = FileHelper.isModelReady(context)
        startDestination = if (isModelReady) "chat" else "model_setup"
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("splash") {
            SplashScreen(
                onTimeout = {
                    // FIXED: Use the outer context, don't create new one
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
                onModelLoaded = {
                    navController.navigate("chat") {
                        popUpTo("model_setup") { inclusive = true }
                    }
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
