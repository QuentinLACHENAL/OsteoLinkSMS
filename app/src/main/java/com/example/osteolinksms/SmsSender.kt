package com.example.osteolinksms

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object SmsSender {

    private const val SENT_SMS_ACTION = "com.example.osteolinksms.SMS_SENT"

    fun sendSms(context: Context, phoneNumber: String, message: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Logger.log(context, "SEND_SMS permission is missing.")
            return
        }

        try {
            // Determine the best SmsManager instance
            val smsManager = getBestSmsManager(context)

            val parts = smsManager.divideMessage(message)
            Logger.log(context, "SmsSender: Sending message to $phoneNumber (${message.length} chars, ${parts.size} parts).")

            if (parts.size > 1) {
                val sentIntents = ArrayList<PendingIntent>()
                for (i in parts.indices) {
                    sentIntents.add(createPendingIntent(context, i, phoneNumber))
                }
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, null)
                Logger.log(context, "SmsSender: Sent multipart message.")
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, createPendingIntent(context, 0, phoneNumber), null)
                Logger.log(context, "SmsSender: Sent single part message.")
            }

            HistoryManager.addHistoryEntry(context, "Tentative d\'envoi à $phoneNumber")

        } catch (e: Exception) {
            Logger.log(context, "FATAL: Exception during SMS sending: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun getBestSmsManager(context: Context): SmsManager {
        var subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID
        val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val targetSlot = prefs.getInt(EditMessagesActivity.KEY_SIM_SLOT, -1)

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            
            // Try to find the specific SIM based on slot preference
            if (targetSlot != -1) {
                val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                val activeSubs = subscriptionManager.activeSubscriptionInfoList
                activeSubs?.forEach { subInfo ->
                    if (subInfo.simSlotIndex == targetSlot) {
                        subId = subInfo.subscriptionId
                        Logger.log(context, "SmsSender: Found subscription ID $subId for target slot $targetSlot")
                    }
                }
            }

            // Fallback to default if no specific slot selected or not found
            if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                subId = SubscriptionManager.getDefaultSmsSubscriptionId().takeIf { it != SubscriptionManager.INVALID_SUBSCRIPTION_ID }
                    ?: SubscriptionManager.getDefaultSubscriptionId()
            }
        }

        return if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            Logger.log(context, "SmsSender: Using Subscription ID: $subId")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java).createForSubscriptionId(subId)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getSmsManagerForSubscriptionId(subId)
            }
        } else {
            Logger.log(context, "SmsSender: Using default SmsManager (no valid subId).")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
        }
    }

    private fun createPendingIntent(context: Context, offset: Int, phoneNumber: String): PendingIntent {
        val sentIntent = Intent(context, SmsResultReceiver::class.java)
        sentIntent.action = SENT_SMS_ACTION
        sentIntent.putExtra("phoneNumber", phoneNumber)
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
        // Unique request code per part/call
        return PendingIntent.getBroadcast(context, System.currentTimeMillis().toInt() + offset, sentIntent, flags)
    }
}
