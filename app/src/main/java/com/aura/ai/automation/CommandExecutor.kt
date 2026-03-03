package com.aura.ai.automation

import android.content.Context
import com.aura.ai.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CommandExecutor(
    private val context: Context,
    private val deviceController: DeviceController,
    private val accessibilityService: AuraAccessibilityService?
) {
    
    private val _lastCommandResult = MutableStateFlow("")
    val lastCommandResult: StateFlow<String> = _lastCommandResult
    
    private val _commandHistory = MutableStateFlow<List<ExecutedCommand>>(emptyList())
    val commandHistory: StateFlow<List<ExecutedCommand>> = _commandHistory
    
    data class ExecutedCommand(
        val command: String,
        val result: String,
        val timestamp: Long,
        val success: Boolean
    )
    
    suspend fun executeCommand(command: String): String {
        val result = when {
            isOpenAppCommand(command) -> executeOpenApp(command)
            isCloseAppCommand(command) -> executeCloseApp(command)
            isScrollCommand(command) -> executeScroll(command)
            isSearchCommand(command) -> executeSearch(command)
            isClickCommand(command) -> executeClick(command)
            isBackCommand(command) -> executeBack()
            isHomeCommand(command) -> executeHome()
            else -> null
        }
        
        val finalResult = result ?: "I don't know how to execute that command yet."
        
        // Add to history
        _commandHistory.value = _commandHistory.value + ExecutedCommand(
            command = command,
            result = finalResult,
            timestamp = System.currentTimeMillis(),
            success = result != null
        )
        
        _lastCommandResult.value = finalResult
        return finalResult
    }
    
    private fun isOpenAppCommand(command: String): Boolean {
        return Constants.APP_OPEN_COMMANDS.any { command.startsWith(it, ignoreCase = true) }
    }
    
    private fun isCloseAppCommand(command: String): Boolean {
        return Constants.APP_CLOSE_COMMANDS.any { command.startsWith(it, ignoreCase = true) }
    }
    
    private fun isScrollCommand(command: String): Boolean {
        return Constants.SCROLL_COMMANDS.any { command.startsWith(it, ignoreCase = true) }
    }
    
    private fun isSearchCommand(command: String): Boolean {
        return Constants.SEARCH_COMMANDS.any { command.startsWith(it, ignoreCase = true) }
    }
    
    private fun isClickCommand(command: String): Boolean {
        return Constants.CLICK_COMMANDS.any { command.startsWith(it, ignoreCase = true) }
    }
    
    private fun isBackCommand(command: String): Boolean {
        return Constants.BACK_COMMANDS.any { command.contains(it, ignoreCase = true) }
    }
    
    private fun isHomeCommand(command: String): Boolean {
        return Constants.HOME_COMMANDS.any { command.contains(it, ignoreCase = true) }
    }
    
    private fun executeOpenApp(command: String): String {
        // Extract app name from command
        val commandWords = command.split(" ")
        val openCommandIndex = commandWords.indexOfFirst { it in Constants.APP_OPEN_COMMANDS }
        
        if (openCommandIndex != -1 && openCommandIndex < commandWords.size - 1) {
            val appName = commandWords.subList(openCommandIndex + 1, commandWords.size)
                .joinToString(" ")
                .trim()
            
            if (appName.isNotEmpty()) {
                val success = deviceController.openAppByName(appName)
                return if (success) {
                    "Opening $appName"
                } else {
                    "Could not find app: $appName. Try using the exact app name."
                }
            }
        }
        
        // Try extracting from the end
        val appName = command.replace(Regex("(?i)^(open|launch|start|run)\\s+"), "").trim()
        if (appName.isNotEmpty()) {
            val success = deviceController.openAppByName(appName)
            return if (success) {
                "Opening $appName"
            } else {
                "Could not find app: $appName"
            }
        }
        
        return "Please specify which app to open. For example: 'open Chrome'"
    }
    
    private fun executeCloseApp(command: String): String {
        return "Close app feature requires accessibility service. Please enable it in settings."
    }
    
    private fun executeScroll(command: String): String {
        val direction = when {
            command.contains("up", ignoreCase = true) -> "up"
            command.contains("down", ignoreCase = true) -> "down"
            command.contains("left", ignoreCase = true) -> "left"
            command.contains("right", ignoreCase = true) -> "right"
            else -> null
        }
        
        return if (direction != null && accessibilityService != null) {
            accessibilityService.performScroll(direction)
            "Scrolling $direction"
        } else if (direction == null) {
            "Please specify scroll direction (up/down/left/right)"
        } else {
            "Please enable accessibility service in settings to use scroll feature"
        }
    }
    
    private fun executeSearch(command: String): String {
        val searchTerm = command.replace(Regex("(?i)(search|find|lookup|google)\\s+for?\\s*"), "").trim()
        
        if (searchTerm.isNotEmpty()) {
            deviceController.searchQuery(searchTerm)
            return "Searching for: $searchTerm"
        }
        
        return "What would you like me to search for?"
    }
    
    private fun executeClick(command: String): String {
        // Extract what to click
        val clickTarget = command.replace(Regex("(?i)(click|tap|press|select)\\s+"), "").trim()
        
        return if (clickTarget.isNotEmpty() && accessibilityService != null) {
            // This would need UI element detection
            "Clicking on '$clickTarget' is not yet implemented. This feature requires screen analysis."
        } else {
            "Please enable accessibility service to use click features"
        }
    }
    
    private fun executeBack(): String {
        return if (accessibilityService != null) {
            accessibilityService.performBack()
            "Going back"
        } else {
            "Please enable accessibility service to use back navigation"
        }
    }
    
    private fun executeHome(): String {
        deviceController.goHome()
        return "Going home"
    }
}
