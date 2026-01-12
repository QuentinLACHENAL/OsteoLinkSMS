package com.example.osteolinksms

import android.content.Context

object HistoryManager {

    private const val PREFS_NAME = "HistoryPrefs"
    private const val KEY_HISTORY = "history"
    private const val MAX_HISTORY_SIZE = 50

    fun addNumberToHistory(context: Context, number: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val historyString = prefs.getString(KEY_HISTORY, "") ?: ""
        val historyList = historyString.split("\n").filter { it.isNotBlank() }.toMutableList()

        historyList.add(0, number)

        while (historyList.size > MAX_HISTORY_SIZE) {
            historyList.removeAt(historyList.size - 1)
        }

        prefs.edit().putString(KEY_HISTORY, historyList.joinToString("\n")).apply()
    }

    fun getHistory(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_HISTORY, "Aucun historique pour le moment.") ?: ""
    }

    fun clearHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY).apply()
    }
}
