package com.example.osteolinksms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SmsResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val resultCode = resultCode
        val resultText = when (resultCode) {
            Activity.RESULT_OK -> "SMS envoyé avec succès par le système."
            else -> "Échec de l'envoi du SMS par le système. Code: $resultCode"
        }

        Logger.log(context, "SMS_RESULT: $resultText")
    }
}
