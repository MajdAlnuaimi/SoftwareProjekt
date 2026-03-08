package com.example.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.ui.theme.Shre2FixTheme
import com.google.firebase.FirebaseApp

/**
 * MainActivity ist der Einstiegspunkt der gesamten Android-App.
 *
 * Aufgaben dieser Klasse:
 * - Prüfen der Standortberechtigung (für WLAN-Scan / Raumerkennung)
 * - Starten der Haupt-UI der App
 * - Initialisieren von Firebase
 * - Laden des Compose-Layouts
 */
class MainActivity : ComponentActivity() {

    /**
     * Launcher zum Anfragen der Standortberechtigung.
     *
     * Wird benötigt, weil Android für WLAN-Scans
     * die ACCESS_FINE_LOCATION Berechtigung verlangt.
     */
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {

            // Sobald der Benutzer eine Entscheidung trifft,
            // wird die Hauptoberfläche gestartet
            startRootUI()
        }

    /**
     * Wird aufgerufen wenn die Activity erstellt wird.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prüfen ob Standortberechtigung bereits vorhanden ist
        if (hasLocationPermission()) {

            // UI sofort starten
            startRootUI()

        } else {

            // Berechtigung beim Benutzer anfragen
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    /**
     * Prüft ob die Standortberechtigung bereits erteilt wurde.
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Öffnet die Android-App-Einstellungen.
     *
     * Wird verwendet, wenn der Benutzer die Berechtigung dauerhaft
     * verweigert hat und sie manuell aktivieren muss.
     */
    private fun openAppSettings() {

        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        startActivity(intent)
    }

    /**
     * Startet die eigentliche App-Oberfläche.
     */
    private fun startRootUI() {

        // Firebase initialisieren
        FirebaseApp.initializeApp(this)

        // Aktiviert Edge-to-Edge Layout (Android 13+ Design)
        enableEdgeToEdge()

        // Zugriff auf Netzwerkstatus
        val connectivityManager =
            getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        /**
         * Jetpack Compose UI starten
         */
        setContent {

            // App Theme laden
            Shre2FixTheme {

                // Root der gesamten UI
                MainRoot(
                    activity = this,

                    connectivityManager = connectivityManager,

                    // Erneut Standortberechtigung anfordern
                    requestLocationPermission = {
                        requestPermissionLauncher.launch(
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    },

                    // Öffnet App-Einstellungen
                    openAppSettings = ::openAppSettings
                )
            }
        }
    }
}