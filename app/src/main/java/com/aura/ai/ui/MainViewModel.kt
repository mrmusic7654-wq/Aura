package com.aura.ai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.ai.AuraApplication
import com.aura.ai.data.ChatMessage
import com.aura.ai.data.Conversation
import com.aura.ai.model.InferenceEngine
import com.aura.ai.automation.CommandExecutor
import com.aura.ai.automation.AuraAccessibilityService
import com.aura.ai.utils.Constants
import com.aura.ai.utils.FileHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.util.Log

class MainViewModel(
    private val app: AuraApplication
) : ViewModel() {
    
    private val TAG = "MainViewModel"
    private val chatDao = app.database.chatDao()
    private val inferenceEngine = InferenceEngine(app)
    private val commandExecutor = CommandExecutor(
        app, 
        app.deviceController,
        null
    )
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()
    
    private val _isModelReady = MutableStateFlow(false)
    val isModelReady: StateFlow<Boolean> = _isModelReady.asStateFlow()
    
    private val _modelName = MutableStateFlow("")
    val modelName: StateFlow<String> = _modelName.asStateFlow()
    
    private var currentConversationId: Long = 0
    
    init {
        viewModelScope.launch {
            // Initialize model first
            initializeModel()
            // Then create conversation
            createNewConversation()
        }
    }
    
    private suspend fun initializeModel() {
        try {
            Log.d(TAG, "Initializing model...")
            
            // Scan for model files
            val modelFound = app.modelManager.scanForModel()
            
            if (modelFound) {
                _modelName.value = app.modelManager.modelName.value
                
                // Initialize inference engine
                val initialized = inferenceEngine.initialize()
                
                if (initialized) {
                    _isModelReady.value = true
                    Log.d(TAG, "✅ Model initialized: ${_modelName.value}")
                    
                    // Add system message to chat
                    val systemMessage = ChatMessage(
                        conversationId = currentConversationId,
                        content = "✅ Model loaded: ${_modelName.value}. Ready to chat!",
                        isUser = false,
                        isCommand = false
                    )
                    chatDao.insertMessage(systemMessage)
                } else {
                    Log.e(TAG, "❌ Model initialization failed")
                    showErrorMessage("Model initialization failed. Please check your model files.")
                }
            } else {
                Log.e(TAG, "❌ No model found")
                showErrorMessage("No model found. Please place a .onnx file in the models folder.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing model", e)
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
            isUser = false,
            isCommand = false
        )
        chatDao.insertMessage(errorMessage)
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
            
            try {
                // Check if it's a command
                val isCommand = isCommand(content)
                
                val response = if (isCommand) {
                    // Execute device command
                    commandExecutor.executeCommand(content)
                } else {
                    // Check if model is ready for chat
                    if (_isModelReady.value) {
                        // Generate response from model
                        inferenceEngine.generateResponse(content)
                    } else {
                        "Model is not ready yet. Please check your model files."
                    }
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
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing message", e)
                
                // Save error message
                val errorMessage = ChatMessage(
                    conversationId = currentConversationId,
                    content = "❌ Error: ${e.message}",
                    isUser = false,
                    isCommand = false
                )
                chatDao.insertMessage(errorMessage)
            } finally {
                _isTyping.value = false
            }
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
            
            // Re-add system message if model is ready
            if (_isModelReady.value) {
                val systemMessage = ChatMessage(
                    conversationId = currentConversationId,
                    content = "✅ Model loaded: ${_modelName.value}. Ready to chat!",
                    isUser = false,
                    isCommand = false
                )
                chatDao.insertMessage(systemMessage)
            }
        }
    }
    
    fun retryModelLoad() {
        viewModelScope.launch {
            _isModelReady.value = false
            showErrorMessage("🔄 Retrying model load...")
            initializeModel()
        }
    }
    
    fun setAccessibilityService(service: AuraAccessibilityService?) {
        // Update command executor with service instance when needed
    }
    
    override fun onCleared() {
        super.onCleared()
        inferenceEngine.shutdown()
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
