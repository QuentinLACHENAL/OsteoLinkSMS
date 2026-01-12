package com.example.osteolinksms

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {

    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS
        )
    }

    private val PERMISSIONS_REQUEST_CODE = 123

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var practitionerRadioGroup: RadioGroup
    private lateinit var unknownOnlyCheckBox: CheckBox
    private lateinit var phoneNumberEditText: EditText

    companion object {
        const val PREFS_NAME = "OsteoLinkPrefs"
        const val KEY_SELECTED_PRACTITIONER_ID = "selectedPractitionerId"
        const val KEY_UNKNOWN_ONLY = "unknownOnly"
        const val ID_QUENTIN = 1
        const val ID_LAURA = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        practitionerRadioGroup = findViewById(R.id.practitionerRadioGroup)
        unknownOnlyCheckBox = findViewById(R.id.unknownOnlyCheckBox)
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)

        NotificationManager.createNotificationChannel(this)

        if (!hasPermissions()) {
            requestPermissions()
        }

        setupInitialMessages()
        setupPractitionerSelection()
        setupManualSms()

        findViewById<Button>(R.id.editMessagesButton).setOnClickListener {
            startActivity(Intent(this, EditMessagesActivity::class.java))
        }

        findViewById<Button>(R.id.logsButton).setOnClickListener {
            startActivity(Intent(this, LogsActivity::class.java))
        }

        findViewById<Button>(R.id.historyButton).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        findViewById<Button>(R.id.checkPermissionsButton).setOnClickListener {
            checkAndNotifyPermissions()
        }
    }

    private fun setupInitialMessages() {
        sharedPreferences.edit {
            if (!sharedPreferences.contains(EditMessagesActivity.KEY_QUENTIN_MESSAGE)) {
                putString(EditMessagesActivity.KEY_QUENTIN_MESSAGE, "Bonjour, je suis actuellement en consultation. Vous pouvez me laisser un message vocal/SMS, ou consulter Doctolib: https://www.doctolib.fr/osteopathe/longeville-les-metz/quentin-lachenal/booking/")
            }
            if (!sharedPreferences.contains(EditMessagesActivity.KEY_LAURA_MESSAGE)) {
                putString(EditMessagesActivity.KEY_LAURA_MESSAGE, "Bonjour, je suis actuellement en consultation. Vous pouvez me laisser un message vocal/SMS, ou consulter Doctolib: https://www.doctolib.fr/osteopathe/longeville-les-metz/laura-hugues/booking/")
            }
        }
    }

    private fun setupPractitionerSelection() {
        val selectedId = sharedPreferences.getInt(KEY_SELECTED_PRACTITIONER_ID, ID_QUENTIN)
        if (selectedId == ID_QUENTIN) {
            findViewById<RadioButton>(R.id.quentinRadioButton).isChecked = true
        } else {
            findViewById<RadioButton>(R.id.lauraRadioButton).isChecked = true
        }

        practitionerRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val newId = if (checkedId == R.id.quentinRadioButton) ID_QUENTIN else ID_LAURA
            sharedPreferences.edit { putInt(KEY_SELECTED_PRACTITIONER_ID, newId) }
        }

        unknownOnlyCheckBox.isChecked = sharedPreferences.getBoolean(KEY_UNKNOWN_ONLY, false)
        unknownOnlyCheckBox.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit { putBoolean(KEY_UNKNOWN_ONLY, isChecked) }
        }
    }

    private fun setupManualSms() {
        findViewById<Button>(R.id.sendSmsButton).setOnClickListener {
            val phoneNumber = phoneNumberEditText.text.toString()
            if (phoneNumber.isNotBlank()) {
                sendManualSms(phoneNumber)
            } else {
                Toast.makeText(this, "Veuillez entrer un numéro de téléphone", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendManualSms(phoneNumber: String) {
        val selectedId = sharedPreferences.getInt(KEY_SELECTED_PRACTITIONER_ID, ID_QUENTIN)
        val messageKey = if (selectedId == ID_QUENTIN) EditMessagesActivity.KEY_QUENTIN_MESSAGE else EditMessagesActivity.KEY_LAURA_MESSAGE
        val message = sharedPreferences.getString(messageKey, "")

        if (!message.isNullOrEmpty()) {
            try {
                val smsManager = getSystemService(SmsManager::class.java)
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                Toast.makeText(this, "SMS envoyé au $phoneNumber", Toast.LENGTH_SHORT).show()
                HistoryManager.addNumberToHistory(this, phoneNumber)
                NotificationManager.showSmsSentNotification(this, phoneNumber)
            } catch (e: Exception) {
                Toast.makeText(this, "Erreur lors de l'envoi du SMS", Toast.LENGTH_SHORT).show()
                Logger.log(this, "Manual SMS error: ${e.message}")
            }
        } else {
            Toast.makeText(this, "Le message pour ce praticien est vide.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasPermissions(): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions accordées", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Certaines permissions ont été refusées.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkAndNotifyPermissions() {
        if (hasPermissions()) {
            Toast.makeText(this, "Toutes les autorisations sont accordées.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Certaines autorisations sont manquantes. Lancement de la demande...", Toast.LENGTH_LONG).show()
            requestPermissions()
        }
    }
}
