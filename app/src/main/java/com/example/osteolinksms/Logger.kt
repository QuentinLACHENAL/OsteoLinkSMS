package com.example.osteolinksms

import android.content.Context
import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Logger {

    private const val PREFS_NAME = "LogPrefs"
    private const val KEY_LOGS = "logs"
    private const val MAX_LOG_LINES = 200

    fun log(context: Context, message: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val newLog = "$timestamp: $message"

        val logs = prefs.getString(KEY_LOGS, "")?.split("\n")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
        logs.add(newLog)

        while (logs.size > MAX_LOG_LINES) {
            logs.removeAt(0)
        }

        prefs.edit { putString(KEY_LOGS, logs.joinToString("\n")) }
    }

    fun getLogs(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LOGS, "Aucun log pour le moment.") ?: ""
    }

    fun clearLogs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { remove(KEY_LOGS) }
    }
}
