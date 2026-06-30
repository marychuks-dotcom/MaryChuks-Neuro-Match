package com.example.data

import kotlinx.coroutines.flow.Flow

class SessionRepository(private val sessionDao: SessionDao) {
    val allSessions: Flow<List<CognitiveSession>> = sessionDao.getAllSessions()

    suspend fun insertSession(session: CognitiveSession) {
        sessionDao.insertSession(session)
    }

    suspend fun clearAllSessions() {
        sessionDao.clearAllSessions()
    }
}
