package com.example.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.network.WifiUtils
import com.example.homeNavigation.AppNavigator
import com.example.ui.screens.wifi.WrongWifiScreen
import com.example.state.WifiGuardState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * MainRoot ist der zentrale Einstiegspunkt der App-Logik.
 *
 * Diese Composable entscheidet:
 * - ob das richtige WLAN verwendet wird
 * - ob die App gestartet werden darf
 * - oder ob ein Fehlerbildschirm angezeigt wird
 *
 * Ablauf:
 * 1. erlaubte WLAN-Netze aus Firebase laden
 * 2. aktuelles WLAN prüfen
 * 3. wenn WLAN erlaubt → AppContent starten
 * 4. wenn WLAN falsch → WrongWifiScreen anzeigen
 */
@Composable
fun MainRoot(
    activity: MainActivity,
    connectivityManager: ConnectivityManager,
    requestLocationPermission: () -> Unit,
    openAppSettings: () -> Unit
) {

    // CoroutineScope für asynchrone Aktionen
    val scope = rememberCoroutineScope()

    // Navigation der App
    val navigator = remember { AppNavigator() }

    // Zustand der WLAN-Prüfung
    var wifiState by remember { mutableStateOf(WifiGuardState()) }

    /**
     * Prüft ob Standortberechtigung vorhanden ist.
     * Diese ist notwendig um die SSID des WLANs zu lesen.
     */
    val hasLocationPermission = ContextCompat.checkSelfPermission(
        activity,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    /**
     * Prüft ob Android die Berechtigung erneut anfragen darf.
     */
    val canAskLocationAgain = activity.shouldShowRequestPermissionRationale(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    /**
     * Lädt erlaubte WLAN SSIDs aus Firestore.
     */
    suspend fun loadAllowedSsids() {

        val list = FirebaseFirestore.getInstance()
            .collection("config")
            .document("wifi")
            .get()
            .await()
            .get("allowedSsids") as? List<*> ?: emptyList<Any>()

        wifiState = wifiState.copy(
            allowedSet = list.filterIsInstance<String>().toSet(),
            configLoaded = true
        )
    }

    /**
     * Prüft ob aktuell eine WLAN-Verbindung besteht.
     */
    fun isCurrentlyWifiTransport(): Boolean {

        val net = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(net) ?: return false

        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Führt die eigentliche WLAN-Prüfung durch.
     */
    suspend fun checkWifiNow() {

        if (!wifiState.configLoaded) {
            wifiState = wifiState.copy(wifiAllowed = null)
            return
        }

        // wenn keine Berechtigung oder kein WLAN
        if (!hasLocationPermission || !isCurrentlyWifiTransport()) {

            wifiState = wifiState.copy(
                currentSsid = null,
                wifiAllowed = false
            )

            return
        }

        try {

            // aktuelle SSID auslesen
            val cleaned = WifiUtils.getCurrentSsid(activity)?.replace("\"", "")

            wifiState = wifiState.copy(
                currentSsid = cleaned,
                wifiAllowed = cleaned != null && wifiState.allowedSet.contains(cleaned)
            )

        } catch (_: SecurityException) {

            wifiState = wifiState.copy(
                currentSsid = null,
                wifiAllowed = false
            )
        }
    }

    /**
     * Startet eine neue WLAN-Prüfung.
     */
    fun retryWifiCheck() {

        if (wifiState.isRetrying) return

        wifiState = wifiState.copy(isRetrying = true)

        scope.launch {

            try {

                try {
                    loadAllowedSsids()
                } catch (_: Exception) {
                }

                checkWifiNow()

            } finally {

                wifiState = wifiState.copy(isRetrying = false)
            }
        }
    }

    /**
     * Wird beim Start der App ausgeführt.
     */
    LaunchedEffect(Unit) {

        try {

            loadAllowedSsids()
            checkWifiNow()

        } catch (_: Exception) {

            wifiState = wifiState.copy(wifiAllowed = false)
        }
    }

    /**
     * Listener für Netzwerkänderungen (WLAN Wechsel).
     */
    DisposableEffect(Unit) {

        val callback = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {

                scope.launch {
                    delay(250)
                    checkWifiNow()
                }
            }

            override fun onLost(network: Network) {

                scope.launch {
                    checkWifiNow()
                }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {

                scope.launch {
                    delay(250)
                    checkWifiNow()
                }
            }
        }

        connectivityManager.registerDefaultNetworkCallback(callback)

        onDispose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    /**
     * UI-Logik abhängig vom WLAN Status
     */
    when (wifiState.wifiAllowed) {

        // Ladezustand
        null -> FullScreenLoading()

        // falsches WLAN
        false -> WrongWifiScreen(

            detectedSsid = wifiState.currentSsid,

            hasLocationPermission = hasLocationPermission,

            isRetrying = wifiState.isRetrying,

            onRetry = ::retryWifiCheck,

            onOpenWifiSettings = {
                activity.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            },

            onRequestLocationPermission = {

                when {

                    hasLocationPermission ->
                        retryWifiCheck()

                    !canAskLocationAgain ->
                        openAppSettings()

                    else ->
                        requestLocationPermission()
                }
            }
        )

        // korrektes WLAN → App starten
        true -> AppContent(
            activity = activity,
            navigator = navigator
        )
    }
}

/**
 * Vollbild-Ladeanzeige.
 */
@Composable
private fun FullScreenLoading() {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}