package com.cashq.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cashq.data.local.dao.SavedSessionDao
import com.cashq.data.local.entity.SavedSessionEntity

@Database(
    entities = [SavedSessionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class CashQDatabase : RoomDatabase() {

    abstract fun savedSessionDao(): SavedSessionDao

    companion object {
        @Volatile
        private var INSTANCE: CashQDatabase? = null

        fun getDatabase(context: Context): CashQDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CashQDatabase::class.java,
                    "cashq_database"
                )
                    .fallbackToDestructiveMigrationOnDowngrade(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
