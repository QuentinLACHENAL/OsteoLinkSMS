package com.example.osteolinksms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

object NotificationManager {

    private const val CHANNEL_ID = "sms_sent_channel"

    fun createNotificationChannel(context: Context) {
        val name = "SMS Sent"
        val descriptionText = "Notifications for sent SMS"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager:
                NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun showSmsSentNotification(context: Context, phoneNumber: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_osteolink)
            .setContentTitle("SMS Envoyé")
            .setContentText("Un SMS a été envoyé au $phoneNumber")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(phoneNumber.hashCode(), builder.build())
    }
}
