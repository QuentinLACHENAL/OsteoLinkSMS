package com.example.osteolinksms

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
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
    private lateinit var vacationModeSwitch: SwitchCompat
    private lateinit var unknownOnlyCheckBox: CheckBox
    private lateinit var forceSendCheckBox: CheckBox
    private lateinit var phoneNumberEditText: EditText
    private lateinit var testButtonsLayout: LinearLayout
    private lateinit var testerModeButton: Button
    private lateinit var smsSentTodayTextView: TextView
    private lateinit var checkPermissionsButton: Button

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
        vacationModeSwitch = findViewById(R.id.vacationModeSwitch)
        unknownOnlyCheckBox = findViewById(R.id.unknownOnlyCheckBox)
        forceSendCheckBox = findViewById(R.id.forceSendCheckBox)
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        testButtonsLayout = findViewById(R.id.testButtonsLayout)
        testerModeButton = findViewById(R.id.testerModeButton)
        smsSentTodayTextView = findViewById(R.id.smsSentTodayTextView)
        checkPermissionsButton = findViewById(R.id.checkPermissionsButton)

        

        NotificationManager.createNotificationChannel(this)



        if (!hasPermissions()) {

            showPermissionRationale()

        }



                setupUI()



        



                        // --- Check Registration (Collect Email) ---



        



                        RegistrationManager.checkRegistration(this)



        



                        



        



                        // --- Check for Remote News/Updates ---



        



                        NewsChecker.checkNews(this) { updateUrl ->



        



                            val btnUpdate = findViewById<Button>(R.id.btnUpdateAvailable)



        



                            btnUpdate.visibility = android.view.View.VISIBLE



        



                            btnUpdate.setOnClickListener {



        



                                try {



        



                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))



        



                                    startActivity(intent)



        



                                } catch (e: Exception) {



        



                                    Toast.makeText(this, "Impossible d'ouvrir le lien.", Toast.LENGTH_SHORT).show()



        



                                }



        



                            }



        



                        }



        



                



        



                        setupListeners()

    }
    private fun showPermissionRationale() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Autorisations Requises")
            .setMessage("Pour fonctionner, OsteoLinkSMS a besoin de :\n\n" +
                    "- 📞 Journal d'appels : Pour détecter les patients qui appellent.\n" +
                    "- 📩 SMS : Pour envoyer la réponse automatique.\n" +
                    "- 👥 Contacts : Pour identifier les numéros connus (si option activée).\n" +
                    "- 🔔 Notifications : Pour vous informer de l'activité.\n\n" +
                    "Vos données restent 100% locales.")
            .setPositiveButton("Compris") { _, _ ->
                requestPermissions()
            }
            .setCancelable(false)
            .show()
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
        val isAppEnabled = sharedPreferences.getBoolean(KEY_APP_ENABLED, true)
        masterSwitch.isChecked = isAppEnabled
        updateMasterSwitchText(isAppEnabled)

        // Load checkboxes (Default false)
        vacationModeSwitch.isChecked = sharedPreferences.getBoolean(KEY_VACATION_MODE, false)
        unknownOnlyCheckBox.isChecked = sharedPreferences.getBoolean(KEY_UNKNOWN_ONLY, false)
        forceSendCheckBox.isChecked = sharedPreferences.getBoolean(KEY_FORCE_SEND, false)
        
        // Hide test buttons initially
        testButtonsLayout.isVisible = false
        forceSendCheckBox.isVisible = false
        checkPermissionsButton.isVisible = true
        updateTesterModeButtonText(false)
    }

    private fun setupListeners() {
        masterSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit { putBoolean(KEY_APP_ENABLED, isChecked) }
            NotificationManager.updateMonitoringNotification(this, isChecked)
            updateMasterSwitchText(isChecked)
            
            if (isChecked) {
                Toast.makeText(this, "Application Active", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Application Désactivée (Pas de surveillance)", Toast.LENGTH_SHORT).show()
            }
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
                // Turning OFF Tester Mode
                testButtonsLayout.isVisible = false
                forceSendCheckBox.isVisible = false
                checkPermissionsButton.isVisible = true
                updateTesterModeButtonText(false)
            } else {
                // Turning ON Tester Mode
                testButtonsLayout.isVisible = true
                forceSendCheckBox.isVisible = true
                checkPermissionsButton.isVisible = false
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

        checkPermissionsButton.setOnClickListener {
            checkAndNotifyPermissions()
        }

        findViewById<Button>(R.id.tutorialButton).setOnClickListener {
            startActivity(Intent(this, TutorialActivity::class.java))
        }

        findViewById<android.widget.ImageView>(R.id.btnLogoOsteoLink).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://osteolink.fr"))
            startActivity(intent)
        }

        findViewById<TextView>(R.id.btnContactSupport).setOnClickListener {
            // Ask user if they want to include logs
            android.app.AlertDialog.Builder(this)
                .setTitle("Contacter le support")
                .setMessage("Voulez-vous inclure les derniers logs techniques pour aider au débogage ?\n\n(Rassurez-vous : Les numéros de téléphone seront automatiquement masqués dans les logs envoyés).")
                .setPositiveButton("Oui, inclure les logs") { _, _ ->
                    sendSupportEmail(includeLogs = true)
                }
                .setNegativeButton("Non, juste un message") { _, _ ->
                    sendSupportEmail(includeLogs = false)
                }
                .show()
        }
    }

    private fun sendSupportEmail(includeLogs: Boolean) {
        // 1. Get App Version safely
        val pInfo = packageManager.getPackageInfo(packageName, 0)
        val version = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            "${pInfo.versionName} (${pInfo.longVersionCode})"
        } else {
            @Suppress("DEPRECATION")
            "${pInfo.versionName} (${pInfo.versionCode})"
        }

        // 2. Build Info Block
        val sb = StringBuilder()
        sb.append("\n\n\n--- Infos Techniques (Ne pas effacer) ---\n")
        sb.append("App Version: $version\n")
        sb.append("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
        sb.append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")

        // 3. Add Logs if requested
        if (includeLogs) {
            sb.append("\n--- Derniers Logs (Anonymisés) ---\n")
            val allLogs = Logger.getAnonymizedLogs(this)
            // Take last 2000 chars roughly to avoid Intent limit
            val truncatedLogs = if (allLogs.length > 2000) {
                "... " + allLogs.takeLast(2000)
            } else {
                allLogs
            }
            sb.append(truncatedLogs)
        }

        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:contact@osteolink.fr")
            putExtra(Intent.EXTRA_SUBJECT, "Support OsteoLinkSMS ($version)")
            putExtra(Intent.EXTRA_TEXT, "Bonjour,\n\n[Votre message ici]\n$sb")
        }

        try {
            startActivity(Intent.createChooser(emailIntent, "Envoyer un email..."))
        } catch (e: Exception) {
            Toast.makeText(this, "Aucune application de mail trouvée.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateMasterSwitchText(isActive: Boolean) {
        if (isActive) {
            masterSwitch.text = getString(R.string.master_switch_active)
        } else {
            masterSwitch.text = getString(R.string.master_switch_inactive)
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