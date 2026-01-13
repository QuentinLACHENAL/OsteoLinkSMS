package com.example.osteolinksms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat

object NotificationManager {

    private const val CHANNEL_ID_EVENTS = "osteolink_events"
    private const val CHANNEL_ID_SERVICE = "osteolink_service"
    private const val CHANNEL_ID_NEWS = "osteolink_news"
    private const val NOTIFICATION_ID_MONITORING = 1001

    fun createNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Channel 1: Events (SMS Sent)
        val channelEvents = NotificationChannel(CHANNEL_ID_EVENTS, "Événements (SMS)", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Notifications lors de l'envoi de SMS"
        }
        notificationManager.createNotificationChannel(channelEvents)

        // Channel 2: Service (Monitoring Status)
        val channelService = NotificationChannel(CHANNEL_ID_SERVICE, "Statut de Surveillance", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Indique si l'application surveille les appels"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channelService)

        // Channel 3: News (Announcements)
        val channelNews = NotificationChannel(CHANNEL_ID_NEWS, "Nouveautés & Annonces", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Annonces importantes du développeur"
        }
        notificationManager.createNotificationChannel(channelNews)
    }

    fun showNewsNotification(context: Context, title: String, message: String, linkUrl: String?) {
        val intent = if (!linkUrl.isNullOrEmpty()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl))
        } else {
            Intent(context, MainActivity::class.java)
        }
        // Ensure we have a valid task stack or new task
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        
        val pendingIntent = PendingIntent.getActivity(context, System.currentTimeMillis().toInt(), intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_NEWS)
            .setSmallIcon(R.drawable.logo_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message)) // Expandable text
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(9999, builder.build()) // ID 9999 for news
    }

    fun showSmsSentNotification(context: Context, phoneNumber: String) {
        // Check user preference
        val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val showNotifs = prefs.getBoolean(EditMessagesActivity.KEY_NOTIFICATIONS_SMS, true)
        
        if (!showNotifs) return

        // Intent to open history when clicked
        val intent = Intent(context, HistoryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_EVENTS)
            .setSmallIcon(R.drawable.logo_notification) // Ensure this resource exists, fallback if needed
            .setContentTitle("SMS Automatique Envoyé")
            .setContentText("Destinataire : $phoneNumber")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Use phoneNumber.hashCode() as ID so multiple parts of the same SMS update the same notification
        // instead of stacking multiple notifications.
        notificationManager.notify(phoneNumber.hashCode(), builder.build())
    }

    fun updateMonitoringNotification(context: Context, isEnabled: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (isEnabled) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            val builder = NotificationCompat.Builder(context, CHANNEL_ID_SERVICE)
                .setSmallIcon(R.drawable.logo_notification)
                .setContentTitle("OsteoLinkSMS Actif")
                .setContentText("L'application surveille vos appels manqués.")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true) // Persistent
                .setContentIntent(pendingIntent)

            notificationManager.notify(NOTIFICATION_ID_MONITORING, builder.build())
        } else {
            notificationManager.cancel(NOTIFICATION_ID_MONITORING)
        }
    }
}
