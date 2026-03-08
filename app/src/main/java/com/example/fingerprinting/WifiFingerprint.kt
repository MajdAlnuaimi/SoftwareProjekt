package com.example.fingerprinting

import android.os.Build

/**
 * Datenmodell für einen WLAN-Fingerprint.
 *
 * Ein WLAN-Fingerprint beschreibt die WLAN-Umgebung eines Raumes
 * zu einem bestimmten Zeitpunkt.
 *
 * Diese Daten werden verwendet, um später anhand eines neuen WLAN-Scans
 * zu erkennen, in welchem Raum sich der Benutzer befindet.
 */
data class WifiFingerprint(

    /**
     * Name des Raumes, zu dem dieser Fingerprint gehört.
     */
    val roomName: String,

    /**
     * Gerät, mit dem der Fingerprint aufgenommen wurde.
     * Standardwert ist das aktuelle Android-Gerätemodell.
     */
    val device: String = Build.MODEL ?: "unknown",

    /**
     * Zeitpunkt der Aufnahme (Unix-Zeit in Millisekunden).
     */
    val timestampMs: Long = System.currentTimeMillis(),

    /**
     * Liste der gefundenen WLAN Access Points.
     *
     * Map:
     * BSSID → Signalstärke (RSSI)
     *
     * Beispiel:
     * {
     *   "AA:BB:CC:DD:EE:FF" -> -55,
     *   "11:22:33:44:55:66" -> -70
     * }
     */
    val bssids: Map<String, Int>
)