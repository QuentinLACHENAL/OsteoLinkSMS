package com.example.osteolinksms

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

object NewsChecker {

    // REMPLACEZ CETTE URL PAR VOTRE LIEN "RAW" GIST
    // Exemple: "https://gist.githubusercontent.com/USER/HASH/raw/news.json"
    private const val NEWS_URL = "https://gist.githubusercontent.com/QuentinLACHENAL/329daadcd453e8f5397c565e462e9ed7/raw/news.json"

    private const val PREFS_NEWS = "news_prefs"
    private const val KEY_LAST_SEEN_ID = "last_seen_msg_id"

    fun checkNews(context: Context, onUpdateAvailable: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Fetch data
                val jsonString = URL(NEWS_URL).readText()
                val json = JSONObject(jsonString)

                // --- CHECK FOR UPDATES ---
                val latestVersionCode = json.optInt("latest_version_code", 0)
                val updateUrl = json.optString("update_url", "")
                
                // Get Current Version Code safely without BuildConfig
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pInfo.longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    pInfo.versionCode
                }

                if (latestVersionCode > currentVersionCode && updateUrl.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        onUpdateAvailable(updateUrl)
                    }
                }

                // --- CHECK FOR NEWS (Dialog) ---
                val serverId = json.optInt("id", -1)
                if (serverId == -1) return@launch

                // 2. Check if already seen
                val prefs = context.getSharedPreferences(PREFS_NEWS, Context.MODE_PRIVATE)
                val lastSeenId = prefs.getInt(KEY_LAST_SEEN_ID, 0)

                if (serverId > lastSeenId) {
                    // 3. Show Dialog on Main Thread
                    val title = json.optString("title", "Info")
                    val message = json.optString("message", "")
                    val linkUrl = json.optString("link_url", "")
                    val linkLabel = json.optString("link_label", "Voir")

                    withContext(Dispatchers.Main) {
                        // Show System Notification
                        NotificationManager.showNewsNotification(context, title, message, linkUrl)

                        // Show In-App Dialog
                        showDialog(context, title, message, linkUrl, linkLabel, serverId, prefs)
                    }
                }
            } catch (e: Exception) {
                // Silent fail: No internet or invalid JSON. Do nothing.
                // e.printStackTrace() 
            }
        }
    }

    private fun showDialog(
        context: Context,
        title: String,
        message: String,
        linkUrl: String,
        linkLabel: String,
        serverId: Int,
        prefs: android.content.SharedPreferences
    ) {
        val builder = AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false) // Force interaction

        if (linkUrl.isNotEmpty()) {
            builder.setPositiveButton(linkLabel) { dialog, _ ->
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl))
                    context.startActivity(browserIntent)
                } catch (e: Exception) {
                    // Fallback or ignore
                }
                // Mark as seen only if user interacts positively? 
                // Usually we mark as seen once displayed to avoid spamming at every launch.
                prefs.edit().putInt(KEY_LAST_SEEN_ID, serverId).apply()
                dialog.dismiss()
            }
            builder.setNegativeButton("Fermer") { dialog, _ ->
                prefs.edit().putInt(KEY_LAST_SEEN_ID, serverId).apply()
                dialog.dismiss()
            }
        } else {
            builder.setPositiveButton("OK") { dialog, _ ->
                prefs.edit().putInt(KEY_LAST_SEEN_ID, serverId).apply()
                dialog.dismiss()
            }
        }

        builder.show()
    }
}
