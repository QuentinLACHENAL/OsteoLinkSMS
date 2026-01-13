package com.example.osteolinksms

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object RegistrationManager {

    private const val KEY_IS_REGISTERED = "is_registered_v1"

    // ===================================================================================
    // CONFIGURATION GOOGLE FORM (A REMPLACER PAR LES VOTRES)
    // ===================================================================================
    
    // URL du "action" du formulaire (se termine par /formResponse)
    private const val FORM_URL = "https://docs.google.com/forms/d/e/1FAIpQLSdGrQpX5D7RF7TKMSE5ZrRxUZ291fZvbfq1QQrDvaiKKaKxDQ/formResponse"
    
    // ID du champ "Nom" (inspecter le formulaire pour trouver entry.XXXXX)
    private const val ENTRY_NAME_ID = "entry.924128009"
    
    // ID du champ "Email"
    private const val ENTRY_EMAIL_ID = "entry.969235305"

    // ===================================================================================

    fun checkRegistration(context: Context) {
        val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        
        // Si déjà enregistré, on ne fait rien
        if (prefs.getBoolean(KEY_IS_REGISTERED, false)) {
            return
        }

        showRegistrationDialog(context, prefs)
    }

    private fun showRegistrationDialog(context: Context, prefs: SharedPreferences) {
        val builder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_registration, null)

        val nameInput = view.findViewById<TextInputEditText>(R.id.regNameInput)
        val emailInput = view.findViewById<TextInputEditText>(R.id.regEmailInput)
        val submitButton = view.findViewById<Button>(R.id.regSubmitButton)
        val laterButton = view.findViewById<TextView>(R.id.regLaterButton)

        // Pré-remplir le nom si connu
        val savedName = prefs.getString(MainActivity.KEY_PRACTITIONER_NAME, "")
        if (!savedName.isNullOrEmpty()) {
            nameInput.setText(savedName)
        }

        builder.setView(view)
        builder.setCancelable(false) // Oblige à choisir

        val dialog = builder.create()

        submitButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()

            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(context, "Merci de remplir les deux champs.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(context, "Email invalide.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Sauvegarde locale du nom si pas encore fait
            if (savedName.isNullOrEmpty()) {
                prefs.edit().putString(MainActivity.KEY_PRACTITIONER_NAME, name).apply()
            }

            // Envoi des données
            sendToGoogleForm(context, name, email) { success ->
                if (success) {
                    prefs.edit().putBoolean(KEY_IS_REGISTERED, true).apply()
                    Toast.makeText(context, "Enregistrement réussi. Merci !", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(context, "Erreur de connexion. Réessayez plus tard.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        laterButton.setOnClickListener {
            // On ne marque PAS comme enregistré, ça reviendra au prochain lancement
            // Mais on laisse l'utilisateur accéder à l'appli
            Toast.makeText(context, "Vous pourrez vous enregistrer plus tard.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun sendToGoogleForm(context: Context, name: String, email: String, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            var success = false
            try {
                val url = URL(FORM_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.doInput = true

                // Construction des données POST
                val postData = StringBuilder()
                postData.append(URLEncoder.encode(ENTRY_NAME_ID, "UTF-8"))
                postData.append("=")
                postData.append(URLEncoder.encode(name, "UTF-8"))
                postData.append("&")
                postData.append(URLEncoder.encode(ENTRY_EMAIL_ID, "UTF-8"))
                postData.append("=")
                postData.append(URLEncoder.encode(email, "UTF-8"))

                val os = conn.outputStream
                val writer = os.writer()
                writer.write(postData.toString())
                writer.flush()
                writer.close()
                os.close()

                val responseCode = conn.responseCode
                // Google Forms renvoie souvent 200 OK
                if (responseCode == 200) {
                    success = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            withContext(Dispatchers.Main) {
                callback(success)
            }
        }
    }
}
