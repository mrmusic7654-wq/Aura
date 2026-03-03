package com.aura.ai

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.aura.ai.data.ChatDatabase
import com.aura.ai.model.ModelManager
import com.aura.ai.automation.DeviceController

class AuraApplication : Application() {
    
    lateinit var database: ChatDatabase
    lateinit var modelManager: ModelManager
    lateinit var deviceController: DeviceController
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize database
        database = Room.databaseBuilder(
            applicationContext,
            ChatDatabase::class.java,
            "aura_database"
        ).build()
        
        // Initialize managers
        modelManager = ModelManager(applicationContext)
        deviceController = DeviceController(applicationContext)
    }
    
    companion object {
        @JvmStatic
        fun getInstance(context: Context): AuraApplication {
            return context.applicationContext as AuraApplication
        }
    }
}
