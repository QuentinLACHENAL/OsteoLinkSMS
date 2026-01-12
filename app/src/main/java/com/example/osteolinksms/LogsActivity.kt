package com.example.osteolinksms

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LogsActivity : AppCompatActivity() {

    private lateinit var logsTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs)

        logsTextView = findViewById(R.id.logsTextView)

        findViewById<Button>(R.id.clearLogsButton).setOnClickListener {
            Logger.clearLogs(this)
            loadLogs()
        }

        findViewById<Button>(R.id.copyLogsButton).setOnClickListener {
            copyLogsToClipboard()
        }

        loadLogs()
    }

    private fun loadLogs() {
        logsTextView.text = Logger.getLogs(this)
    }

    private fun copyLogsToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Logs", logsTextView.text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Logs copiés dans le presse-papiers", Toast.LENGTH_SHORT).show()
    }
}
