package com.example.osteolinksms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager

class SmsResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val resultCode = resultCode
        val resultText = when (resultCode) {
            Activity.RESULT_OK -> "SMS envoyé avec succès par le système."
            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "Échec : Erreur générique."
            SmsManager.RESULT_ERROR_NO_SERVICE -> "Échec : Pas de service."
            SmsManager.RESULT_ERROR_NULL_PDU -> "Échec : PDU nul."
            SmsManager.RESULT_ERROR_RADIO_OFF -> "Échec : Radio éteinte."
            else -> "Échec avec un code d'erreur inconnu: $resultCode"
        }

        Logger.log(context, "SMS_RESULT: $resultText")
        HistoryManager.addHistoryEntry(context, "Rapport d'envoi: $resultText")
    }
}
