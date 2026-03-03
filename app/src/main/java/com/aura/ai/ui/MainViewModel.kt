package com.aura.ai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.ai.AuraApplication
import com.aura.ai.data.ChatMessage
import com.aura.ai.data.Conversation
import com.aura.ai.model.InferenceEngine
import com.aura.ai.automation.CommandExecutor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val app: AuraApplication
) : ViewModel() {
    
    private val chatDao = app.database.chatDao()
    private val inferenceEngine = InferenceEngine(app)
    private val commandExecutor = CommandExecutor(
        app, 
        app.deviceController,
        null // Will be set when service connects
    )
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()
    
    private var currentConversationId: Long = 0
    
    init {
        viewModelScope.launch {
            createNewConversation()
        }
    }
    
    private suspend fun createNewConversation() {
        val conversation = Conversation(title = "New Chat")
        currentConversationId = chatDao.insertConversation(conversation)
        
        chatDao.getMessagesForConversation(currentConversationId).collect { msgs ->
            _messages.value = msgs
        }
    }
    
    fun sendMessage(content: String) {
        viewModelScope.launch {
            // Save user message
            val userMessage = ChatMessage(
                conversationId = currentConversationId,
                content = content,
                isUser = true
            )
            chatDao.insertMessage(userMessage)
            
            // Show typing indicator
            _isTyping.value = true
            
            // Check if it's a command
            val isCommand = isCommand(content)
            
            // Generate response
            val response = if (isCommand) {
                commandExecutor.executeCommand(content)
            } else {
                inferenceEngine.generateResponse(content)
            }
            
            // Save AI response
            val aiMessage = ChatMessage(
                conversationId = currentConversationId,
                content = response,
                isUser = false,
                isCommand = isCommand,
                commandExecuted = isCommand
            )
            chatDao.insertMessage(aiMessage)
            
            // Update conversation title if first message
            if (_messages.value.size <= 2) {
                val newTitle = content.take(30) + if (content.length > 30) "..." else ""
                chatDao.getConversation(currentConversationId)?.let { conv ->
                    chatDao.updateConversation(conv.copy(title = newTitle))
                }
            }
            
            _isTyping.value = false
        }
    }
    
    private fun isCommand(content: String): Boolean {
        return Constants.APP_OPEN_COMMANDS.any { content.startsWith(it, ignoreCase = true) } ||
               Constants.SCROLL_COMMANDS.any { content.startsWith(it, ignoreCase = true) } ||
               Constants.SEARCH_COMMANDS.any { content.startsWith(it, ignoreCase = true) } ||
               Constants.CLICK_COMMANDS.any { content.startsWith(it, ignoreCase = true) } ||
               Constants.BACK_COMMANDS.any { content.contains(it, ignoreCase = true) } ||
               Constants.HOME_COMMANDS.any { content.contains(it, ignoreCase = true) }
    }
    
    fun clearConversation() {
        viewModelScope.launch {
            chatDao.deleteConversationMessages(currentConversationId)
            createNewConversation()
        }
    }
    
    fun setAccessibilityService(service: AuraAccessibilityService?) {
        // Update command executor with service instance
    }
}
