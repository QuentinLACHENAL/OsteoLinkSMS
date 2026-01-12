package com.example.osteolinksms

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class HistoryActivity : AppCompatActivity() {

    private lateinit var historyListView: ListView
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        historyListView = findViewById(R.id.historyListView)
        findViewById<Button>(R.id.clearHistoryButton).setOnClickListener {
            HistoryManager.clearHistory(this)
            loadHistory()
        }

        loadHistory()
    }

    private fun loadHistory() {
        val history = HistoryManager.getHistory(this)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, history)
        historyListView.adapter = adapter
    }
}
