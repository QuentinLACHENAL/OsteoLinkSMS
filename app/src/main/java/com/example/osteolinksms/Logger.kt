package com.example.osteolinksms

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Logger {

    private const val PREFS_NAME = "LogPrefs"
    private const val KEY_LOGS = "logs"

    fun log(context: Context, message: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val newLog = "$timestamp: $message\n"

        val oldLogs = prefs.getString(KEY_LOGS, "")
        prefs.edit().putString(KEY_LOGS, oldLogs + newLog).apply()
    }

    fun getLogs(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LOGS, "Aucun log pour le moment.") ?: ""
    }

    fun clearLogs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_LOGS).apply()
    }
}
