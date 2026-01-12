package com.example.osteolinksms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager

class SmsResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val code = getResultCode()
        val errorCode = intent?.getIntExtra("errorCode", -1)
        
        val resultText = when (code) {
            Activity.RESULT_OK -> "SMS envoyé avec succès."
            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "Échec Système : Erreur générique (Code sup: $errorCode)."
            SmsManager.RESULT_ERROR_NO_SERVICE -> "Échec Système : Pas de service."
            SmsManager.RESULT_ERROR_NULL_PDU -> "Échec Système : PDU nul."
            SmsManager.RESULT_ERROR_RADIO_OFF -> "Échec Système : Radio éteinte."
            else -> "Échec inconnu: $code (Code sup: $errorCode)"
        }

        Logger.log(context, "SMS_REPORT: $resultText")
        HistoryManager.addHistoryEntry(context, "Rapport: $resultText")
    }
}
