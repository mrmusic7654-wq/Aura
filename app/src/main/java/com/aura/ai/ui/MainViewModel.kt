package com.aura.ai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.ai.AuraApplication
import com.aura.ai.data.ChatMessage
import com.aura.ai.data.Conversation
import com.aura.ai.automation.CommandExecutor
import com.aura.ai.utils.Constants
import com.aura.ai.utils.FileHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log

class MainViewModel(
    private val app: AuraApplication
) : ViewModel() {
    
    private val TAG = "MainViewModel"
    private val chatDao = app.database.chatDao()
    private val commandExecutor = CommandExecutor(app, app.deviceController)
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()
    
    private val _isModelReady = MutableStateFlow(false)
    val isModelReady: StateFlow<Boolean> = _isModelReady.asStateFlow()
    
    private var currentConversationId: Long = 0
    
    init {
        viewModelScope.launch {
            initializeModel()
            createNewConversation()
        }
    }
    
    private suspend fun initializeModel() {
        try {
            Log.d(TAG, "Initializing model...")
            
            val modelLoaded = app.modelManager.scanForModel()
            _isModelReady.value = modelLoaded
            
            if (modelLoaded) {
                Log.d(TAG, "✅ Model ready")
                val systemMessage = ChatMessage(
                    conversationId = currentConversationId,
                    content = "✅ Aura AI is ready with DistilGPT2!",
                    isUser = false
                )
                chatDao.insertMessage(systemMessage)
            } else {
                showErrorMessage("Place model.tflite and vocab.json in:\n${FileHelper.getExternalDisplayPath(app)}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
            showErrorMessage("Error: ${e.message}")
        }
    }
    
    private suspend fun createNewConversation() {
        val conversation = Conversation(title = "New Chat")
        currentConversationId = chatDao.insertConversation(conversation)
        
        chatDao.getMessagesForConversation(currentConversationId).collect { msgs ->
            _messages.value = msgs
        }
    }
    
    private suspend fun showErrorMessage(message: String) {
        val errorMessage = ChatMessage(
            conversationId = currentConversationId,
            content = "⚠️ $message",
            isUser = false
        )
        chatDao.insertMessage(errorMessage)
    }
    
    fun sendMessage(content: String) {
        viewModelScope.launch {
            val userMessage = ChatMessage(
                conversationId = currentConversationId,
                content = content,
                isUser = true
            )
            chatDao.insertMessage(userMessage)
            
            _isTyping.value = true
            
            try {
                val response = if (_isModelReady.value) {
                    app.modelManager.generateText(content)
                } else {
                    "Model not ready. Please check your files."
                }
                
                val aiMessage = ChatMessage(
                    conversationId = currentConversationId,
                    content = response,
                    isUser = false
                )
                chatDao.insertMessage(aiMessage)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error", e)
                val errorMessage = ChatMessage(
                    conversationId = currentConversationId,
                    content = "❌ Error: ${e.message}",
                    isUser = false
                )
                chatDao.insertMessage(errorMessage)
            } finally {
                _isTyping.value = false
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        app.modelManager.close()
    }
}

class MainViewModelFactory(private val app: AuraApplication) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
