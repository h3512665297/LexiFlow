package com.lexiflow.wordbook.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

data class UserSettings(
    val dailyGoal: Int,
    val newLimit: Int,
    val autoPlay: Boolean,
    val reminderEnabled: Boolean,
    val reminderHour: Int,
    val darkMode: Boolean,
    val desiredRetention: Int
)

class UserPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("lexiflow_settings", Context.MODE_PRIVATE)

    fun get() = UserSettings(
        dailyGoal = prefs.getInt(KEY_GOAL, 20),
        newLimit = prefs.getInt(KEY_NEW_LIMIT, 10),
        autoPlay = prefs.getBoolean(KEY_AUTO_PLAY, true),
        reminderEnabled = prefs.getBoolean(KEY_REMINDER, false),
        reminderHour = prefs.getInt(KEY_REMINDER_HOUR, 20),
        darkMode = prefs.getBoolean(KEY_DARK, false),
        desiredRetention = prefs.getInt(KEY_RETENTION, 90)
    )

    fun update(settings: UserSettings) {
        prefs.edit()
            .putInt(KEY_GOAL, settings.dailyGoal)
            .putInt(KEY_NEW_LIMIT, settings.newLimit)
            .putBoolean(KEY_AUTO_PLAY, settings.autoPlay)
            .putBoolean(KEY_REMINDER, settings.reminderEnabled)
            .putInt(KEY_REMINDER_HOUR, settings.reminderHour)
            .putBoolean(KEY_DARK, settings.darkMode)
            .putInt(KEY_RETENTION, settings.desiredRetention)
            .apply()
    }

    fun applyTheme() {
        AppCompatDelegate.setDefaultNightMode(
            if (get().darkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun getApiKey(): String = prefs.getString(KEY_API_KEY, "") ?: ""
    fun setApiKey(key: String) = prefs.edit().putString(KEY_API_KEY, key).apply()

    companion object {
        private const val KEY_GOAL = "daily_goal"
        private const val KEY_NEW_LIMIT = "new_limit"
        private const val KEY_AUTO_PLAY = "auto_play"
        private const val KEY_REMINDER = "reminder"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_DARK = "dark"
        private const val KEY_RETENTION = "retention"
        private const val KEY_API_KEY = "deepseek_api_key"
    }
}
