package com.lexiflow.wordbook

import android.app.Application
import com.lexiflow.wordbook.data.AppDatabase
import com.lexiflow.wordbook.data.LearningRepository
import com.lexiflow.wordbook.data.UserPreferences

class LexiFlowApplication : Application() {
    val preferences by lazy { UserPreferences(this) }
    val repository by lazy { LearningRepository(this, AppDatabase.get(this), preferences) }

    override fun onCreate() {
        preferences.applyTheme()
        super.onCreate()
        ReminderScheduler.createChannel(this)
    }
}
