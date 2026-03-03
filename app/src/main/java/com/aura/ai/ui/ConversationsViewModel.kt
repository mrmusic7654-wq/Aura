package com.aura.ai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.ai.AuraApplication
import com.aura.ai.data.Conversation
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ConversationsViewModel(
    private val app: AuraApplication
) : ViewModel() {
    
    private val chatDao = app.database.chatDao()
    
    val conversations: StateFlow<List<Conversation>> = chatDao.getAllConversations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch {
            chatDao.deleteConversationMessages(conversationId)
            chatDao.getConversation(conversationId)?.let { conversation ->
                chatDao.deleteConversation(conversation)
            }
        }
    }
}

class ConversationsViewModelFactory(
    private val app: AuraApplication
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConversationsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ConversationsViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
