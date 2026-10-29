package com.cashq

import android.app.Application
import com.cashq.data.local.CashQDatabase
import com.cashq.data.local.PreferencesManager
import com.cashq.data.repository.SessionRepository
import com.cashq.data.repository.SessionRepositoryImpl

class CashQApplication : Application() {

    val database by lazy { CashQDatabase.getDatabase(this) }
    val preferencesManager by lazy { PreferencesManager(this) }
    val sessionRepository: SessionRepository by lazy { SessionRepositoryImpl(database.savedSessionDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: CashQApplication
            private set
    }
}
