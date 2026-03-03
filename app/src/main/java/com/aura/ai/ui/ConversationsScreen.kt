package com.aura.ai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.ai.AuraApplication
import com.aura.ai.data.Conversation
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    onConversationSelected: (Long) -> Unit,
    onBackPressed: () -> Unit,
    viewModel: ConversationsViewModel = viewModel(
        factory = ConversationsViewModelFactory(
            LocalContext.current.applicationContext as AuraApplication
        )
    )
) {
    val conversations by viewModel.conversations.collectAsState()
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conversations") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No conversations yet", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(conversations) { conversation ->
                    ConversationItem(
                        conversation = conversation,
                        onItemClick = { onConversationSelected(conversation.id) },
                        onDeleteClick = { viewModel.deleteConversation(conversation.id) },
                        dateFormat = dateFormat
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit,
    dateFormat: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onItemClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = conversation.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${conversation.messageCount} messages • ${dateFormat.format(Date(conversation.lastMessageTime))}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}
