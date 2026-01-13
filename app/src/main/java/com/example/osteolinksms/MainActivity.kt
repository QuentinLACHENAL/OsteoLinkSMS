package com.example.osteolinksms

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.isVisible
import java.text.SimpleDateFormat
import java.util.Locale

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
    private lateinit var masterSwitch: SwitchCompat
    private lateinit var practitionerNameEditText: EditText
    private lateinit var vacationModeSwitch: SwitchCompat
    private lateinit var unknownOnlyCheckBox: CheckBox
    private lateinit var forceSendCheckBox: CheckBox
    private lateinit var phoneNumberEditText: EditText
    private lateinit var testButtonsLayout: LinearLayout
    private lateinit var testerModeButton: Button
    private lateinit var smsSentTodayTextView: TextView

    companion object {
        const val PREFS_NAME = "OsteoLinkPrefs"
        const val KEY_APP_ENABLED = "appEnabled"
        const val KEY_PRACTITIONER_NAME = "practitionerName"
        const val KEY_VACATION_MODE = "vacationMode"
        const val KEY_UNKNOWN_ONLY = "unknownOnly"
        const val KEY_FORCE_SEND = "forceSend"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        masterSwitch = findViewById(R.id.masterSwitch)
        practitionerNameEditText = findViewById(R.id.practitionerNameEditText)
        vacationModeSwitch = findViewById(R.id.vacationModeSwitch)
        unknownOnlyCheckBox = findViewById(R.id.unknownOnlyCheckBox)
        forceSendCheckBox = findViewById(R.id.forceSendCheckBox)
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        testButtonsLayout = findViewById(R.id.testButtonsLayout)
        testerModeButton = findViewById(R.id.testerModeButton)
        smsSentTodayTextView = findViewById(R.id.smsSentTodayTextView)

        NotificationManager.createNotificationChannel(this)

        if (!hasPermissions()) {
            requestPermissions()
        }

        setupUI()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updateDashboard()
        // Ensure notification status matches state (in case it was killed or cleared)
        val isAppEnabled = sharedPreferences.getBoolean(KEY_APP_ENABLED, true)
        NotificationManager.updateMonitoringNotification(this, isAppEnabled)
    }

    private fun updateDashboard() {
        val history = HistoryManager.getHistory(this)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
        
        val count = history.count { log ->
            log.startsWith(todayStr) && log.contains("succès")
        }
        
        smsSentTodayTextView.text = getString(R.string.sms_sent_today, count)
    }

    private fun setupUI() {
        // Load saved practitioner name
        val savedName = sharedPreferences.getString(KEY_PRACTITIONER_NAME, "")
        practitionerNameEditText.setText(savedName)

        val isAppEnabled = sharedPreferences.getBoolean(KEY_APP_ENABLED, true)
        masterSwitch.isChecked = isAppEnabled

        // Load checkboxes (Default false)
        vacationModeSwitch.isChecked = sharedPreferences.getBoolean(KEY_VACATION_MODE, false)
        unknownOnlyCheckBox.isChecked = sharedPreferences.getBoolean(KEY_UNKNOWN_ONLY, false)
        forceSendCheckBox.isChecked = sharedPreferences.getBoolean(KEY_FORCE_SEND, false)
        
        // Hide test buttons initially
        testButtonsLayout.isVisible = false
        forceSendCheckBox.isVisible = false
        updateTesterModeButtonText(false)
    }

    private fun setupListeners() {
        masterSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit { putBoolean(KEY_APP_ENABLED, isChecked) }
            NotificationManager.updateMonitoringNotification(this, isChecked)
            
            if (isChecked) {
                Toast.makeText(this, "Application Active", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Application Désactivée (Pas de surveillance)", Toast.LENGTH_SHORT).show()
            }
        }

        // Save Name
        findViewById<Button>(R.id.savePractitionerButton).setOnClickListener {
            val name = practitionerNameEditText.text.toString()
            sharedPreferences.edit { putString(KEY_PRACTITIONER_NAME, name) }
            Toast.makeText(this, "Nom enregistré", Toast.LENGTH_SHORT).show()
        }

        // Switches & Checkboxes
        vacationModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit { putBoolean(KEY_VACATION_MODE, isChecked) }
        }

        unknownOnlyCheckBox.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit { putBoolean(KEY_UNKNOWN_ONLY, isChecked) }
        }

        forceSendCheckBox.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit { putBoolean(KEY_FORCE_SEND, isChecked) }
        }

        // Tester Mode Toggle
        testerModeButton.setOnClickListener {
            val isVisible = testButtonsLayout.isVisible
            if (isVisible) {
                testButtonsLayout.isVisible = false
                forceSendCheckBox.isVisible = false
                updateTesterModeButtonText(false)
            } else {
                testButtonsLayout.isVisible = true
                forceSendCheckBox.isVisible = true
                updateTesterModeButtonText(true)
            }
        }

        // Test Buttons
        findViewById<Button>(R.id.testShortSmsButton).setOnClickListener {
            val phoneNumber = phoneNumberEditText.text.toString()
            if (phoneNumber.isNotBlank()) {
                SmsSender.sendSms(this, phoneNumber, "Ceci est un test court OsteoLink.")
                Toast.makeText(this, "Test court envoyé...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Entrez un numéro pour le test.", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.testLongSmsButton).setOnClickListener {
            val phoneNumber = phoneNumberEditText.text.toString()
            if (phoneNumber.isNotBlank()) {
                val longMsg = "Ceci est un test de message long pour vérifier la compatibilité du système avec les SMS multiparties. Il contient beaucoup de texte pour dépasser la limite des 160 caractères standards imposée par le protocole GSM. 1234567890."
                SmsSender.sendSms(this, phoneNumber, longMsg)
                Toast.makeText(this, "Test long envoyé...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Entrez un numéro pour le test.", Toast.LENGTH_SHORT).show()
            }
        }

        // Manual Send
        findViewById<Button>(R.id.sendSmsButton).setOnClickListener {
            val phoneNumber = phoneNumberEditText.text.toString()
            if (phoneNumber.isNotBlank()) {
                sendManualSms(phoneNumber)
            } else {
                Toast.makeText(this, "Veuillez entrer un numéro de téléphone", Toast.LENGTH_SHORT).show()
            }
        }

        // Navigation
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

    private fun updateTesterModeButtonText(isActive: Boolean) {
        if (isActive) {
            testerModeButton.text = getString(R.string.tester_mode_active)
        } else {
            testerModeButton.text = getString(R.string.tester_mode_button)
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendManualSms(phoneNumber: String) {
        val message = sharedPreferences.getString(EditMessagesActivity.KEY_MSG_WORK, "")

        if (!message.isNullOrEmpty()) {
            SmsSender.sendSms(this, phoneNumber, message)
            Toast.makeText(this, "Tentative d\'envoi du SMS...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Le message 'En Consultation' n'est pas configuré.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, EditMessagesActivity::class.java))
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