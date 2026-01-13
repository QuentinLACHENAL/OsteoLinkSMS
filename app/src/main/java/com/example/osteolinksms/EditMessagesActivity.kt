package com.example.osteolinksms

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class EditMessagesActivity : AppCompatActivity() {

    private lateinit var practitionerNameEditText: EditText
    private lateinit var doctolibIdEditText: EditText
    private lateinit var startHourEditText: EditText
    private lateinit var endHourEditText: EditText
    private lateinit var delayEditText: EditText
    private lateinit var whitelistEditText: EditText
    private lateinit var simRadioGroup: RadioGroup
    private lateinit var msgWorkEditText: EditText
    private lateinit var msgOffEditText: EditText
    private lateinit var msgVacationEditText: EditText

    private lateinit var dayChecks: List<CheckBox>

    companion object {
        const val KEY_DOCTOLIB_ID = "doctolibId"
        const val KEY_START_HOUR = "startHour"
        const val KEY_END_HOUR = "endHour"
        const val KEY_DELAY_MINUTES = "delayMinutes"
        const val KEY_WHITELIST = "whitelist"
        const val KEY_SIM_SLOT = "simSlot" // -1 = Default, 0 = Slot 1, 1 = Slot 2
        const val KEY_WORK_DAYS = "workDays" // Stored as "1,2,3,4,5" (Calendar.MONDAY is 2)

        const val KEY_COUNTRY_FR = "countryFR"
        const val KEY_COUNTRY_BE = "countryBE"
        const val KEY_COUNTRY_LU = "countryLU"
        const val KEY_COUNTRY_DE = "countryDE"
        const val KEY_COUNTRY_CH = "countryCH"
        const val KEY_COUNTRY_ES = "countryES"

        const val KEY_INCLUDE_LINK = "includeBookingLink"

        const val KEY_MSG_WORK = "msgWork"
        const val KEY_MSG_OFF = "msgOff"
        const val KEY_MSG_VACATION = "msgVacation"
        
        const val KEY_NOTIFICATIONS_SMS = "notificationsSmsEnabled"
    }

    private lateinit var checkCountryFR: CheckBox
    private lateinit var checkCountryBE: CheckBox
    private lateinit var checkCountryLU: CheckBox
    private lateinit var checkCountryDE: CheckBox
    private lateinit var checkCountryCH: CheckBox
    private lateinit var checkCountryES: CheckBox

    private lateinit var includeBookingLinkCheckBox: CheckBox
    private lateinit var notificationsEnabledCheckBox: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_messages)

        bindViews()
        loadSettings()
        setupListeners()
    }

    private fun bindViews() {
        practitionerNameEditText = findViewById(R.id.practitionerNameEditText)
        doctolibIdEditText = findViewById(R.id.doctolibIdEditText)
        includeBookingLinkCheckBox = findViewById(R.id.includeBookingLinkCheckBox)
        notificationsEnabledCheckBox = findViewById(R.id.notificationsEnabledCheckBox)

        startHourEditText = findViewById(R.id.startHourEditText)
        endHourEditText = findViewById(R.id.endHourEditText)
        delayEditText = findViewById(R.id.delayEditText)
        whitelistEditText = findViewById(R.id.whitelistEditText)
        simRadioGroup = findViewById(R.id.simRadioGroup)

        checkCountryFR = findViewById(R.id.checkCountryFR)
        checkCountryBE = findViewById(R.id.checkCountryBE)
        checkCountryLU = findViewById(R.id.checkCountryLU)
        checkCountryDE = findViewById(R.id.checkCountryDE)
        checkCountryCH = findViewById(R.id.checkCountryCH)
        checkCountryES = findViewById(R.id.checkCountryES)

        msgWorkEditText = findViewById(R.id.msgWorkEditText)
        msgOffEditText = findViewById(R.id.msgOffEditText)
        msgVacationEditText = findViewById(R.id.msgVacationEditText)

        dayChecks = listOf(
            findViewById(R.id.checkMon), findViewById(R.id.checkTue),
            findViewById(R.id.checkWed), findViewById(R.id.checkThu),
            findViewById(R.id.checkFri), findViewById(R.id.checkSat),
            findViewById(R.id.checkSun)
        )
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)

        practitionerNameEditText.setText(prefs.getString(MainActivity.KEY_PRACTITIONER_NAME, ""))
        doctolibIdEditText.setText(prefs.getString(KEY_DOCTOLIB_ID, ""))
        includeBookingLinkCheckBox.isChecked = prefs.getBoolean(KEY_INCLUDE_LINK, true)
        notificationsEnabledCheckBox.isChecked = prefs.getBoolean(KEY_NOTIFICATIONS_SMS, true)

        startHourEditText.setText(prefs.getInt(KEY_START_HOUR, 8).toString())
        endHourEditText.setText(prefs.getInt(KEY_END_HOUR, 19).toString())
        delayEditText.setText(prefs.getInt(KEY_DELAY_MINUTES, 5).toString())
        whitelistEditText.setText(prefs.getString(KEY_WHITELIST, ""))

        val simSlot = prefs.getInt(KEY_SIM_SLOT, -1)
        when (simSlot) {
            0 -> simRadioGroup.check(R.id.sim1Radio)
            1 -> simRadioGroup.check(R.id.sim2Radio)
            else -> simRadioGroup.check(R.id.simDefaultRadio)
        }

        checkCountryFR.isChecked = prefs.getBoolean(KEY_COUNTRY_FR, true)
        checkCountryBE.isChecked = prefs.getBoolean(KEY_COUNTRY_BE, false)
        checkCountryLU.isChecked = prefs.getBoolean(KEY_COUNTRY_LU, false)
        checkCountryDE.isChecked = prefs.getBoolean(KEY_COUNTRY_DE, false)
        checkCountryCH.isChecked = prefs.getBoolean(KEY_COUNTRY_CH, false)
        checkCountryES.isChecked = prefs.getBoolean(KEY_COUNTRY_ES, false)

        val workDays = prefs.getString(KEY_WORK_DAYS, "2,3,4,5,6")?.split(",") ?: listOf(
            "2",
            "3",
            "4",
            "5",
            "6"
        )
        val calendarDays =
            listOf("2", "3", "4", "5", "6", "7", "1") // Mon, Tue, Wed, Thu, Fri, Sat, Sun
        dayChecks.forEachIndexed { index, checkBox ->
            checkBox.isChecked = workDays.contains(calendarDays[index])
        }

        msgWorkEditText.setText(prefs.getString(KEY_MSG_WORK, ""))
        msgOffEditText.setText(prefs.getString(KEY_MSG_OFF, ""))
        msgVacationEditText.setText(prefs.getString(KEY_MSG_VACATION, ""))
    }

    private fun setupListeners() {
        findViewById<Button>(R.id.saveSettingsButton).setOnClickListener {
            saveSettings()
        }

        findViewById<Button>(R.id.generateTemplatesButton).setOnClickListener {
            generateTemplates()
        }
    }

    private fun saveSettings() {
        val startHour = startHourEditText.text.toString().toIntOrNull() ?: 8
        val endHour = endHourEditText.text.toString().toIntOrNull() ?: 19
        val delay = delayEditText.text.toString().toIntOrNull() ?: 5

        val simSlot = when (simRadioGroup.checkedRadioButtonId) {
            R.id.sim1Radio -> 0
            R.id.sim2Radio -> 1
            else -> -1
        }

        val calendarDays = listOf("2", "3", "4", "5", "6", "7", "1")
        val selectedDays = dayChecks.mapIndexedNotNull { index, checkBox ->
            if (checkBox.isChecked) calendarDays[index] else null
        }.joinToString(",")

        getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(MainActivity.KEY_PRACTITIONER_NAME, practitionerNameEditText.text.toString())
            .putString(KEY_DOCTOLIB_ID, doctolibIdEditText.text.toString())
            .putBoolean(KEY_INCLUDE_LINK, includeBookingLinkCheckBox.isChecked)
            .putBoolean(KEY_NOTIFICATIONS_SMS, notificationsEnabledCheckBox.isChecked)
            .putInt(KEY_START_HOUR, startHour)
            .putInt(KEY_END_HOUR, endHour)
            .putInt(KEY_DELAY_MINUTES, delay)
            .putString(KEY_WHITELIST, whitelistEditText.text.toString())
            .putInt(KEY_SIM_SLOT, simSlot)
            .putBoolean(KEY_COUNTRY_FR, checkCountryFR.isChecked)
            .putBoolean(KEY_COUNTRY_BE, checkCountryBE.isChecked)
            .putBoolean(KEY_COUNTRY_LU, checkCountryLU.isChecked)
            .putBoolean(KEY_COUNTRY_DE, checkCountryDE.isChecked)
            .putBoolean(KEY_COUNTRY_CH, checkCountryCH.isChecked)
            .putBoolean(KEY_COUNTRY_ES, checkCountryES.isChecked)
            .putString(KEY_WORK_DAYS, selectedDays)
            .putString(KEY_MSG_WORK, msgWorkEditText.text.toString())
            .putString(KEY_MSG_OFF, msgOffEditText.text.toString())
            .putString(KEY_MSG_VACATION, msgVacationEditText.text.toString())
            .apply()

        Toast.makeText(this, "Réglages enregistrés !", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun generateTemplates() {
        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        // Use the text from the input field directly
        val name = practitionerNameEditText.text.toString()

        val actions: String

        if (includeBookingLinkCheckBox.isChecked) {
            val input = doctolibIdEditText.text.toString().trim()
            val docUrl = when {
                input.isEmpty() -> "https://www.doctolib.fr/"
                input.startsWith("http") -> input
                else -> "https://www.doctolib.fr/praticien/$input"
            }

            val linkSuffix = if (docUrl.contains("doctolib", true)) {
                getString(R.string.suffix_doctolib, docUrl)
            } else {
                getString(R.string.suffix_generic, docUrl)
            }
            actions = getString(R.string.actions_with_link, linkSuffix)
        } else {
            actions = getString(R.string.actions_no_link)
        }

        val greeting = if (!name.isNullOrEmpty()) {
            getString(R.string.cabinet_prefix, name)
        } else {
            ""
        }

        val prefixWork = greeting + getString(R.string.prefix_work)
        val prefixOff = greeting + getString(R.string.prefix_off)
        val prefixVacation = greeting + getString(R.string.prefix_vacation)

        msgWorkEditText.setText("$prefixWork$actions")
        msgOffEditText.setText("$prefixOff$actions")
        msgVacationEditText.setText("$prefixVacation$actions")

        Toast.makeText(this, "Modèles générés (pensez à sauvegarder)", Toast.LENGTH_SHORT).show()
    }
}