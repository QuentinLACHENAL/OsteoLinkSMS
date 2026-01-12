package com.example.osteolinksms

import android.content.Context
import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HistoryManager {

    private const val PREFS_NAME = "HistoryPrefs"
    private const val KEY_HISTORY = "history"
    private const val MAX_HISTORY_SIZE = 100 // Increased size for more detailed logs

    fun addHistoryEntry(context: Context, message: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val newEntry = "$timestamp - $message"

        val history = prefs.getString(KEY_HISTORY, "")?.split("\n")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()

        history.add(0, newEntry)

        // Trim the list if it exceeds the max size
        while (history.size > MAX_HISTORY_SIZE) {
            history.removeAt(history.size - 1)
        }

        prefs.edit { putString(KEY_HISTORY, history.joinToString("\n")) }
    }

    fun getHistory(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val historyString = prefs.getString(KEY_HISTORY, "Aucun historique pour le moment.") ?: ""
        return historyString.split("\n").filter { it.isNotBlank() }
    }

    fun clearHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { remove(KEY_HISTORY) }
    }
}
