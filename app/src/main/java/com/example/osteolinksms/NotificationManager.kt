package com.example.osteolinksms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

object NotificationManager {

    private const val CHANNEL_ID_EVENTS = "osteolink_events"
    private const val CHANNEL_ID_SERVICE = "osteolink_service"
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
    }

    fun showSmsSentNotification(context: Context, phoneNumber: String) {
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
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
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
