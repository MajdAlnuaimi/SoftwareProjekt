package com.example.fingerprinting

import android.Manifest
import android.content.Context
import android.net.wifi.ScanResult
import androidx.annotation.RequiresPermission
import com.example.scan.WifiScanner
import kotlin.math.roundToInt

/**
 * Repository für die Erstellung von WLAN-Fingerprints.
 *
 * Diese Klasse ist verantwortlich für:
 * - Sammeln von WLAN-Scans
 * - Berechnung eines stabilen WLAN-Fingerprints
 * - Umwandlung der Scan-Ergebnisse in ein Fingerprint-Modell
 *
 * Sie wird im Fingerprinting-System zur Raum-Erkennung verwendet.
 */
object WifiFingerprintRepository {

    /**
     * Berechnet einen Fingerprint aus mehreren WLAN-Scans.
     *
     * Vorgehen:
     * - mehrere Snapshots von WLAN-Scans werden übergeben
     * - für jede BSSID wird der Durchschnitt der Signalstärken berechnet
     *
     * Dadurch wird ein stabilerer Fingerprint erzeugt,
     * weil kurzfristige Signal-Schwankungen reduziert werden.
     */
    fun computeFingerprintFromScans(scans: List<List<ScanResult>>): Map<String, Int> {

        val accum = mutableMapOf<String, MutableList<Int>>()

        for (snapshot in scans) {

            for (r in snapshot) {

                val b = r.BSSID ?: continue

                accum.getOrPut(b) { mutableListOf() }.add(r.level)
            }
        }

        // Durchschnittliche Signalstärke pro Access Point berechnen
        return accum.mapValues { (_, levels) ->
            levels.average().roundToInt()
        }
    }

    /**
     * Erstellt einen Fingerprint aus EINEM WLAN-Scan.
     */
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    suspend fun collectFingerprintSingle(
        context: Context,
        roomName: String
    ): WifiFingerprint {

        val snapshot = WifiScanner.oneShotSnapshot(context)

        val bssids = snapshot
            .filter { it.BSSID != null }
            .associate { it.BSSID!! to it.level }

        return WifiFingerprint(
            roomName = roomName,
            bssids = bssids
        )
    }

    /**
     * Erstellt einen Fingerprint aus mehreren WLAN-Scans.
     *
     * Ablauf:
     * - mehrere WLAN-Scans durchführen
     * - RSSI-Werte mitteln
     * - stabilen Fingerprint erzeugen
     *
     * Parameter:
     * samples → Anzahl der Scans
     * delayBetweenMs → Zeit zwischen den Scans
     */
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    suspend fun collectFingerprintMulti(
        context: Context,
        roomName: String,
        samples: Int = 10,
        delayBetweenMs: Long = 1_000L
    ): WifiFingerprint {

        val scans = WifiScanner.multiScan(
            context = context,
            nTimes = samples,
            delayBetweenMs = delayBetweenMs
        )

        val bssids = computeFingerprintFromScans(scans)

        return WifiFingerprint(
            roomName = roomName,
            bssids = bssids
        )
    }
}