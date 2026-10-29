package com.cashq.data.repository

import com.cashq.data.local.dao.SavedSessionDao
import com.cashq.data.local.entity.SavedSessionEntity
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getAllSessions(): Flow<List<SavedSessionEntity>>
    suspend fun getSessionById(id: String): SavedSessionEntity?
    suspend fun saveSession(session: SavedSessionEntity)
    suspend fun deleteSession(id: String)
}

class SessionRepositoryImpl(
    private val savedSessionDao: SavedSessionDao
) : SessionRepository {

    override fun getAllSessions(): Flow<List<SavedSessionEntity>> {
        return savedSessionDao.getAllSessions()
    }

    override suspend fun getSessionById(id: String): SavedSessionEntity? {
        return savedSessionDao.getSessionById(id)
    }

    override suspend fun saveSession(session: SavedSessionEntity) {
        savedSessionDao.insertSession(session)
    }

    override suspend fun deleteSession(id: String) {
        savedSessionDao.deleteSessionById(id)
    }
}
