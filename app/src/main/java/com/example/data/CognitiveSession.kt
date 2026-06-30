package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cognitive_sessions")
data class CognitiveSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val score: Int,
    val movesCount: Int,
    val twistMode: String,
    val memoryMode: Boolean,
    val patternRecognition: Float,
    val planningAhead: Float,
    val workingMemory: Float,
    val decisionMaking: Float,
    val processingSpeed: Float
)
