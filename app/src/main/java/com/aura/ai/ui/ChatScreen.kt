package com.aura.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.ai.AuraApplication
import com.aura.ai.data.ChatMessage
import com.aura.ai.ui.theme.PremiumError
import com.aura.ai.ui.theme.PremiumGradient1
import com.aura.ai.ui.theme.PremiumGradient2
import com.aura.ai.ui.theme.PremiumGradient3
import com.aura.ai.ui.theme.PremiumPrimary
import com.aura.ai.ui.theme.PremiumSuccess

@Composable
fun ChatScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToConversations: () -> Unit,
    viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(LocalContext.current.applicationContext as AuraApplication)
    )
) {
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val showDeleteConfirmation by viewModel.showDeleteConfirmation.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // Scroll to bottom when messages change
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.confirmDelete(false) },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = PremiumError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm Delete", fontWeight = FontWeight.Bold)
                }
            },
            text = { Text("Are you sure you want to delete these files?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDelete(true) },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = PremiumError
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.confirmDelete(false) }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PremiumPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Aura AI", style = MaterialTheme.typography.headlineSmall)
            }
            
            Row {
                IconButton(onClick = onNavigateToConversations) {
                    Icon(Icons.Default.History, contentDescription = "History")
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Messages List
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message)
            }
            
            if (isTyping) {
                item {
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp)
                    ) {
                        Text(
                            "Aura is typing...",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Input Field
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Add, contentDescription = "Attach")
                }
                
                Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (inputText.isEmpty()) {
                                Text(
                                    "Type a message...",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    )
                }
                
                if (inputText.isNotBlank()) {
                    Button(
                        onClick = {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.isUser
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .shadow(4.dp),
            color = if (isUser) PremiumPrimary else MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                )
                
                if (message.isCommand && message.commandExecuted) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (isUser) Color.White.copy(alpha = 0.7f) else PremiumSuccess
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Executed",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUser) Color.White.copy(alpha = 0.7f) else PremiumSuccess
                        )
                    }
                }
            }
        }
    }
}
