package com.example.osteolinksms

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val permissions = arrayOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.SEND_SMS,
        Manifest.permission.PROCESS_OUTGOING_CALLS,
        Manifest.permission.READ_CONTACTS
    )

    private val PERMISSIONS_REQUEST_CODE = 123

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var selectionTextView: TextView
    private lateinit var unknownOnlyCheckBox: CheckBox

    companion object {
        const val PREFS_NAME = "OsteoLinkPrefs"
        const val KEY_SELECTED_MESSAGE = "selectedMessage"
        const val KEY_SELECTED_PRACTITIONER = "selectedPractitioner"
        const val KEY_UNKNOWN_ONLY = "unknownOnly"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        selectionTextView = findViewById(R.id.selectionTextView)
        unknownOnlyCheckBox = findViewById(R.id.unknownOnlyCheckBox)

        if (!hasPermissions()) {
            requestPermissions()
        }

        findViewById<Button>(R.id.quentinButton).setOnClickListener {
            selectPractitioner(
                "Quentin Lachenal",
                "Bonjour, je suis actuellement en consultation. Vous pouvez me laisser un message vocal/SMS, ou consulter Doctolib: https://www.doctolib.fr/osteopathe/longeville-les-metz/quentin-lachenal/booking/"
            )
        }

        findViewById<Button>(R.id.lauraButton).setOnClickListener {
            selectPractitioner(
                "Laura Hugues",
                "Bonjour, je suis actuellement en consultation. Vous pouvez me laisser un message vocal/SMS, ou consulter Doctolib: https://www.doctolib.fr/osteopathe/longeville-les-metz/laura-hugues/booking/"
            )
        }

        unknownOnlyCheckBox.isChecked = sharedPreferences.getBoolean(KEY_UNKNOWN_ONLY, false)
        unknownOnlyCheckBox.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(KEY_UNKNOWN_ONLY, isChecked).apply()
        }

        updateSelectionText()
    }

    private fun selectPractitioner(name: String, message: String) {
        sharedPreferences.edit()
            .putString(KEY_SELECTED_PRACTITIONER, name)
            .putString(KEY_SELECTED_MESSAGE, message)
            .apply()
        updateSelectionText()
    }

    private fun updateSelectionText() {
        val selectedPractitioner = sharedPreferences.getString(KEY_SELECTED_PRACTITIONER, "Aucun")
        selectionTextView.text = "Sélection actuelle : $selectedPractitioner"
    }


    private fun hasPermissions(): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permissions granted
            } else {
                // Permissions denied. You can show a message to the user.
            }
        }
    }
}
