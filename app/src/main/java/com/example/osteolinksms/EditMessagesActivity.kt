package com.example.osteolinksms

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class EditMessagesActivity : AppCompatActivity() {

    private lateinit var quentinMessageEditText: EditText
    private lateinit var lauraMessageEditText: EditText

    companion object {
        const val KEY_QUENTIN_MESSAGE = "quentinMessage"
        const val KEY_LAURA_MESSAGE = "lauraMessage"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_messages)

        quentinMessageEditText = findViewById(R.id.quentinMessageEditText)
        lauraMessageEditText = findViewById(R.id.lauraMessageEditText)

        val sharedPreferences = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)

        quentinMessageEditText.setText(sharedPreferences.getString(KEY_QUENTIN_MESSAGE, ""))
        lauraMessageEditText.setText(sharedPreferences.getString(KEY_LAURA_MESSAGE, ""))

        findViewById<Button>(R.id.saveMessagesButton).setOnClickListener {
            sharedPreferences.edit()
                .putString(KEY_QUENTIN_MESSAGE, quentinMessageEditText.text.toString())
                .putString(KEY_LAURA_MESSAGE, lauraMessageEditText.text.toString())
                .apply()
            Toast.makeText(this, "Messages enregistrés", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
