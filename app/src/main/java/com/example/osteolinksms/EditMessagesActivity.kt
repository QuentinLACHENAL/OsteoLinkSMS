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
        
        const val KEY_MSG_WORK = "msgWork"
        const val KEY_MSG_OFF = "msgOff"
        const val KEY_MSG_VACATION = "msgVacation"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_messages)

        bindViews()
        loadSettings()
        setupListeners()
    }

    private fun bindViews() {
        doctolibIdEditText = findViewById(R.id.doctolibIdEditText)
        startHourEditText = findViewById(R.id.startHourEditText)
        endHourEditText = findViewById(R.id.endHourEditText)
        delayEditText = findViewById(R.id.delayEditText)
        whitelistEditText = findViewById(R.id.whitelistEditText)
        simRadioGroup = findViewById(R.id.simRadioGroup)
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
        
        doctolibIdEditText.setText(prefs.getString(KEY_DOCTOLIB_ID, ""))
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

        val workDays = prefs.getString(KEY_WORK_DAYS, "2,3,4,5,6")?.split(",") ?: listOf("2","3","4","5","6")
        val calendarDays = listOf("2", "3", "4", "5", "6", "7", "1") // Mon, Tue, Wed, Thu, Fri, Sat, Sun
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
            .putString(KEY_DOCTOLIB_ID, doctolibIdEditText.text.toString())
            .putInt(KEY_START_HOUR, startHour)
            .putInt(KEY_END_HOUR, endHour)
            .putInt(KEY_DELAY_MINUTES, delay)
            .putString(KEY_WHITELIST, whitelistEditText.text.toString())
            .putInt(KEY_SIM_SLOT, simSlot)
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
        val name = prefs.getString(MainActivity.KEY_PRACTITIONER_NAME, "Votre Nom")
        val docId = doctolibIdEditText.text.toString().ifEmpty { "votre-id" }
        val docUrl = "https://www.doctolib.fr/osteopathe/longeville-les-metz/$docId/booking/"

        msgWorkEditText.setText(getString(R.string.template_work, name, docUrl))
        msgOffEditText.setText(getString(R.string.template_off, name, docUrl))
        msgVacationEditText.setText(getString(R.string.template_vacation, name, docUrl))
        
        Toast.makeText(this, "Modèles générés (pensez à sauvegarder)", Toast.LENGTH_SHORT).show()
    }
}