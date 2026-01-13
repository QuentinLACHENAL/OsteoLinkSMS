package com.example.osteolinksms

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileWriter

class HistoryActivity : AppCompatActivity() {

    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        historyRecyclerView = findViewById(R.id.historyRecyclerView)
        historyRecyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<Button>(R.id.clearHistoryButton).setOnClickListener {
            HistoryManager.clearHistory(this)
            loadHistory()
        }

        findViewById<Button>(R.id.exportHistoryButton).setOnClickListener {
            exportHistoryToCsv()
        }

        loadHistory()
    }

    private fun loadHistory() {
        val rawHistory = HistoryManager.getHistory(this)
        val items = rawHistory.map { parseLogLine(it) }
        
        adapter = HistoryAdapter(items)
        historyRecyclerView.adapter = adapter
    }

    private fun parseLogLine(line: String): HistoryItem {
        // Format expected: "yyyy-MM-dd HH:mm:ss - Message"
        val parts = line.split(" - ", limit = 2)
        val date = if (parts.size > 0) parts[0] else ""
        val message = if (parts.size > 1) parts[1] else line

        val type = when {
            message.contains("Appel", true) -> HistoryType.CALL_MISSED
            message.contains("envoyé", true) || message.contains("succès", true) || message.contains("tentative", true) -> HistoryType.SMS_SENT
            message.contains("échec", true) || message.contains("erreur", true) || message.contains("ignoré", true) -> HistoryType.ERROR
            else -> HistoryType.INFO
        }
        
        return HistoryItem(date, message, type)
    }

    private fun exportHistoryToCsv() {
        val rawHistory = HistoryManager.getHistory(this)
        if (rawHistory.isEmpty()) {
            Toast.makeText(this, "Rien à exporter", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val fileName = "osteolink_history_${System.currentTimeMillis()}.csv"
            val file = File(getExternalFilesDir(null), fileName)
            val writer = FileWriter(file)
            
            writer.append("Date,Message\n")
            for (line in rawHistory) {
                val parts = line.split(" - ", limit = 2)
                val date = if (parts.isNotEmpty()) parts[0] else ""
                val msg = if (parts.size > 1) parts[1].replace(",", " ") else line.replace(",", " ")
                writer.append("$date,$msg\n")
            }
            writer.flush()
            writer.close()

            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Exporter l'historique"))

        } catch (e: Exception) {
            Toast.makeText(this, "Erreur export: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}

enum class HistoryType { CALL_MISSED, SMS_SENT, ERROR, INFO }
data class HistoryItem(val date: String, val message: String, val type: HistoryType)

class HistoryAdapter(private val items: List<HistoryItem>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.historyIcon)
        val title: TextView = view.findViewById(R.id.historyTitle)
        val date: TextView = view.findViewById(R.id.historyDate)
        val detail: TextView = view.findViewById(R.id.historyDetail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        holder.date.text = item.date
        holder.detail.text = item.message

        when (item.type) {
            HistoryType.CALL_MISSED -> {
                holder.icon.setImageResource(R.drawable.ic_call_missed)
                holder.title.text = "Appel"
            }
            HistoryType.SMS_SENT -> {
                holder.icon.setImageResource(R.drawable.ic_sms_sent)
                holder.title.text = "SMS Envoyé"
            }
            HistoryType.ERROR -> {
                holder.icon.setImageResource(R.drawable.ic_error)
                holder.title.text = "Info / Alerte"
            }
            HistoryType.INFO -> {
                holder.icon.setImageResource(R.drawable.ic_sms_sent) // Default
                holder.title.text = "Info"
            }
        }
    }

    override fun getItemCount() = items.size
}