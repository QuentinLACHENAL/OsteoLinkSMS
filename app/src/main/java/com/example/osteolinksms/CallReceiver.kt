package com.example.osteolinksms

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.CallLog
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiver : BroadcastReceiver() {

    private var wasRinging = false

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            return
        }

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        Log.d("CallReceiver", "State: $state")

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                wasRinging = true
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                if (wasRinging) {
                    wasRinging = false
                    checkLastCall(context)
                }
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                wasRinging = false
            }
        }
    }

    @SuppressLint("Range")
    private fun checkLastCall(context: Context) {
        val contentResolver = context.contentResolver
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE
        )
        val sortOrder = CallLog.Calls.DATE + " DESC"

        try {
            contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                    val typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE)
                    val dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE)

                    if (numberIndex == -1 || typeIndex == -1 || dateIndex == -1) {
                        Log.e("CallReceiver", "A column was not found in the cursor.")
                        return
                    }

                    val type = cursor.getInt(typeIndex)
                    val callDate = cursor.getLong(dateIndex)
                    val phoneNumber = cursor.getString(numberIndex)

                    val isMissedCall = type == CallLog.Calls.MISSED_TYPE
                    val isRecent = (System.currentTimeMillis() - callDate) < 5000 // 5 seconds

                    Log.d("CallReceiver", "Last call: type=$type, recent=$isRecent")


                    if (isMissedCall && isRecent) {
                        val sharedPreferences = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
                        val unknownOnly = sharedPreferences.getBoolean(MainActivity.KEY_UNKNOWN_ONLY, false)

                        if (unknownOnly) {
                            if (isUnknownNumber(context, phoneNumber)) {
                                sendSms(context, phoneNumber)
                                Log.i("CallReceiver", "Missed call from unknown number $phoneNumber. Sending SMS.")
                            } else {
                                Log.i("CallReceiver", "Missed call from known number $phoneNumber, but option is for unknown only.")
                            }
                        } else {
                            sendSms(context, phoneNumber)
                            Log.i("CallReceiver", "Missed call from $phoneNumber. Sending SMS.")
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("CallReceiver", "SecurityException: Check READ_CALL_LOG permission.", e)
        } catch (e: Exception) {
            Log.e("CallReceiver", "Error checking last call", e)
        }
    }

    private fun isUnknownNumber(context: Context, phoneNumber: String): Boolean {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?"
        val selectionArgs = arrayOf(phoneNumber)

        return try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                !cursor.moveToFirst()
            } ?: true
        } catch (e: Exception) {
            Log.e("CallReceiver", "Error checking if number is unknown", e)
            true // Assume unknown in case of error
        }
    }

    private fun sendSms(context: Context, phoneNumber: String) {
        val sharedPreferences = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val message = sharedPreferences.getString(MainActivity.KEY_SELECTED_MESSAGE, null)

        if (message == null) {
            Log.w("CallReceiver", "No message selected, not sending SMS.")
            return
        }

        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.i("CallReceiver", "SMS sent to $phoneNumber")
        } catch (e: Exception) {
            Log.e("CallReceiver", "Error sending SMS", e)
        }
    }
}
