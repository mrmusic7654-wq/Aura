package com.aura.ai.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [ChatMessage::class, Conversation::class],
    version = 1,
    exportSchema = false
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}

@Dao
interface ChatDao {
    // Conversations
    @Insert
    suspend fun insertConversation(conversation: Conversation): Long
    
    @Query("SELECT * FROM conversations ORDER BY lastMessageTime DESC")
    fun getAllConversations(): Flow<List<Conversation>>
    
    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversation(id: Long): Conversation?
    
    @Update
    suspend fun updateConversation(conversation: Conversation)
    
    @Delete
    suspend fun deleteConversation(conversation: Conversation)
    
    // Messages
    @Insert
    suspend fun insertMessage(message: ChatMessage): Long
    
    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    fun getMessagesForConversation(convId: Long): Flow<List<ChatMessage>>
    
    @Query("UPDATE messages SET commandExecuted = 1 WHERE id = :messageId")
    suspend fun markCommandExecuted(messageId: Long)
    
    @Query("DELETE FROM messages WHERE conversationId = :convId")
    suspend fun deleteConversationMessages(convId: Long)
    
    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :convId")
    suspend fun getMessageCount(convId: Long): Int
}
