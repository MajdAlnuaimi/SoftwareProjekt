package com.example.network

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

/**
 * Hilfsklasse für WLAN-bezogene Funktionen.
 *
 * Diese Klasse stellt Methoden bereit, um Informationen
 * über die aktuelle WLAN-Verbindung zu erhalten.
 *
 * Aktuell implementiert:
 * - Ermittlung der aktuellen WLAN-SSID
 *
 * Die Implementierung berücksichtigt verschiedene
 * Android-Versionen (API < 31 und API ≥ 31).
 */
object WifiUtils {

    /**
     * Liefert die aktuelle WLAN-SSID oder null.
     *
     * Unterschiedliche Implementierung je nach Android-Version:
     *
     * Android 12+ (API 31+):
     * - Nutzung von ConnectivityManager
     * - Zugriff über NetworkCapabilities und WifiInfo
     *
     * Android 11 und älter:
     * - Fallback über WifiManager.connectionInfo
     *
     * Benötigte Berechtigungen:
     * - ACCESS_FINE_LOCATION
     * - ACCESS_WIFI_STATE
     */
    @RequiresPermission(
        allOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE
        ]
    )
    suspend fun getCurrentSsid(context: Context): String? =
        suspendCancellableCoroutine { cont ->

            // Android 12+ → neue API verwenden
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                getCurrentSsidApi31Plus(context, cont)

            } else {

                // Android 11 und älter → klassischer Zugriff
                @Suppress("DEPRECATION")
                run {

                    val wm =
                        context.applicationContext
                            .getSystemService(WifiManager::class.java)

                    val raw = wm?.connectionInfo?.ssid

                    val ssid = raw
                        ?.takeIf { it != "<unknown ssid>" }
                        ?.replace("\"", "")

                    cont.resume(ssid)
                }
            }
        }

    /**
     * Implementierung für Android 12+ (API 31+).
     *
     * Hier muss der Zugriff auf WLAN-Daten über
     * ConnectivityManager und NetworkCapabilities erfolgen.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(
        allOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE
        ]
    )
    private fun getCurrentSsidApi31Plus(
        context: Context,
        cont: Continuation<String?>
    ) {

        val cm =
            context.getSystemService(ConnectivityManager::class.java)

        if (cm == null) {
            cont.resume(null)
            return
        }

        /**
         * Versuch 1:
         * SSID direkt über das aktuell aktive Netzwerk auslesen.
         */
        cm.activeNetwork?.let { net ->

            val caps = cm.getNetworkCapabilities(net)

            val first =
                (caps?.transportInfo as? WifiInfo)
                    ?.ssid
                    ?.takeIf { it != WifiManager.UNKNOWN_SSID }
                    ?.replace("\"", "")

            if (first != null) {
                cont.resume(first)
                return
            }
        }

        /**
         * Versuch 2:
         * Netzwerk-Callback registrieren,
         * um SSID zu erhalten sobald sich Netzwerkdaten ändern.
         */
        var resumed = false

        val cb = object : ConnectivityManager.NetworkCallback(
            FLAG_INCLUDE_LOCATION_INFO
        ) {

            override fun onCapabilitiesChanged(
                n: Network,
                nc: NetworkCapabilities
            ) {

                val s =
                    (nc.transportInfo as? WifiInfo)
                        ?.ssid
                        ?.takeIf { it != WifiManager.UNKNOWN_SSID }
                        ?.replace("\"", "")

                if (s != null && !resumed) {

                    resumed = true

                    cont.resume(s)

                    cm.unregisterNetworkCallback(this)
                }
            }

            override fun onLost(network: Network) {

                if (!resumed) {

                    resumed = true

                    cont.resume(null)
                }

                cm.unregisterNetworkCallback(this)
            }
        }

        /**
         * Callback registrieren
         */
        cm.registerDefaultNetworkCallback(cb)

        /**
         * Aufräumen wenn Coroutine abgebrochen wird
         */
        (cont as? kotlinx.coroutines.CancellableContinuation)
            ?.invokeOnCancellation {

                try {
                    cm.unregisterNetworkCallback(cb)
                } catch (_: Exception) {
                }
            }
    }
}