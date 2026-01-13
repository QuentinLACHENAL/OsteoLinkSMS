package com.example.osteolinksms

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CallLog
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.example.osteolinksms.MainActivity.Companion.KEY_APP_ENABLED
import com.example.osteolinksms.MainActivity.Companion.KEY_FORCE_SEND
import com.example.osteolinksms.MainActivity.Companion.KEY_UNKNOWN_ONLY
import com.example.osteolinksms.MainActivity.Companion.KEY_VACATION_MODE
import com.example.osteolinksms.MainActivity.Companion.PREFS_NAME
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class CallReceiver : BroadcastReceiver() {

    companion object {
        private const val KEY_WAS_RINGING = "was_ringing"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Logger.log(context, "CallReceiver onReceive, action: ${intent.action}")

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Master Switch Check
        if (!prefs.getBoolean(KEY_APP_ENABLED, true)) {
            Logger.log(context, "CallReceiver: App is disabled. Event ignored.")
            return
        }

        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            return
        }

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
            // Smart Polling: Try up to 10 times (5 seconds max) to find the new call log entry
            var found = false
            for (i in 1..10) {
                delay(500) // Wait 500ms between attempts

                context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC"
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER))
                        val type = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE))
                        val date = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE))
                        val timeSinceCall = System.currentTimeMillis() - date

                        Logger.log(context, "BACKGROUND_TASK (Attempt $i): Last call: num=$number, type=$type, recent=${timeSinceCall}ms ago")

                        // Threshold 60s (60000ms)
                        if (type == CallLog.Calls.MISSED_TYPE && timeSinceCall < 60000) {
                            Logger.log(context, "BACKGROUND_TASK: Call identified as a recent missed call. Handling SMS.")
                            handleMissedCall(context, number)
                            found = true
                        }
                    }
                }
                
                if (found) break
            }
            
            if (!found) {
                Logger.log(context, "BACKGROUND_TASK: Failed to identify a recent missed call after 5 seconds.")
            }

        } catch (e: Exception) {
            Logger.log(context, "BACKGROUND_TASK: Exception reading call log: ${e.message}")
        }
    }

    private fun handleMissedCall(context: Context, phoneNumber: String) {
        Logger.log(context, "handleMissedCall for number: $phoneNumber")
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (!isMobileNumber(context, phoneNumber)) {
            Logger.log(context, "SMS not sent: $phoneNumber is not a recognized mobile number.")
            HistoryManager.addHistoryEntry(context, "Appel manqué ignoré (fixe/étranger) : $phoneNumber")
            return
        }

        // --- WHITELIST CHECK ---
        val whitelist = sharedPreferences.getString(EditMessagesActivity.KEY_WHITELIST, "") ?: ""
        if (isInWhitelist(phoneNumber, whitelist)) {
            Logger.log(context, "SMS not sent: $phoneNumber is in the exclusion list.")
            HistoryManager.addHistoryEntry(context, "Appel ignoré (Exclusion) : $phoneNumber")
            return
        }

        val forceSend = sharedPreferences.getBoolean(KEY_FORCE_SEND, false)
        val delayMinutes = sharedPreferences.getInt(EditMessagesActivity.KEY_DELAY_MINUTES, 5)
        
        if (!forceSend && hasRecentSms(context, phoneNumber, delayMinutes)) {
            Logger.log(context, "SMS not sent: SMS already sent to $phoneNumber in the last $delayMinutes min.")
            return
        }

        val unknownOnly = sharedPreferences.getBoolean(KEY_UNKNOWN_ONLY, false)
        if (unknownOnly && !isUnknownNumber(context, phoneNumber)) {
            Logger.log(context, "SMS not sent: call is from a known number and option is set to unknown only.")
            return
        }

        // --- INTELLIGENT MESSAGE SELECTION ---
        val isVacation = sharedPreferences.getBoolean(KEY_VACATION_MODE, false)
        val startHour = sharedPreferences.getInt(EditMessagesActivity.KEY_START_HOUR, 8)
        val endHour = sharedPreferences.getInt(EditMessagesActivity.KEY_END_HOUR, 19)
        val workDays = sharedPreferences.getString(EditMessagesActivity.KEY_WORK_DAYS, "2,3,4,5,6") ?: "2,3,4,5,6"
        
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentDay = now.get(Calendar.DAY_OF_WEEK).toString()

        val isWorkDay = workDays.split(",").contains(currentDay)
        val isWorkHour = currentHour in startHour until endHour

        val message: String? = when {
            isVacation -> {
                Logger.log(context, "Mode: Vacation active.")
                sharedPreferences.getString(EditMessagesActivity.KEY_MSG_VACATION, "")
            }
            isWorkDay && isWorkHour -> {
                Logger.log(context, "Mode: Working Hours.")
                sharedPreferences.getString(EditMessagesActivity.KEY_MSG_WORK, "")
            }
            else -> {
                Logger.log(context, "Mode: Off Hours / Off Day.")
                sharedPreferences.getString(EditMessagesActivity.KEY_MSG_OFF, "")
            }
        }

        if (message.isNullOrEmpty()) {
            Logger.log(context, "SMS not sent: selected message is empty or not configured.")
            HistoryManager.addHistoryEntry(context, "Échec: Message (type adapté) non configuré")
            return
        }

        Logger.log(context, "Message to send: '$message'")
        sendSms(context, phoneNumber, message)
    }

    private fun isMobileNumber(context: Context, phoneNumber: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cleanNumber = phoneNumber.replace(" ", "")

        val allowFR = prefs.getBoolean(EditMessagesActivity.KEY_COUNTRY_FR, true)
        val allowBE = prefs.getBoolean(EditMessagesActivity.KEY_COUNTRY_BE, false)
        val allowLU = prefs.getBoolean(EditMessagesActivity.KEY_COUNTRY_LU, false)
        val allowDE = prefs.getBoolean(EditMessagesActivity.KEY_COUNTRY_DE, false)
        val allowCH = prefs.getBoolean(EditMessagesActivity.KEY_COUNTRY_CH, false)
        val allowES = prefs.getBoolean(EditMessagesActivity.KEY_COUNTRY_ES, false)

        if (allowFR && (cleanNumber.startsWith("+336") || cleanNumber.startsWith("+337") || cleanNumber.startsWith("06") || cleanNumber.startsWith("07"))) return true
        if (allowBE && (cleanNumber.startsWith("+324") || cleanNumber.startsWith("00324"))) return true
        if (allowLU && (cleanNumber.startsWith("+3526") || cleanNumber.startsWith("003526"))) return true
        if (allowDE && (cleanNumber.startsWith("+491") || cleanNumber.startsWith("00491"))) return true
        if (allowCH && (cleanNumber.startsWith("+417") || cleanNumber.startsWith("00417"))) return true
        if (allowES && (cleanNumber.startsWith("+346") || cleanNumber.startsWith("00346") || cleanNumber.startsWith("+347") || cleanNumber.startsWith("00347"))) return true

        return false
    }
    
    private fun isInWhitelist(number: String, whitelist: String): Boolean {
        if (whitelist.isBlank()) return false
        val cleanNumber = number.replace(" ", "").replace("+33", "0")
        return whitelist.split(",").any { 
            val entry = it.trim().replace(" ", "").replace("+33", "0")
            entry.isNotEmpty() && (cleanNumber == entry || entry.endsWith(cleanNumber.takeLast(9)))
        }
    }

    private fun hasRecentSms(context: Context, phoneNumber: String, delayMinutes: Int): Boolean {
        val history = HistoryManager.getHistory(context)
        val limitTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(delayMinutes.toLong())

        return history.any { log ->
            val cleanLog = log.substringAfter(" - ")
            val sentToThisNumber = cleanLog.contains("envoyée à $phoneNumber") || cleanLog.contains("manuel à $phoneNumber")

            if (sentToThisNumber) {
                try {
                    val logTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(log.substringBefore(" - "))?.time ?: 0
                    return@any logTimestamp > limitTime
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
