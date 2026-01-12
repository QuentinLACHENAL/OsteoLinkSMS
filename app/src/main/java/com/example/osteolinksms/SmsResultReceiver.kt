package com.example.osteolinksms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager

class SmsResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        // THIS IS THE DEFINITIVE FIX: Using getResultCode() instead of the property.
        val code = getResultCode()
        val resultText = when (code) {
            Activity.RESULT_OK -> "SMS envoyé avec succès par le système."
            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "Échec Système : Erreur générique."
            SmsManager.RESULT_ERROR_NO_SERVICE -> "Échec Système : Pas de service (mode avion ?)."
            SmsManager.RESULT_ERROR_NULL_PDU -> "Échec Système : PDU nul."
            SmsManager.RESULT_ERROR_RADIO_OFF -> "Échec Système : Radio (réseau mobile) éteinte."
            else -> "Échec Système avec un code d'erreur inconnu: $code"
        }

        Logger.log(context, "SMS_REPORT: $resultText")
        HistoryManager.addHistoryEntry(context, "Rapport d'envoi: $resultText")
    }
}
