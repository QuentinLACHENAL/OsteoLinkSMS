package com.example.osteolinksms

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

import android.provider.CallLog
import android.provider.ContactsContract

import android.telephony.TelephonyManager

import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.example.osteolinksms.MainActivity.Companion.ID_QUENTIN
import com.example.osteolinksms.MainActivity.Companion.KEY_FORCE_SEND
import com.example.osteolinksms.MainActivity.Companion.KEY_SELECTED_PRACTITIONER_ID
import com.example.osteolinksms.MainActivity.Companion.KEY_UNKNOWN_ONLY
import com.example.osteolinksms.MainActivity.Companion.PREFS_NAME
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class CallReceiver : BroadcastReceiver() {

    companion object {
        private const val KEY_WAS_RINGING = "was_ringing"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Logger.log(context, "CallReceiver onReceive, action: ${intent.action}")

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
                    Logger.log(context, "Condition 'wasRinging' is true. Launching background task to check call log.")
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        checkLastCall(context)
                        pendingResult.finish()
                    }
                }
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                prefs.edit { putBoolean(KEY_WAS_RINGING, false) }
                Logger.log(context, "State is OFFHOOK. Stored was_ringing = false.")
            }
        }
    }

    @SuppressLint("Range")
    private suspend fun checkLastCall(context: Context) {
        Logger.log(context, "BACKGROUND_TASK: Checking last call...")
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            Logger.log(context, "BACKGROUND_TASK: READ_CALL_LOG permission is missing.")
            return
        }

        try {
            delay(1500) 

            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER))
                    val type = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE))
                    val date = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE))
                    val timeSinceCall = System.currentTimeMillis() - date

                    Logger.log(context, "BACKGROUND_TASK: Last call details: num=$number, type=$type, recent=${timeSinceCall}ms ago")

                    if (type == CallLog.Calls.MISSED_TYPE && timeSinceCall < 15000) {
                        Logger.log(context, "BACKGROUND_TASK: Call identified as a recent missed call. Handling SMS.")
                        handleMissedCall(context, number)
                    } else {
                        Logger.log(context, "BACKGROUND_TASK: Call is not a recent missed call. No action taken.")
                    }
                } else {
                    Logger.log(context, "BACKGROUND_TASK: Could not read last call. Cursor is empty.")
                }
            }
        } catch (e: Exception) {
            Logger.log(context, "BACKGROUND_TASK: Exception reading call log: ${e.message}")
        }
    }

    private fun handleMissedCall(context: Context, phoneNumber: String) {
        Logger.log(context, "handleMissedCall for number: $phoneNumber")
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (!isMobileNumber(phoneNumber)) {
            Logger.log(context, "SMS not sent: $phoneNumber is not a mobile number.")
            HistoryManager.addHistoryEntry(context, "Appel manqué ignoré (fixe) : $phoneNumber")
            return
        }

        val forceSend = sharedPreferences.getBoolean(KEY_FORCE_SEND, false)
        if (!forceSend && hasRecentSms(context, phoneNumber)) {
            Logger.log(context, "SMS not sent: An SMS was already sent to $phoneNumber recently.")
            return
        }

        val unknownOnly = sharedPreferences.getBoolean(KEY_UNKNOWN_ONLY, false)
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

    private fun isMobileNumber(phoneNumber: String): Boolean {
        return phoneNumber.startsWith("+336") || phoneNumber.startsWith("+337") || phoneNumber.startsWith("06") || phoneNumber.startsWith("07")
    }

    private fun hasRecentSms(context: Context, phoneNumber: String): Boolean {
        val history = HistoryManager.getHistory(context)
        val fiveMinutesAgo = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5)

        return history.any { log ->
            val cleanLog = log.substringAfter(" - ")
            val sentToThisNumber = cleanLog.contains("envoyée à $phoneNumber") || cleanLog.contains("manuel à $phoneNumber")

            if (sentToThisNumber) {
                try {
                    val logTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(log.substringBefore(" - "))?.time ?: 0
                    return@any logTimestamp > fiveMinutesAgo
                } catch (_: Exception) {
                    return@any false
                }
            }
            false
        }
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
        SmsSender.sendSms(context, phoneNumber, message)
    }
}
