package com.aura.ai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.ai.AuraApplication
import com.aura.ai.data.ChatMessage
import com.aura.ai.data.Conversation
import com.aura.ai.utils.FileHelper
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log

class MainViewModel(
    private val app: AuraApplication
) : ViewModel() {
    
    private val TAG = "MainViewModel"
    private val chatDao = app.database.chatDao()
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()
    
    private val _isModelReady = MutableStateFlow(false)
    val isModelReady: StateFlow<Boolean> = _isModelReady.asStateFlow()
    
    private val _modelName = MutableStateFlow("")
    val modelName: StateFlow<String> = _modelName.asStateFlow()
    
    private var currentConversationId: Long = 0
    private var llmInference: LlmInference? = null
    
    init {
        viewModelScope.launch {
            createNewConversation()
            initializeModel()
        }
    }
    
    private suspend fun createNewConversation() {
        val conversation = Conversation(title = "New Chat")
        currentConversationId = chatDao.insertConversation(conversation)
        
        chatDao.getMessagesForConversation(currentConversationId).collect { msgs ->
            _messages.value = msgs
        }
    }
    
    private suspend fun initializeModel() {
        try {
            val modelFound = app.modelManager.scanForModel()
            
            if (modelFound) {
                _modelName.value = app.modelManager.modelName.value
                val modelPath = app.modelManager.modelPath.value
                
                if (modelPath != null) {
                    // Initialize MediaPipe inference
                    llmInference = LlmInference.createFromOptions(
                        app,
                        LlmInference.LlmInferenceOptions.builder()
                            .setModelPath(modelPath)
                            .setMaxTokens(512)
                            .setTemperature(0.7f)
                            .build()
                    )
                    _isModelReady.value = true
                    Log.d(TAG, "✅ MediaPipe LLM ready: ${_modelName.value}")
                    
                    val systemMessage = ChatMessage(
                        conversationId = currentConversationId,
                        content = "✅ AI is ready with ${_modelName.value}!",
                        isUser = false
                    )
                    chatDao.insertMessage(systemMessage)
                }
            } else {
                showErrorMessage("Place a .gguf file in:\n${FileHelper.getExternalDisplayPath(app)}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing model", e)
            showErrorMessage("Error: ${e.message}")
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
                val response = if (_isModelReady.value && llmInference != null) {
                    // ACTUAL AI GENERATION - using generateResponse
                    llmInference?.generateResponse(content) ?: "No response"
                } else {
                    "Model not ready. Place a .gguf file in:\n${FileHelper.getExternalDisplayPath(app)}"
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
    
    fun clearConversation() {
        viewModelScope.launch {
            chatDao.deleteConversationMessages(currentConversationId)
            createNewConversation()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        llmInference?.close()
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
