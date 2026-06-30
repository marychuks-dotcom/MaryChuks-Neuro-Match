package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM cognitive_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<CognitiveSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: CognitiveSession)

    @Query("DELETE FROM cognitive_sessions")
    suspend fun clearAllSessions()
}
