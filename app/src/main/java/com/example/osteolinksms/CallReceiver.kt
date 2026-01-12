package com.example.osteolinksms

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CallLog
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

class CallReceiver : BroadcastReceiver() {

    private var wasRinging = false

    override fun onReceive(context: Context, intent: Intent) {
        Logger.log(context, "BroadcastReceiver onReceive, action: ${intent.action}")

        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            return
        }

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        Logger.log(context, "Telephony state changed: $state")

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                wasRinging = true
                Logger.log(context, "State is RINGING. `wasRinging` is now true.")
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                Logger.log(context, "State is IDLE. `wasRinging` is $wasRinging.")
                if (wasRinging) {
                    wasRinging = false
                    checkLastCall(context)
                }
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                wasRinging = false
                Logger.log(context, "State is OFFHOOK. `wasRinging` is now false.")
            }
        }
    }

    @SuppressLint("Range")
    private fun checkLastCall(context: Context) {
        Logger.log(context, "Checking last call...")
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            Logger.log(context, "READ_CALL_LOG permission is missing.")
            return
        }

        try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER))
                    val type = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE))
                    val date = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE))
                    val timeSinceCall = System.currentTimeMillis() - date

                    Logger.log(context, "Last call: num=$number, type=$type, recent=${timeSinceCall}ms ago")

                    if (type == CallLog.Calls.MISSED_TYPE && timeSinceCall < 5000) {
                        handleMissedCall(context, number)
                    }
                } else {
                    Logger.log(context, "Could not read last call. Cursor is empty.")
                }
            }
        } catch (e: Exception) {
            Logger.log(context, "Exception reading call log: ${e.message}")
        }
    }

    private fun handleMissedCall(context: Context, phoneNumber: String) {
        val sharedPreferences = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val unknownOnly = sharedPreferences.getBoolean(MainActivity.KEY_UNKNOWN_ONLY, false)

        if (unknownOnly && !isUnknownNumber(context, phoneNumber)) {
            Logger.log(context, "SMS not sent: call is from a known number and option is set to unknown only.")
            return
        }

        val selectedPractitionerId = sharedPreferences.getInt(MainActivity.KEY_SELECTED_PRACTITIONER_ID, MainActivity.ID_QUENTIN)
        val messageKey = if (selectedPractitionerId == MainActivity.ID_QUENTIN) {
            EditMessagesActivity.KEY_QUENTIN_MESSAGE
        } else {
            EditMessagesActivity.KEY_LAURA_MESSAGE
        }

        val message = sharedPreferences.getString(messageKey, null)

        if (message.isNullOrEmpty()) {
            Logger.log(context, "SMS not sent: message for the selected practitioner is empty.")
            return
        }

        sendSms(context, phoneNumber, message)
    }

    private fun isUnknownNumber(context: Context, phoneNumber: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            Logger.log(context, "READ_CONTACTS permission is missing.")
            return true // Assume unknown if permission is missing
        }
        Logger.log(context, "Checking if '$phoneNumber' is in contacts.")
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?"
        val selectionArgs = arrayOf(phoneNumber)

        return try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use {
                val isKnown = it.moveToFirst()
                Logger.log(context, "Contact lookup successful. Number is known: $isKnown")
                !isKnown
            } ?: true
        } catch (e: Exception) {
            Logger.log(context, "Exception checking contacts: ${e.message}")
            true // Assume unknown in case of error
        }
    }

    private fun sendSms(context: Context, phoneNumber: String, message: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Logger.log(context, "SEND_SMS permission is missing.")
            return
        }

        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Logger.log(context, "SMS sent to $phoneNumber.")
            HistoryManager.addNumberToHistory(context, phoneNumber)
            NotificationManager.showSmsSentNotification(context, phoneNumber)
        } catch (e: Exception) {
            Logger.log(context, "Error sending SMS: ${e.message}")
        }
    }
}
