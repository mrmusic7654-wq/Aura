package com.aura.ai.automation

import android.content.Context
import com.aura.ai.utils.Constants

class CommandExecutor(
    private val context: Context,
    private val deviceController: DeviceController
) {
    
    suspend fun executeCommand(command: String): String {
        return when {
            isOpenAppCommand(command) -> executeOpenApp(command)
            isSearchCommand(command) -> executeSearch(command)
            isHomeCommand(command) -> executeHome()
            else -> "I don't know how to execute that command."
        }
    }
    
    private fun isOpenAppCommand(command: String): Boolean {
        return Constants.APP_OPEN_COMMANDS.any { command.startsWith(it, ignoreCase = true) }
    }
    
    private fun isSearchCommand(command: String): Boolean {
        return Constants.SEARCH_COMMANDS.any { command.startsWith(it, ignoreCase = true) }
    }
    
    private fun isHomeCommand(command: String): Boolean {
        return Constants.HOME_COMMANDS.any { command.contains(it, ignoreCase = true) }
    }
    
    private fun executeOpenApp(command: String): String {
        val appName = command.replace(Regex("(?i)^(open|launch|start|run)\\s+"), "").trim()
        return if (deviceController.openAppByName(appName)) {
            "✅ Opening $appName"
        } else {
            "❌ Could not find app: $appName"
        }
    }
    
    private fun executeSearch(command: String): String {
        val query = command.replace(Regex("(?i)(search|find|lookup|google)\\s+for?\\s*"), "").trim()
        deviceController.searchQuery(query)
        return "🔍 Searching for: $query"
    }
    
    private fun executeHome(): String {
        deviceController.goHome()
        return "🏠 Going home"
    }
}
