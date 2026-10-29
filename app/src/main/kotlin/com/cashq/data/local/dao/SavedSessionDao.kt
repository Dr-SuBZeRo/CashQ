package com.cashq.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cashq.data.local.entity.SavedSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedSessionDao {

    @Query("SELECT * FROM saved_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<SavedSessionEntity>>

    @Query("SELECT * FROM saved_sessions WHERE id = :id")
    suspend fun getSessionById(id: String): SavedSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SavedSessionEntity)

    @Update
    suspend fun updateSession(session: SavedSessionEntity)

    @Delete
    suspend fun deleteSession(session: SavedSessionEntity)

    @Query("DELETE FROM saved_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: String)

    @Query("SELECT COUNT(*) FROM saved_sessions")
    suspend fun getSessionCount(): Int
}
