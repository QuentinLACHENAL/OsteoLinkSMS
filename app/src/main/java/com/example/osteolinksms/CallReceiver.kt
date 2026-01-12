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
import androidx.core.content.edit
import com.example.osteolinksms.MainActivity.Companion.ID_QUENTIN
import com.example.osteolinksms.MainActivity.Companion.KEY_SELECTED_PRACTITIONER_ID
import com.example.osteolinksms.MainActivity.Companion.KEY_UNKNOWN_ONLY
import com.example.osteolinksms.MainActivity.Companion.PREFS_NAME

class CallReceiver : BroadcastReceiver() {

    companion object {
        private const val KEY_WAS_RINGING = "was_ringing"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Logger.log(context, "BroadcastReceiver onReceive, action: ${intent.action}")

        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            return
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        Logger.log(context, "Telephony state changed: $state")

        @Suppress("DEPRECATION")
        val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                prefs.edit { putBoolean(KEY_WAS_RINGING, true) }
                if (phoneNumber != null) {
                    HistoryManager.addHistoryEntry(context, "Appel entrant de $phoneNumber")
                }
                Logger.log(context, "State is RINGING. Stored was_ringing = true.")
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                val wasRinging = prefs.getBoolean(KEY_WAS_RINGING, false)
                Logger.log(context, "State is IDLE. Read was_ringing = $wasRinging.")
                if (wasRinging) {
                    prefs.edit { putBoolean(KEY_WAS_RINGING, false) }
                    Logger.log(context, "Condition 'wasRinging' is true. Proceeding to check last call.")
                    checkLastCall(context)
                }
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                prefs.edit { putBoolean(KEY_WAS_RINGING, false) }
                Logger.log(context, "State is OFFHOOK. Stored was_ringing = false.")
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
            Thread.sleep(1000)

            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC LIMIT 1"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER))
                    val type = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE))
                    val date = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE))
                    val timeSinceCall = System.currentTimeMillis() - date

                    Logger.log(context, "Last call details: num=$number, type=$type, recent=${timeSinceCall}ms ago")

                    if (type == CallLog.Calls.MISSED_TYPE && timeSinceCall < 10000) {
                        Logger.log(context, "Call identified as a recent missed call. Handling SMS.")
                        handleMissedCall(context, number)
                    } else {
                        Logger.log(context, "Call is not a recent missed call. No action taken.")
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
        Logger.log(context, "handleMissedCall for number: $phoneNumber")
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val unknownOnly = sharedPreferences.getBoolean(KEY_UNKNOWN_ONLY, false)
        Logger.log(context, "Option 'unknown only' is set to: $unknownOnly")

        if (unknownOnly && !isUnknownNumber(context, phoneNumber)) {
            Logger.log(context, "SMS not sent: call is from a known number and option is set to unknown only.")
            return
        }

        val selectedPractitionerId = sharedPreferences.getInt(KEY_SELECTED_PRACTITIONER_ID, ID_QUENTIN)
        val messageKey = if (selectedPractitionerId == ID_QUENTIN) {
            EditMessagesActivity.KEY_QUENTIN_MESSAGE
        } else {
            EditMessagesActivity.KEY_LAURA_MESSAGE
        }

        val message = sharedPreferences.getString(messageKey, null)

        if (message.isNullOrEmpty()) {
            Logger.log(context, "SMS not sent: message for the selected practitioner is empty.")
            return
        }

        Logger.log(context, "Message to send: '$message'")
        sendSms(context, phoneNumber, message)
    }

    private fun isUnknownNumber(context: Context, phoneNumber: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            Logger.log(context, "READ_CONTACTS permission is missing.")
            return true
        }
        Logger.log(context, "Checking if '$phoneNumber' is in contacts.")
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
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
            true
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
            Logger.log(context, "SUCCESS: SmsManager.sendTextMessage called for $phoneNumber.")
            HistoryManager.addHistoryEntry(context, "Réponse auto. envoyée à $phoneNumber")
            NotificationManager.showSmsSentNotification(context, phoneNumber)
        } catch (e: Exception) {
            Logger.log(context, "FATAL: Exception during SMS sending: ${e.message}")
        }
    }
}
